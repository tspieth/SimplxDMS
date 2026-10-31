package service;

import database.model.SchwimmLage;

import java.util.Locale;

/** Shared German discipline label used in the competition grid and CSV exports. */
public final class WettkampfDisziplinText {
    private WettkampfDisziplinText() {
    }

    public static String format(int distanz, SchwimmLage lage) {
        return format(distanz, lage.getAnzeigename());
    }

    public static String format(int distanz, String lage) {
        return String.format(Locale.ROOT, "%d m %s", distanz, lage);
    }
}
