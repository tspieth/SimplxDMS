package database.model;

public class FinaReferenceTime {

    private int id;
    private int disziplin_id;
    private String geschlecht;
    private int fina_time;

    public FinaReferenceTime(int id, int disziplin_id, String geschlecht, int fina_time) {
        this.id = id;
        this.disziplin_id = disziplin_id;
        this.geschlecht = geschlecht;
        this.fina_time = fina_time;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getGeschlecht() {
        return geschlecht;
    }

    public void setGeschlecht(String geschlecht) {
        this.geschlecht = geschlecht;
    }

    public int getDisziplin_id() {
        return disziplin_id;
    }

    public void setDisziplin_id(int disziplin_id) {
        this.disziplin_id = disziplin_id;
    }

    public int getFina_time() {
        return fina_time;
    }

    public void setFina_time(int fina_time) {
        this.fina_time = fina_time;
    }
}
