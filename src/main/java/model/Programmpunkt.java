package model;

public class Programmpunkt {
    private int id;
    private int wettkampfId;
    private int position;
    private ProgrammpunktTyp typ;

    public Programmpunkt() {}

    public Programmpunkt(int id, int wettkampfId, int position, ProgrammpunktTyp typ) {
        this.id = id;
        this.wettkampfId = wettkampfId;
        this.position = position;
        this.typ = typ;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getWettkampfId() { return wettkampfId; }
    public void setWettkampfId(int wettkampfId) { this.wettkampfId = wettkampfId; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public ProgrammpunktTyp getTyp() { return typ; }
    public void setTyp(ProgrammpunktTyp typ) { this.typ = typ; }
}
