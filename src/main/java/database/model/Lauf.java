package database.model;

public class Lauf {
    private int id;
    private int programmpunktId;
    private int disziplinId;
    private Geschlecht geschlecht;
    private int wettkampfNummer;
    private int abschnitt;

    public Lauf() {}

    public Lauf(int id, int programmpunktId, int disziplinId) {
        this.id = id;
        this.programmpunktId = programmpunktId;
        this.disziplinId = disziplinId;
    }

    public Lauf(int id, int programmpunktId, int disziplinId, Geschlecht geschlecht,
                int wettkampfNummer, int abschnitt) {
        this(id, programmpunktId, disziplinId);
        this.geschlecht = geschlecht;
        this.wettkampfNummer = wettkampfNummer;
        this.abschnitt = abschnitt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProgrammpunktId() { return programmpunktId; }
    public void setProgrammpunktId(int programmpunktId) { this.programmpunktId = programmpunktId; }

    public int getDisziplinId() { return disziplinId; }
    public void setDisziplinId(int disziplinId) { this.disziplinId = disziplinId; }

    public Geschlecht getGeschlecht() { return geschlecht; }
    public void setGeschlecht(Geschlecht geschlecht) { this.geschlecht = geschlecht; }

    public int getWettkampfNummer() { return wettkampfNummer; }
    public void setWettkampfNummer(int wettkampfNummer) { this.wettkampfNummer = wettkampfNummer; }

    public int getAbschnitt() { return abschnitt; }
    public void setAbschnitt(int abschnitt) { this.abschnitt = abschnitt; }
}
