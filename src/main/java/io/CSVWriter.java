package io;

import localsearch.TeamState;
import objects.Swimmer;
import objects.SwimmingEvent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CSVWriter {

    private final File file;
    private final String delimiter;

    public CSVWriter(String filePath) {
        this(filePath, ";");
    }

    public CSVWriter(String filePath, String delimiter) {
        this.file = new File(filePath);
        this.delimiter = delimiter;
    }

    // Kopfzeile schreiben (überschreibt Datei)
    public void writeHeader(List<String> header) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8))) {

            writer.write(formatRow(header));
            writer.newLine();
        }
    }

    // Eine Zeile anhängen
    public void appendRow(List<String> row) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {

            writer.write(formatRow(row));
            writer.newLine();
        }
    }

    // Mehrere Zeilen anhängen
    public void appendRows(List<List<String>> rows) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {

            for (List<String> row : rows) {
                writer.write(formatRow(row));
                writer.newLine();
            }
        }
    }

    // CSV-konforme Formatierung
    private String formatRow(List<String> row) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < row.size(); i++) {
            sb.append(escape(row.get(i)));

            if (i < row.size() - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    // Sonderzeichen korrekt escapen
    private String escape(String value) {
        if (value == null)
            return "";

        boolean containsSpecial = value.contains(delimiter) ||
                value.contains("\"") ||
                value.contains("\n") ||
                value.contains("\r");

        if (containsSpecial) {
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }

        return value;
    }

    /**
     * Writes a file containing the Lineup from a
     * state
     * 
     * @param state
     * @throws IOException
     */
    public void writeLineUp(TeamState state) throws IOException {
        writeHeader(Arrays.asList("Schwimmart", "Name", "Zeit", "Punkte", "Name", "Zeit", "Punkte"));
        Map<Integer, Swimmer> lineup = state.getLineup();

        int[][] orderCopy = state.getOrder();
        int lengthSec = 0;
        for (int i = 0; i < orderCopy.length; i++) {
            lengthSec++;
            if (orderCopy[i][0] == -1) {
                break;
            }
        }

        int totalPoints1 = 0;
        int totalPoints2 = 0;
        for (int i = 0; i < lengthSec; i++) {
            if (orderCopy[i][0] < 0) {
                continue;
            }
            SwimmingEvent event = SwimmingEvent.values()[orderCopy[i][0]];
            String eventName = event.getDisplayName();

            Swimmer firstSec = lineup.get(i);
            Swimmer secondSec = lineup.get(i + lengthSec);
            if (firstSec == null) {
                if (secondSec == null) {
                    appendRow(Arrays.asList(event.getDisplayName(), "",
                            "", "", "", "", ""));
                } else {
                    String name2 = secondSec.getName();
                    double time2 = secondSec.getTimeforEvent(SwimmingEvent.FREESTYLE_800);
                    int point2 = secondSec.getPointsForEvent(SwimmingEvent.FREESTYLE_800);

                    appendRow(Arrays.asList(event.getDisplayName(), "", "", "", name2,
                            Double.toString(time2), Integer.toString(point2)));
                }
                continue;
            }
            if (secondSec == null) {
                String name1 = firstSec.getName();

                double time1 = firstSec.getTimeforEvent(event);

                int point1 = firstSec.getPointsForEvent(event);

                appendRow(Arrays.asList(event
                        .getDisplayName(), name1,
                        Double.toString(time1), Integer.toString(point1), "", "", ""));
                continue;
            }

            String name1 = firstSec.getName();
            String name2 = secondSec.getName();

            if (event.equals(SwimmingEvent.FREESTYLE_1500)) {
                double time1 = firstSec.getTimeforEvent(event);
                double time2 = secondSec.getTimeforEvent(SwimmingEvent.FREESTYLE_800);

                int point1 = firstSec.getPointsForEvent(event);
                int point2 = secondSec.getPointsForEvent(SwimmingEvent.FREESTYLE_800);

                totalPoints1 += point1;
                totalPoints2 += point2;

                appendRow(Arrays.asList("1500K", name1,
                        Double.toString(time1), Integer.toString(point1), "", "", ""));
                appendRow(Arrays.asList("800K", "", "", "", name2,
                        Double.toString(time2), Integer.toString(point2)));

            } else if (event.equals(SwimmingEvent.FREESTYLE_800)) {
                double time1 = firstSec.getTimeforEvent(event);
                double time2 = secondSec.getTimeforEvent(SwimmingEvent.FREESTYLE_1500);

                int point1 = firstSec.getPointsForEvent(event);
                int point2 = secondSec.getPointsForEvent(SwimmingEvent.FREESTYLE_1500);

                totalPoints1 += point1;
                totalPoints2 += point2;

                appendRow(Arrays.asList("800K", name1,
                        Double.toString(time1), Integer.toString(point1), "", "", ""));
                appendRow(Arrays.asList("1500K", "", "", "", name2,
                        Double.toString(time2), Integer.toString(point2)));
            } else {
                double time1 = firstSec.getTimeforEvent(event);
                double time2 = secondSec.getTimeforEvent(event);

                int point1 = firstSec.getPointsForEvent(event);
                int point2 = secondSec.getPointsForEvent(event);

                totalPoints1 += point1;
                totalPoints2 += point2;

                appendRow(Arrays.asList(eventName, name1, Double.toString(
                        time1), Integer.toString(point1), name2,
                        Double.toString(time2), Integer.toString(point2)));

            }

        }
        appendRow(Arrays.asList(
                "", "", "Punkte Abs1",
                Integer.toString(totalPoints1), "", "Punkte Abs2",
                Integer.toString(totalPoints2)));
        appendRow(Arrays.asList("Punkte Gesamt", "", "", "", "", "", Integer.toString(totalPoints1 + totalPoints2)));
    }
}
