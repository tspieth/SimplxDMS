package service;

import database.dao.DisziplinDao;
import database.dao.FinaReferenceTimeDAO;
import database.dao.LaufDao;
import database.dao.LeistungDao;
import database.dao.MannschaftDao;
import database.dao.SchwimmerDao;
import database.dao.StartDao;
import database.model.Disziplin;
import database.model.Geschlecht;
import database.model.Mannschaft;
import database.model.Schwimmer;
import database.model.SchwimmLage;
import database.model.Start;
import database.model.Verein;
import database.model.Wettkampf;
import linearProgramming.LinearProgramming;
import objects.SwimmingEvent;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads eligible swimmers, runs the optimizer and persists the generated team and starts.
 */
public class TeamOptimizationService {
    private final Connection connection;

    public TeamOptimizationService(Connection connection) {
        this.connection = connection;
    }

    public String generateAndSaveTeamForClub(Verein club, Wettkampf competition, boolean isMale, int pauseGap)
            throws SQLException {
        if (club == null || competition == null) {
            throw new IllegalArgumentException("Bitte einen Verein und einen Wettkampf auswählen.");
        }
        if (pauseGap < 0) {
            throw new IllegalArgumentException("Die Pausenlänge darf nicht negativ sein.");
        }
        Geschlecht gender = isMale ? Geschlecht.MAENNLICH : Geschlecht.WEIBLICH;
        SchwimmerDao swimmerDao = new SchwimmerDao(connection);
        List<Schwimmer> candidates = swimmerDao.findUnassignedByVereinUndGeschlecht(
                club.getId(), isMale ? "m" : "w");
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Keine freien Schwimmer in der Datenbank gefunden.");
        }

        OptimizationInput input = createOptimizationInput(candidates, isMale, pauseGap);
        LinearProgramming.OptimizationResult result = LinearProgramming.toStringsimplexXBnB(
                input.points(), input.swimmerIds(), input.swimmerNames(), input.eventIndexes(), pauseGap);
        int[] runIds = resolveRuns(competition.getId(), gender, result);

        boolean oldAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            List<Integer> assignedSinceRead = swimmerDao.findAssignedIds(
                    candidates.stream().map(Schwimmer::getId).toList());
            if (!assignedSinceRead.isEmpty()) {
                throw new SQLException("Mindestens ein Schwimmer wurde während der Berechnung bereits zugeordnet. "
                        + "Bitte die Mannschaft erneut berechnen.");
            }

            String teamName = nextTeamName(new MannschaftDao(connection)
                    .findByWettkampfUndVerein(competition.getId(), club.getId()), isMale);
            Mannschaft team = new Mannschaft(0, club.getId(), competition.getId(), teamName);
            new MannschaftDao(connection).insert(team);

