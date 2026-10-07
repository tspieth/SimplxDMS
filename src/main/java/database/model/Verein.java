package database.model;

public class Verein {
    private int id;
    private String name;

    public Verein() {}

    public Verein(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() { return name; } // wichtig fuer Anzeige in ListView/ComboBox
}
