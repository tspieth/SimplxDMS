package model;

public class Disziplin {
    private int id;
    private String lage;
    private int distanz;

    public Disziplin() {}

    public Disziplin(int id, String lage, int distanz) {
        this.id = id;
        this.lage = lage;
        this.distanz = distanz;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLage() { return lage; }
    public void setLage(String lage) { this.lage = lage; }

    public int getDistanz() { return distanz; }
    public void setDistanz(int distanz) { this.distanz = distanz; }

    @Override
    public String toString() { return distanz + "m " + lage; }
}
