package com.torretti.alarmsms;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends BaseSmsActivity {

    private EditText numberField, pinField;
    private TextView replyView, stateView;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
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

        root.addView(bigButton("Inserisci TOTALE", RED, v ->
                confirmAndSend("Inserimento totale", "Inserire l'allarme in modo totale?", "MODE:A", () -> saveState("A"))));
        root.addView(bigButton("Inserisci PARZIALE (casa)", ORANGE, v ->
                confirmAndSend("Inserimento parziale", "Inserire l'allarme in modo parziale?", "MODE:H", () -> saveState("H"))));
        root.addView(bigButton("DISINSERISCI", GREEN, v ->
                confirmAndSend("Disinserimento", "Disinserire l'allarme?", "MODE:D", () -> saveState("D"))));
        root.addView(bigButton("Leggi stato", BLUE, v ->
                confirmAndSend("Lettura stato", "Chiedere lo stato alla centrale?", "MOD?:", null)));

        root.addView(makeStatusView());

        root.addView(label("Ultima risposta della centrale"));
        replyView = box();
        root.addView(replyView);

        root.addView(section("Impostazioni"));
        root.addView(label("Numero della centrale"));
        numberField = new EditText(this);
        numberField.setInputType(InputType.TYPE_CLASS_PHONE);
        numberField.setHint("es. +39 347 1234567");
        root.addView(numberField);

        root.addView(label("Codice PIN (utente 1)"));
        pinField = new EditText(this);
        pinField.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pinField.setHint("PIN utente");
        root.addView(pinField);

        Button save = new Button(this);
        save.setText("Salva impostazioni");
        save.setOnClickListener(v -> { saveSettings(); toast("Impostazioni salvate"); });
        root.addView(save);

        Button adv = new Button(this);
        adv.setText("Funzioni avanzate (utenti, sensori, report…)");
        adv.setOnClickListener(v -> { saveSettings(); startActivity(new Intent(this, AdvancedActivity.class)); });
        root.addView(adv);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);

        askPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences p = Prefs.get(this);
        numberField.setText(p.getString(Prefs.KEY_NUMBER, ""));
        pinField.setText(p.getString(Prefs.KEY_PIN, ""));
        showState();
    }

    private void saveSettings() {
        Prefs.get(this).edit()
                .putString(Prefs.KEY_NUMBER, numberField.getText().toString().trim())
                .putString(Prefs.KEY_PIN, pinField.getText().toString().trim())
                .apply();
    }

    @Override
    protected void confirmAndSend(String label, String question, String cmd, Runnable onOk) {
        saveSettings();
        super.confirmAndSend(label, question, cmd, onOk);
    }

    private void saveState(String mode) {
        Prefs.get(this).edit()
                .putString(Prefs.KEY_STATE_MODE, mode)
                .putLong(Prefs.KEY_STATE_TIME, System.currentTimeMillis())
                .apply();
    }

    @Override
    protected void onReply(String text, long time, boolean answersLastCommand) {
        if (text == null) {
            replyView.setText("Nessuna risposta ricevuta");
        } else {
            replyView.setText(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
                    .format(new Date(time)) + "\n" + text);
        }
        showState();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String key) {
        super.onSharedPreferenceChanged(p, key);
        if (Prefs.KEY_STATE_TIME.equals(key)) handler.post(this::showState);
    }

    private void showState() {
        if (stateView == null) return;
        SharedPreferences p = Prefs.get(this);
        String mode = p.getString(Prefs.KEY_STATE_MODE, "");
        long time = p.getLong(Prefs.KEY_STATE_TIME, 0);
        String name = Panel.modeName(mode);
        int color = "A".equals(mode) ? RED : "H".equals(mode) ? ORANGE : "D".equals(mode) ? GREEN : GREY;
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(10));
        if (name == null) {
            stateView.setText("Stato allarme: sconosciuto (premi Leggi stato)");
            stateView.setTextColor(GREY);
            bg.setColor(Color.parseColor("#F1F1F1"));
        } else {
            stateView.setText("Stato allarme:\n" + name + "\n"
                    + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(time)));
            stateView.setTextColor(Color.WHITE);
            bg.setColor(color);
        }
        stateView.setBackground(bg);
    }
}
