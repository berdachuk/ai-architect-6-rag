package com.berdachuk.docurag.core.util;

public final class PgVector {

    private PgVector() {
    }

    public static String toLiteral(float[] v) {
        if (v == null || v.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
