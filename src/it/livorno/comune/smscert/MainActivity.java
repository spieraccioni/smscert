package it.livorno.comune.smscert;

//import android.R;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.*;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//import mp.app1.R;

public class MainActivity extends Activity {

    Button btnSendSMS;
    EditText cf;
    EditText txtEmail;
    private RadioGroup radioTipoCert;
    private RadioButton radioB;
    Intent selectUso;
    EditText selectedUso;
    private String cures;
    private String codfis;
    private String email;
    TextView codUso;
    TextView curesult;
    static final int selectUsoCode = 1;  // The request code

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
        txtEmail = (EditText) findViewById(R.id.txtEmail);
        email = txtEmail.getText().toString();
        radioTipoCert = (RadioGroup) findViewById(R.id.radioTipoCert);
        selectedUso = (EditText) findViewById(R.id.selectedUso);
        codUso = (TextView) findViewById(R.id.cu);
        //codUso=(TextView) findViewById(R.id.codUso);
        View.OnClickListener gestore = new View.OnClickListener() {

            public void onClick(View view) {

                switch (view.getId()) {

                    case R.id.btnSendSMS:
                        codfis = cf.getText().toString().toUpperCase();
                        email = txtEmail.getText().toString();
                        String message = "";
                        //get selected radio button from radioGroup
                        int selectedId = radioTipoCert.getCheckedRadioButtonId();
                        // find the radiobutton by returned id
                        radioB = (RadioButton) findViewById(selectedId);
//                        sendPostRequest(codfis, codiceUso);
                        if (codfis.length() != 16) {
                            Toast.makeText(MainActivity.this, "Codice fiscale errato", Toast.LENGTH_LONG).show();
                        } else if (!validaemail(email)) {
                            Toast.makeText(MainActivity.this, "E-mail formalmente errata", Toast.LENGTH_LONG).show();
                        } else if(curesult==null || curesult.getText().toString()==null ||curesult.getText().toString().isEmpty() ){
                            Toast.makeText(MainActivity.this, "Scegli l'uso", Toast.LENGTH_LONG).show();
                        } else {
                            message="cert\n";
                            message +="cert;"+ codfis + ";" + email + ";" + radioB.getText() + ";"+curesult.getText().toString()+"\n";
                            sendSMS("3773069406", message);
                        }

                        break;
                    case R.id.selectedUso:
                        selectUso = new Intent(MainActivity.this, ListaUsi.class);
                        startActivityForResult(selectUso, selectUsoCode);
                        break;
                }
            }
        };

        btnSendSMS.setOnClickListener(gestore);
        selectedUso.setOnClickListener(gestore);
    }

    private boolean validaemail(String email) {
        Pattern pattern;
        Matcher matcher;
//         String EMAIL_REGEX ="^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
        String EMAIL_REGEX = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
        pattern = Pattern.compile(EMAIL_REGEX);
        matcher = pattern.matcher(email);
        boolean b = matcher.matches();
        return b;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Handle the logic for the requestCode, resultCode and data returned...
        //Toast.makeText(MainActivity.this, resultCode, Toast.LENGTH_LONG).show();
        //super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == selectUsoCode) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // Get extra data
                String uso = data.getStringExtra("USO");
                String codice = data.getStringExtra("CODICE");
                selectedUso.setText(uso);
                curesult = (TextView) findViewById(R.id.curesult);
                curesult.setText(codice);
                //Toast.makeText(MainActivity.this, codice, Toast.LENGTH_LONG).show();
            }
        }
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
                        Toast.makeText(MainActivity.this, "SMS inviato", Toast.LENGTH_LONG).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(MainActivity.this, "Errore generico", Toast.LENGTH_LONG).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(MainActivity.this, "Nessun servizio", Toast.LENGTH_LONG).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(MainActivity.this, "PDU nullo", Toast.LENGTH_LONG).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(MainActivity.this, "Radio off", Toast.LENGTH_LONG).show();
                        break;
                }

            }
        }, new IntentFilter(SENT));

        //quando l'sms è stato consegnato
        registerReceiver(new BroadcastReceiver() {

            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(MainActivity.this, "SMS inviato", Toast.LENGTH_LONG).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(MainActivity.this, "Azione annullata", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
    }
}
