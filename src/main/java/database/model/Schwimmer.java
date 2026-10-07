package database.model;

import java.time.LocalDate;

public class Schwimmer {
    private int id;
    private String name;
    private String vorname;
    private LocalDate geburtsdatum;
    private String geschlecht; // "m" oder "w"
    private int vereinId;

    public Schwimmer() {}

    public Schwimmer(int id, String name, String vorname, LocalDate geburtsdatum, String geschlecht, int vereinId) {
        this.id = id;
        this.name = name;
        this.vorname = vorname;
        this.geburtsdatum = geburtsdatum;
        this.geschlecht = geschlecht;
        this.vereinId = vereinId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVorname() { return vorname; }
    public void setVorname(String vorname) { this.vorname = vorname; }

    public LocalDate getGeburtsdatum() { return geburtsdatum; }
    public void setGeburtsdatum(LocalDate geburtsdatum) { this.geburtsdatum = geburtsdatum; }

    public String getGeschlecht() { return geschlecht; }
    public void setGeschlecht(String geschlecht) { this.geschlecht = geschlecht; }

    public int getVereinId() { return vereinId; }
    public void setVereinId(int vereinId) { this.vereinId = vereinId; }

    @Override
    public String toString() { return vorname + " " + name; }
}
