package com.guestpro.nfc.app;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private final static String prefix = "com.guestpro.nfc.app.";

    private TextView mTextViewNfc;
    private EditText inputTextField;
    private Button submitButton;

    public final String[][] techList = new String[][]{
            new String[]{
                    NfcA.class.getName(),
                    NfcB.class.getName(),
                    NfcF.class.getName(),
                    NfcV.class.getName(),
                    IsoDep.class.getName(),
                    MifareClassic.class.getName(),
                    MifareUltralight.class.getName(),
                    Ndef.class.getName()
            }
    };

    public NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mTextViewNfc = findViewById(R.id.text_view_nfc);
        // inputTextField = findViewById(R.id.input_text_field);
        // submitButton = findViewById(R.id.button_submit);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        /*
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputText = inputTextField.getText().toString();

                // Do something with the input text, e.g., display a toast
                Toast.makeText(MainActivity.this, "You entered: " + inputText, Toast.LENGTH_SHORT).show();
            }
        });
         */
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        Log.e(TAG, intent.toString());

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            StringBuilder temp = new StringBuilder();

            temp.append(NfcAdapter.EXTRA_ID);
            temp.append(getByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)));
            Log.e(TAG, temp.toString());
            mTextViewNfc.setText(temp.toString());

            Parcelable tagN = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tagN != null) {
                Log.e(ContentValues.TAG, "Parcelable OK");
                NdefMessage[] msgs;
                byte[] empty = new byte[0];
                byte[] id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
                String data = dumpTagData(tagN);
                byte[] payload = data.getBytes();

                mTextViewNfc.setText(data);

                temp.append(getDateTimeNow(data));
                Log.e(TAG, temp.toString());
                mTextViewNfc.setText(temp.toString());

                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, id, payload);
                NdefMessage msg = new NdefMessage(new NdefRecord[]{record});
                msgs = new NdefMessage[]{msg};
                Log.e(ContentValues.TAG, "Parcelable " + Arrays.toString(msgs));
            } else {
                Log.e(ContentValues.TAG, "Parcelable NULL");
            }
            Parcelable[] messages1 = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (messages1 != null) {
                Log.e(ContentValues.TAG, "Found " + messages1.length + " NDEF messages");
            } else {
                Log.e(ContentValues.TAG, "Not EXTRA_NDEF_MESSAGES");
            }
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                Log.e(ContentValues.TAG, "onNewIntent: NfcAdapter.EXTRA_TAG");
                Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                if (messages != null) {
                    Log.e(ContentValues.TAG, "Found " + messages.length + " NDEF messages");
                }
            } else {
                Log.e(ContentValues.TAG, "Write to an unformatted tag not implemented");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (nfcAdapter == null) {
            // Handle case where NFC is not available
            showNfcUnavailableToast();
        } else if (!nfcAdapter.isEnabled()) {
            // Handle case where NFC is not enabled
            showNfcDisabledToast();
        } else {
            // NFC is available and enabled
            Intent intent = new Intent(this, this.getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);



            nfcAdapter.enableForegroundDispatch(this, pendingIntent, getIntentFilter(), techList);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    public IntentFilter[] getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
        return new IntentFilter[]{filter};
    }

    private void showNfcUnavailableToast() {
        Toast.makeText(this, "NFC unavailable", Toast.LENGTH_LONG).show();
    }

    private void showNfcDisabledToast() {
        Toast.makeText(this, "Please enable NFC", Toast.LENGTH_LONG).show();
    }

    public String dumpTagData(final Parcelable parcelable) {
        Log.e(TAG, "dumpTagData(" + parcelable + ")");
        StringBuilder sb = new StringBuilder();
        Tag tag = (Tag) parcelable;
        byte[] id = tag.getId();
        sb.append("Tag ID (hex): ").append(getHex(id)).append("\n");
        sb.append("Tag ID (dec): ").append(getDec(id)).append("\n");
        sb.append("ID (reversed): ").append(getReversed(id)).append("\n");
        sb.append("Technologies: ");
        for (String tech : tag.getTechList()) {
            sb.append(tech.substring(prefix.length()));
            sb.append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        for (String tech : tag.getTechList()) {
            if (tech.equals(MifareClassic.class.getName())) {
                sb.append('\n');
                MifareClassic mifareTag = MifareClassic.get(tag);
                String type = "Unknown";
                switch (mifareTag.getType()) {
                    case MifareClassic.TYPE_CLASSIC:
                        type = "Classic";
                        break;
                    case MifareClassic.TYPE_PLUS:
                        type = "Plus";
                        break;
                    case MifareClassic.TYPE_PRO:
                        type = "Pro";
                        break;
                }
                sb.append("Mifare Classic type: ");
                sb.append(type);
                sb.append('\n');

                sb.append("Mifare size: ");
                sb.append(mifareTag.getSize()).append(" bytes");
                sb.append('\n');

                sb.append("Mifare sectors: ");
                sb.append(mifareTag.getSectorCount());
                sb.append('\n');

                sb.append("Mifare blocks: ");
                sb.append(mifareTag.getBlockCount());
            }

            if (tech.equals(MifareUltralight.class.getName())) {
                sb.append('\n');
                MifareUltralight mifareUlTag = MifareUltralight.get(tag);
                String type = "Unknown";
                switch (mifareUlTag.getType()) {
                    case MifareUltralight.TYPE_ULTRALIGHT:
                        type = "Ultralight";
                        break;
                    case MifareUltralight.TYPE_ULTRALIGHT_C:
                        type = "Ultralight C";
                        break;
                }
                sb.append("Mifare Ultralight type: ");
                sb.append(type);
            }
        }
        Log.e(TAG, "Datum: " + sb);
        Log.e(ContentValues.TAG, "dumpTagData Return \n" + sb);
        return sb.toString();
    }

    private String ByteArrayToHexString(final byte[] inarray) {
        Log.e(ContentValues.TAG, "ByteArrayToHexString " + Arrays.toString(inarray));
        int i, j, in;
        String[] hex = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
        StringBuilder out = new StringBuilder();
        for (j = 0; j < inarray.length; ++j) {
            in = (int) inarray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out.append(hex[i]);
            i = in & 0x0f;
            out.append(hex[i]);
        }
        //CE7AEED4
        //EE7BEED4
        Log.e(TAG, "ByteArrayToHexString" + String.format("%0" + (inarray.length * 2) + "X", new BigInteger(1, inarray)));
        return out.toString();
    }

    public String getByteArrayToHexString(final byte[] inarray) {
        Log.e(TAG, "getByteArrayToHexString " + Arrays.toString(inarray));
        Log.e(ContentValues.TAG, "getByteArrayToHexString " + Arrays.toString(inarray));
        Log.e(ContentValues.TAG, "getByteArrayToHexString Return " + ByteArrayToHexString(inarray));
        return "NFC Tag\n" + ByteArrayToHexString(inarray);
    }

    public String getDateTimeNow(String data) {
        Log.e(TAG, "getDateTime(" + data + ")");
        DateFormat TIME_FORMAT = SimpleDateFormat.getDateTimeInstance();
        Date now = new Date();
        Log.e(ContentValues.TAG, "getDateTimeNow() Return \n" + TIME_FORMAT.format(now) + '\n' + data);
        return TIME_FORMAT.format(now) + '\n' + data;
    }

    private String getHex(final byte[] bytes) {
        Log.e(TAG, "getHex()");
        StringBuilder sb = new StringBuilder();
        for (int i = bytes.length - 1; i >= 0; --i) {
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b));
            if (i > 0) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    private long getDec(final byte[] bytes) {
        Log.e(TAG, "getDec()");
        long result = 0;
        long factor = 1;
        for (byte aByte : bytes) {
            long value = aByte & 0xffL;
            result += value * factor;
            factor *= 256L;
        }
        return result;
    }

    private long getReversed(final byte[] bytes) {
        Log.e(TAG, "getReversed()");
        long result = 0;
        long factor = 1;
        for (int i = bytes.length - 1; i >= 0; --i) {
            long value = bytes[i] & 0xffL;
            result += value * factor;
            factor *= 256L;
        }
        return result;
    }
}