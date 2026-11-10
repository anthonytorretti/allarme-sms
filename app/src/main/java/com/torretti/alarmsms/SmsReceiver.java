package com.torretti.alarmsms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

/** Stores any SMS coming from the alarm panel's number; the activity picks it up. */
public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (parts == null || parts.length == 0) return;

        String from = parts[0].getDisplayOriginatingAddress();
        String alarm = Prefs.get(context).getString(Prefs.KEY_NUMBER, "");
        if (!Prefs.sameNumber(from, alarm)) return;

        StringBuilder body = new StringBuilder();
        for (SmsMessage m : parts) body.append(m.getDisplayMessageBody());

        Prefs.get(context).edit()
                .putString(Prefs.KEY_REPLY_TEXT, body.toString().trim())
                .putLong(Prefs.KEY_REPLY_TIME, System.currentTimeMillis())
                .apply();
    }
}
