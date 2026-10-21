package database.model;

public enum SchwimmLage {
    FREISTIL("Freistil"),
    BRUST("Brust"),
    SCHMETTERLING("Schmetterling"),
    RUECKEN("Rücken"),
    LAGEN("Lagen");

    private final String anzeigename;

    SchwimmLage(String anzeigename) {
        this.anzeigename = anzeigename;
    }

    /**
     * Returns the German display name of the swimming stroke.
     *
     * @return localized stroke name
     */
    public String getAnzeigename() {
        return anzeigename;
    }

    /**
     * Uses the German display name when the enum is rendered as text.
     *
     * @return localized stroke name
     */
    @Override
    public String toString() {
        return anzeigename;
    }

    /**
     * Mapper method to Map Strings from the Database to enums
     * @param value
     * @return
     */
    public static SchwimmLage fromDbValue(String value){
        if(value == null || value.isBlank()){
            return null;
        }
        try{
            return SchwimmLage.valueOf(value.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e){
            throw new IllegalArgumentException("Unbekannter Lage Wert in Datenbank: " + e);
        }
    }


}
