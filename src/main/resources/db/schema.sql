-- ============================================================
-- SimplxDMS Datenbankschema
-- Wird bei jedem App-Start automatisch ausgeführt (CREATE TABLE IF NOT EXISTS)
-- ============================================================

CREATE TABLE IF NOT EXISTS verein (
    id   INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    farbe VARCHAR(7) NOT NULL DEFAULT '#1E90FF'
);

CREATE TABLE IF NOT EXISTS schwimmer (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    vorname       VARCHAR(100) NOT NULL,
    geburtsdatum  DATE,
    geschlecht    VARCHAR(1) NOT NULL CHECK (geschlecht IN ('m', 'w')),
    verein_id     INT NOT NULL,
    FOREIGN KEY (verein_id) REFERENCES verein(id)
);

CREATE TABLE IF NOT EXISTS disziplin (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    lage     VARCHAR(50) NOT NULL,
    distanz  INT NOT NULL,

    -- TO MAKE IT IMPOSSIBLE TO HAVE DOUBLES 2x (FREISTIL, 100)
    CONSTRAINT uq_disziplin_lage_distanz UNIQUE (lage, distanz)
);

-- Table for FINA WR-Times for Genders
CREATE TABLE IF NOT EXISTS fina_referenzzeit (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    disziplin_id INT NOT NULL,
    geschlecht VARCHAR(30) NOT NULL, -- Indicates the Gender MAENNLICH || WEIBLICH
    fina_time DOUBLE NOT NULL DEFAULT 0.0,

    -- TO MAKE IT IMPOSSIBLE TO HAVE DOUBLES
    CONSTRAINT uq_disziplin_kombination UNIQUE (disziplin_id, geschlecht),
    FOREIGN KEY (disziplin_id) REFERENCES disziplin(id)
);

-- Bekannte/gemeldete Zeiten eines Schwimmers, unabhängig vom Wettkampf
CREATE TABLE IF NOT EXISTS leistung (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    schwimmer_id INT NOT NULL,
    disziplin_id INT NOT NULL,
    zeit         TIME(3) NOT NULL,
    datum        DATE,
    FOREIGN KEY (schwimmer_id) REFERENCES schwimmer(id),
    FOREIGN KEY (disziplin_id) REFERENCES disziplin(id)
);

-- Upgrade existing installations without changing already stored club colors.
ALTER TABLE verein ADD COLUMN IF NOT EXISTS farbe VARCHAR(7) NOT NULL DEFAULT '#1E90FF';
ALTER TABLE leistung ALTER COLUMN zeit TIME(3);

CREATE TABLE IF NOT EXISTS wettkampf (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    name   VARCHAR(150) NOT NULL UNIQUE,
    datum  DATE
);

-- Gemeinsame Reihenfolge für Läufe UND Pausen innerhalb eines Wettkampfs
CREATE TABLE IF NOT EXISTS programmpunkt (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    wettkampf_id  INT NOT NULL,
    position      INT NOT NULL,
    typ           VARCHAR(10) NOT NULL CHECK (typ IN ('LAUF', 'PAUSE')),
    FOREIGN KEY (wettkampf_id) REFERENCES wettkampf(id) ON DELETE CASCADE,
    UNIQUE (wettkampf_id, position)
);

CREATE TABLE IF NOT EXISTS lauf (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    programmpunkt_id   INT NOT NULL UNIQUE,
    disziplin_id       INT NOT NULL,
    geschlecht         VARCHAR(10) NOT NULL CHECK (geschlecht IN ('MAENNLICH', 'WEIBLICH')),
    wettkampf_nr       INT NOT NULL,
    abschnitt          INT NOT NULL,
    FOREIGN KEY (programmpunkt_id) REFERENCES programmpunkt(id) ON DELETE CASCADE,
    FOREIGN KEY (disziplin_id) REFERENCES disziplin(id)
);

-- Upgrade databases created before runs stored their gender and schedule numbers.
ALTER TABLE lauf ADD COLUMN IF NOT EXISTS geschlecht VARCHAR(10);
ALTER TABLE lauf ADD COLUMN IF NOT EXISTS wettkampf_nr INT;
ALTER TABLE lauf ADD COLUMN IF NOT EXISTS abschnitt INT;

CREATE TABLE IF NOT EXISTS pause (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    programmpunkt_id   INT NOT NULL UNIQUE,
    dauer              INT NOT NULL,
    FOREIGN KEY (programmpunkt_id) REFERENCES programmpunkt(id) ON DELETE CASCADE
);

-- Eine DMS-Mannschaft eines Vereins bei einem bestimmten Wettkampf
CREATE TABLE IF NOT EXISTS mannschaft (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    verein_id     INT NOT NULL,
    wettkampf_id  INT NOT NULL,
    bezeichnung   VARCHAR(50) NOT NULL,
    FOREIGN KEY (verein_id) REFERENCES verein(id),
    FOREIGN KEY (wettkampf_id) REFERENCES wettkampf(id) ON DELETE CASCADE
);

-- Wer schwimmt tatsächlich in diesem Lauf, für welche Mannschaft, mit welchem Ergebnis
CREATE TABLE IF NOT EXISTS start (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    lauf_id         INT NOT NULL,
    mannschaft_id   INT NOT NULL,
    schwimmer_id    INT NOT NULL,
    erzielte_zeit   TIME,
    punkte          DOUBLE,
    FOREIGN KEY (lauf_id) REFERENCES lauf(id) ON DELETE CASCADE,
    FOREIGN KEY (mannschaft_id) REFERENCES mannschaft(id) ON DELETE CASCADE,
    FOREIGN KEY (schwimmer_id) REFERENCES schwimmer(id),
    CONSTRAINT uq_start_lauf_mannschaft UNIQUE (lauf_id, mannschaft_id)
);
