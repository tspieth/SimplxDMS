package model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Leistung {
    private int id;
    private int schwimmerId;
    private int disziplinId;
    private LocalTime zeit;
    private LocalDate datum;

    public Leistung() {}

    public Leistung(int id, int schwimmerId, int disziplinId, LocalTime zeit, LocalDate datum) {
        this.id = id;
        this.schwimmerId = schwimmerId;
        this.disziplinId = disziplinId;
        this.zeit = zeit;
        this.datum = datum;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSchwimmerId() { return schwimmerId; }
    public void setSchwimmerId(int schwimmerId) { this.schwimmerId = schwimmerId; }

    public int getDisziplinId() { return disziplinId; }
    public void setDisziplinId(int disziplinId) { this.disziplinId = disziplinId; }

    public LocalTime getZeit() { return zeit; }
    public void setZeit(LocalTime zeit) { this.zeit = zeit; }

    public LocalDate getDatum() { return datum; }
    public void setDatum(LocalDate datum) { this.datum = datum; }
}
