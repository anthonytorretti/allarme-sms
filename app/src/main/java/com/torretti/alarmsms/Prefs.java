package com.torretti.alarmsms;

import android.content.Context;
import android.content.SharedPreferences;

/** Settings and last-reply storage shared by the activity and the SMS receiver. */
final class Prefs {
    static final String FILE = "alarm_sms";
    static final String KEY_NUMBER = "number";
    static final String KEY_PIN = "pin";
    static final String KEY_REPLY_TEXT = "reply_text";
    static final String KEY_REPLY_TIME = "reply_time";
    static final String KEY_STATE_MODE = "state_mode";
    static final String KEY_STATE_TIME = "state_time";

    /** Keyword expected by the panel in front of the PIN. */
    static final String KEYWORD = "PROG";

    private Prefs() {}

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
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
