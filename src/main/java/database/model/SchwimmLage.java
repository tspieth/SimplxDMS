package database.model;

public enum SchwimmLage {
    FREISTIL,
    BRUST,
    SCHMETTERLING,
    RUECKEN,
    LAGEN;

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
            return SchwimmLage.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e){
            throw new IllegalArgumentException("Unbekannter Lage Wert in Datenbank: " + e);
        }
    }


}
