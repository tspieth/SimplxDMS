package service;

import database.dao.DisziplinDao;
import database.dao.FinaReferenceTimeDAO;
import database.dao.LaufDao;
import database.dao.LeistungDao;
import database.dao.MannschaftDao;
import database.dao.ProgrammpunktDao;
import database.dao.SchwimmerDao;
import database.dao.StartDao;
import database.dao.VereinDao;
import database.dao.WettkampfDao;
import database.model.Disziplin;
import database.model.Geschlecht;
import database.model.Lauf;
import database.model.Leistung;
import database.model.Mannschaft;
import database.model.Programmpunkt;
import database.model.ProgrammpunktTyp;
import database.model.Schwimmer;
import database.model.SchwimmLage;
import database.model.Start;
import database.model.Verein;
import database.model.Wettkampf;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvExportServiceTest {
    @TempDir
    Path temporaryDirectory;

    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:csv" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        initializeSchema(connection);
        new DisziplinDao(connection).initDefaultDisziplinen();
    }

    private void initializeSchema(Connection target) throws Exception {
        try (InputStreamReader schema = new InputStreamReader(
                getClass().getResourceAsStream("/db/schema.sql"), StandardCharsets.UTF_8)) {
            RunScript.execute(target, schema);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    @Test
    void teamTimeUsesRoundedHundredths() {
        assertEquals("1:38.61", MannschaftExportService.formatHundredths(LocalTime.of(0, 1, 38, 614_000_000)));
        assertEquals("0:19.90", MannschaftExportService.formatHundredths(LocalTime.of(0, 0, 19, 900_000_000)));
    }

    @Test
    void exportFileNamesReplaceSpacesWithUnderscores() {
        assertEquals("SV_Test_Team_Maennlich_DMS_2026.csv",
                gui.CsvExportDialogSupport.sanitizeFileName("SV Test Team Maennlich DMS 2026.csv"));
    }

    @Test
    void swimmerExportCanBeReadBackWithQuotedNamesAndBestTimes() throws Exception {
        Verein club = new Verein(0, "Wasserfreunde Ä", "#123456");
        new VereinDao(connection).insert(club);
        Schwimmer first = new Schwimmer(0, "Müller;Test", "Anna \"Änne\"", LocalDate.of(2001, 5, 23), "w",
                club.getId());
        Schwimmer second = new Schwimmer(0, "Öztürk", "Jörg", LocalDate.of(2000, 1, 1), "m", club.getId());
        SchwimmerDao swimmerDao = new SchwimmerDao(connection);
        swimmerDao.insert(first);
        swimmerDao.insert(second);

        Disziplin freestyle = new DisziplinDao(connection).findByLageUndDistanz(SchwimmLage.FREISTIL, 100);
        LeistungDao performanceDao = new LeistungDao(connection);
        performanceDao.insert(new Leistung(0, first.getId(), freestyle.getId(), LocalTime.of(0, 1, 2, 300_000_000), null));
        performanceDao.insert(new Leistung(0, first.getId(), freestyle.getId(), LocalTime.of(0, 1, 3), null));

        List<List<String>> rows = new SchwimmerCsvExportService(connection).ladeZeilen(club);
        Path csv = temporaryDirectory.resolve("verein.csv");
        CsvWriter.writeAtomically(csv, rows);
        List<SchwimmerCsvImportService.ImportSchwimmer> imported =
                new SchwimmerCsvImportService(connection).parse(csv);

        assertEquals(2, imported.size());
        assertEquals("Müller;Test", imported.get(0).name());
        assertEquals("Anna \"Änne\"", imported.get(0).vorname());
        assertEquals("w", imported.get(0).geschlecht());
        assertEquals(LocalDate.of(2001, 5, 23), imported.get(0).geburtsdatum());
        assertEquals(List.of(new SchwimmerCsvImportService.ImportLeistung(
                SchwimmLage.FREISTIL, 100, LocalTime.of(0, 1, 2, 300_000_000))), imported.get(0).leistungen());
        assertEquals("Öztürk", imported.get(1).name());
        assertEquals("m", imported.get(1).geschlecht());
        assertEquals(LocalDate.of(2000, 1, 1), imported.get(1).geburtsdatum());
        assertTrue(imported.get(1).leistungen().isEmpty());
        assertFalse(Files.readString(csv).startsWith("\uFEFF"));

        try (Connection emptyDatabase = DriverManager.getConnection(
                "jdbc:h2:mem:csv-import" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")) {
            initializeSchema(emptyDatabase);
            new DisziplinDao(emptyDatabase).initDefaultDisziplinen();
            Verein importedClub = new Verein(0, "Importverein", "#654321");
            new VereinDao(emptyDatabase).insert(importedClub);
            SchwimmerCsvImportService.ImportErgebnis result = new SchwimmerCsvImportService(emptyDatabase)
                    .importSwimmers(importedClub, imported, Set.of());
            assertEquals(2, result.neueSchwimmer());

            List<Schwimmer> copiedSwimmers = new SchwimmerDao(emptyDatabase).findByVerein(importedClub.getId());
            assertEquals(2, copiedSwimmers.size());
            assertEquals(first.getName(), copiedSwimmers.get(0).getName());
            assertEquals(first.getVorname(), copiedSwimmers.get(0).getVorname());
            assertEquals(first.getGeschlecht(), copiedSwimmers.get(0).getGeschlecht());
            assertEquals(first.getGeburtsdatum(), copiedSwimmers.get(0).getGeburtsdatum());
            Disziplin copiedDiscipline = new DisziplinDao(emptyDatabase)
                    .findByLageUndDistanz(SchwimmLage.FREISTIL, 100);
            assertEquals(LocalTime.of(0, 1, 2, 300_000_000), new LeistungDao(emptyDatabase)
                    .findBySchwimmerUndDisziplin(copiedSwimmers.get(0).getId(), copiedDiscipline.getId())
                    .get(0).getZeit());
            assertEquals(second.getName(), copiedSwimmers.get(1).getName());
            assertEquals(second.getVorname(), copiedSwimmers.get(1).getVorname());
            assertEquals(second.getGeschlecht(), copiedSwimmers.get(1).getGeschlecht());
            assertEquals(second.getGeburtsdatum(), copiedSwimmers.get(1).getGeburtsdatum());
        }
    }

    @Test
    void teamExportPairsDifferentSectionDisciplinesAndRepresentsMissingData() throws Exception {
        Verein club = new Verein(0, "SV Test", "#123456");
        new VereinDao(connection).insert(club);
        Wettkampf competition = new Wettkampf(0, "DMS Test", LocalDate.of(2026, 1, 1));
        new WettkampfDao(connection).insert(competition);
        Mannschaft team = new Mannschaft(0, club.getId(), competition.getId(), "Team A");
        new MannschaftDao(connection).insert(team);

        Schwimmer first = new Schwimmer(0, "Schwimmer", "Anna", LocalDate.of(2000, 1, 1), "m", club.getId());
        Schwimmer second = new Schwimmer(0, "Beispiel", "Eva", LocalDate.of(2000, 1, 1), "m", club.getId());
        Schwimmer withoutPerformance = new Schwimmer(0, "Ohnezeit", "Max", LocalDate.of(2000, 1, 1), "m",
                club.getId());
        SchwimmerDao swimmerDao = new SchwimmerDao(connection);
        swimmerDao.insert(first);
        swimmerDao.insert(second);
        swimmerDao.insert(withoutPerformance);

        DisziplinDao disciplineDao = new DisziplinDao(connection);
        Disziplin free800 = disciplineDao.findByLageUndDistanz(SchwimmLage.FREISTIL, 800);
        Disziplin free1500 = disciplineDao.findByLageUndDistanz(SchwimmLage.FREISTIL, 1500);
        Disziplin breast100 = disciplineDao.findByLageUndDistanz(SchwimmLage.BRUST, 100);
        Disziplin breast200 = disciplineDao.findByLageUndDistanz(SchwimmLage.BRUST, 200);
        new FinaReferenceTimeDAO(connection).upsert(free800.getId(), Geschlecht.MAENNLICH, 600);
        new FinaReferenceTimeDAO(connection).upsert(free1500.getId(), Geschlecht.MAENNLICH, 1200);
        new LeistungDao(connection).insert(new Leistung(0, first.getId(), free800.getId(), LocalTime.of(0, 10), null));
        new LeistungDao(connection).insert(new Leistung(0, second.getId(), free1500.getId(), LocalTime.of(0, 20), null));

        int firstRun = addRun(competition, free800, 1, 1);
        int unassignedRun = addRun(competition, breast100, 2, 1);
        int secondRun = addRun(competition, free1500, 3, 2);
        int noTimeRun = addRun(competition, breast200, 4, 2);
        StartDao startDao = new StartDao(connection);
        startDao.insert(new Start(0, firstRun, team.getId(), first.getId(), null, null));
        startDao.insert(new Start(0, secondRun, team.getId(), second.getId(), null, null));
        startDao.insert(new Start(0, noTimeRun, team.getId(), withoutPerformance.getId(), null, null));

        List<List<String>> rows = new MannschaftExportService(connection)
                .ladeZeilen(team, competition, Geschlecht.MAENNLICH);
        Path csv = temporaryDirectory.resolve("mannschaft.csv");
        CsvWriter.writeAtomically(csv, rows);

        assertEquals("Schwimmart;Name;Zeit;Punkte;Schwimmart;Name;Zeit;Punkte\n"
                        + "800 m Freistil;Anna Schwimmer;10:00.00;1000;1500 m Freistil;Eva Beispiel;20:00.00;1000\n"
                        + "100 m Brust;;;;200 m Brust;Max Ohnezeit;;0\n"
                        + "Summe;;;1000;;;;1000\n"
                        + "Gesamt;;;;;;;2000\n",
                Files.readString(csv, StandardCharsets.UTF_8));
        assertEquals(8, rows.get(2).size());
        assertEquals(unassignedRun, new LaufDao(connection).findByWettkampf(competition.getId()).stream()
                .filter(run -> run.abschnitt() == 1 && run.wettkampfNummer() == 2).findFirst().orElseThrow().id());
    }

    @Test
    void teamWithoutAssignmentsProducesNoExportRows() throws Exception {
        Verein club = new Verein(0, "SV Test", "#123456");
        new VereinDao(connection).insert(club);
        Wettkampf competition = new Wettkampf(0, "DMS Test", LocalDate.of(2026, 1, 1));
        new WettkampfDao(connection).insert(competition);
        Mannschaft team = new Mannschaft(0, club.getId(), competition.getId(), "Team A");
        new MannschaftDao(connection).insert(team);

        assertTrue(new MannschaftExportService(connection).ladeZeilen(team, competition, Geschlecht.WEIBLICH).isEmpty());
    }

    @Test
    void restoreBundledReferenceTimesReplacesCustomAndStaleEntries() throws Exception {
        DisziplinDao disciplineDao = new DisziplinDao(connection);
        Disziplin freestyle50 = disciplineDao.findByLageUndDistanz(SchwimmLage.FREISTIL, 50);
        Disziplin customDiscipline = new Disziplin(0, SchwimmLage.FREISTIL, 25);
        disciplineDao.insert(customDiscipline);

        FinaReferenceTimeDAO referenceTimeDao = new FinaReferenceTimeDAO(connection);
        referenceTimeDao.upsert(freestyle50.getId(), Geschlecht.MAENNLICH, 123.45);
        referenceTimeDao.upsert(customDiscipline.getId(), Geschlecht.MAENNLICH, 10.0);

        int restored = new StartupService(connection).restoreBundledReferenceTimes();
        var storedTimes = referenceTimeDao.findAll();

        assertEquals(36, restored);
        assertEquals(18, storedTimes.size());
        assertEquals(19.90, storedTimes.get(freestyle50.getId()).get(Geschlecht.MAENNLICH));
        assertEquals(22.83, storedTimes.get(freestyle50.getId()).get(Geschlecht.WEIBLICH));
        assertFalse(storedTimes.containsKey(customDiscipline.getId()));
    }

    private int addRun(Wettkampf competition, Disziplin discipline, int number, int section) throws Exception {
        Programmpunkt point = new Programmpunkt(0, competition.getId(), number, ProgrammpunktTyp.LAUF);
        new ProgrammpunktDao(connection).insert(point);
        Lauf run = new Lauf(0, point.getId(), discipline.getId(), Geschlecht.MAENNLICH, number, section);
        new LaufDao(connection).insert(run);
        return run.getId();
    }
}
