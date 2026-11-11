package com.torretti.alarmsms;

import android.content.Context;
import android.content.SharedPreferences;

/** Settings and last-reply storage shared by the activities and the SMS receiver. */
final class Prefs {
    static final String FILE = "alarm_sms";
    static final String KEY_NUMBER = "number";
    static final String KEY_PIN = "pin";
    static final String KEY_KEYWORD = "keyword";
    static final String KEY_REPLY_TEXT = "reply_text";
    static final String KEY_REPLY_TIME = "reply_time";
    static final String KEY_STATE_MODE = "state_mode";
    static final String KEY_STATE_TIME = "state_time";
    /** "app" = last command sent by this app, "centrale" = read from the panel (MOD?:). */
    static final String KEY_STATE_SOURCE = "state_source";
    static final String KEY_USERS_TEXT = "users_text";
    /** Commands in a row that got no reply (hint that the alarm SIM is out of credit). */
    static final String KEY_MISSES = "misses";
    /** Estimated credit of the alarm SIM in cents (-1 = not set) and cost of one SMS. */
    static final String KEY_CREDIT = "credit_cents";
    static final String KEY_SMS_COST = "sms_cost_cents";
    static final String KEY_CREDIT_SET_TIME = "credit_set_time";
    static final String KEY_THEME = "theme";
    static final String VODAFONE_PACKAGE = "it.vodafone.my190";
    static final String KEY_USERS_TIME = "users_time";

    /** Factory/default keyword expected by the panel in front of the PIN. */
    static final String DEFAULT_KEYWORD = "PROG";

    private Prefs() {}

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static String number(Context c) { return get(c).getString(KEY_NUMBER, "").trim(); }
    static String pin(Context c) { return get(c).getString(KEY_PIN, "").trim(); }
    static String keyword(Context c) {
        String k = get(c).getString(KEY_KEYWORD, DEFAULT_KEYWORD).trim();
        return k.isEmpty() ? DEFAULT_KEYWORD : k;
    }

    /** Full SMS text: "<keyword> <pin> <command>". */
    static String command(Context c, String cmd) {
        return keyword(c) + " " + pin(c) + " " + cmd;
    }

    /** Text safe to show on screen: hides the PIN and the user/PIN list. */
    static String safe(Context c, String text) {
        if (text == null) return null;
        if (text.contains("PIN?:"))
            return "Elenco utenti e PIN ricevuto e salvato.\nPer vederlo: Avanzate → «Mostra utenti e PIN salvati».";
        String pin = pin(c);
        return pin.length() >= 4 ? text.replace(pin, "••••") : text;
    }

    /** Keeps only digits and compares the last 9, so "+39 347..." matches "347...". */
    static boolean sameNumber(String a, String b) {
        String x = a == null ? "" : a.replaceAll("\\D", "");
        String y = b == null ? "" : b.replaceAll("\\D", "");
        if (x.isEmpty() || y.isEmpty()) return false;
        int n = Math.min(9, Math.min(x.length(), y.length()));
        return x.substring(x.length() - n).equals(y.substring(y.length() - n));
    }
}
