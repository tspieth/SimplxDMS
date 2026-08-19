package localsearch;

import experiments.ExperimentLocalSearch;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class LocalSearch {

    public static long statesCreated;
    public static int iterations;
    public static double avgIterations;
    public static List<Integer> pointsDevelopment = new ArrayList<Integer>();

    /**
     * Standard HillClimbing from Lecture SPS at Universitiy of Mannheim
     * 
     * in each Turn the Best Neighbor is choosen;
     * 
     * @param current
     * @return
     */
    public static TeamState hillClimbing(TeamState current) {

        TeamState currentState = current;

        statesCreated = 0; // reset statesCreated
        iterations = 0; // reset avgIterations
        pointsDevelopment = new ArrayList<Integer>();

        while (true) {

            List<TeamState> neighbors = currentState.createAllNeighbors();

            statesCreated += neighbors.size(); // add created states to total

            TeamState bestNeighbor = TeamState.getBestState(neighbors);

            int bestValue = bestNeighbor.getTotalPointsFast();
            pointsDevelopment.add(bestValue);

            if (bestValue <= currentState.getTotalPointsFast()) {
                break;
            }

            currentState = bestNeighbor;
            iterations++; // update Iterations
        }
        return currentState;
    }

    /**
     * Standard HillClimbing from Lecture SPS at Universitiy of Mannheim
     * 
     * in each Turn the Best Neighbor is choosen;
     * 
     * @param current
     * @return
     */
    public static TeamState hillClimbingCompleatlyFly(TeamState current) {

        TeamState currentState = current;

        statesCreated = 0; // reset statesCreated
        iterations = 0; // reset avgIterations
        pointsDevelopment = new ArrayList<Integer>();

        while (true) {

            TeamState bestNeighbor = currentState.getBestNeighborFast();

            statesCreated += 1; // add created states to total

            int bestValue = bestNeighbor.getTotalPointsFast();
            pointsDevelopment.add(bestValue);

            if (bestValue <= currentState.getTotalPointsFast()) {
                break;
            }

            currentState = bestNeighbor;
            iterations++; // update Iterations
        }
        return currentState;
    }

    public static TeamState firstChoiceHillClimbing(TeamState current) {

        TeamState currentState = current;

        statesCreated = 0; // reset statesCreated
        iterations = 0; // reset avgIterations
        pointsDevelopment = new ArrayList<Integer>();

        while (true) {

            pointsDevelopment.add(currentState.getTotalPoints());

            // States Created get Updated over TeamState
            TeamState firstBetter = currentState.getFirstBetterRandomNeighbor();

            if (firstBetter == null) {
                break; // no better neighbor was found
            }

            currentState = firstBetter;
            iterations++; // update Iterations
        }
        return currentState;
    }

    public static TeamState firstChoiceHillClimbingWithSwaps(TeamState current, int k) {

        TeamState currentState = current;

        statesCreated = 0; // reset statesCreated
        iterations = 0; // reset avgIterations
        pointsDevelopment = new ArrayList<Integer>(); // So we can plot the internal Development

        while (true) {

            pointsDevelopment.add(currentState.getTotalPoints());

            // StatesCreated get Updated over TeamState
            TeamState firstBetter = currentState.firstBetterRandomNeighborkSwaps(k);

            if (firstBetter == null) {
                break; // no better neighbor was found
            }
            // System.out.println(firstBetter.getTotalPoints());
            currentState = firstBetter;
            iterations++; // update Iterations
        }
        return currentState;
    }

    public static TeamState firstChoiceHillClimbingWithKStarts(TeamState current, int k) {
        List<TeamState> allBest = new ArrayList<TeamState>();

        statesCreated = 0; // reset statesCreated
        iterations = 0; // reset from previoud runs
        avgIterations = 0; // reset avgIterations

        for (int i = 0; i < k; i++) {

            allBest.add(firstChoiceHillClimbing(current));

            avgIterations += iterations;

        }
        TeamState best = allBest.get(0);

        for (TeamState state : allBest) {
            if (state.getTotalPoints() > best.getTotalPoints()) {
                best = state;
            }
        }

        avgIterations = avgIterations / (double) k; // calculate avgIterations
        return best;
    }

    public static TeamState hillClimbingWithKStarts(TeamState current, int k) {
        List<TeamState> allBest = new ArrayList<TeamState>();

        statesCreated = 0; // reset statesCreated
        iterations = 0; // reset from previoud runs
        avgIterations = 0; // reset avgIterations

        for (int i = 0; i < k; i++) {

            current.newRandomLineUp();

            allBest.add(hillClimbingCompleatlyFly(current));

            avgIterations += iterations;

        }
        TeamState best = allBest.get(0);

        for (TeamState state : allBest) {
            if (state.getTotalPoints() > best.getTotalPoints()) {
                best = state;
            }
        }

        avgIterations = avgIterations / (double) k; // calculate avgIterations
        return best;
    }

    public static TeamState hillClimbingWithKSide(TeamState current, int k) {

        int css = 0;

        TeamState currentState = current;

        statesCreated = 0; // reset statesCreated
        iterations = 0; // reset avgIterations
        avgIterations = 0;

        while (true) {

            List<TeamState> neighbors = currentState.createAllNeighbors();

            statesCreated += neighbors.size(); // add created states to total

            TeamState bestNeighbor = TeamState.getBestState(neighbors);
            int valBest = bestNeighbor.getTotalPointsFast();
            int valCur = currentState.getTotalPointsFast();
            if (valBest < valCur) {
                break;
            } else {
                if (valBest == valCur) {
                    css++;
                    if (css == k) {
                        break;
                    }
                } else {
                    css = 0;
                }
                currentState = bestNeighbor;
            }

            iterations++; // update Iterations
        }
        return currentState;
    }

    public static TeamState beamSearch(TeamState teamState, int k) {

        List<TeamState> currentStates = teamState.createRandomStates(k);

        TeamState currentBest = TeamState.getBestState(currentStates);

        while (true) {
            PriorityQueue<TeamState> maxHeap = new PriorityQueue<>(
                    (a, b) -> Integer.compare(b.getTotalPointsFast(), a.getTotalPointsFast()));
            for (TeamState c : currentStates) {

                List<TeamState> topNeigh = c.createTopKNeighbors(k);
                statesCreated += topNeigh.size();
                for (TeamState b : topNeigh) {
                    maxHeap.add(b);
                }

                // maxHeap.add(TeamState.getBestState(c.createAllNeighbors()));

            }
            List<TeamState> neighborsBest = new ArrayList<>();

            // Bestes wird zuerst hinzugefuegt
            TeamState best = maxHeap.poll();
            neighborsBest.add(best);

            for (int i = 1; i < k && !maxHeap.isEmpty(); i++) {
                neighborsBest.add(maxHeap.poll()); // bestes Element holen
            }

            if (currentBest.getTotalPointsFast() >= best.getTotalPointsFast()) {
                return currentBest;
            } else {
                currentStates = neighborsBest;
                System.out.println(best.getTotalPointsFast());
                currentBest = best;
            }
        }
    }

    public static TeamState SimulatedAnnealing(TeamState current, Schedule schedule) {

        statesCreated = 0; // reset statesCreated
        iterations = 0; // reset iterations
        avgIterations = 0; // reset avgIterations

        for (int i = 1;; i++) {
            double t = schedule.getTemperature(i);
            iterations++;

            // System.out.println("Aktuelle Temperatur: " + t);
            if (t == 0) {
                return current;
            }

            TeamState next = current.getRandomNeighbor();
            statesCreated++;
            // System.out.println(statesCreated);
            int E = next.getTotalPointsFast() - current.getTotalPointsFast();

            if (E > 0) {
                current = next;
            } else {
                double p = ExperimentLocalSearch.rng.nextDouble();

                if (p < someFunction1(E, t)) {
                    current = next;
                }
            }
        }
        // return current; => unreachable
    }

    // Only gets Top N Neighbors each round
    public static TeamState SimulatedAnnealingShavedN(TeamState current, Schedule schedule, int n) {

        statesCreated = 0; // reset statesCreated
        iterations = 0; // reset iterations
        avgIterations = 0; // reset avgIterations

        for (int i = 1;; i++) {
            double t = schedule.getTemperature(i);
            iterations++;

            // System.out.println("Aktuelle Temperatur: " + t);
            if (t == 0) {
                return current;
            }

            TeamState next = current.getRandomNeighborFromTopN(n);

            int E = next.getTotalPointsFast() - current.getTotalPointsFast();

            if (E > 0) {
                current = next;
            } else {
                double p = ExperimentLocalSearch.rng.nextDouble();

                if (p < someFunction1(E, t)) {
                    current = next;
                }
            }
        }
        // return current; => unreachable
    }

    public static double someFunction1(int E, double t) {
        return Math.exp(E / t);
    }

    // =============================================================
    // Faster Versions (Only look at Top 7 from Leads)
    // =============================================================

    /**
     * Standard HillClimbing from Lecture SPS at Universitiy of Mannheim
     * 
     * in each Turn the Best Neighbor is choosen;
     * 
     * @param current
     * @return
     */
    public static TeamState hillClimbingFast(TeamState current) {

        TeamState currentState = current;

        statesCreated = 0; // reset statesCreated
        iterations = 0; // reset avgIterations
        pointsDevelopment = new ArrayList<Integer>();

        while (true) {

            List<TeamState> neighbors = currentState.createAllNeighborsFast();

            statesCreated += neighbors.size(); // add created states to total

            TeamState bestNeighbor = TeamState.getBestState(neighbors);

            int bestValue = bestNeighbor.getTotalPointsFast();
            pointsDevelopment.add(bestValue);

            if (bestValue <= currentState.getTotalPointsFast()) {
                break;
            }

            currentState = bestNeighbor;
            iterations++; // update Iterations
        }
        return currentState;
    }

    public static TeamState hillClimbingWithKStartsFast(TeamState current, int k) {
        List<TeamState> allBest = new ArrayList<TeamState>();

        statesCreated = 0; // reset statesCreated
        iterations = 0; // reset from previoud runs
        avgIterations = 0; // reset avgIterations
        int tempStatesCreated = 0;

        for (int i = 0; i < k; i++) {

            current.newRandomLineUp();

            allBest.add(hillClimbingCompleatlyFly(current));
            tempStatesCreated += statesCreated;
            avgIterations += iterations;

        }
        TeamState best = allBest.get(0);

        for (TeamState state : allBest) {
            if (state.getTotalPoints() > best.getTotalPoints()) {
                best = state;
            }
        }

        statesCreated = tempStatesCreated;
        avgIterations = avgIterations / (double) k; // calculate avgIterations
        return best;
    }

    public static TeamState beamSearchFast(TeamState teamState, int k) {

        statesCreated = 0; // reset statesCreated
        iterations = 0; // reset from previoud runs
        avgIterations = 0; // reset avgIterations

        List<TeamState> currentStates = teamState.createRandomStates(k);

        TeamState currentBest = TeamState.getBestState(currentStates);

        while (true) {
            iterations++;
            PriorityQueue<TeamState> maxHeap = new PriorityQueue<>(
                    (a, b) -> Integer.compare(b.getTotalPointsFast(), a.getTotalPointsFast()));
            for (TeamState c : currentStates) {

                List<TeamState> topNeigh = c.createTopKNeighborsFast(k);
                statesCreated += topNeigh.size();
                for (TeamState b : topNeigh) {
                    maxHeap.add(b);
                }

                // maxHeap.add(TeamState.getBestState(c.createAllNeighbors()));

            }
            List<TeamState> neighborsBest = new ArrayList<>();

            // Bestes wird zuerst hinzugefuegt
            TeamState best = maxHeap.poll();
            neighborsBest.add(best);

            for (int i = 1; i < k && !maxHeap.isEmpty(); i++) {
                neighborsBest.add(maxHeap.poll()); // bestes Element holen
            }

            if (currentBest.getTotalPointsFast() >= best.getTotalPointsFast()) {
                return currentBest;
            } else {
                currentStates = neighborsBest;
                // System.out.println(best.getTotalPointsFast());
                currentBest = best;
            }
        }
    }

    /*
     * DEPRECATED
     * public static TeamState hillClimbingWithKStarts(int k) {
     * List<TeamState> allBest = new ArrayList<TeamState>();
     * 
     * List<Swimmer> schwimmerListe =
     * CSVReader.createSwimmer("OptimalDMSAlgo/resources/SVM.csv");
     * 
     * SwimmingClub club = new SwimmingClub(schwimmerListe);
     * 
     * for (Swimmer schwimmer : club.getAllSwimmer()) {
     * schwimmer.updatePoints(); // update points for each swimmer based on their
     * times
     * }
     * 
     * club.generateLeaderboards();
     * 
     * for (int i = 0; i < k; i++) {
     * TeamState teamState;
     * do {
     * teamState = new TeamState(club, true);
     * } while (teamState.lineUp == null);
     * 
     * long start = System.nanoTime();
     * 
     * allBest.add(hillClimbing(teamState));
     * 
     * long end = System.nanoTime();
     * 
     * double seconds = (end - start) / 1_000_000_000.0;
     * 
     * System.out.println("Dauer HillClimb: " + seconds + " s");
     * 
     * }
     * TeamState best = allBest.get(0);
     * 
     * for (TeamState state : allBest) {
     * if (state.getTotalPoints() > best.getTotalPoints()) {
     * best = state;
     * }
     * }
     * return best;
     * }
     */

    /*
     * DEPRECATED
     * public static int hillClimbing(SwimmingClub club, boolean isMale) {
     * 
     * TeamState currentState = new TeamState(club, isMale); // initialize current
     * state with the given club
     * 
     * while (true) {
     * long start = System.nanoTime();
     * List<TeamState> neighbors = currentState.createAllNeighbors();
     * long end = System.nanoTime();
     * TeamState bestNeighbor = TeamState.getBestState(neighbors);
     * if (bestNeighbor.getTotalPointsFast() <= currentState.getTotalPointsFast()) {
     * break;
     * }
     * currentState = bestNeighbor;
     * }
     * 
     * return currentState.getTotalPoints(); // return the value of the final state
     * }
     */

}
