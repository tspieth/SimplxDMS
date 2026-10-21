package service;

import database.dao.DisziplinDao;
import database.dao.LaufDao;
import database.dao.PauseDao;
import database.dao.ProgrammpunktDao;
import database.dao.WettkampfDao;
import database.model.Disziplin;
import database.model.Lauf;
import database.model.Pause;
import database.model.Programmpunkt;
import database.model.ProgrammpunktTyp;
import database.model.Wettkampf;
import importer.DsvWettkampfParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Parses and atomically imports a DSV7 competition definition list.
 */
public class WettkampfImportService {
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    private final Connection connection;

    public WettkampfImportService(Connection connection) {
        this.connection = connection;
    }

    public boolean wettkampfNameExistiert(String name) throws SQLException {
        return new WettkampfDao(connection).existsByName(name);
    }

    public ImportErgebnis importiere(String name, byte[] content)
            throws IOException, importer.DsvParseException, SQLException {
        if (wettkampfNameExistiert(name)) {
            throw new SQLException("Ein Wettkampf mit diesem Namen existiert bereits: " + name);
        }
        DsvWettkampfParser.ParsedWettkampf parsed = parseWithEncodingFallback(content);
        boolean autoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            WettkampfDao wettkampfDao = new WettkampfDao(connection);
            if (wettkampfDao.existsByName(name)) {
                throw new SQLException("Ein Wettkampf mit diesem Namen existiert bereits: " + name);
            }

            Wettkampf wettkampf = new Wettkampf(0, name, parsed.datum());
            wettkampfDao.insert(wettkampf);
            ProgrammpunktDao programmpunktDao = new ProgrammpunktDao(connection);
            LaufDao laufDao = new LaufDao(connection);
            PauseDao pauseDao = new PauseDao(connection);
            DisziplinDao disziplinDao = new DisziplinDao(connection);
            int position = 0;
            for (DsvWettkampfParser.ProgrammpunktDaten item : parsed.programmpunkte()) {
                Programmpunkt punkt = new Programmpunkt(0, wettkampf.getId(), ++position,
                        item.typ() == DsvWettkampfParser.ProgrammpunktTyp.LAUF
                                ? ProgrammpunktTyp.LAUF : ProgrammpunktTyp.PAUSE);
                programmpunktDao.insert(punkt);
                if (item.typ() == DsvWettkampfParser.ProgrammpunktTyp.LAUF) {
                    Disziplin disziplin = disziplinDao.findByLageUndDistanz(item.lage(), item.distanz());
                    if (disziplin == null) {
                        disziplin = new Disziplin(0, item.lage(), item.distanz());
                        disziplinDao.insert(disziplin);
                    }
                    laufDao.insert(new Lauf(0, punkt.getId(), disziplin.getId(),
                            item.geschlecht(), item.wettkampfNummer(), item.abschnitt()));
                } else {
                    pauseDao.insert(new Pause(0, punkt.getId(), item.pauseSekunden()));
                }
            }
            connection.commit();
            return new ImportErgebnis(name, parsed.anzahlLaeufe(), parsed.anzahlPausen(),
                    parsed.uebersprungeneNichtEinzel());
        } catch (SQLException | RuntimeException e) {
            rollback(e);
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private DsvWettkampfParser.ParsedWettkampf parseWithEncodingFallback(byte[] bytes)
            throws IOException, importer.DsvParseException {
        try {
            String utf8 = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes)).toString();
            return parse(utf8);
        } catch (java.nio.charset.CharacterCodingException e) {
            return parse(new String(bytes, WINDOWS_1252));
        }
    }

    private DsvWettkampfParser.ParsedWettkampf parse(String content)
            throws IOException, importer.DsvParseException {
        try (Reader reader = new java.io.StringReader(content)) {
            return new DsvWettkampfParser().parse(reader);
        }
    }

    private void rollback(Exception cause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            cause.addSuppressed(rollbackError);
        }
    }

    public record ImportErgebnis(String wettkampfName, long anzahlLaeufe, long anzahlPausen,
                                 int uebersprungeneNichtEinzel) {
    }
}
