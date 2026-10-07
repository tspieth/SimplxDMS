package database.model;

public class Disziplin {
    private int id;
    private SchwimmLage lage;
    private int distanz;

    public Disziplin() {}

    public Disziplin(int id, SchwimmLage lage, int distanz) {
        this.id = id;
        this.lage = lage;
        this.distanz = distanz;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public SchwimmLage getLage() { return lage; }
    public void setLage(SchwimmLage lage) { this.lage = lage; }

    public int getDistanz() { return distanz; }
    public void setDistanz(int distanz) { this.distanz = distanz; }

    @Override
    public String toString() { return distanz + "m " + lage; }
}
