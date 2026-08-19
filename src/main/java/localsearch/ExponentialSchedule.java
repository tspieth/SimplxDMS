package localsearch;

public class ExponentialSchedule implements Schedule {
    private double temperature;
    private final double alpha;
    private final double minTemp;

    public ExponentialSchedule(double startTemp, double alpha, double minTemp) {
        this.temperature = startTemp;
        this.alpha = alpha;
        this.minTemp = minTemp;
    }

    public double getTemperature(int steps) {
        double currentTemperature = temperature * Math.exp(alpha * steps);
        return currentTemperature < minTemp ? 0 : temperature;
    }

    public void reset(double startTemp) {
        this.temperature = startTemp;
    }
}
