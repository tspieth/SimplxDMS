# SimplxDMS

Desktop-Programm zur Planung von Mannschaftsaufstellungen für den Deutschen Mannschaftswettbewerb Schwimmen (DMS). Es läuft unter Windows, Linux und macOS und braucht keine Java-Installation.

## 🎥 Videos auf YouTube

**Anleitungen zur Installation und zur Bedienung gibt es als Videos auf meinem YouTube-Kanal:**

👉 **[YouTube-Kanal ansehen](https://www.youtube.com/@SimplxDMS)**

Dort findest du:

- **Installationsvideos** für Windows, Linux und macOS, Schritt für Schritt vom Download bis zum ersten Start (in Bearbeitung)
- **Anwendungsvideos**, die zeigen, wie du  Vereine und Schwimmer/innen verwaltest, FINA-Referenzzeiten pflegst und Mannschaften exportierst

Wenn du das Programm zum ersten Mal benutzt, schau dir am besten zuerst das Installationsvideo für dein Betriebssystem an. (werden noch erstellt)

## Download

Die Installer liegen unter **[Releases](../../releases)**. Lade dort die Datei für dein Betriebssystem aus der neuesten Version herunter:

| Betriebssystem | Datei |
|---|---|
| Windows | `.msi` |
| Linux (Debian, Ubuntu) | `.deb` |
| macOS | `.dmg` |

## Installation in Kurzform

**Windows:** `.msi` doppelklicken und dem Assistenten folgen. Wenn Windows vor einem unbekannten Herausgeber warnt, wähle *Weitere Informationen* und dann *Trotzdem ausführen*. Der Installer ist nicht signiert, das ist normal für Projekte ohne Zertifikat.

**Linux:** Im Ordner mit der Datei im Terminal ausführen:

```bash
sudo apt install ./SimplxDMS_*.deb
```

Danach startest du das Programm über das Anwendungsmenü oder mit `/opt/simplxdms/bin/SimplxDMS`.

**macOS:** `.dmg` öffnen und das Programm in den Ordner *Programme* ziehen. (Funktionsweise noch nicht bestätigt, weil ich mir keinen Mac leisten kann...)

Das Programm bringt seine eigene Java-Laufzeit mit. Du musst nichts weiter installieren.

## Funktionen

- Wettkampfdateien (DSV7) einlesen und Wettkämpfe verwalten
- Vereine und Schwimmer/innen anlegen, bearbeiten und per CSV importieren oder exportieren
- FINA-Referenzzeiten bearbeiten
- Mannschaften anzeigen, Punkte berechnen und als CSV exportieren

## Wo werden meine Daten gespeichert?

Die Datenbank liegt im Benutzerverzeichnis und bleibt bei Updates erhalten:

| Betriebssystem | Ordner |
|---|---|
| Windows | `%APPDATA%\SimplxDMS` |
| macOS | `~/Library/Application Support/SimplxDMS` |
| Linux | `~/.simplxdms` |

## Selbst bauen

Voraussetzungen: JDK 21 und Maven.

```bash
mvn clean package
```

Die fertige JAR liegt danach in `target/`. Die Installer werden automatisch über GitHub Actions erzeugt, sobald ein neues Release angelegt wird.

## Fehler melden und Fragen

Für Fehler oder Wünsche lege bitte ein [Issue](../../issues) an oder schreib einen Kommentar unter das passende Video auf YouTube.

## Lizenz

Siehe Datei [LICENSE](LICENSE).
