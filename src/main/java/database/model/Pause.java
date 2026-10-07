package database.model;

public class Pause {
    private int id;
    private int programmpunktId;
    private int dauerSekunden;

    public Pause() {}

    public Pause(int id, int programmpunktId, int dauerSekunden) {
        this.id = id;
        this.programmpunktId = programmpunktId;
        this.dauerSekunden = dauerSekunden;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProgrammpunktId() { return programmpunktId; }
    public void setProgrammpunktId(int programmpunktId) { this.programmpunktId = programmpunktId; }

    public int getDauerSekunden() { return dauerSekunden; }
    public void setDauerSekunden(int dauerSekunden) { this.dauerSekunden = dauerSekunden; }
}
