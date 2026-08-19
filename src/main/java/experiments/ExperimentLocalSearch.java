package experiments;

import io.CSVWriter;
import localsearch.AdaptiveSchedule;
import localsearch.ExponentialSchedule;
import localsearch.LocalSearch;
import localsearch.TeamState;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ExperimentLocalSearch {

    public static Random rng = new Random(42); // fester Seed
    public static int countData = 1000;

    // Just for Fun
    private static boolean progressBarInitialized = false;

    /**
     * Startet Standard-Hill-Climbing k Mal und erstellt ein .csv Datei im
     * Verzeichnis:
     * OptimalDMSAlgo/data
     * 
     * @param k
     */
    public static void standardHillClimbing(TeamState teamState) throws IOException {

        // Just for Fun
        // Print initial progress bar (0%)
        progressBarInitialized = false;
        System.out.println("Experiment \"Standart Hill Climbing\":");
        printProgressBar(0, countData);

        CSVWriter standardWriter = new CSVWriter("OptimalDMSAlgo/data/standardHill.csv", ";");

        /*
         * writingHeader()
         * variant,run,score,time_ms,iterations,states
         */
        standardWriter.writeHeader(Arrays.asList("variant", "run", "score", "time_ms", "iterations", "states"));

        for (int i = 0; i <= 3; i++) {
            LocalSearch.hillClimbing(teamState); // JVM Warm-Up
        }

        for (int i = 1; i <= countData; i++) {

            teamState.newRandomLineUp(); // so runs dont give same outcome

            long start = System.nanoTime();
            TeamState best = LocalSearch.hillClimbing(teamState);
            System.out.println(best.toStringLineUp());
            long end = System.nanoTime();

            long durationNs = end - start;
            double durationMs = durationNs / 1_000_000.0;

            String run = Integer.toString(i);
            String score = Integer.toString(best.getTotalPoints());
            String time_ms = String.format(Locale.US, "%.3f", durationMs); // Locale.US so it uses . instead of ,
            String iterations = Double.toString(LocalSearch.iterations);
            String states = Long.toString(LocalSearch.statesCreated);

            /*
             * appendRow()
             * standard;1;838;7239;30;145000
             */
            standardWriter.appendRow(Arrays.asList("standard", run, score, time_ms, iterations, states));

            // Write bestState
            CSVWriter lineupWriter = new CSVWriter("OptimalDMSAlgo/data/teams/standardHillTeam" + i + ".csv");
            lineupWriter.writeLineUp(best);

            // Just for Fun
            // Update progress bar after finishing this run
            printProgressBar(i, countData);
        }
    }

    /**
     * Startet Standard-Hill-Climbing k Mal und erstellt ein .csv Datei im
     * Verzeichnis:
     * OptimalDMSAlgo/data
     * 
     * @param k
     */
    public static void hillClimbingOnTheFly(TeamState teamState) throws IOException {

        // Just for Fun
        // Print initial progress bar (0%)
        progressBarInitialized = false;
        System.out.println("Experiment \"Standart Hill Climbing\":");
        printProgressBar(0, countData);

        CSVWriter standardWriter = new CSVWriter("OptimalDMSAlgo/data/standardHillOnTheFly.csv", ";");

        /*
         * writingHeader()
         * variant,run,score,time_ms,iterations,states
         */
        standardWriter.writeHeader(Arrays.asList("variant", "run", "score", "time_ms", "iterations", "states"));

        for (int i = 0; i <= 3; i++) {
            // LocalSearch.hillClimbing(teamState); // JVM Warm-Up
        }

        for (int i = 1; i <= countData; i++) {

            teamState.newRandomLineUp(); // so runs dont give same outcome

            long start = System.nanoTime();
            TeamState best = LocalSearch.hillClimbingCompleatlyFly(teamState);
            // System.out.println(best.toStringLineUp());
            long end = System.nanoTime();

            long durationNs = end - start;
            double durationMs = durationNs / 1_000_000.0;

            String run = Integer.toString(i);
            String score = Integer.toString(best.getTotalPoints());
            String time_ms = String.format(Locale.US, "%.3f", durationMs); // Locale.US so it uses . instead of ,
            String iterations = Double.toString(LocalSearch.iterations);
            String states = Long.toString(LocalSearch.statesCreated);

            /*
             * appendRow()
             * standard;1;838;7239;30;145000
             */
            standardWriter.appendRow(Arrays.asList("standard", run, score, time_ms, iterations, states));

            // Write bestState
            CSVWriter lineupWriter = new CSVWriter("OptimalDMSAlgo/data/teams/standardHillTeamFly" + i + ".csv");
            lineupWriter.writeLineUp(best);

            // Just for Fun
            // Update progress bar after finishing this run
            printProgressBar(i, countData);
        }
    }

    /**
     * Startet Standard-Hill-Climbing k Mal und erstellt ein .csv Datei im
     * Verzeichnis:
     * OptimalDMSAlgo/data
     * 
     * @param k
     */
    public static void beamSearch(TeamState teamState, int k) throws IOException {

        // Just for Fun
        // Print initial progress bar (0%)
        progressBarInitialized = false;
        System.out.println("Experiment \"Local Beam Search\":");
        printProgressBar(0, countData);

        CSVWriter standardWriter = new CSVWriter("OptimalDMSAlgo/data/local" + k + "Beam.csv", ";");

        /*
         * writingHeader()
         * variant,run,score,time_ms,iterations,states
         */
        standardWriter.writeHeader(Arrays.asList("variant", "run", "score", "time_ms", "iterations", "states"));

        for (int i = 1; i <= countData; i++) {

            teamState.newRandomLineUp(); // so runs dont give same outcome

            long start = System.nanoTime();
            TeamState best = LocalSearch.beamSearch(teamState, k);
            long end = System.nanoTime();

            long durationNs = end - start;
            double durationMs = durationNs / 1_000_000.0;

            String run = Integer.toString(i);
            String score = Integer.toString(best.getTotalPoints());
            String time_ms = String.format(Locale.US, "%.3f", durationMs); // Locale.US so it uses . instead of ,
            String iterations = Double.toString(LocalSearch.iterations);
            String states = Long.toString(LocalSearch.statesCreated);

            /*
             * appendRow()
             * standard;1;838;7239;30;145000
             */
            standardWriter.appendRow(Arrays.asList("beamSearch", run, score, time_ms, iterations, states));

            // Just for Fun
            // Update progress bar after finishing this run
            printProgressBar(i, countData);
        }
    }

    /**
     * Startet Standard-Hill-Climbing k Mal und erstellt ein .csv Datei im
     * Verzeichnis:
     * OptimalDMSAlgo/data
     * 
     * @param k
     */
    public static void firstChoiceHillClimbing(TeamState teamState) throws IOException {

        // Just for Fun
        // Print initial progress bar (0%)
        progressBarInitialized = false;
        System.out.println("Experiment \"First Choice Hill Climbing\":");
        printProgressBar(0, countData);

        CSVWriter standardWriter = new CSVWriter("OptimalDMSAlgo/data/firstChoiceHill.csv", ";");

        /*
         * writingHeader()
         * variant,run,score,time_ms,iterations,states
         */
        standardWriter.writeHeader(Arrays.asList("variant", "run", "score", "time_ms", "iterations", "states"));

        // for (int i = 0; i <= 3; i++) {
        // LocalSearch.hillClimbing(teamState); // JVM Warm-Up
        // }

        for (int i = 1; i <= countData; i++) {

            teamState.newRandomLineUp(); // so runs dont give same outcome

            long start = System.nanoTime();
            TeamState best = LocalSearch.firstChoiceHillClimbing(teamState);
            long end = System.nanoTime();

            long durationNs = end - start;
            double durationMs = durationNs / 1_000_000.0;

            String run = Integer.toString(i);
            String score = Integer.toString(best.getTotalPoints());
            String time_ms = String.format(Locale.US, "%.3f", durationMs); // Locale.US so it uses . instead of ,
            String iterations = Double.toString(LocalSearch.iterations);
            String states = Long.toString(LocalSearch.statesCreated);

            /*
             * appendRow()
             * firstChoice;1;838;7239;30;145000
             */
            standardWriter.appendRow(Arrays.asList("firstChoice", run, score, time_ms, iterations, states));

            // Just for Fun
            // Update progress bar after finishing this run
            printProgressBar(i, countData);
        }
    }

    /**
     * Startet Standard-Hill-Climbing k Mal und erstellt ein .csv Datei im
     * Verzeichnis:
     * OptimalDMSAlgo/data
     * 
     * @param k
     */
    public static void firstChoiceHillClimbingWithKSwaps(TeamState teamState, int k) throws IOException {

        // Just for Fun
        // Print initial progress bar (0%)
        progressBarInitialized = false;
        System.out.println("Experiment \"First Choice Hill Climbing\":");
        printProgressBar(0, countData);

        CSVWriter standardWriter = new CSVWriter("OptimalDMSAlgo/data/firstChoiceHill" + k + "Swaps.csv", ";");

        /*
         * writingHeader()
         * variant,run,score,time_ms,iterations,states
         */
        standardWriter.writeHeader(Arrays.asList("variant", "run", "score", "time_ms", "iterations", "states"));

        // for (int i = 0; i <= 3; i++) {
        // LocalSearch.hillClimbing(teamState); // JVM Warm-Up
        // }

        for (int i = 1; i <= countData; i++) {

            teamState.newRandomLineUp(); // so runs dont give same outcome

            long start = System.nanoTime();
            TeamState best = LocalSearch.firstChoiceHillClimbingWithSwaps(teamState, k);
            System.out.println(best.toStringLineUp());
            long end = System.nanoTime();

            long durationNs = end - start;
            double durationMs = durationNs / 1_000_000.0;

            String run = Integer.toString(i);
            String score = Integer.toString(best.getTotalPoints());
            String time_ms = String.format(Locale.US, "%.3f", durationMs); // Locale.US so it uses . instead of ,
            String iterations = Double.toString(LocalSearch.iterations);
            String states = Long.toString(LocalSearch.statesCreated);

            /*
             * appendRow()
             * firstChoice;1;838;7239;30;145000
             */
            standardWriter.appendRow(Arrays.asList("firstChoiceSwaps", run, score, time_ms, iterations, states));

            CSVWriter lineupWriter = new CSVWriter("OptimalDMSAlgo/data/teams/firstChoiceSwapTeam" + i + ".csv");
            lineupWriter.writeLineUp(best);
            // Just for Fun
            // Update progress bar after finishing this run
            printProgressBar(i, countData);
        }
    }

    /**
     * Startet k-Restart-Hill-Climbing k Mal und erstellt ein .csv Datei im
     * Verzeichnis:
     * OptimalDMSAlgo/data
     * 
     * @param k
     */
    public static void kRestartsFirstChoiceHillClimbing(int k, TeamState teamState) throws IOException {

        // Just for Fun
        // Print initial progress bar (0%)
        progressBarInitialized = false;
        System.out.println("Experiment \"" + k + "-Restarts Hill Climbing with First Choice\":");
        printProgressBar(0, ExperimentLocalSearch.countData);

        CSVWriter standardWriter = new CSVWriter("OptimalDMSAlgo/data/firstChoice_" + k + "_RestartsHill_.csv", ";");

        /*
         * writingHeader()
         * variant,run,score,time_ms,avg_iterations,states
         */
        standardWriter.writeHeader(Arrays.asList("variant", "run", "score", "time_ms", "avg_iterations", "states"));

        for (int i = 0; i <= 3; i++) {
            LocalSearch.firstChoiceHillClimbing(teamState); // JVM Warm-Up
        }

        for (int i = 1; i <= ExperimentLocalSearch.countData; i++) {

            teamState.newRandomLineUp(); // so runs dont give same outcome

            long start = System.nanoTime();
            TeamState best = LocalSearch.firstChoiceHillClimbingWithKStarts(teamState, k);
            long end = System.nanoTime();

            long durationNs = end - start;
            double durationMs = durationNs / 1_000_000.0;

            String run = Integer.toString(i);
            String score = Integer.toString(best.getTotalPoints());
            String time_ms = String.format(Locale.US, "%.3f", durationMs); // Locale.US so it uses . instead of ,
            String iterations = Double.toString(LocalSearch.avgIterations);
            String states = Long.toString(LocalSearch.statesCreated);

            /*
             * appendRows()
             * k_Restarts;1;838;7239;30;145000
             */
            standardWriter
                    .appendRow(Arrays.asList("firstChoice" + k + "_Restarts", run, score, time_ms, iterations, states));

            // Just for Fun
            // Update progress bar after finishing this run
            printProgressBar(i, ExperimentLocalSearch.countData);

        }

    }

    /**
     * Startet k-Restart-Hill-Climbing k Mal und erstellt ein .csv Datei im
     * Verzeichnis:
     * OptimalDMSAlgo/data
     * 
     * @param k
     */
    public static void kRestartsHillClimbing(int k, TeamState teamState) throws IOException {

        // Just for Fun
        // Print initial progress bar (0%)
        progressBarInitialized = false;
        System.out.println("Experiment \"" + k + "-Restarts Hill Climbing\":");
        printProgressBar(0, ExperimentLocalSearch.countData);

        CSVWriter standardWriter = new CSVWriter("OptimalDMSAlgo/data/" + k + "_RestartsHill.csv", ";");

        /*
         * writingHeader()
         * variant,run,score,time_ms,avg_iterations,states
         */
        standardWriter.writeHeader(Arrays.asList("variant", "run", "score", "time_ms", "avg_iterations", "states"));

        for (int i = 0; i <= 3; i++) {
            LocalSearch.hillClimbing(teamState); // JVM Warm-Up
        }

        for (int i = 1; i <= ExperimentLocalSearch.countData; i++) {

            for (int j = 0; j < 600000; j++) {
                teamState.newRandomLineUp();
            }
            teamState.newRandomLineUp(); // so runs dont give same outcome

            long start = System.nanoTime();
            TeamState best = LocalSearch.hillClimbingWithKStarts(teamState, k);
            long end = System.nanoTime();

            long durationNs = end - start;
            double durationMs = durationNs / 1_000_000.0;
            System.out.println(best.toStringLineUp());

            String run = Integer.toString(i);
            String score = Integer.toString(best.getTotalPoints());
            String time_ms = String.format(Locale.US, "%.3f", durationMs); // Locale.US so it uses . instead of ,
            String iterations = Double.toString(LocalSearch.avgIterations);
            String states = Long.toString(LocalSearch.statesCreated);

            /*
             * appendRows()
             * k_Restarts;1;838;7239;30;145000
             */
            standardWriter.appendRow(Arrays.asList("" + k + "_Restarts", run, score, time_ms, iterations, states));

            // Just for Fun
            // Update progress bar after finishing this run
            printProgressBar(i, ExperimentLocalSearch.countData);

        }

    }

    /**
     * Startet SideSteps-Hill-Climbing k Mal und erstellt ein .csv Datei im
     * Verzeichnis:
     * OptimalDMSAlgo/data
     * 
     * @param k
     */
    public static void kSideStepsHillClimbing(int k, TeamState teamState) throws IOException {

        // Just for Fun
        // Print initial progress bar (0%)
        progressBarInitialized = false;
        System.out.println("Experiment \"" + k + "-SideStep Hill Climbing\":");
        printProgressBar(0, ExperimentLocalSearch.countData);

        CSVWriter standardWriter = new CSVWriter("OptimalDMSAlgo/data/" + k + "_SideStepsHill.csv", ";");

        /*
         * writingHeader()
         * variant,run,score,time_ms,avg_iterations,states
         */
        standardWriter.writeHeader(Arrays.asList("variant", "run", "score", "time_ms", "avg_iterations", "states"));

        for (int i = 0; i <= 3; i++) {
            LocalSearch.hillClimbing(teamState); // JVM Warm-Up
        }

        for (int i = 1; i <= ExperimentLocalSearch.countData; i++) {

            teamState.newRandomLineUp(); // so runs dont give same outcome

            long start = System.nanoTime();
            TeamState best = LocalSearch.hillClimbingWithKSide(teamState, k);
            long end = System.nanoTime();

            long durationNs = end - start;
            double durationMs = durationNs / 1_000_000.0;

            String run = Integer.toString(i);
            String score = Integer.toString(best.getTotalPoints());
            String time_ms = String.format(Locale.US, "%.3f", durationMs); // Locale.US so it uses . instead of ,
            String iterations = Double.toString(LocalSearch.iterations);
            String states = Long.toString(LocalSearch.statesCreated);

            /*
             * appendRows()
             * k_Restarts;1;838;7239;30;145000
             */
            standardWriter.appendRow(Arrays.asList("" + k + "_SideSteps", run, score, time_ms, iterations, states));

            // Just for Fun
            // Update progress bar after finishing this run
            printProgressBar(i, ExperimentLocalSearch.countData);

        }

    }

    // Just for Fun indicator on how far the run is
    private static void printProgressBar(int done, int total) {
        int width = 40;
        double ratio = (double) done / (double) total;
        int filled = (int) (ratio * width);
        StringBuilder bar = new StringBuilder();
        bar.append('[');
        for (int i = 0; i < filled; i++)
            bar.append('#');
        for (int i = filled; i < width; i++)
            bar.append(' ');
        bar.append(']');
        int percent = (int) (ratio * 100);

        // Printed the bar once, move cursor up and clear that line
        if (progressBarInitialized) {
            System.out.print("\u001B[1A"); // move cursor up 1 line
            System.out.print("\u001B[2K"); // clear whole line
        } else {
            progressBarInitialized = true;
        }

        System.out.print(String.format("%s %3d%% (%d/%d)%n", bar.toString(), percent, done, total));
        System.out.flush();
    }

    // =========================================
    // Simulated Annealing Variations
    // =========================================

    /**
     * Startet Simulated Annealing "countData" Mal und erstellt ein .csv Datei im
     * Verzeichnis:
     * OptimalDMSAlgo/data
     * 
     * ATTENTION: RESULTS VARY DEPENDING ON SCHEDULE
     * 
     * @param k
     */
    public static void simulatedAnnealing(TeamState teamState) throws IOException {

        // Just for Fun
        // Print initial progress bar (0%)
        progressBarInitialized = false;
        System.out.println("Experiment \"Simulated Annealing\":");
        printProgressBar(0, ExperimentLocalSearch.countData);

        CSVWriter standardWriter = new CSVWriter("OptimalDMSAlgo/data/simulatedAnnealing.csv", ";");

        /*
         * writingHeader()
         * variant,run,score,time_ms,iterations,states
         */
        standardWriter.writeHeader(Arrays.asList("variant", "run", "score", "time_ms", "iterations", "states"));

        ExponentialSchedule schedule = new ExponentialSchedule(10, 0.99, 0.0001);

        // for (int i = 0; i <= 1; i++) {
        // schedule.reset(1000);
        // LocalSearch.SimulatedAnnealing(teamState, schedule); // JVM Warm-Up
        // }

        for (int i = 1; i <= ExperimentLocalSearch.countData; i++) {

            teamState.newRandomLineUp(); // so runs dont give same outcome
            schedule.reset(1); // so Schedule starts with 1000 degrees

            long start = System.nanoTime();
            TeamState best = LocalSearch.SimulatedAnnealing(teamState, schedule);
            long end = System.nanoTime();

            long durationNs = end - start;
            double durationMs = durationNs / 1_000_000.0;

            String run = Integer.toString(i);
            String score = Integer.toString(best.getTotalPoints());
            String time_ms = String.format(Locale.US, "%.3f", durationMs); // Locale.US so it uses . instead of ,
            String iterations = Double.toString(LocalSearch.iterations);
            String states = Long.toString(LocalSearch.statesCreated);

            /*
             * appendRow()
             * standard;1;838;7239;30;145000
             */
            standardWriter.appendRow(Arrays.asList("simulatedAnnealing", run, score, time_ms, iterations, states));

            // Just for Fun
            // Update progress bar after finishing this run
            printProgressBar(i, ExperimentLocalSearch.countData);
        }
    }

    /**
     * Startet Shavd Annealing countData Mal und erstellt ein .csv Datei im
     * Verzeichnis:
     * OptimalDMSAlgo/data
     * 
     * ATTENTION: RESULTS VARY DEPENDING ON SCHEDULE
     * 
     * @param k
     */
    public static void simulatedAnnealingShavedN(TeamState teamState, int n) throws IOException {

        // Just for Fun
        // Print initial progress bar (0%)
        progressBarInitialized = false;
        System.out.println("Experiment \"Shaved Annealing\":");
        printProgressBar(0, ExperimentLocalSearch.countData);

        int startTemp = 10; // to Set starting Temperatures;

        CSVWriter standardWriter = new CSVWriter(
                "OptimalDMSAlgo/data/temp" + startTemp + "Shaved" + n + "AnnealingSVM.csv", ";");

        /*
         * writingHeader()
         * variant,run,score,time_ms,iterations,states
         */
        standardWriter.writeHeader(Arrays.asList("variant", "run", "score", "time_ms", "iterations", "states"));

        AdaptiveSchedule schedule = new AdaptiveSchedule(startTemp, 0.001, 100);
        // ExponentialSchedule schedule = new ExponentialSchedule(startTemp, 0.995,
        // 0.1);

        // for (int i = 0; i <= 1; i++) {
        // schedule.reset(1000);
        // LocalSearch.SimulatedAnnealing(teamState, schedule); // JVM Warm-Up
        // }

        for (int i = 1; i <= ExperimentLocalSearch.countData; i++) {

            teamState.newRandomLineUp(); // so runs dont give same outcome
            schedule.reset(startTemp); // so Schedule starts with startTemp degrees

            long start = System.nanoTime();
            TeamState best = LocalSearch.SimulatedAnnealingShavedN(teamState, schedule, n);
            long end = System.nanoTime();

            long durationNs = end - start;
            double durationMs = durationNs / 1_000_000.0;

            String run = Integer.toString(i);
            String score = Integer.toString(best.getTotalPoints());
            String time_ms = String.format(Locale.US, "%.3f", durationMs); // Locale.US so it uses . instead of ,
            String iterations = Double.toString(LocalSearch.iterations); // FAKED RIGHT NOW IN COMMIT #30
            String states = Long.toString(LocalSearch.statesCreated); // FAKED RIGHT NOW IN COMMIT #30

            /*
             * appendRow()
             * standard;1;838;7239;30;145000
             */
            standardWriter.appendRow(Arrays.asList("temp" + startTemp + "shaved" + n + "Annealing", run, score, time_ms,
                    iterations, states));

            // Just for Fun
            // Update progress bar after finishing this run
            printProgressBar(i, ExperimentLocalSearch.countData);
        }
    }

    // =============================================================
    // Faster Tests
    // =============================================================

    /**
     * Startet Standard-Hill-Climbing k Mal und erstellt ein .csv Datei im
     * Verzeichnis:
     * OptimalDMSAlgo/data
     * 
     * @param k
     */
    public static void standardHillClimbingFast(TeamState teamState) throws IOException {

        // Just for Fun
        // Print initial progress bar (0%)
        progressBarInitialized = false;
        System.out.println("Experiment \"Standart Hill Climbing Fast\":");
        printProgressBar(0, countData);

        CSVWriter standardWriter = new CSVWriter("OptimalDMSAlgo/data/standardHillFast.csv", ";");

        /*
         * writingHeader()
         * variant,run,score,time_ms,iterations,states
         */
        standardWriter.writeHeader(Arrays.asList("variant", "run", "score", "time_ms", "iterations", "states"));

        for (int i = 0; i <= 3; i++) {
            LocalSearch.hillClimbing(teamState); // JVM Warm-Up
        }

        for (int i = 1; i <= countData; i++) {

            teamState.newRandomLineUp(); // so runs dont give same outcome

            long start = System.nanoTime();
            TeamState best = LocalSearch.hillClimbingFast(teamState);
            long end = System.nanoTime();

            long durationNs = end - start;
            double durationMs = durationNs / 1_000_000.0;

            String run = Integer.toString(i);
            String score = Integer.toString(best.getTotalPoints());
            String time_ms = String.format(Locale.US, "%.3f", durationMs); // Locale.US so it uses . instead of ,
            String iterations = Double.toString(LocalSearch.iterations);
            String states = Long.toString(LocalSearch.statesCreated);

            /*
             * appendRow()
             * standard;1;838;7239;30;145000
             */
            standardWriter.appendRow(Arrays.asList("standardFast", run, score, time_ms, iterations, states));

            // Just for Fun
            // Update progress bar after finishing this run
            printProgressBar(i, countData);
        }
    }

    /**
     * Startet k-Restart-Hill-Climbing k Mal und erstellt ein .csv Datei im
     * Verzeichnis:
     * OptimalDMSAlgo/data
     * 
     * @param k
     */
    public static void kRestartsHillClimbinFast(int k, TeamState teamState) throws IOException {

        // Just for Fun
        // Print initial progress bar (0%)
        progressBarInitialized = false;
        System.out.println("Experiment \"" + k + "-Restarts Hill Climbing Fast\":");
        printProgressBar(0, ExperimentLocalSearch.countData);

        CSVWriter standardWriter = new CSVWriter("OptimalDMSAlgo/data/" + k + "_RestartsHillFast.csv", ";");

        /*
         * writingHeader()
         * variant,run,score,time_ms,avg_iterations,states
         */
        standardWriter.writeHeader(Arrays.asList("variant", "run", "score", "time_ms", "avg_iterations", "states"));

        for (int i = 0; i <= 3; i++) {
            LocalSearch.hillClimbing(teamState); // JVM Warm-Up
        }

        for (int i = 1; i <= ExperimentLocalSearch.countData; i++) {

            teamState.newRandomLineUp(); // so runs dont give same outcome

            long start = System.nanoTime();
            TeamState best = LocalSearch.hillClimbingWithKStartsFast(teamState, k);
            System.out.println(best.toStringTeamSwimmers());
            System.out.println(best.toStringLineUp());
            long end = System.nanoTime();

            long durationNs = end - start;
            double durationMs = durationNs / 1_000_000.0;

            String run = Integer.toString(i);
            String score = Integer.toString(best.getTotalPoints());
            String time_ms = String.format(Locale.US, "%.3f", durationMs); // Locale.US so it uses . instead of ,
            String iterations = Double.toString(LocalSearch.avgIterations);
            String states = Long.toString(LocalSearch.statesCreated);

            /*
             * appendRows()
             * k_Restarts;1;838;7239;30;145000
             */
            standardWriter.appendRow(Arrays.asList("" + k + "_RestartsFast", run, score, time_ms, iterations, states));

            // Just for Fun
            // Update progress bar after finishing this run
            printProgressBar(i, ExperimentLocalSearch.countData);

        }
    }

    /**
     * Startet Standard-Hill-Climbing k Mal und erstellt ein .csv Datei im
     * Verzeichnis:
     * OptimalDMSAlgo/data
     * 
     * @param k
     */
    public static void beamSearchFast(TeamState teamState, int k) throws IOException {

        // Just for Fun
        // Print initial progress bar (0%)
        progressBarInitialized = false;
        System.out.println("Experiment \"Local Beam Search Fast\":");
        printProgressBar(0, countData);

        CSVWriter standardWriter = new CSVWriter("OptimalDMSAlgo/data/local" + k + "BeamFast.csv", ";");

        /*
         * writingHeader()
         * variant,run,score,time_ms,iterations,states
         */
        standardWriter.writeHeader(Arrays.asList("variant", "run", "score", "time_ms", "iterations", "states"));

        for (int i = 1; i <= countData; i++) {

            teamState.newRandomLineUp(); // so runs dont give same outcome

            long start = System.nanoTime();
            TeamState best = LocalSearch.beamSearchFast(teamState, k);
            long end = System.nanoTime();

            long durationNs = end - start;
            double durationMs = durationNs / 1_000_000.0;

            String run = Integer.toString(i);
            String score = Integer.toString(best.getTotalPoints());
            String time_ms = String.format(Locale.US, "%.3f", durationMs); // Locale.US so it uses . instead of ,
            String iterations = Double.toString(LocalSearch.iterations);
            String states = Long.toString(LocalSearch.statesCreated);

            /*
             * appendRow()
             * standard;1;838;7239;30;145000
             */
            standardWriter.appendRow(Arrays.asList("beamSearchFast", run, score, time_ms, iterations, states));

            // Just for Fun
            // Update progress bar after finishing this run
            printProgressBar(i, countData);
        }
    }

    // =============================================================
    // Internal Tests
    // =============================================================

    /**
     * Startet Standard-Hill-Climbing k Mal und erstellt ein .csv Datei im
     * Verzeichnis:
     * OptimalDMSAlgo/data
     * 
     * @param k
     */
    public static void standardHillClimbingFastIterations(TeamState teamState) throws IOException {

        // Just for Fun
        // Print initial progress bar (0%)
        progressBarInitialized = false;
        System.out.println("Experiment \"Standart Hill Climbing Iterations\":");
        printProgressBar(0, countData);

        CSVWriter iterationWriter = new CSVWriter("OptimalDMSAlgo/data/internEval/standardHillFastIterations.csv", ";");
        CSVWriter firstAndLastWriter = new CSVWriter("OptimalDMSAlgo/data/internEval/standardHillFirstLast.csv",
                ";");

        /*
         * writingHeader()
         * run_id; iteration; points
         */
        iterationWriter.writeHeader(Arrays.asList("run_id", "iteration", "points"));
        /*
         * writingHeader()
         * run_id; startPoints; endPoints
         */
        firstAndLastWriter.writeHeader(Arrays.asList("run_id", "startPoints", "endPoints"));

        for (int i = 0; i <= 3; i++) {
            LocalSearch.hillClimbing(teamState); // JVM Warm-Up
        }

        for (int i = 1; i <= countData; i++) {

            teamState.newRandomLineUp(); // so runs dont give same outcome

            long start = System.nanoTime();
            TeamState best = LocalSearch.hillClimbingCompleatlyFly(teamState);
            long end = System.nanoTime();

            long durationNs = end - start;
            double durationMs = durationNs / 1_000_000.0;

            int iter = 1;
            for (Integer points : LocalSearch.pointsDevelopment) {

                /*
                 * appendRow()
                 * 1; 1; 19000
                 * ...
                 */
                iterationWriter
                        .appendRow(Arrays.asList(Integer.toString(i), Integer.toString(iter), points.toString()));
                iter++;

            }

            Integer first = LocalSearch.pointsDevelopment.getFirst();
            Integer last = LocalSearch.pointsDevelopment.getLast();
            firstAndLastWriter.appendRow(Arrays.asList(Integer.toString(i), first.toString(), last.toString()));

            // Just for Fun
            // Update progress bar after finishing this run
            printProgressBar(i, countData);
        }
    }

    /**
     * Startet Standard-Hill-Climbing k Mal und erstellt ein .csv Datei im
     * Verzeichnis:
     * OptimalDMSAlgo/data
     * 
     * @param k
     */
    public static void firstChoiceHillClimbingKSwapsIterationsTeamName(TeamState teamState, int k, String teamString)
            throws IOException {

        // Just for Fun
        // Print initial progress bar (0%)
        progressBarInitialized = false;
        System.out.println("Experiment \"First Choice Climbing kSwaps Iterations\":");
        printProgressBar(0, countData);

        CSVWriter iterationWriter = new CSVWriter(
                "OptimalDMSAlgo/data/internEval/first" + k + "SwapsHillFastIterations " + teamString + ".csv",
                ";");
        CSVWriter firstAndLastWriter = new CSVWriter(
                "OptimalDMSAlgo/data/internEval/first" + k + "SwapsHillFirstLast" + teamString + ".csv",
                ";");

        /*
         * writingHeader()
         * run_id; iteration; points
         */
        iterationWriter.writeHeader(Arrays.asList("run_id", "iteration", "points"));
        /*
         * writingHeader()
         * run_id; startPoints; endPoints
         */
        firstAndLastWriter.writeHeader(Arrays.asList("run_id", "startPoints", "endPoints"));

        for (int i = 0; i <= 3; i++) {
            // LocalSearch.hillClimbing(teamState); // JVM Warm-Up
        }

        for (int i = 1; i <= countData; i++) {

            teamState.newRandomLineUp(); // so runs dont give same outcome

            long start = System.nanoTime();
            TeamState best = LocalSearch.firstChoiceHillClimbingWithSwaps(teamState, k);
            long end = System.nanoTime();

            long durationNs = end - start;
            double durationMs = durationNs / 1_000_000.0;

            int iter = 1;
            for (Integer points : LocalSearch.pointsDevelopment) {

                /*
                 * appendRow()
                 * 1; 1; 19000
                 * ...
                 */
                iterationWriter
                        .appendRow(Arrays.asList(Integer.toString(i), Integer.toString(iter), points.toString()));
                iter++;

            }

            Integer first = LocalSearch.pointsDevelopment.getFirst();
            Integer last = LocalSearch.pointsDevelopment.getLast();
            firstAndLastWriter.appendRow(Arrays.asList(Integer.toString(i), first.toString(), last.toString()));

            // Just for Fun
            // Update progress bar after finishing this run
            printProgressBar(i, countData);
        }
    }

    // =============================================================
    // Comparison by Time
    // =============================================================

    /**
     * Tests each important funktion by time
     * Every funktion gets tested 30 times per Timestamp
     * 
     * 5s - 10s - 20s - 60s - 120s
     * 
     * @param k
     */
    public static void comparisonByTime(TeamState teamState, int time) throws IOException {

        // Just for Fun
        // Print initial progress bar (0%)
        progressBarInitialized = false;
        System.out.println("Experiment \"Time " + time + "\":");
        printProgressBar(0, countData * 5);

        CSVWriter standardWriter = new CSVWriter("OptimalDMSAlgo/data/internEval/" + time + "standardHill.csv", ";");
        CSVWriter simulatedAnnealingWriter = new CSVWriter(
                "OptimalDMSAlgo/data/internEval/" + time + "simulatedAnnealing.csv", ";");
        CSVWriter beamSearchWriter = new CSVWriter("OptimalDMSAlgo/data/internEval/" + time + "beamSearch.csv", ";");
        CSVWriter fristWriter = new CSVWriter("OptimalDMSAlgo/data/internEval/" + time + "firstChoice.csv", ";");
        CSVWriter firstSwapWriter = new CSVWriter("OptimalDMSAlgo/data/internEval/" + time + "firstSwaps.csv", ";");

        List<String> header = Arrays.asList("time", "points", "states");

        standardWriter.writeHeader(header);
        simulatedAnnealingWriter.writeHeader(header);
        beamSearchWriter.writeHeader(header);
        fristWriter.writeHeader(header);
        firstSwapWriter.writeHeader(header);

        int progress = 1;
        for (int i = 1; i <= countData; i++) {

            // StandardHill
            long start = System.currentTimeMillis();
            long end = System.currentTimeMillis();

            TeamState best = null;
            long states = 0;
            while (end - start < time) {
                teamState.newRandomLineUp(); // so runs dont give same outcome
                TeamState current = LocalSearch.hillClimbingFast(teamState);
                states += LocalSearch.statesCreated;
                if (best == null) {
                    best = current;
                } else {
                    if (best.getTotalPointsFast() < current.getTotalPointsFast()) {
                        best = current;
                    }
                }
                end = System.currentTimeMillis();
            }
            // appendsRow
            standardWriter.appendRow(Arrays.asList(Long.toString(
                    (end - start)), Integer.toString(best.getTotalPoints()),
                    Long.toString(states)));

            // Just for Fun
            // Update progress bar after finishing this run
            printProgressBar(progress++, countData * 5);

            // SimulatedAnnealing
            start = System.currentTimeMillis();
            end = System.currentTimeMillis();

            best = null;
            states = 0;
            ExponentialSchedule schedule = new ExponentialSchedule(10, 0.99, 0.0001);
            while (end - start < time) {
                teamState.newRandomLineUp(); // so runs dont give same outcome
                TeamState current = LocalSearch.SimulatedAnnealing(teamState, schedule);
                schedule.reset(10);
                // System.out.println(LocalSearch.statesCreated);
                states += LocalSearch.statesCreated;
                if (best == null) {
                    best = current;
                } else {
                    if (best.getTotalPointsFast() < current.getTotalPointsFast()) {
                        best = current;
                    }
                }
                end = System.currentTimeMillis();
            }
            // appendsRow
            simulatedAnnealingWriter
                    .appendRow(Arrays.asList(Long.toString((end - start)), Integer.toString(best.getTotalPoints()),
                            Long.toString(states)));

            // Just for Fun
            // Update progress bar after finishing this run
            printProgressBar(progress++, countData * 5);

            // beamSearch
            start = System.currentTimeMillis();
            end = System.currentTimeMillis();

            best = null;
            states = 0;
            while (end - start < time) {
                teamState.newRandomLineUp(); // so runs dont give same outcome
                TeamState current = LocalSearch.beamSearchFast(teamState, 20);
                states += LocalSearch.statesCreated;
                if (best == null) {
                    best = current;
                } else {
                    if (best.getTotalPointsFast() < current.getTotalPointsFast()) {
                        best = current;
                    }
                }
                end = System.currentTimeMillis();
            }
            // appendsRow
            beamSearchWriter
                    .appendRow(Arrays.asList(Long.toString((end - start)), Integer.toString(best.getTotalPoints()),
                            Long.toString(states)));

            // Just for Fun
            // Update progress bar after finishing this run
            printProgressBar(progress++, countData * 5);

            // FirstChoice
            start = System.currentTimeMillis();
            end = System.currentTimeMillis();

            best = null;
            states = 0;
            while (end - start < time) {
                teamState.newRandomLineUp(); // so runs dont give same outcome
                TeamState current = LocalSearch.firstChoiceHillClimbing(teamState);
                states += LocalSearch.statesCreated;
                if (best == null) {
                    best = current;
                } else {
                    if (best.getTotalPointsFast() < current.getTotalPointsFast()) {
                        best = current;
                    }
                }
                end = System.currentTimeMillis();
            }
            // appendsRow
            fristWriter.appendRow(Arrays.asList(Long.toString((end - start)), Integer.toString(best.getTotalPoints()),
                    Long.toString(states)));

            // Just for Fun
            // Update progress bar after finishing this run
            printProgressBar(progress++, countData * 5);

            // FirstChoiceSwaps
            start = System.currentTimeMillis();
            end = System.currentTimeMillis();

            best = null;
            states = 0;
            while (end - start < time) {
                teamState.newRandomLineUp(); // so runs dont give same outcome
                TeamState current = LocalSearch.firstChoiceHillClimbingWithSwaps(teamState, 3);
                states += LocalSearch.statesCreated;
                if (best == null) {
                    best = current;
                } else {
                    if (best.getTotalPointsFast() < current.getTotalPointsFast()) {
                        best = current;
                    }
                }
                end = System.currentTimeMillis();
            }
            // appendsRow
            firstSwapWriter.appendRow(Arrays.asList(Long.toString(
                    (end - start)), Integer.toString(best.getTotalPoints()),
                    Long.toString(states)));

            // Just for Fun
            // Update progress bar after finishing this run
            printProgressBar(progress++, countData * 5);

        }

    }

    // Experiment 1
    public static void compareFive(TeamState teamState) throws IOException {
        ExperimentLocalSearch.hillClimbingOnTheFly(teamState);
        ExperimentLocalSearch.beamSearch(teamState, 20);
        ExperimentLocalSearch.firstChoiceHillClimbing(teamState);
        ExperimentLocalSearch.simulatedAnnealing(teamState);
        ExperimentLocalSearch.firstChoiceHillClimbingWithKSwaps(teamState, 3);
    }

    // Experiment 2
    public static void compareAllTimes(TeamState teamState) throws IOException {

        for (int i = 0; i <= 3; i++) {
            LocalSearch.hillClimbing(teamState); // JVM Warm-Up
        }

        comparisonByTime(teamState, 2000);
        comparisonByTime(teamState, 5000);
        comparisonByTime(teamState, 10000);
        comparisonByTime(teamState, 30000);
        comparisonByTime(teamState, 60000);

    }

    public static void compareDataSets(TeamState better, TeamState svm, TeamState fsdFemale, TeamState fsdMale)
            throws IOException {

        for (int i = 0; i <= 3; i++) {
            LocalSearch.hillClimbing(better); // JVM Warm-Up
        }

        firstChoiceHillClimbingKSwapsIterationsTeamName(better, 3, "betterClub");
        firstChoiceHillClimbingKSwapsIterationsTeamName(svm, 3, "SVM");
        firstChoiceHillClimbingKSwapsIterationsTeamName(fsdFemale, 3, "DuesseldorfFemale");
        firstChoiceHillClimbingKSwapsIterationsTeamName(fsdMale, 3, "DuesseldorfMale");
    }

    public static void compareStartingStates(TeamState state) throws IOException {
        CSVWriter emptyLineUp = new CSVWriter("OptimalDMSAlgo/data/internEval/EmptyLineUp.csv", ";");
        CSVWriter emptyAllowed = new CSVWriter("OptimalDMSAlgo/data/internEval/EmptyAllowed.csv", ";");
        CSVWriter emptyForbidden = new CSVWriter("OptimalDMSAlgo/data/internEval/EmptyForbidden.csv", ";");

        List<String> header = Arrays.asList("time", "points", "iterations");

        emptyLineUp.writeHeader(header);
        emptyAllowed.writeHeader(header);
        emptyForbidden.writeHeader(header);

        for (int i = 0; i <= 3; i++) {
            LocalSearch.hillClimbing(state); // JVM Warm-Up
        }

        progressBarInitialized = false;
        System.out.println("EmptyLineup:");
        printProgressBar(0, 1000);
        for (int i = 1; i <= 1000; i++) {
            printProgressBar(i, 1000);
            state.setEmptyLineup();

            long start = System.nanoTime();
            TeamState best = LocalSearch.firstChoiceHillClimbingWithSwaps(state, 3);
            long end = System.nanoTime();

            long durationNs = end - start;
            double durationMs = durationNs / 1_000_000.0;

            String score = Integer.toString(best.getTotalPoints());
            String time_ms = String.format(Locale.US, "%.3f", durationMs); // Locale.US so it uses . instead of ,
            String iterations = Long.toString(LocalSearch.iterations);

            /*
             * appendRows()
             * time;points;iterations
             */
            emptyLineUp.appendRow(Arrays.asList(time_ms, score, iterations));

        }

        progressBarInitialized = false;
        System.out.println("EmptyForbidden:");
        printProgressBar(0, 1000);
        for (int i = 1; i <= 1000; i++) {
            printProgressBar(i, 1000);
            state.newRandomLineUpNoEmpty();

            long start = System.nanoTime();
            TeamState best = LocalSearch.firstChoiceHillClimbingWithSwaps(state, 3);
            long end = System.nanoTime();

            long durationNs = end - start;
            double durationMs = durationNs / 1_000_000.0;

            String score = Integer.toString(best.getTotalPoints());
            String time_ms = String.format(Locale.US, "%.3f", durationMs); // Locale.US so it uses . instead of ,
            String iterations = Long.toString(LocalSearch.iterations);

            /*
             * appendRows()
             * time;points;iterations
             */
            emptyForbidden.appendRow(Arrays.asList(time_ms, score, iterations));

        }

        progressBarInitialized = false;
        System.out.println("EmptyAllowed:");
        printProgressBar(0, 1000);
        for (int i = 1; i <= 1000; i++) {
            printProgressBar(i, 1000);
            state.newRandomLineUp();

            long start = System.nanoTime();
            TeamState best = LocalSearch.firstChoiceHillClimbingWithSwaps(state, 3);
            long end = System.nanoTime();

            long durationNs = end - start;
            double durationMs = durationNs / 1_000_000.0;

            String score = Integer.toString(best.getTotalPoints());
            String time_ms = String.format(Locale.US, "%.3f", durationMs); // Locale.US so it uses . instead of ,
            String iterations = Long.toString(LocalSearch.iterations);

            /*
             * appendRows()
             * time;points;iterations
             */
            emptyAllowed.appendRow(Arrays.asList(time_ms, score, iterations));

        }
    }

    public static void compareStartingStatesCompleatly(TeamState state) throws IOException {
        CSVWriter emptyLineUp = new CSVWriter("OptimalDMSAlgo/data/internEval/CompleatEmptyLineUp.csv", ";");
        CSVWriter emptyAllowed = new CSVWriter("OptimalDMSAlgo/data/internEval/CompleatEmptyAllowed.csv", ";");
        CSVWriter emptyForbidden = new CSVWriter("OptimalDMSAlgo/data/internEval/CompleatEmptyForbidden.csv", ";");

        List<String> header = Arrays.asList("time", "points", "iterations");

        emptyLineUp.writeHeader(header);
        emptyAllowed.writeHeader(header);
        emptyForbidden.writeHeader(header);

        for (int i = 0; i <= 3; i++) {
            LocalSearch.hillClimbing(state); // JVM Warm-Up
        }

        progressBarInitialized = false;
        System.out.println("EmptyLineup:");
        printProgressBar(0, 1000);
        for (int i = 1; i <= 1000; i++) {
            printProgressBar(i, 1000);
            state.setEmptyLineup();

            long start = System.nanoTime();
            TeamState best = LocalSearch.hillClimbingCompleatlyFly(state);
            long end = System.nanoTime();

            long durationNs = end - start;
            double durationMs = durationNs / 1_000_000.0;

            String score = Integer.toString(best.getTotalPoints());
            String time_ms = String.format(Locale.US, "%.3f", durationMs); // Locale.US so it uses . instead of ,
            String iterations = Long.toString(LocalSearch.iterations);

            /*
             * appendRows()
             * time;points;iterations
             */
            emptyLineUp.appendRow(Arrays.asList(time_ms, score, iterations));

        }

        progressBarInitialized = false;
        System.out.println("EmptyForbidden:");
        printProgressBar(0, 1000);
        for (int i = 1; i <= 1000; i++) {
            printProgressBar(i, 1000);
            state.newRandomLineUpNoEmpty();

            long start = System.nanoTime();
            TeamState best = LocalSearch.hillClimbingCompleatlyFly(state);
            long end = System.nanoTime();

            long durationNs = end - start;
            double durationMs = durationNs / 1_000_000.0;

            String score = Integer.toString(best.getTotalPoints());
            String time_ms = String.format(Locale.US, "%.3f", durationMs); // Locale.US so it uses . instead of ,
            String iterations = Long.toString(LocalSearch.iterations);

            /*
             * appendRows()
             * time;points;iterations
             */
            emptyForbidden.appendRow(Arrays.asList(time_ms, score, iterations));

        }

        progressBarInitialized = false;
        System.out.println("EmptyAllowed:");
        printProgressBar(0, 1000);
        for (int i = 1; i <= 1000; i++) {
            printProgressBar(i, 1000);
            state.newRandomLineUp();

            long start = System.nanoTime();
            TeamState best = LocalSearch.hillClimbingCompleatlyFly(state);
            long end = System.nanoTime();

            long durationNs = end - start;
            double durationMs = durationNs / 1_000_000.0;

            String score = Integer.toString(best.getTotalPoints());
            String time_ms = String.format(Locale.US, "%.3f", durationMs); // Locale.US so it uses . instead of ,
            String iterations = Long.toString(LocalSearch.iterations);

            /*
             * appendRows()
             * time;points;iterations
             */
            emptyAllowed.appendRow(Arrays.asList(time_ms, score, iterations));

        }
    }

}
