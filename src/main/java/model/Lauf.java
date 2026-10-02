package model;

public class Lauf {
    private int id;
    private int programmpunktId;
    private int disziplinId;

    public Lauf() {}

    public Lauf(int id, int programmpunktId, int disziplinId) {
        this.id = id;
        this.programmpunktId = programmpunktId;
        this.disziplinId = disziplinId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProgrammpunktId() { return programmpunktId; }
    public void setProgrammpunktId(int programmpunktId) { this.programmpunktId = programmpunktId; }

    public int getDisziplinId() { return disziplinId; }
    public void setDisziplinId(int disziplinId) { this.disziplinId = disziplinId; }
}
