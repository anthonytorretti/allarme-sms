package com.torretti.alarmsms;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends BaseSmsActivity {

    private RingView ring;
    private LinearLayout creditCard;
    private TextView creditValue, creditNote;
    private TextView badge, stateName, stateSub, stateNote, replyView, replyTime;
    private final LinearLayout[] cards = new LinearLayout[3];
    private static final String[] MODES = {"A", "H", "D"};

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        styleSystemBars();

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(16));

        // header
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView kicker = text("CENTRALE URMET 1059", 12, GREY, false);
        kicker.setLetterSpacing(0.12f);
        titles.addView(kicker);
        titles.addView(text("Allarme Rolando", 24, TEXT, true));
        header.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(iconButton(R.drawable.ic_settings, "Impostazioni", v -> openSettings()));
        root.addView(header);

        // status ring
        ring = new RingView(this);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(dp(210), dp(210));
        rp.gravity = Gravity.CENTER_HORIZONTAL;
        rp.topMargin = dp(16);
        root.addView(ring, rp);

        badge = text("", 11, GREY, true);
        badge.setLetterSpacing(0.14f);
        badge.setAllCaps(true);
        badge.setPadding(dp(10), dp(4), dp(10), dp(4));
        badge.setBackground(rounded(0, BORDER, 999));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.gravity = Gravity.CENTER_HORIZONTAL;
        bp.topMargin = dp(14);
        root.addView(badge, bp);

        stateName = centered(text("", 30, TEXT, true));
        stateName.setLetterSpacing(0.02f);
        root.addView(stateName);
        stateSub = centered(text("", 13, SUBTEXT, false));
        root.addView(stateSub);
        stateNote = centered(text("", 12, GREY, false));
        stateNote.setPadding(dp(16), dp(6), dp(16), 0);
        stateNote.setLineSpacing(0, 1.25f);
        root.addView(stateNote);

        // command cards
        LinearLayout grid = new LinearLayout(this);
        grid.setPadding(0, dp(20), 0, 0);
        String[][] defs = {
                {"Totale", "Esco di casa"},
                {"Parziale", "Sono in casa"},
                {"Disinserisci", "Spegni tutto"}};
        int[] icons = {R.drawable.ic_shield, R.drawable.ic_home, R.drawable.ic_unlock};
        int[] colors = {RED, ORANGE, GREEN};
        String[] questions = {"Inserire l'allarme in modo TOTALE?", "Inserire l'allarme in modo PARZIALE?", "DISINSERIRE l'allarme?"};
        for (int i = 0; i < 3; i++) {
            final int k = i;
            cards[i] = commandCard(defs[i][0], defs[i][1], icons[i], colors[i], v ->
                    confirmAndSend(defs[k][0], questions[k], "MODE:" + MODES[k], () -> saveState(MODES[k])));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(118), 1);
            if (i > 0) cp.leftMargin = dp(10);
            grid.addView(cards[i], cp);
        }
        root.addView(grid);

        // read state
        LinearLayout read = new LinearLayout(this);
        read.setGravity(Gravity.CENTER);
        read.setBackground(rounded(CARD, BORDER_STRONG, 14));
        read.setClickable(true);
        read.setFocusable(true);
        read.setContentDescription("Leggi stato dalla centrale");
        ImageView ri = new ImageView(this);
        ri.setImageDrawable(tinted(R.drawable.ic_refresh, SUBTEXT));
        read.addView(ri, new LinearLayout.LayoutParams(dp(18), dp(18)));
        TextView rt = text("  Leggi stato dalla centrale", 14, SUBTEXT, false);
        rt.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        read.addView(rt);
        read.setOnClickListener(v -> confirmAndSend("Lettura stato",
                "Chiedere lo stato attuale alla centrale?", "MOD?:", null));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        rlp.topMargin = dp(12);
        root.addView(read, rlp);

        root.addView(makeStatusView());

        // last reply card
        LinearLayout replyCard = new LinearLayout(this);
        replyCard.setOrientation(LinearLayout.VERTICAL);
        replyCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        replyCard.setBackground(rounded(CARD, BORDER, 16));
        LinearLayout rh = new LinearLayout(this);
        TextView rk = text("ULTIMA RISPOSTA", 11, GREY, false);
        rk.setLetterSpacing(0.12f);
        rh.addView(rk, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        replyTime = text("", 11, GREY, false);
        rh.addView(replyTime);
        replyCard.addView(rh);
        replyView = text("", 13, SUBTEXT, false);
        replyView.setTypeface(Typeface.MONOSPACE);
        replyView.setPadding(0, dp(8), 0, 0);
        replyView.setTextIsSelectable(true);
        replyCard.addView(replyView);
        LinearLayout.LayoutParams rcp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rcp.topMargin = dp(12);
        root.addView(replyCard, rcp);

        // credit card
        creditCard = new LinearLayout(this);
        creditCard.setOrientation(LinearLayout.VERTICAL);
        creditCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        creditCard.setBackground(rounded(CARD, BORDER, 16));
        creditCard.setClickable(true);
        creditCard.setFocusable(true);
        creditCard.setContentDescription("Credito SIM allarme");
        creditCard.setOnClickListener(v -> openCredit());
        LinearLayout ch = new LinearLayout(this);
        TextView ck = text("CREDITO SIM ALLARME", 11, GREY, false);
        ck.setLetterSpacing(0.12f);
        ch.addView(ck, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        ch.addView(text("Aggiorna ›", 11, BLUE, false));
        creditCard.addView(ch);
        creditValue = text("", 20, TEXT, true);
        creditValue.setPadding(0, dp(6), 0, 0);
        creditCard.addView(creditValue);
        creditNote = text("", 12, GREY, false);
        creditNote.setLineSpacing(0, 1.2f);
        creditCard.addView(creditNote);
        LinearLayout.LayoutParams ccp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ccp.topMargin = dp(12);
        root.addView(creditCard, ccp);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.addView(root);
        page.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        // bottom bar
        LinearLayout nav = new LinearLayout(this);
        nav.setBackgroundColor(NAV_BG);
        nav.setPadding(0, dp(10), 0, dp(14));
        nav.addView(navItem(R.drawable.ic_shield, "Allarme", true, null), navLp());
        nav.addView(navItem(R.drawable.ic_list, "Avanzate", false,
                v -> startActivity(new Intent(this, AdvancedActivity.class))), navLp());
        nav.addView(navItem(R.drawable.ic_settings, "Impostazioni", false, v -> openSettings()), navLp());
        View line = new View(this);
        line.setBackgroundColor(LINE);
        page.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        page.addView(nav);

        setContentView(page);
        askPermissions();
        if (Prefs.number(this).isEmpty() || Prefs.pin(this).isEmpty()) handler.post(this::openSettings);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showState();
        showCredit();
    }

    // ---------------------------------------------------------------- state

    private void saveState(String mode) {
        Prefs.get(this).edit()
                .putString(Prefs.KEY_STATE_MODE, mode)
                .putLong(Prefs.KEY_STATE_TIME, System.currentTimeMillis())
                .putString(Prefs.KEY_STATE_SOURCE, "app")
                .apply();
    }

    @Override
    protected void onWaitingChanged() { showState(); }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String key) {
        super.onSharedPreferenceChanged(p, key);
        if (Prefs.KEY_STATE_TIME.equals(key) || Prefs.KEY_STATE_SOURCE.equals(key)) handler.post(this::showState);
        if (Prefs.KEY_CREDIT.equals(key) || Prefs.KEY_MISSES.equals(key)) handler.post(this::showCredit);
    }

    @Override
    protected void onReply(String text, long time, boolean answersLastCommand) {
        if (replyView == null) return;
        if (text == null) {
            replyView.setText("Nessuna risposta ricevuta");
            replyTime.setText("");
        } else {
            replyView.setText(Prefs.safe(this, text));
            replyTime.setText(when(time));
        }
        showState();
    }

    private void showState() {
        if (ring == null) return;
        SharedPreferences p = Prefs.get(this);
        String mode = p.getString(Prefs.KEY_STATE_MODE, "");
        long time = p.getLong(Prefs.KEY_STATE_TIME, 0);
        boolean fromPanel = "centrale".equals(p.getString(Prefs.KEY_STATE_SOURCE, "app"));

        if (isWaiting()) {
            ring.setState(GREY, null, true);
            badge.setText("Invio in corso");
            stateName.setText("INVIO…");
            stateName.setTextColor(GREY);
            stateSub.setText("In attesa della risposta della centrale");
            stateNote.setText("");
            highlight(null);
            return;
        }

        int color; int icon; String name, sub;
        switch (mode) {
            case "A": color = RED; icon = R.drawable.ic_shield; name = "INSERITO"; sub = "Totale"; break;
            case "H": color = ORANGE; icon = R.drawable.ic_home; name = "PARZIALE"; sub = "Modalità casa"; break;
            case "D": color = GREEN; icon = R.drawable.ic_unlock; name = "DISINSERITO"; sub = "Nessuna zona attiva"; break;
            default:
                ring.setState(GREY, getDrawable(R.drawable.ic_refresh), false);
                badge.setText("Stato sconosciuto");
                stateName.setText("—");
                stateName.setTextColor(GREY);
                stateSub.setText("Nessun comando ancora inviato");
                stateNote.setText("Premi «Leggi stato dalla centrale» per conoscere lo stato attuale.");
                highlight(null);
                return;
        }
        ring.setState(color, getDrawable(icon), false);
        badge.setText(fromPanel ? "Letto dalla centrale" : "Ultimo comando dall'app");
        stateName.setText(name);
        stateName.setTextColor(color);
        stateSub.setText(sub + " · " + when(time));
        stateNote.setText(fromPanel
                ? "Stato confermato dalla centrale al momento della lettura."
                : "Non è lo stato in tempo reale: se qualcuno ha usato telecomando o tastiera potrebbe essere cambiato. Premi «Leggi stato» per verificarlo.");
        highlight(mode);
    }

    private void highlight(String mode) {
        int[] colors = {RED, ORANGE, GREEN};
        for (int i = 0; i < 3; i++) {
            boolean on = MODES[i].equals(mode);
            cards[i].setBackground(rounded(on ? CARD_ACTIVE : CARD, on ? colors[i] : BORDER, 18, on ? 2 : 1));
        }
    }

    private String when(long t) {
        if (t <= 0) return "";
        Calendar now = Calendar.getInstance(), c = Calendar.getInstance();
        c.setTimeInMillis(t);
        String hm = new SimpleDateFormat("HH:mm", Locale.ITALY).format(new Date(t));
        if (now.get(Calendar.YEAR) == c.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR))
            return "oggi " + hm;
        now.add(Calendar.DAY_OF_YEAR, -1);
        if (now.get(Calendar.YEAR) == c.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR))
            return "ieri " + hm;
        return new SimpleDateFormat("d MMM HH:mm", Locale.ITALY).format(new Date(t));
    }

    // ---------------------------------------------------------------- credit

    private void showCredit() {
        if (creditValue == null) return;
        SharedPreferences p = Prefs.get(this);
        long c = p.getLong(Prefs.KEY_CREDIT, -1);
        int misses = p.getInt(Prefs.KEY_MISSES, 0);
        if (c < 0) {
            creditValue.setText("Non impostato");
            creditValue.setTextColor(GREY);
        } else {
            creditValue.setText(String.format(Locale.ITALY, "≈ %.2f €  stimati", c / 100.0));
            creditValue.setTextColor(c < 100 ? ORANGE : TEXT);
        }
        String note;
        if (misses >= 2) {
            note = "La centrale non ha risposto agli ultimi " + misses + " comandi: possibile credito esaurito.";
            creditNote.setTextColor(RED);
        } else if (c < 0) {
            note = "Tocca per inserire il credito dopo una ricarica: l'app scala il costo di ogni SMS della centrale.";
            creditNote.setTextColor(GREY);
        } else {
            long t = p.getLong(Prefs.KEY_CREDIT_SET_TIME, 0);
            note = "Stima: non conta gli SMS di allarme e i costi del piano. Aggiornato da te "
                    + when(t) + ". Tocca per verificarlo su My Vodafone.";
            creditNote.setTextColor(GREY);
        }
        creditNote.setText(note);
    }

    private void openCredit() {
        SharedPreferences p = Prefs.get(this);
        long c = p.getLong(Prefs.KEY_CREDIT, -1);
        Form f = new Form(this)
                .note("Inserisci il credito reale della SIM dell'allarme (per esempio subito dopo una ricarica, "
                        + "o dopo averlo controllato su My Vodafone).")
                .text("eur", "Credito attuale (€)", c >= 0 ? String.format(Locale.ITALY, "%.2f", c / 100.0) : "", 8, false)
                .text("cost", "Costo di un SMS della centrale (€)",
                        String.format(Locale.ITALY, "%.2f", p.getInt(Prefs.KEY_SMS_COST, 29) / 100.0), 5, false);
        f.show("Credito SIM allarme", form -> {
            try {
                String e = form.text("eur").replace(',', '.').replace("€", "").trim();
                String k = form.text("cost").replace(',', '.').replace("€", "").trim();
                SharedPreferences.Editor ed = p.edit();
                if (e.isEmpty()) ed.putLong(Prefs.KEY_CREDIT, -1);
                else ed.putLong(Prefs.KEY_CREDIT, Math.round(Double.parseDouble(e) * 100));
                if (!k.isEmpty()) ed.putInt(Prefs.KEY_SMS_COST, (int) Math.round(Double.parseDouble(k) * 100));
                ed.putLong(Prefs.KEY_CREDIT_SET_TIME, System.currentTimeMillis()).putInt(Prefs.KEY_MISSES, 0).apply();
                showCredit();
                return null;
            } catch (NumberFormatException ex) {
                return "Importo non valido (es. 12,50)";
            }
        }, "Apri My Vodafone", this::openVodafone);
    }

    private void openVodafone() {
        Intent i = getPackageManager().getLaunchIntentForPackage(Prefs.VODAFONE_PACKAGE);
        if (i == null) {
            i = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=" + Prefs.VODAFONE_PACKAGE));
            toast("My Vodafone non è installata: si apre il Play Store");
        }
        try { startActivity(i); } catch (Exception e) { toast("Impossibile aprire My Vodafone"); }
    }

    // ---------------------------------------------------------------- settings

    private void openSettings() {
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.VERTICAL);
        f.setPadding(dp(22), dp(8), dp(22), 0);
        TextView l1 = text("Numero della SIM della centrale", 13, GREY, false);
        f.addView(l1);
        EditText num = new EditText(this);
        num.setInputType(InputType.TYPE_CLASS_PHONE);
        num.setHint("es. +39 347 1234567");
        num.setText(Prefs.number(this));
        f.addView(num);
        TextView l2 = text("PIN utente 1", 13, GREY, false);
        l2.setPadding(0, dp(12), 0, 0);
        f.addView(l2);
        EditText pin = new EditText(this);
        pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pin.setText(Prefs.pin(this));
        f.addView(pin);
        TextView l3 = text("Tema", 13, GREY, false);
        l3.setPadding(0, dp(12), 0, 0);
        f.addView(l3);
        String[] themeKeys = {"dark", "light", "auto"};
        String[] themeLabels = {"Scuro", "Chiaro", "Automatico (come il telefono)"};
        android.widget.Spinner theme = new android.widget.Spinner(this);
        theme.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, themeLabels));
        String cur = Prefs.get(this).getString(Prefs.KEY_THEME, "dark");
        for (int i = 0; i < themeKeys.length; i++) if (themeKeys[i].equals(cur)) theme.setSelection(i);
        f.addView(theme);
        TextView note = text("Parola chiave SMS: " + Prefs.keyword(this) + " (si cambia da Avanzate)", 12, GREY, false);
        note.setPadding(0, dp(12), 0, 0);
        f.addView(note);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Impostazioni")
                .setView(f)
                .setPositiveButton("Salva", (d, w) -> {
                    Prefs.get(this).edit()
                            .putString(Prefs.KEY_NUMBER, num.getText().toString().trim())
                            .putString(Prefs.KEY_PIN, pin.getText().toString().trim())
                            .putString(Prefs.KEY_THEME, themeKeys[theme.getSelectedItemPosition()])
                            .apply();
                    toast("Impostazioni salvate");
                    if (!themeKeys[theme.getSelectedItemPosition()].equals(cur)) recreate();
                })
                .setNegativeButton("Annulla", null)
                .show();
    }

    // ---------------------------------------------------------------- view helpers

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        return t;
    }

    private TextView centered(TextView t) {
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(4);
        t.setLayoutParams(lp);
        return t;
    }

    private GradientDrawable rounded(int fill, int stroke, int radiusDp) { return rounded(fill, stroke, radiusDp, 1); }

    private GradientDrawable rounded(int fill, int stroke, int radiusDp, int strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        if (stroke != 0) g.setStroke(dp(strokeDp), stroke);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private Drawable tinted(int res, int color) {
        Drawable d = getDrawable(res).mutate();
        d.setTint(color);
        return d;
    }

    private View iconButton(int res, String desc, View.OnClickListener l) {
        FrameLayout f = new FrameLayout(this);
        f.setBackground(rounded(CARD, BORDER, 14));
        f.setContentDescription(desc);
        f.setClickable(true);
        f.setFocusable(true);
        f.setOnClickListener(l);
        ImageView i = new ImageView(this);
        i.setImageDrawable(tinted(res, SUBTEXT));
        f.addView(i, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
        f.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        return f;
    }

    private LinearLayout commandCard(String title, String sub, int icon, int color, View.OnClickListener l) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(12), dp(14), dp(12), dp(12));
        c.setClickable(true);
        c.setFocusable(true);
        c.setContentDescription(title);
        c.setOnClickListener(l);
        FrameLayout chip = new FrameLayout(this);
        chip.setBackground(rounded((color & 0x00FFFFFF) | 0x24000000, 0, 12));
        ImageView i = new ImageView(this);
        i.setImageDrawable(tinted(icon, color));
        chip.addView(i, new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER));
        c.addView(chip, new LinearLayout.LayoutParams(dp(40), dp(40)));
        View spacer = new View(this);
        c.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));
        TextView t = text(title, 14, TEXT, true);
        c.addView(t);
        c.addView(text(sub, 11, GREY, false));
        return c;
    }

    private LinearLayout.LayoutParams navLp() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
    }

    private LinearLayout navItem(int icon, String label, boolean active, View.OnClickListener l) {
        LinearLayout n = new LinearLayout(this);
        n.setOrientation(LinearLayout.VERTICAL);
        n.setGravity(Gravity.CENTER_HORIZONTAL);
        n.setContentDescription(label);
        if (l != null) { n.setClickable(true); n.setFocusable(true); n.setOnClickListener(l); }
        int color = active ? TEXT : GREY;
        ImageView i = new ImageView(this);
        i.setImageDrawable(tinted(icon, color));
        n.addView(i, new LinearLayout.LayoutParams(dp(22), dp(22)));
        TextView t = text(label, 11, color, active);
        t.setPadding(0, dp(4), 0, 0);
        n.addView(t);
        return n;
    }
}
