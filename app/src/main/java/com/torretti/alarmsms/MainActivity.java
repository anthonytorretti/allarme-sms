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
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String ACTION_SENT = "com.torretti.alarmsms.SMS_SENT";
    private static final long REPLY_TIMEOUT_MS = 3 * 60 * 1000;
    private static final int REQ_PERMS = 1;

    private static final int GREEN = Color.parseColor("#2E7D32");
    private static final int RED = Color.parseColor("#C62828");
    private static final int ORANGE = Color.parseColor("#EF6C00");
    private static final int GREY = Color.parseColor("#616161");

    private EditText numberField, pinField;
    private TextView statusView, replyView, stateView;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private long waitingSince = 0;   // 0 = not waiting for a reply
    private String pendingLabel = "";
    private String pendingMode = "";

    private final Runnable timeout = () -> {
        if (waitingSince != 0) {
            waitingSince = 0;
            setStatus("Nessuna risposta dalla centrale dopo 3 minuti", ORANGE);
        }
    };

    private final BroadcastReceiver sentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            if (getResultCode() == Activity.RESULT_OK) {
                setStatus(pendingLabel + ": SMS inviato, in attesa di risposta…", GREY);
            } else {
                waitingSince = 0;
                handler.removeCallbacks(timeout);
                setStatus("Invio SMS non riuscito (codice " + getResultCode()
                        + "). Controlla credito e segnale.", RED);
            }
        }
    };

    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();

        SharedPreferences p = Prefs.get(this);
        numberField.setText(p.getString(Prefs.KEY_NUMBER, ""));
        pinField.setText(p.getString(Prefs.KEY_PIN, ""));
        showLastReply();
        showState();

        IntentFilter f = new IntentFilter(ACTION_SENT);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(sentReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(sentReceiver, f);
        }
        askPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Prefs.get(this).registerOnSharedPreferenceChangeListener(this);
        showLastReply();
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

    // ---------------------------------------------------------------- UI

    private void buildUi() {
        int pad = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad + dp(16), pad, pad);

        TextView title = new TextView(this);
        title.setText("Allarme SMS");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        stateView = new TextView(this);
        stateView.setTextSize(17);
        stateView.setTypeface(Typeface.DEFAULT_BOLD);
        stateView.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(12);
        stateView.setLayoutParams(slp);
        root.addView(stateView);

        root.addView(label("Numero della centrale"));
        numberField = new EditText(this);
        numberField.setInputType(InputType.TYPE_CLASS_PHONE);
        numberField.setHint("es. +39 347 1234567");
        root.addView(numberField);

        root.addView(label("Codice PIN"));
        pinField = new EditText(this);
        pinField.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pinField.setHint("PIN utente");
        root.addView(pinField);

        Button save = new Button(this);
        save.setText("Salva impostazioni");
        save.setOnClickListener(v -> { saveSettings(); toast("Impostazioni salvate"); });
        root.addView(save);

        root.addView(spacer(dp(18)));
        root.addView(commandButton("Inserisci TOTALE", "Inserimento totale", "A", RED));
        root.addView(commandButton("Inserisci PARZIALE (casa)", "Inserimento parziale", "H", ORANGE));
        root.addView(commandButton("DISINSERISCI", "Disinserimento", "D", GREEN));
        root.addView(commandButton("LEGGI STATO", "Lettura stato", "?", Color.parseColor("#1565C0")));

        root.addView(spacer(dp(18)));
        statusView = new TextView(this);
        statusView.setTextSize(16);
        statusView.setTypeface(Typeface.DEFAULT_BOLD);
        statusView.setText("Pronto");
        statusView.setTextColor(GREY);
        root.addView(statusView);

        root.addView(label("Ultima risposta della centrale"));
        replyView = new TextView(this);
        replyView.setTextSize(15);
        replyView.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable box = new GradientDrawable();
        box.setColor(Color.parseColor("#F1F1F1"));
        box.setCornerRadius(dp(8));
        replyView.setBackground(box);
        root.addView(replyView);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    private Button commandButton(String text, String label, String mode, int color) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(18);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(10));
        btn.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64));
        lp.topMargin = dp(10);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(v -> confirmAndSend(label, mode));
        return btn;
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(GREY);
        t.setPadding(0, dp(14), 0, 0);
        return t;
    }

    private android.view.View spacer(int h) {
        android.view.View v = new android.view.View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, h));
        return v;
    }

    private void setStatus(String s, int color) {
        statusView.setText(s);
        statusView.setTextColor(color);
    }

    // ---------------------------------------------------------------- logic

    private void saveSettings() {
        Prefs.get(this).edit()
                .putString(Prefs.KEY_NUMBER, numberField.getText().toString().trim())
                .putString(Prefs.KEY_PIN, pinField.getText().toString().trim())
                .apply();
    }

    private void confirmAndSend(String label, String mode) {
        saveSettings();
        String number = numberField.getText().toString().trim();
        String pin = pinField.getText().toString().trim();
        if (number.isEmpty() || pin.isEmpty()) {
            toast("Inserisci numero e PIN");
            return;
        }
        if (!hasPerm(Manifest.permission.SEND_SMS)) {
            askPermissions();
            toast("Serve il permesso per inviare SMS");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(label)
                .setMessage("?".equals(mode) ? "Chiedere lo stato alla centrale?"
                        : "Inviare il comando alla centrale?")
                .setPositiveButton("Invia", (d, w) -> send(label, number, pin, mode))
                .setNegativeButton("Annulla", null)
                .show();
    }

    private void send(String label, String number, String pin, String mode) {
        String body = "?".equals(mode)
                ? Prefs.KEYWORD + " " + pin + " MOD?:"
                : Prefs.KEYWORD + " " + pin + " MODE:" + mode;
        try {
            SmsManager sms = Build.VERSION.SDK_INT >= 31
                    ? getSystemService(SmsManager.class)
                    : SmsManager.getDefault();
            Intent sent = new Intent(ACTION_SENT).setPackage(getPackageName());
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, sent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            pendingLabel = label;
            pendingMode = mode;
            waitingSince = System.currentTimeMillis();
            handler.removeCallbacks(timeout);
            handler.postDelayed(timeout, REPLY_TIMEOUT_MS);
            setStatus(label + ": invio in corso…", GREY);

            sms.sendTextMessage(number, null, body, pi, null);
        } catch (Exception e) {
            waitingSince = 0;
            setStatus("Errore: " + e.getMessage(), RED);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String key) {
        if (Prefs.KEY_REPLY_TIME.equals(key)) handler.post(this::showLastReply);
    }

    private void showLastReply() {
        SharedPreferences p = Prefs.get(this);
        String text = p.getString(Prefs.KEY_REPLY_TEXT, null);
        long time = p.getLong(Prefs.KEY_REPLY_TIME, 0);
        if (text == null) {
            replyView.setText("Nessuna risposta ricevuta");
            return;
        }
        replyView.setText(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
                .format(new Date(time)) + "\n" + text);

        // A status reply ("MOD?:A", "MOD?:H", "MOD?:D") gives the real state.
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("MOD\\?:\\s*([AHD])").matcher(text.toUpperCase());
        boolean isStatus = m.find();
        if (isStatus && time > p.getLong(Prefs.KEY_STATE_TIME, 0)) {
            p.edit().putString(Prefs.KEY_STATE_MODE, m.group(1))
                    .putLong(Prefs.KEY_STATE_TIME, time).apply();
            showState();
        }

        // A reply that arrived after the last command closes the wait.
        if (waitingSince != 0 && time >= waitingSince) {
            waitingSince = 0;
            handler.removeCallbacks(timeout);
            String up = text.toUpperCase();
            if (isStatus) {
                setStatus(pendingLabel + ": stato ricevuto ✓", GREEN);
            } else if (up.contains("ERROR")) {
                setStatus(pendingLabel + ": RIFIUTATO dalla centrale (" + text
                        + "). Possibile anomalia o PIN errato.", RED);
            } else if (up.contains("OK")) {
                setStatus(pendingLabel + ": CONFERMATO ✓", GREEN);
                Prefs.get(this).edit()
                        .putString(Prefs.KEY_STATE_MODE, pendingMode)
                        .putLong(Prefs.KEY_STATE_TIME, time)
                        .apply();
                showState();
            } else {
                setStatus(pendingLabel + ": risposta ricevuta", GREY);
            }
        }
    }

    /** Shows the last state the panel confirmed with OK (not a live reading). */
    private void showState() {
        SharedPreferences p = Prefs.get(this);
        String mode = p.getString(Prefs.KEY_STATE_MODE, "");
        long time = p.getLong(Prefs.KEY_STATE_TIME, 0);
        String name; int color;
        switch (mode) {
            case "A": name = "INSERITO TOTALE"; color = RED; break;
            case "H": name = "INSERITO PARZIALE"; color = ORANGE; break;
            case "D": name = "DISINSERITO"; color = GREEN; break;
            default:  name = null; color = GREY;
        }
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(10));
        if (name == null) {
            stateView.setText("Stato allarme: sconosciuto (premi LEGGI STATO)");
            stateView.setTextColor(GREY);
            bg.setColor(Color.parseColor("#F1F1F1"));
        } else {
            stateView.setText("Stato allarme:\n" + name + "\n"
                    + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                      .format(new Date(time)));
            stateView.setTextColor(Color.WHITE);
            bg.setColor(color);
        }
        stateView.setBackground(bg);
    }

    // ---------------------------------------------------------------- permissions

    private boolean hasPerm(String p) {
        return checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED;
    }

    private void askPermissions() {
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

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
