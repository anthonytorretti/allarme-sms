package com.torretti.alarmsms;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tiny dialog form builder: text fields, drop-downs and checkboxes. */
final class Form {
    interface OnSubmit { String submit(Form f); }   // returns an error message, or null when OK

    private final Context ctx;
    private final LinearLayout root;
    private final Map<String, View> fields = new LinkedHashMap<>();
    private final List<Runnable> visibilityRules = new ArrayList<>();
    private final int pad;

    Form(Context ctx) {
        this.ctx = ctx;
        pad = Math.round(20 * ctx.getResources().getDisplayMetrics().density);
        root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad / 2, pad, 0);
    }

    Form note(String text) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextSize(13);
        t.setPadding(0, pad / 3, 0, pad / 3);
        root.addView(t);
        return this;
    }

    Form text(String key, String label, String value, int maxLen, boolean numeric) {
        LinearLayout row = row(label);
        EditText e = new EditText(ctx);
        e.setText(value);
        e.setSingleLine(true);
        if (numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER);
        else if (key.contains("phone")) e.setInputType(InputType.TYPE_CLASS_PHONE);
        if (maxLen > 0) e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLen)});
        row.addView(e);
        fields.put(key, row);
        return this;
    }

    Form choice(String key, String label, String[] options, int selected) {
        LinearLayout row = row(label);
        Spinner s = new Spinner(ctx);
        ArrayAdapter<String> a = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, options);
        s.setAdapter(a);
        if (selected >= 0 && selected < options.length) s.setSelection(selected);
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { applyRules(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        row.addView(s);
        fields.put(key, row);
        return this;
    }

    Form check(String key, String label, boolean checked) {
        CheckBox c = new CheckBox(ctx);
        c.setText(label);
        c.setChecked(checked);
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        row.addView(c);
        root.addView(row);
        fields.put(key, row);
        return this;
    }

    /** Shows field "key" only while condition is true (re-evaluated when any choice changes). */
    Form showIf(String key, java.util.function.Predicate<Form> condition) {
        visibilityRules.add(() -> {
            View v = fields.get(key);
            if (v != null) v.setVisibility(condition.test(this) ? View.VISIBLE : View.GONE);
        });
        return this;
    }

    private void applyRules() { for (Runnable r : visibilityRules) r.run(); }

    private LinearLayout row(String label) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, pad / 3, 0, 0);
        TextView t = new TextView(ctx);
        t.setText(label);
        t.setTextSize(13);
        row.addView(t);
        root.addView(row);
        return row;
    }

    String text(String key) {
        View v = ((LinearLayout) fields.get(key)).getChildAt(1);
        return ((EditText) v).getText().toString().trim();
    }

    int choice(String key) {
        View v = ((LinearLayout) fields.get(key)).getChildAt(1);
        return ((Spinner) v).getSelectedItemPosition();
    }

    boolean checked(String key) {
        return ((CheckBox) ((LinearLayout) fields.get(key)).getChildAt(0)).isChecked();
    }

    boolean visible(String key) {
        return fields.get(key).getVisibility() == View.VISIBLE;
    }

    /** Shows the form; when "Avanti" is pressed and submit returns null the dialog closes. */
    void show(String title, OnSubmit submit) {
        applyRules();
        ScrollView sv = new ScrollView(ctx);
        sv.addView(root);
        AlertDialog d = new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setView(sv)
                .setPositiveButton("Avanti", null)
                .setNegativeButton("Annulla", null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String err = submit.submit(this);
            if (err == null) d.dismiss();
            else android.widget.Toast.makeText(ctx, err, android.widget.Toast.LENGTH_LONG).show();
        }));
        d.show();
    }
}
