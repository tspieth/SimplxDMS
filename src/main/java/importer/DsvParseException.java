package importer;

/**
 * A malformed DSV7 record with its source line number.
 */
public class DsvParseException extends Exception {
    private final int lineNumber;

    public DsvParseException(int lineNumber, String message) {
        super(lineNumber > 0 ? "Zeile " + lineNumber + ": " + message : message);
        this.lineNumber = lineNumber;
    }

    public DsvParseException(int lineNumber, String message, Throwable cause) {
        super(lineNumber > 0 ? "Zeile " + lineNumber + ": " + message : message, cause);
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