            StartDao startDao = new StartDao(connection);
            int[] swimmerIds = result.swimmerIds();
            for (int i = 0; i < swimmerIds.length; i++) {
                if (swimmerIds[i] >= 0) {
                    startDao.insert(new Start(0, runIds[i], team.getId(), swimmerIds[i], null, null));
                }
            }
            connection.commit();
            return "Mannschaft \"" + teamName + "\" gespeichert.\n\n" + result.log();
        } catch (SQLException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(oldAutoCommit);
        }
    }

    private OptimizationInput createOptimizationInput(List<Schwimmer> candidates, boolean isMale, int pauseGap)
            throws SQLException {
        DisziplinDao disziplinDao = new DisziplinDao(connection);
        Map<DisciplineKey, Integer> disciplineIds = new HashMap<>();
        for (Disziplin discipline : disziplinDao.findAll()) {
            disciplineIds.put(new DisciplineKey(discipline.getLage(), discipline.getDistanz()), discipline.getId());
        }
        Map<Integer, Integer> eventDisciplineIds = new HashMap<>();
        for (SwimmingEvent event : SwimmingEvent.values()) {
            DisciplineKey key = disciplineFor(event);
            Integer disciplineId = disciplineIds.get(key);
            if (disciplineId == null) {
                throw new SQLException("Disziplin fehlt in der Datenbank: " + event.getDisplayName());
            }
            eventDisciplineIds.put(event.getIndex(), disciplineId);
        }

        int[] eventIndexes = eventOrder(isMale);
        double[][] points = new double[candidates.size()][34];
        int[] swimmerIds = new int[candidates.size()];
        String[] swimmerNames = new String[candidates.size()];
        Map<Integer, Integer> rowBySwimmerId = new HashMap<>();
        for (int swimmerIndex = 0; swimmerIndex < candidates.size(); swimmerIndex++) {
            Schwimmer swimmer = candidates.get(swimmerIndex);
            swimmerIds[swimmerIndex] = swimmer.getId();
            swimmerNames[swimmerIndex] = swimmer.toString();
            rowBySwimmerId.put(swimmer.getId(), swimmerIndex);
        }

        List<Integer> disciplineIdList = List.copyOf(eventDisciplineIds.values());
        Map<Integer, Map<Integer, Double>> bestTimes = new LeistungDao(connection)
                .findBestTimesSeconds(candidates.stream().map(Schwimmer::getId).toList(), disciplineIdList);
        Map<Integer, Map<Geschlecht, Double>> referenceTimes = new FinaReferenceTimeDAO(connection).findAll();
        int topCount = (int) Math.min(candidates.size(), 8L + pauseGap);

        for (SwimmingEvent event : SwimmingEvent.values()) {
            int disciplineId = eventDisciplineIds.get(event.getIndex());
            Double referenceTime = referenceTimes.getOrDefault(disciplineId, Map.of()).get(
                    isMale ? Geschlecht.MAENNLICH : Geschlecht.WEIBLICH);
            if (referenceTime == null || referenceTime <= 0) {
                throw new SQLException("FINA-Referenzzeit fehlt für " + event.getDisplayName()
                        + (isMale ? " (männlich)." : " (weiblich)."));
            }

            List<EventCandidate> eventCandidates = new java.util.ArrayList<>();
            for (Map.Entry<Integer, Map<Integer, Double>> swimmerTimes : bestTimes.entrySet()) {
                Double time = swimmerTimes.getValue().get(disciplineId);
                if (time == null || time <= 0) {
                    continue;
                }
                Integer row = rowBySwimmerId.get(swimmerTimes.getKey());
                if (row != null) {
                    long score = (int) (Math.pow(referenceTime / time, 3) * 1000);
                    eventCandidates.add(new EventCandidate(row, time, score));
                }
            }
            eventCandidates.sort(java.util.Comparator.comparingDouble(EventCandidate::seconds)
                    .thenComparingInt(EventCandidate::swimmerIndex));

            int firstPosition = positionForEvent(event, isMale);
            int lastPosition = event.getDisplayName().equals("1500K") || event.getDisplayName().equals("800K")
                    ? firstPosition : firstPosition + 17;
            for (int i = 0; i < Math.min(topCount, eventCandidates.size()); i++) {
                EventCandidate candidate = eventCandidates.get(i);
                points[candidate.swimmerIndex()][firstPosition] = candidate.points();
                if (lastPosition != firstPosition) {
                    points[candidate.swimmerIndex()][lastPosition] = candidate.points();
                }
            }
        }
        return new OptimizationInput(points, swimmerIds, swimmerNames, eventIndexes);
    }

    private int[] resolveRuns(int competitionId, Geschlecht gender,
                              LinearProgramming.OptimizationResult result) throws SQLException {
        List<LaufDao.LaufMitDisziplin> runs = new LaufDao(connection).findByWettkampf(competitionId);
        Map<RunKey, Integer> runIdByKey = new HashMap<>();
        for (LaufDao.LaufMitDisziplin run : runs) {
            if (run.geschlecht() != gender) {
                continue;
            }
            RunKey key = new RunKey(run.abschnitt(),
                    new DisciplineKey(run.lage(), run.distanz()));
            if (runIdByKey.put(key, run.id()) != null) {
                throw new SQLException("Mehrere passende Läufe für Abschnitt " + run.abschnitt()
                        + " und " + run.lage() + " " + run.distanz() + "m gefunden.");
            }
        }
        int[] runIds = new int[result.eventIndexes().length];
        java.util.Arrays.fill(runIds, -1);
        Set<Integer> uniqueRuns = new HashSet<>();
        int[] swimmerIds = result.swimmerIds();
        int[] eventIndexes = result.eventIndexes();
        for (int i = 0; i < eventIndexes.length; i++) {
            if (swimmerIds[i] < 0) {
                continue;
            }
            int section = i < 17 ? 1 : 2;
            SwimmingEvent event = SwimmingEvent.values()[eventIndexes[i]];
            Integer runId = runIdByKey.get(new RunKey(section, disciplineFor(event)));
            if (runId == null) {
                throw new SQLException("Kein Lauf für Abschnitt " + section + " und "
                        + event.getDisplayName() + " im ausgewählten Wettkampf gefunden.");
            }
            if (!uniqueRuns.add(runId)) {
                throw new SQLException("Ein Lauf wurde mehreren Optimierungspositionen zugeordnet: "
                        + event.getDisplayName());
            }
            runIds[i] = runId;
        }
        return runIds;
    }

    private int[] eventOrder(boolean isMale) {
        String[] firstSection = {
                "100Lg", "200K", "100B", "200R", "100S", "50B", "200Lg",
                isMale ? "1500K" : "800K", "50S", "200B", "100R", "200S",
                "50K", "400Lg", "50R", "400K", "100K"
        };
        int[] result = new int[34];
        for (int position = 0; position < 34; position++) {
            String eventName;
            if (position == 24) {
                eventName = isMale ? "800K" : "1500K";
            } else {
                eventName = firstSection[position % 17];
            }
            result[position] = SwimmingEvent.getByDisplayName(eventName).getIndex();
        }
        return result;
    }

    private int positionForEvent(SwimmingEvent event, boolean isMale) {
        String eventName = event.getDisplayName();
        if (eventName.equals(isMale ? "1500K" : "800K")) {
            return 7;
        }
        if (eventName.equals(isMale ? "800K" : "1500K")) {
            return 24;
        }
        int[] eventOrder = eventOrder(isMale);
        for (int position = 0; position < 17; position++) {
            if (eventOrder[position] == event.getIndex()) {
                return position;
            }
        }
        throw new IllegalArgumentException("Event fehlt in der Optimierungsreihenfolge: " + eventName);
    }

    private String nextTeamName(List<Mannschaft> teams, boolean isMale) {
        String prefix = isMale ? "Optimierung männlich " : "Optimierung weiblich ";
        Set<String> names = new HashSet<>();
        for (Mannschaft team : teams) {
            names.add(team.getBezeichnung());
        }
        int suffix = 1;
        while (names.contains(prefix + suffix)) {
            suffix++;
        }
        return prefix + suffix;
    }

    private DisciplineKey disciplineFor(SwimmingEvent event) {
        String displayName = event.getDisplayName();
        int firstNonDigit = 0;
        while (firstNonDigit < displayName.length()
                && Character.isDigit(displayName.charAt(firstNonDigit))) {
            firstNonDigit++;
        }
        int distance = Integer.parseInt(displayName.substring(0, firstNonDigit));
        String stroke = displayName.substring(firstNonDigit);
        SchwimmLage lage = switch (stroke) {
            case "K" -> SchwimmLage.FREISTIL;
            case "B" -> SchwimmLage.BRUST;
            case "R" -> SchwimmLage.RUECKEN;
            case "S" -> SchwimmLage.SCHMETTERLING;
            case "Lg" -> SchwimmLage.LAGEN;
            default -> throw new IllegalArgumentException("Unbekannte Disziplin: " + displayName);
        };
        return new DisciplineKey(lage, distance);
    }

    private record DisciplineKey(SchwimmLage lage, int distance) {
    }

    private record RunKey(int section, DisciplineKey discipline) {
    }

    private record EventCandidate(int swimmerIndex, double seconds, long points) {
    }

    private record OptimizationInput(double[][] points, int[] swimmerIds, String[] swimmerNames,
                                     int[] eventIndexes) {
    }
}
