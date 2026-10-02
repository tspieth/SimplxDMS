package model;

public class Mannschaft {
    private int id;
    private int vereinId;
    private int wettkampfId;
    private String bezeichnung;

    public Mannschaft() {}

    public Mannschaft(int id, int vereinId, int wettkampfId, String bezeichnung) {
        this.id = id;
        this.vereinId = vereinId;
        this.wettkampfId = wettkampfId;
        this.bezeichnung = bezeichnung;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getVereinId() { return vereinId; }
    public void setVereinId(int vereinId) { this.vereinId = vereinId; }

    public int getWettkampfId() { return wettkampfId; }
    public void setWettkampfId(int wettkampfId) { this.wettkampfId = wettkampfId; }

    public String getBezeichnung() { return bezeichnung; }
    public void setBezeichnung(String bezeichnung) { this.bezeichnung = bezeichnung; }

    @Override
    public String toString() { return bezeichnung; }
}
