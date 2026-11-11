package com.torretti.alarmsms;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SMS protocol of the Urmet 1059/012 (Climax "Vesta EZ Home" platform), as used by the
 * original My1059 app: every command is "<keyword> <pin> <CMD>:<params>".
 */
final class Panel {
    private Panel() {}

    // ---- labels / values taken from the original app ----
    static final String[] DELAY_LABELS = {"0 sec", "10 sec", "20 sec", "30 sec", "40 sec", "50 sec", "60 sec", "70 sec"};
    static final String[] DELAY_VALUES = {"0", "10", "20", "30", "40", "50", "60", "70"};
    static final String[] VOLUME_LABELS = {"Disattivato", "Basso", "Alto"};          // sent as index
    static final String[] JAMMING_LABELS = {"Disattivato", "Attivo"};               // sent as index
    static final String[] LENGTH_LABELS = new String[15];
    static final String[] LENGTH_VALUES = new String[15];
    static {
        for (int i = 0; i < 15; i++) {
            LENGTH_VALUES[i] = String.valueOf(i + 1);
            LENGTH_LABELS[i] = (i + 1) + " min";
        }
    }

    static final String[] TEMP_LABELS = new String[62];   // "Disattivato", 50 … -10 °C
    static final String[] TEMP_VALUES = new String[62];   // "0", "050" … "000", "101" … "110"
    static {
        TEMP_LABELS[0] = "Disattivato";
        TEMP_VALUES[0] = "0";
        int i = 1;
        for (int t = 50; t >= -10; t--, i++) {
            TEMP_LABELS[i] = t + " °C";
            TEMP_VALUES[i] = tempCode(t);
        }
    }
    /** Panel temperature encoding: 0..50 -> "000".."050", -1..-10 -> "101".."110". */
    static String tempCode(int t) {
        return t >= 0 ? String.format(Locale.ROOT, "%03d", t) : String.valueOf(100 - t);
    }
    static String tempLabel(String code) {
        try {
            int v = Integer.parseInt(code);
            return (v > 100 ? -(v - 100) : v) + " °C";
        } catch (NumberFormatException e) { return code; }
    }

    static final String[] REPORT_TYPE_LABELS = {"SMS", "Chiamata vocale", "CID", "CID + SMS"};
    static final String[] REPORT_TYPE_VALUES = {"S", "V", "C", "D"};
    static final String[] REPORT_EVENT_LABELS = {"Solo allarmi", "Solo stato", "Tutti"};
    static final String[] REPORT_EVENT_VALUES = {"A", "S", "X"};

    static final String[] ZONES = new String[30];
    static { for (int i = 0; i < 30; i++) ZONES[i] = String.valueOf(i + 1); }
    static final String[] ATTRIBUTE_LABELS = {"Furto (istantaneo)", "Escluso in parziale", "Ingresso (ritardato)"};

    static final String[] SIREN_LABELS = {"Manomissione", "Conferma", "Ingresso/Uscita", "Apprendimento sirena"};
    static final String[] SIREN_VALUES = {"T", "C", "E", "L"};
    static final String[] ONOFF_LABELS = {"Disattivato", "Attivato"};               // sent as index

    static final String[] PSS_CHANNELS = {"1", "2", "3", "4"};
    static final String[] PSS_ACTION_LABELS = {"Accendi", "Spegni", "Apprendimento"};
    static final String[] PSS_ACTION_VALUES = {"1", "0", "L"};

    static final String[] RULE_LABELS = {
            "Quando si inserisce TOTALE", "Quando si inserisce PARZIALE", "Quando si disinserisce",
            "Se la temperatura supera…", "Se la temperatura scende sotto…",
            "A un orario…", "Quando c'è un allarme…"};
    static final String[] RULE_LETTERS = {"A", "H", "D", "G", "L", "T", "E"};
    static final String[] SWITCH_LABELS = {"Spegni", "Accendi"};                    // sent as index
    static final String[] DURATION_LABELS = {"Sempre", "5 min", "10 min", "15 min", "20 min", "25 min",
            "30 min", "35 min", "40 min", "45 min", "50 min", "55 min", "1 h", "2 h", "3 h", "4 h", "5 h",
            "6 h", "7 h", "8 h", "9 h", "10 h", "11 h", "12 h", "13 h", "14 h", "15 h", "16 h", "17 h",
            "18 h", "19 h", "20 h", "21 h", "22 h", "23 h", "24 h"};
    static final String[] DURATION_VALUES = {"0", "5", "10", "15", "20", "25", "30", "35", "40", "45",
            "50", "55", "60", "120", "180", "240", "300", "360", "420", "480", "540", "600", "660", "720",
            "780", "840", "900", "960", "1020", "1080", "1140", "1200", "1260", "1320", "1380", "1440"};
    static final String[] EVENT_LABELS = {"Emergenza", "Panico", "Allagamento", "Incendio", "Furto"};
    static final int[] EVENT_BITS = {1, 2, 4, 8, 16};

    // ---- reply parsing ----

