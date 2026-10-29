package com.example.incidents.util;

import java.util.Locale;

public final class EmailUtils {

    private EmailUtils() { }

    /** Les emails sont comparés sans tenir compte de la casse : on stocke toujours la forme normalisée. */
    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
