package com.torretti.alarmsms;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Sends commands to the panel and waits for its SMS reply. Shared by all screens. */
public abstract class BaseSmsActivity extends Activity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    // Palette: set by applyTheme() before any view is built.
    static int GREEN, RED, ORANGE, BLUE;   // disinserito, totale, parziale, info
    static int GREY;                       // testo secondario
    static int BG, CARD, CARD_ACTIVE, BORDER, BORDER_STRONG, TEXT, SUBTEXT, NAV_BG, LINE;
    static boolean DARK = true;

    /** "dark", "light" or "auto" (follows the phone). */
    static boolean isDark(Context c) {
        String t = Prefs.get(c).getString(Prefs.KEY_THEME, "dark");
        if ("auto".equals(t)) {
            int night = c.getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            return night == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }
        return !"light".equals(t);
    }

    static void applyPalette(boolean dark) {
        DARK = dark;
        if (dark) {
            GREEN = 0xFF34D399; RED = 0xFFFF6B4A; ORANGE = 0xFFF5A524; BLUE = 0xFF4C8DFF;
            GREY = 0xFF8B95A3; BG = 0xFF0B0D10; CARD = 0xFF12151A; CARD_ACTIVE = 0xFF161A20;
            BORDER = 0xFF1F242C; BORDER_STRONG = 0xFF252B34; TEXT = 0xFFE8ECF1; SUBTEXT = 0xFFC9D1DB;
            NAV_BG = 0xFF0E1114; LINE = 0xFF1A1E25;
        } else {
            GREEN = 0xFF0E8F63; RED = 0xFFD9432B; ORANGE = 0xFFB46F00; BLUE = 0xFF2F6FE0;
            GREY = 0xFF5F6B7A; BG = 0xFFF4F6F8; CARD = 0xFFFFFFFF; CARD_ACTIVE = 0xFFFFFFFF;
            BORDER = 0xFFE1E5EA; BORDER_STRONG = 0xFFD3D9E0; TEXT = 0xFF12151A; SUBTEXT = 0xFF3A4350;
            NAV_BG = 0xFFFFFFFF; LINE = 0xFFE1E5EA;
        }
    }

    /** Call first thing in onCreate, before super.onCreate. */
    protected void applyTheme() {
        boolean dark = isDark(this);
        applyPalette(dark);
        setTheme(dark ? android.R.style.Theme_Material_NoActionBar
                      : android.R.style.Theme_Material_Light_NoActionBar);
    }

    /** Colours the system bars to match the page. */
    protected void styleSystemBars() {
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(DARK ? BG : NAV_BG);
        int flags = 0;
        if (!DARK) flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private static final String ACTION_SENT = "com.torretti.alarmsms.SMS_SENT";
    private static final long REPLY_TIMEOUT_MS = 3 * 60 * 1000;
    private static final int REQ_PERMS = 1;

    protected final Handler handler = new Handler(Looper.getMainLooper());
    protected TextView statusView;

    protected long waitingSince = 0;     // 0 = not waiting
    protected String pendingLabel = "";
    private Runnable pendingOnOk;

    private final Runnable timeout = () -> {
        if (waitingSince != 0) {
            waitingSince = 0;
            pendingOnOk = null;
            SharedPreferences p = Prefs.get(BaseSmsActivity.this);
            int misses = p.getInt(Prefs.KEY_MISSES, 0) + 1;
            p.edit().putInt(Prefs.KEY_MISSES, misses).apply();
            onWaitingChanged();
            setStatus(misses >= 2
                    ? "Nessuna risposta agli ultimi " + misses + " comandi: possibile CREDITO ESAURITO sulla SIM dell'allarme."
                    : "Nessuna risposta dalla centrale dopo 3 minuti.", ORANGE);
        }
    };

    private final BroadcastReceiver sentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            if (getResultCode() == Activity.RESULT_OK) {
                if (waitingSince != 0) setStatus(pendingLabel + ": SMS inviato, in attesa di risposta…", GREY);
            } else {
                waitingSince = 0;
                pendingOnOk = null;
                handler.removeCallbacks(timeout);
                onWaitingChanged();
                setStatus("Invio SMS non riuscito (codice " + getResultCode()
                        + "). Controlla credito e segnale del tuo telefono.", RED);
            }
        }
    };

    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void onCreate(Bundle b) {
        applyTheme();
        super.onCreate(b);
        IntentFilter f = new IntentFilter(ACTION_SENT);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(sentReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(sentReceiver, f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Prefs.get(this).registerOnSharedPreferenceChangeListener(this);
        checkReply();
        AppLock.require(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        AppLock.leftApp();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        AppLock.onResult(this, req, res);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Prefs.get(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(sentReceiver);
        handler.removeCallbacks(timeout);
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String key) {
        if (Prefs.KEY_REPLY_TIME.equals(key)) handler.post(this::checkReply);
    }

    // ---------------------------------------------------------------- sending

    /**
     * Asks for confirmation, then sends "<keyword> <pin> <cmd>".
     * onOk runs only if the panel's reply does not contain ERROR.
     */
    protected void confirmAndSend(String label, String question, String cmd, Runnable onOk) {
        if (Prefs.number(this).isEmpty() || Prefs.pin(this).isEmpty()) {
            toast("Imposta prima numero della centrale e PIN");
            return;
        }
        if (!hasPerm(Manifest.permission.SEND_SMS)) {
            askPermissions();
            toast("Serve il permesso per inviare SMS");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(label)
                .setMessage(question)
                .setPositiveButton("Invia", (d, w) -> send(label, cmd, onOk))
                .setNegativeButton("Annulla", null)
                .show();
    }

    private void send(String label, String cmd, Runnable onOk) {
        String body = Prefs.command(this, cmd);
        try {
            SmsManager sms = Build.VERSION.SDK_INT >= 31
                    ? getSystemService(SmsManager.class) : SmsManager.getDefault();
            Intent sent = new Intent(ACTION_SENT).setPackage(getPackageName());
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, sent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            pendingLabel = label;
            pendingOnOk = onOk;
            waitingSince = System.currentTimeMillis();
            handler.removeCallbacks(timeout);
            handler.postDelayed(timeout, REPLY_TIMEOUT_MS);
            setStatus(label + ": invio in corso…", GREY);
            onWaitingChanged();
            java.util.ArrayList<String> parts = sms.divideMessage(body);
            if (parts.size() > 1) {
                java.util.ArrayList<PendingIntent> pis = new java.util.ArrayList<>();
                for (int i = 0; i < parts.size(); i++) pis.add(i == parts.size() - 1 ? pi : null);
                sms.sendMultipartTextMessage(Prefs.number(this), null, parts, pis, null);
            } else {
                sms.sendTextMessage(Prefs.number(this), null, body, pi, null);
            }
        } catch (Exception e) {
            waitingSince = 0;
            pendingOnOk = null;
            onWaitingChanged();
            setStatus("Errore: " + e.getMessage(), RED);
        }
    }

    // ---------------------------------------------------------------- replies

    private void checkReply() {
        SharedPreferences p = Prefs.get(this);
        String text = p.getString(Prefs.KEY_REPLY_TEXT, null);
        long time = p.getLong(Prefs.KEY_REPLY_TIME, 0);
        if (text == null) { onReply(null, 0, false); return; }

        // The user/PIN list is kept, but only shown after asking the alarm PIN.
        if (text.contains("PIN?:") && time > p.getLong(Prefs.KEY_USERS_TIME, 0)) {
            p.edit().putString(Prefs.KEY_USERS_TEXT, text).putLong(Prefs.KEY_USERS_TIME, time).apply();
        }

        // A status reply ("MOD?:A") always updates the stored state.
        String mode = Panel.parseMode(text);
        if (mode != null && time > p.getLong(Prefs.KEY_STATE_TIME, 0)) {
            p.edit().putString(Prefs.KEY_STATE_MODE, mode).putLong(Prefs.KEY_STATE_TIME, time)
                    .putString(Prefs.KEY_STATE_SOURCE, "centrale").apply();
        }

        boolean answersCommand = waitingSince != 0 && time >= waitingSince;
        if (answersCommand) {
            waitingSince = 0;
            onWaitingChanged();
            handler.removeCallbacks(timeout);
            String up = text.toUpperCase();
            if (up.contains("ERROR")) {
                pendingOnOk = null;
                setStatus(pendingLabel + ": RIFIUTATO dalla centrale (" + Prefs.safe(this, text) + ")", RED);
            } else {
                Runnable r = pendingOnOk;
                pendingOnOk = null;
                if (r != null) r.run();
                setStatus(pendingLabel + ": " + (mode != null || text.contains("?:")
                        ? "risposta ricevuta ✓" : "CONFERMATO ✓"), GREEN);
            }
        }
        onReply(text, time, answersCommand);
    }

    /** Called whenever the "waiting for a reply" state changes. */
    protected void onWaitingChanged() {}

    protected boolean isWaiting() { return waitingSince != 0; }

    /** Called with the latest reply from the panel (text may be null if none yet). */
    protected abstract void onReply(String text, long time, boolean answersLastCommand);

    // ---------------------------------------------------------------- UI helpers

    protected void setStatus(String s, int color) {
        if (statusView == null) return;
        statusView.setText(s);
        statusView.setTextColor(color);
    }

    protected TextView makeStatusView() {
        statusView = new TextView(this);
        statusView.setTextSize(14);
        statusView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        statusView.setText("Pronto");
        statusView.setTextColor(GREY);
        statusView.setPadding(0, dp(14), 0, 0);
        return statusView;
    }

    protected Button bigButton(String text, int color, View.OnClickListener l) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(color == GREY ? TEXT : color);
        btn.setTextSize(15);
        btn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        btn.setAllCaps(false);
        btn.setStateListAnimator(null);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setStroke(dp(1), color == GREY ? BORDER : color);
        bg.setCornerRadius(dp(14));
        btn.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
        lp.topMargin = dp(10);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(l);
        return btn;
    }

    protected TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(GREY);
        t.setPadding(0, dp(14), 0, 0);
        return t;
    }

    protected TextView section(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(12);
        t.setLetterSpacing(0.12f);
        t.setAllCaps(true);
        t.setTextColor(GREY);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setPadding(0, dp(22), 0, 0);
        return t;
    }

    protected TextView box() {
        TextView v = new TextView(this);
        v.setTextSize(13);
        v.setTextColor(SUBTEXT);
        v.setTypeface(Typeface.MONOSPACE);
        v.setPadding(dp(16), dp(14), dp(16), dp(14));
        v.setTextIsSelectable(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setStroke(dp(1), BORDER);
        bg.setCornerRadius(dp(16));
        v.setBackground(bg);
        return v;
    }

    protected void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    protected int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    // ---------------------------------------------------------------- permissions

    protected boolean hasPerm(String p) {
        return checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED;
    }

    protected void askPermissions() {
        if (!hasPerm(Manifest.permission.SEND_SMS) || !hasPerm(Manifest.permission.RECEIVE_SMS)) {
            requestPermissions(new String[]{
                    Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS}, REQ_PERMS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        if (code == REQ_PERMS && !hasPerm(Manifest.permission.RECEIVE_SMS)) {
            toast("Senza il permesso di ricezione SMS l'app non può mostrare le risposte");
        }
    }
}