    /** Returns "A", "H" or "D" if the text is a MOD?: reply, else null. */
    static String parseMode(String text) {
        Matcher m = Pattern.compile("MOD\\?:\\s*([AHD])").matcher(text.toUpperCase(Locale.ROOT));
        return m.find() ? m.group(1) : null;
    }

    static String modeName(String m) {
        if ("A".equals(m)) return "INSERITO TOTALE";
        if ("H".equals(m)) return "INSERITO PARZIALE";
        if ("D".equals(m)) return "DISINSERITO";
        return null;
    }

    /** Human readable version of a query reply, or null if not recognised. */
    static String describe(String raw) {
        String text = raw.replace("\r", "").replace("\n", "").replaceAll("\\s", "");
        int q = text.indexOf("?:");
        if (q < 3) return null;
        String cmd = text.substring(q - 3, q).toUpperCase(Locale.ROOT);
        String body = text.substring(q + 2);
        StringBuilder sb = new StringBuilder();
        switch (cmd) {
            case "MOD": {
                String n = modeName(body.isEmpty() ? "" : body.substring(0, 1).toUpperCase(Locale.ROOT));
                return n == null ? null : "Stato: " + n;
            }
            case "PIN": {
                sb.append("Utenti e codici:\n");
                for (String g : groups(body)) {
                    String[] f = g.split(",", -1);
                    if (f.length == 0) continue;
                    if ("K".equalsIgnoreCase(f[0])) sb.append("• Parola chiave SMS: ").append(f.length > 1 ? f[1] : "").append('\n');
                    else if ("C".equalsIgnoreCase(f[0])) sb.append("• Codice SMS: ").append(f.length > 1 ? f[1] : "").append('\n');
                    else if (f.length < 2 || f[1].isEmpty()) sb.append("• Utente ").append(f[0]).append(": (vuoto)\n");
                    else sb.append("• Utente ").append(f[0]).append(": ")
                                .append(f.length > 2 && !f[2].isEmpty() ? f[2] : "senza nome")
                                .append(" – PIN ").append(f[1]).append('\n');
                }
                return sb.toString().trim();
            }
            case "HAR": {
                sb.append("Regole di automazione:\n");
                for (String g : groups(body)) {
                    String[] f = g.split(",", -1);
                    if (f.length < 2) continue;
                    int idx = parseInt(f[0]) - 1;
                    String name = idx >= 0 && idx < RULE_LABELS.length ? RULE_LABELS[idx] : "Regola " + f[0];
                    if ("0".equals(f[1])) { sb.append("• ").append(f[0]).append(". ").append(name).append(": disattivata\n"); continue; }
                    sb.append("• ").append(f[0]).append(". ").append(name);
                    String param = f.length > 2 ? f[2] : "";
                    if ("G".equalsIgnoreCase(f[1]) || "L".equalsIgnoreCase(f[1])) sb.append(" ").append(tempLabel(param));
                    else if ("T".equalsIgnoreCase(f[1]) && param.length() == 4) sb.append(" ").append(param, 0, 2).append(':').append(param.substring(2));
                    if (f.length > 6) {
                        sb.append(" → presa ").append(f[4]).append(' ')
                          .append("1".equals(f[5]) ? "accesa" : "spenta");
                        int d = indexOf(DURATION_VALUES, f[6]);
                        if (d > 0) sb.append(" per ").append(DURATION_LABELS[d]);
                    }
                    sb.append('\n');
                }
                return sb.toString().trim();
            }
            case "CFG": {
                String[] f = body.split(",");
                if (f.length < 7) return null;
                return "Impostazioni allarme:\n• Ritardo uscita: " + f[0] + " s (volume " + label(VOLUME_LABELS, f[1]) + ")"
                        + "\n• Ritardo ingresso: " + f[2] + " s (volume " + label(VOLUME_LABELS, f[3]) + ")"
                        + "\n• Durata allarme: " + f[4] + " min"
                        + "\n• Volume campanello: " + label(VOLUME_LABELS, f[5])
                        + "\n• Antidisturbo radio: " + label(JAMMING_LABELS, f[6]);
            }
            case "HAT": {
                String[] f = body.split(",");
                if (f.length < 4) return null;
                return "Allarmi temperatura:\n• Alta: " + ("1".equals(f[0]) ? tempLabel(f[1]) : "disattivato")
                        + "\n• Bassa: " + ("1".equals(f[2]) ? tempLabel(f[3]) : "disattivato");
            }
            default:
                return null;
        }
    }

    private static java.util.List<String> groups(String body) {
        java.util.List<String> out = new java.util.ArrayList<>();
        Matcher m = Pattern.compile("\\(([^)]*)\\)").matcher(body);
        while (m.find()) out.add(m.group(1));
        return out;
    }
    private static String label(String[] labels, String idx) {
        int i = parseInt(idx);
        return i >= 0 && i < labels.length ? labels[i] : idx;
    }
    static int indexOf(String[] a, String v) {
        for (int i = 0; i < a.length; i++) if (a[i].equalsIgnoreCase(v)) return i;
        return -1;
    }
    private static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return -1; }
    }
}
