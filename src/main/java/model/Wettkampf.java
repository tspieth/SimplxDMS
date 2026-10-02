package model;

import java.time.LocalDate;

public class Wettkampf {
    private int id;
    private String name;
    private LocalDate datum;

    public Wettkampf() {}

    public Wettkampf(int id, String name, LocalDate datum) {
        this.id = id;
        this.name = name;
        this.datum = datum;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getDatum() { return datum; }
    public void setDatum(LocalDate datum) { this.datum = datum; }

    @Override
    public String toString() { return name; }
}
