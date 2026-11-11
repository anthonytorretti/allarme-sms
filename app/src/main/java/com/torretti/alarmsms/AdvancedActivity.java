package com.torretti.alarmsms;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

/** All the other functions of the original My1059 app. */
public class AdvancedActivity extends BaseSmsActivity {

    private TextView resultView;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int pad = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad + dp(16), pad, pad);

        TextView title = new TextView(this);
        title.setText("Funzioni avanzate");
        title.setTextSize(24);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(title);

        root.addView(makeStatusView());
        root.addView(label("Risposta della centrale"));
        resultView = box();
        root.addView(resultView);

        root.addView(section("Utenti e codici"));
        root.addView(bigButton("Leggi utenti e PIN", BLUE, v ->
                confirmAndSend("Lettura utenti", "Chiedere alla centrale l'elenco utenti?", "PIN?:", null)));
        root.addView(bigButton("Modifica utente / PIN", GREY, v -> editUser()));
        root.addView(bigButton("Cambia parola chiave SMS", GREY, v -> editKeyword()));

        root.addView(section("Impostazioni allarme"));
        root.addView(bigButton("Modifica ritardi, sirena, temperatura", GREY, v -> editAlarmSettings()));

        root.addView(section("Numeri di avviso (report)"));
        root.addView(bigButton("Modifica numero di avviso", GREY, v -> editReporting()));

        root.addView(section("Sensori e dispositivi"));
        root.addView(bigButton("Rinomina / tipo di zona", GREY, v -> editDevice()));
        root.addView(bigButton("Elimina un sensore", RED, v -> deleteDevice()));
        root.addView(bigButton("Sirene", GREY, v -> sirenControl()));
        root.addView(bigButton("Prese comandate (PSS)", GREY, v -> pssControl()));

        root.addView(section("Automazioni"));
        root.addView(bigButton("Leggi regole", BLUE, v ->
                confirmAndSend("Lettura regole", "Chiedere alla centrale le regole di automazione?", "HAR?:", null)));
        root.addView(bigButton("Modifica regola", GREY, v -> editRule()));

        root.addView(section("Esperto"));
        root.addView(bigButton("Comando manuale", GREY, v -> manualCommand()));

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    @Override
    protected void onReply(String text, long time, boolean answersLastCommand) {
        if (text == null) { resultView.setText("Nessuna risposta ricevuta"); return; }
        String nice = Panel.describe(text);
        resultView.setText(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
                .format(new Date(time)) + "\n" + (nice != null ? nice : text));
    }

    // ---------------------------------------------------------------- users

    private void editUser() {
        String[] users = {"Utente 1 (è anche il PIN per gli SMS)", "Utente 2", "Utente 3", "Utente 4"};
        new Form(this)
                .choice("user", "Utente", users, 0)
                .text("name", "Nome (max 10 caratteri)", "", 10, false)
                .text("pin", "Nuovo PIN (4 cifre)", "", 4, true)
                .note("Attenzione: se cambi il PIN dell'utente 1 cambia anche il PIN usato da questa app; "
                        + "l'app lo aggiorna da sola quando la centrale conferma.")
                .show("Modifica utente", f -> {
                    String pin = f.text("pin"), name = f.text("name");
                    int u = f.choice("user") + 1;
                    if (!pin.matches("\\d{4}")) return "Il PIN deve essere di 4 cifre";
                    if (u == 1 && name.isEmpty()) return "L'utente 1 deve avere un nome";
                    if (name.contains(",") || name.contains("(") || name.contains(")")) return "Il nome non può contenere , ( )";
                    confirmAndSend("Modifica utente " + u,
                            "Impostare l'utente " + u + " con nome \"" + name + "\" e PIN " + pin + "?",
                            "PINU:" + u + "," + pin + "," + name,
                            u == 1 ? () -> Prefs.get(this).edit().putString(Prefs.KEY_PIN, pin).apply() : null);
                    return null;
                });
    }

    private void editKeyword() {
        new Form(this)
                .note("Parola chiave attuale: " + Prefs.keyword(this))
                .text("kw", "Nuova parola chiave (1–10 caratteri)", "", 10, false)
                .show("Parola chiave SMS", f -> {
                    String kw = f.text("kw");
                    if (kw.isEmpty() || kw.length() > 10 || kw.contains(" ")) return "1–10 caratteri, senza spazi";
                    confirmAndSend("Cambio parola chiave", "Cambiare la parola chiave da "
                                    + Prefs.keyword(this) + " a " + kw + "?", "PINK:" + kw,
                            () -> Prefs.get(this).edit().putString(Prefs.KEY_KEYWORD, kw).apply());
                    return null;
                });
    }

    // ---------------------------------------------------------------- alarm settings

    private void editAlarmSettings() {
        new Form(this)
                .note("Tutti i valori vengono inviati insieme: controllali tutti prima di confermare.")
                .choice("exitT", "Ritardo di uscita", Panel.DELAY_LABELS, 3)
                .choice("exitV", "Volume ritardo di uscita", Panel.VOLUME_LABELS, 2)
                .choice("entryT", "Ritardo di ingresso", Panel.DELAY_LABELS, 3)
                .choice("entryV", "Volume ritardo di ingresso", Panel.VOLUME_LABELS, 2)
                .choice("len", "Durata suono allarme", Panel.LENGTH_LABELS, 2)
                .choice("chime", "Volume campanello porta", Panel.VOLUME_LABELS, 2)
                .choice("jam", "Rilevamento disturbo radio (jamming)", Panel.JAMMING_LABELS, 0)
                .choice("hi", "Allarme temperatura alta", Panel.TEMP_LABELS, 0)
                .choice("lo", "Allarme temperatura bassa", Panel.TEMP_LABELS, 0)
                .show("Impostazioni allarme", f -> {
                    int hi = f.choice("hi"), lo = f.choice("lo");
                    // labels go from 50 °C (index 1) down to -10 °C (index 61)
                    if (hi > 0 && lo > 0 && (51 - hi) < (51 - lo) + 4)
                        return "La temperatura alta deve superare quella bassa di almeno 4 °C";
                    String cmd = "CFGA:" + Panel.DELAY_VALUES[f.choice("exitT")] + "," + f.choice("exitV") + ","
                            + Panel.DELAY_VALUES[f.choice("entryT")] + "," + f.choice("entryV") + ","
                            + Panel.LENGTH_VALUES[f.choice("len")] + "," + f.choice("chime") + "," + f.choice("jam")
                            + ";HATA:" + (hi > 0 ? "1," + Panel.TEMP_VALUES[hi] : "0,050") + ","
                            + (lo > 0 ? "1," + Panel.TEMP_VALUES[lo] : "0,110");
                    confirmAndSend("Impostazioni allarme", "Inviare le nuove impostazioni alla centrale?", cmd, null);
                    return null;
                });
    }

    // ---------------------------------------------------------------- reporting

    private void editReporting() {
        String[] slots = {"Avviso 1", "Avviso 2", "Avviso 3", "Avviso 4", "Avviso 5", "Avviso 6"};
        new Form(this)
                .choice("slot", "Posizione", slots, 0)
                .choice("type", "Tipo di avviso", Panel.REPORT_TYPE_LABELS, 0)
                .text("acct", "Numero account (solo per CID)", "", 4, true)
                .showIf("acct", f -> f.choice("type") >= 2)
                .text("phone", "Numero di telefono (vuoto = cancella)", "", 20, false)
                .choice("events", "Eventi da inviare", Panel.REPORT_EVENT_LABELS, 2)
                .note("Per ricevere un SMS a ogni inserimento/disinserimento scegli tipo SMS ed eventi \"Tutti\".")
                .show("Numero di avviso", f -> {
                    int t = f.choice("type");
                    String phone = f.text("phone").replace(" ", "");
                    String acct = f.text("acct");
                    if (t >= 2 && phone.length() > 0 && acct.isEmpty()) return "Inserisci il numero account";
                    String cmd = "RPTN:" + (f.choice("slot") + 1) + ","
                            + (t >= 2 && phone.length() > 0 ? acct + "@" : "") + phone + ","
                            + Panel.REPORT_TYPE_VALUES[t] + "," + Panel.REPORT_EVENT_VALUES[f.choice("events")];
                    confirmAndSend("Numero di avviso", phone.isEmpty()
                            ? "Cancellare l'avviso " + (f.choice("slot") + 1) + "?"
                            : "Impostare l'avviso " + (f.choice("slot") + 1) + " verso " + phone + "?", cmd, null);
                    return null;
                });
    }

    // ---------------------------------------------------------------- devices

    private void editDevice() {
        new Form(this)
                .choice("zone", "Zona (numero del sensore)", Panel.ZONES, 0)
                .text("name", "Nome (max 10 caratteri)", "", 10, false)
                .choice("attr", "Comportamento", Panel.ATTRIBUTE_LABELS, 0)
                .note("Furto: allarme immediato. Escluso in parziale: ignorato con inserimento parziale. "
                        + "Ingresso: dà il tempo di ritardo prima dell'allarme.")
                .show("Sensore", f -> {
                    String name = f.text("name");
                    if (name.contains(",") || name.contains("(") || name.contains(")")) return "Il nome non può contenere , ( )";
                    String zone = Panel.ZONES[f.choice("zone")];
                    confirmAndSend("Sensore zona " + zone, "Aggiornare la zona " + zone + "?",
                            "DEVE:" + zone + "," + f.choice("attr") + "," + name, null);
                    return null;
                });
    }

    private void deleteDevice() {
        new Form(this)
                .note("Il sensore verrà tolto dalla centrale e andrà riappreso dal pannello per usarlo di nuovo.")
                .choice("zone", "Zona da eliminare", Panel.ZONES, 0)
                .show("Elimina sensore", f -> {
                    String zone = Panel.ZONES[f.choice("zone")];
                    confirmAndSend("Elimina zona " + zone,
                            "ELIMINARE definitivamente il sensore della zona " + zone + "?", "DEVD:" + zone, null);
                    return null;
                });
    }

    private void sirenControl() {
        new Form(this)
                .choice("attr", "Funzione della sirena", Panel.SIREN_LABELS, 1)
                .choice("en", "Stato", Panel.ONOFF_LABELS, 1)
                .show("Sirene", f -> {
                    String a = Panel.SIREN_VALUES[f.choice("attr")];
                    confirmAndSend("Sirene", "Inviare l'impostazione sirena \""
                                    + Panel.SIREN_LABELS[f.choice("attr")] + "\" = "
                                    + Panel.ONOFF_LABELS[f.choice("en")] + "?",
                            "SRTX:" + a + "," + f.choice("en"), null);
                    return null;
                });
    }

    private void pssControl() {
        new Form(this)
                .choice("ch", "Presa (canale)", Panel.PSS_CHANNELS, 0)
                .choice("act", "Azione", Panel.PSS_ACTION_LABELS, 0)
                .show("Prese comandate", f -> {
                    String ch = Panel.PSS_CHANNELS[f.choice("ch")];
                    confirmAndSend("Presa " + ch, Panel.PSS_ACTION_LABELS[f.choice("act")] + " la presa " + ch + "?",
                            "PSSX:" + ch + "," + Panel.PSS_ACTION_VALUES[f.choice("act")], null);
                    return null;
                });
    }

    // ---------------------------------------------------------------- automations

    private void editRule() {
        String[] tempLabels = java.util.Arrays.copyOfRange(Panel.TEMP_LABELS, 1, Panel.TEMP_LABELS.length);
        String[] tempValues = java.util.Arrays.copyOfRange(Panel.TEMP_VALUES, 1, Panel.TEMP_VALUES.length);
        Form form = new Form(this)
                .choice("rule", "Regola", Panel.RULE_LABELS, 0)
                .choice("on", "Regola attiva?", new String[]{"No", "Sì"}, 1)
                .choice("temp", "Temperatura", tempLabels, 25)
                .showIf("temp", f -> f.choice("rule") == 3 || f.choice("rule") == 4)
                .text("time", "Orario (HH:MM)", "07:00", 5, false)
                .showIf("time", f -> f.choice("rule") == 5);
        for (int i = 0; i < Panel.EVENT_LABELS.length; i++) {
            final String key = "ev" + i;
            form.check(key, Panel.EVENT_LABELS[i], i == 4).showIf(key, f -> f.choice("rule") == 6);
        }
        form.choice("ch", "Presa comandata", Panel.PSS_CHANNELS, 0)
                .choice("sw", "Azione sulla presa", Panel.SWITCH_LABELS, 1)
                .choice("dur", "Per quanto tempo", Panel.DURATION_LABELS, 0)
                .show("Regola di automazione", f -> {
                    int r = f.choice("rule");
                    String param = "0";
                    if (r == 3 || r == 4) param = tempValues[f.choice("temp")];
                    else if (r == 5) {
                        String t = f.text("time");
                        if (!t.matches("([01]\\d|2[0-3]):[0-5]\\d")) return "Orario non valido (es. 07:30)";
                        param = t.replace(":", "");
                    } else if (r == 6) {
                        int ev = 0;
                        for (int i = 0; i < Panel.EVENT_BITS.length; i++) if (f.checked("ev" + i)) ev |= Panel.EVENT_BITS[i];
                        if (ev == 0) return "Scegli almeno un tipo di allarme";
                        param = String.valueOf(ev);
                    }
                    String action = f.choice("on") == 1 ? Panel.RULE_LETTERS[r] : "0";
                    String cmd = "HARN:" + (r + 1) + "," + action + "," + param + ",P,"
                            + Panel.PSS_CHANNELS[f.choice("ch")] + "," + f.choice("sw") + ","
                            + Panel.DURATION_VALUES[f.choice("dur")];
                    confirmAndSend("Regola " + (r + 1), "Salvare la regola \"" + Panel.RULE_LABELS[r] + "\"?", cmd, null);
                    return null;
                });
    }

    // ---------------------------------------------------------------- manual

    private void manualCommand() {
        new Form(this)
                .note("Scrivi solo il comando: l'app aggiunge da sola \"" + Prefs.keyword(this) + " <PIN> \" davanti.\n"
                        + "Esempi: MOD?:   PIN?:   HAR?:   MODE:A")
                .text("cmd", "Comando", "", 150, false)
                .show("Comando manuale", f -> {
                    String c = f.text("cmd");
                    if (c.isEmpty()) return "Scrivi un comando";
                    confirmAndSend("Comando manuale", "Inviare \"" + c + "\" alla centrale?", c, null);
                    return null;
                });
    }
}
