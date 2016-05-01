package com.labs.okey.oneride.utils.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;

/**
 * Created by Oleg on 16-Apr-16.
 */
public class SMSReceiver extends BroadcastReceiver {

    String strMessageBody = "";
    String strMessageFrom = "";

    @Override
    public void onReceive(Context context, Intent intent) {

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

                for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {

                    strMessageFrom = smsMessage.getOriginatingAddress();
                    strMessageBody = smsMessage.getMessageBody();
                }

            } else {
                String format = intent.getStringExtra("format");

                Bundle bundle = intent.getExtras();
                SmsMessage[] msgs = null;

                if (bundle != null) {

                    Object[] pdus = (Object[]) bundle.get("pdus");
                    msgs = new SmsMessage[pdus.length];

                    for (int i = 0; i < msgs.length; i++) {
                        msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);

                        strMessageFrom = msgs[i].getOriginatingAddress();
                        strMessageBody = msgs[i].getMessageBody();
                    }
                }

            }
        }  catch (Exception ex) {
         }
    }
}
