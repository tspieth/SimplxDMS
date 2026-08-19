package io;

import objects.Swimmer;
import objects.SwimmingEvent;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVReader {

    private String filePath;
    private String delimiter;

    // Constructor with default delimiter (semicolon)
    public CSVReader(String filePath) {
        this(filePath, ";"); // Common delimiter is ;
    }

    public CSVReader(String filePath, String delimiter) {
        this.filePath = filePath;
        this.delimiter = delimiter;
    }

    // reads the entire CSV file and returns a list of string arrays,
    // where each array represents a line in the CSV
    public List<String[]> readAll() throws IOException {
        List<String[]> records = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(delimiter);
                records.add(values);
            }
        }

        return records;
    }

    // reads the CSV file line by line and processes each line using a provided
    // LineProcessor callback

    public void readLineByLine(LineProcessor processor) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(delimiter);
                processor.process(values);
            }
        }
    }

    // IMPORTANT: the base times in the CSV file must be in the same order as the
    // events in the SwimmingEvent enum
    // ALSO: the CSV file needs to contain every event
    public static double[] getBaseTimes(boolean isMale, String filePath) {
        CSVReader baseTimesReader = new CSVReader(filePath);

        double[] baseTimes = new double[SwimmingEvent.values().length];

        try {
            List<String[]> lines;

            lines = baseTimesReader.readAll();

            for (SwimmingEvent event : SwimmingEvent.values()) {
                int i = event.getIndex();
                baseTimes[i] = Double.parseDouble(
                        lines.get(i + 1)[isMale ? 3 : 5]);
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null; // return null if there was an error reading the file
        }

        return baseTimes;
    }

    /**
     * Reads all swimmers from a CSV file and creates a list of Swimmer objects.
     * The CSV file should have the following format:
     * Name, Vorname, Geschlecht (m/f), Ausdauer, Event1, Zeit1
     * Event2, Zeit2, ... are in row[4], row[5] until the next swimmer starts.
     * The method assumes that the first line of the CSV file is a header and skips
     * it.
     * 
     * @param filePath
     * @return List of Swimmer objects created from the CSV file
     * @author Timon Spieth
     * @since 2026-02-11
     */
    public static List<Swimmer> createSwimmer(String filePath) {
        CSVReader reader = new CSVReader(filePath);

        List<String[]> csvData;
        List<Swimmer> schwimmerListe = new ArrayList<>();
        try {
            csvData = reader.readAll();
            Swimmer aktuellerSchwimmer = null;

            // Header überspringen (erste Zeile)
            for (int i = 1; i < csvData.size(); i++) {

                String[] row = csvData.get(i);

                // Neue Swimmer-Zeile (Name nicht leer)
                if (row[0] != null && !row[0].isEmpty()) {
                    int ausdauer = 50;
                    if (row[3] != "") {
                        ausdauer = Integer.parseInt(row[3]);
                    }
                    aktuellerSchwimmer = new Swimmer(
                            row[0] + " " + row[1], // Name
                            (row[2].equals("m")), // Geschlecht
                            ausdauer // Ausdauer
                    );

                    schwimmerListe.add(aktuellerSchwimmer);
                }

                // Strecke + Zeit hinzufügen

                if (row.length < 6 && aktuellerSchwimmer != null && row[4] != null && !row[4].isEmpty()) {
                    aktuellerSchwimmer.setTimeForEvent(row[4], "-1");
                    continue;
                }
                if (aktuellerSchwimmer != null && row[4] != null && !row[4].isEmpty()) {
                    aktuellerSchwimmer.setTimeForEvent(row[4], row[5]);
                    // aktuellerSchwimmer.setPointsForEvent(row[4], row[5]);
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } // read all lines from the CSV file

        return schwimmerListe;
    }

    /**
     * Method is refined
     * The file should have the following format:
     * Number, EventName, (m/f) -> for Events
     * or
     * 00, BREAK, Duration in minutes -> for breaks
     * 
     * @param filePath
     * @return order of the Competition Events
     * @author Timon Spieth
     * @since 2026-02-11
     */
    public static int[][] getEventOrder(String filePath) {
        CSVReader reader = new CSVReader(filePath);

        List<String[]> csvData;
        int[][] orderCompetition = new int[0][2];
        try {
            csvData = reader.readAll();
            orderCompetition = new int[csvData.size() - 1][2];
            int j = 0; // index for orderCompetition array
            for (int i = 1; i < csvData.size(); i++) {
                String[] row = csvData.get(i);
                if (row[1].equals("BREAK")) {
                    orderCompetition[j][0] = -1;
                    orderCompetition[j][1] = Integer.parseInt(row[2]);
                    j++;
                    continue;
                }
                SwimmingEvent event = SwimmingEvent.getByDisplayName(row[1]);
                int eventIndex = event.getIndex();
                int duration = event.getTypicalDuration();
                orderCompetition[j][0] = eventIndex;
                orderCompetition[j][1] = duration;
                j++;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } // read all lines from the CSV file

        return orderCompetition;
    }

    /**
     * 
     * @param filePath
     * @param isMale
     * @return
     */
    public static int[][] getEventOrder(String filePath, boolean isMale) {
        CSVReader reader = new CSVReader(filePath);

        List<String[]> csvData;
        int[][] orderCompetition = new int[0][2];
        try {
            csvData = reader.readAll();
            orderCompetition = new int[csvData.size() - 1][2];
            int j = 0;
            for (int i = 1; i < csvData.size(); i++) {
                String[] row = csvData.get(i);
                if (row[1].equals("BREAK")) {
                    orderCompetition[j][0] = -1;
                    orderCompetition[j][1] = Integer.parseInt(row[2]);
                    j++;
                    continue;
                }

                SwimmingEvent event = SwimmingEvent.getByDisplayName(row[1]);
                int eventIndex = event.getIndex();
                int duration = event.getTypicalDuration();

                if (row[2].equals(isMale ? "f" : "m")) {
                    orderCompetition[j][0] = -2; // marks events from other gender
                    orderCompetition[j][1] = duration;
                    j++;
                } else {
                    orderCompetition[j][0] = eventIndex;
                    orderCompetition[j][1] = duration;
                    j++;
                }

            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } // read all lines from the CSV file

        return orderCompetition;
    }

    // Callback interface for processing lines of the CSV file
    public interface LineProcessor {
        void process(String[] values);
    }
}
