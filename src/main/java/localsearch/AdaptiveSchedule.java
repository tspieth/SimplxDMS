package localsearch;

public class AdaptiveSchedule implements Schedule {

    private double startTemp;
    private final double minTemp;
    private final int maxRounds;

    public AdaptiveSchedule(double startTemp, double minTemp, int maxRounds) {
        this.startTemp = startTemp;
        this.minTemp = minTemp;
        this.maxRounds = maxRounds;
    }

    @Override
    public double getTemperature(int round) {
        if (round >= maxRounds)
            return 0;

        // progress: 0 am Start, 1 am Ende
        double progress = (double) round / maxRounds;

        // α linear von 0.9 → 0.999
        double alpha = 0.9 + 0.0099 * progress;

        // aktuelle Temperatur
        return Math.max(minTemp, startTemp * Math.pow(alpha, round));
    }

    public void reset(double startTem) {
        this.startTemp = startTem;
    }
}
