package com.berdachuk.docurag.evaluation.internal;

public final class TextNormalization {

    private TextNormalization() {
    }

    public static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase().replaceAll("\\s+", " ").trim();
    }
}
