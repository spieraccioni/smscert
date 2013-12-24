/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.livorno.comune.smscert;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
//import mp.app1.R;

/**
 *
 * @author MPisu
 */
public class SMS extends Activity {

    Button btnSendSMS;
    EditText cf;
    private RadioGroup radioTipoCert;
    private RadioButton radioB;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ToDo add your GUI initialization code here       
        setContentView(R.layout.main);
        btnSendSMS = (Button) findViewById(R.id.btnSendSMS);
        cf = (EditText) findViewById(R.id.cf);
        radioTipoCert = (RadioGroup) findViewById(R.id.radioTipoCert);
        btnSendSMS.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                String codfis = cf.getText().toString().toUpperCase();
                String message = "";
                if (codfis.length() == 16) {
                    message = codfis + ";" + radioB.getText() + ";";
                } 
                else {
                    Toast.makeText(SMS.this, "Codice fiscale errato", Toast.LENGTH_LONG).show();
                }


            }
        });

    }
    //send ana SMS message to another device

    private void sendSMS(String phoneNumber, String message) {
        //Intent è un messaggio contenente info che vengono inviate ad un altro oggetto che le legge e fa
        // qualcosa stabilita dal programmatore
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED), 0);
        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver() {

            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(SMS.this, "SMS inviato", Toast.LENGTH_LONG).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(SMS.this, "Errore generico", Toast.LENGTH_LONG).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(SMS.this, "Nessun servizio", Toast.LENGTH_LONG).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(SMS.this, "PDU nullo", Toast.LENGTH_LONG).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(SMS.this, "Radio off", Toast.LENGTH_LONG).show();
                        break;
                }

            }
        }, new IntentFilter(SENT));

        //quando l'sms è stato spedito
        registerReceiver(new BroadcastReceiver() {

            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) 
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(SMS.this, "SMS inviato", Toast.LENGTH_LONG).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(SMS.this, "Azione annullata", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));
    SmsManager sms = SmsManager.getDefault();
    sms.sendTextMessage(phoneNumber, null,message, sentPI, deliveredPI);
    }
}
