package database.model;

public class Verein {
    private int id;
    private String name;
    private String farbe = "#1E90FF";

    public Verein() {}

    public Verein(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Creates a club model with its persisted display color.
     *
     * @param id club database ID
     * @param name club name
     * @param farbe six-digit hex color
     */
    public Verein(int id, String name, String farbe) {
        this(id, name);
        this.farbe = farbe;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    /**
     * Returns the club's UI color.
     *
     * @return color as a hex string
     */
    public String getFarbe() { return farbe; }

    /**
     * Sets the club's UI color.
     *
     * @param farbe color as a hex string
     */
    public void setFarbe(String farbe) { this.farbe = farbe; }

    @Override
    public String toString() { return name; } // wichtig fuer Anzeige in ListView/ComboBox
}
