package model;

import java.time.LocalTime;

public class Start {
    private int id;
    private int laufId;
    private int mannschaftId;
    private int schwimmerId;
    private LocalTime erzielteZeit;
    private Double punkte;

    public Start() {}

    public Start(int id, int laufId, int mannschaftId, int schwimmerId, LocalTime erzielteZeit, Double punkte) {
        this.id = id;
        this.laufId = laufId;
        this.mannschaftId = mannschaftId;
        this.schwimmerId = schwimmerId;
        this.erzielteZeit = erzielteZeit;
        this.punkte = punkte;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getLaufId() { return laufId; }
    public void setLaufId(int laufId) { this.laufId = laufId; }

    public int getMannschaftId() { return mannschaftId; }
    public void setMannschaftId(int mannschaftId) { this.mannschaftId = mannschaftId; }

    public int getSchwimmerId() { return schwimmerId; }
    public void setSchwimmerId(int schwimmerId) { this.schwimmerId = schwimmerId; }

    public LocalTime getErzielteZeit() { return erzielteZeit; }
    public void setErzielteZeit(LocalTime erzielteZeit) { this.erzielteZeit = erzielteZeit; }

    public Double getPunkte() { return punkte; }
    public void setPunkte(Double punkte) { this.punkte = punkte; }
}
