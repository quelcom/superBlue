/*
 * Written by Hesham Omran, heshamo@kth.se
 * Edited from :
http://mifareclassicdetectiononandroid.blogspot.com/2011/04/reading-mifare-classic-1k-from-android.html
 */
package com.nfc.demo.imageviewer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class ImageViewer extends Activity implements OnClickListener {
    // UI Elements
    private static TextView block_0_Data;
    private static TextView status_Data;

    // NFC parts
    private static NfcAdapter mAdapter;
    private static PendingIntent mPendingIntent;
    private static IntentFilter[] mFilters;
    private static String[][] mTechLists;

    // Hex help
    private static final byte[] HEX_CHAR_TABLE = { (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4',
            (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D',
            (byte) 'E', (byte) 'F' };

    // Just for alerts
    // Mifare application directory keyA
    // public static final byte[] UID1 = {(byte) 0xDB, (byte) 0xAE, (byte) 0xD8,
    // (byte) 0xE9};

    /* hardcoded strings we have in a hash*/
    public Map<String, Integer> mHash = new HashMap<String, Integer>();

    public static final byte[] KEY_MIFARE_APPLICATION_DIRECTORY = { (byte) 0xA0, (byte) 0xA1, (byte) 0xA2, (byte) 0xA3,
            (byte) 0xA4, (byte) 0xA5 };
    // NFC Forum default keyA
    public static final byte[] KEY_NFC_FORUM = { (byte) 0xD3, (byte) 0xF7, (byte) 0xD3, (byte) 0xF7, (byte) 0xD3,
            (byte) 0xF7 };
    private static final int AUTH = 1;
    private static final int EMPTY_BLOCK_0 = 2;

    private static final int NETWORK = 4;
    private static final String TAG = "purchtagscanact";
    public static final byte[] NXP_APP_KEYA = { (byte) 'A' };

    private ImageView image;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Fill in the hash with tags and images
        mHash.put("0478578A", R.drawable.image1);
        mHash.put("0496578A", R.drawable.image2);
        mHash.put("0495578A", R.drawable.image3);
        mHash.put("0485578A", R.drawable.image4);
        mHash.put("0486578A", R.drawable.image5);
        mHash.put("04112F9A", R.drawable.image6);

        image = (ImageView) findViewById(R.id.image);
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.image1);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(bmp);
        image.setImageDrawable(bitmapDrawable);

        block_0_Data = (TextView) findViewById(R.id.block_0_data);
        status_Data = (TextView) findViewById(R.id.status_data);
        
        // Capture Purchase button from layout
        Button scanBut = (Button) findViewById(R.id.clear_but);
        // Register the onClick listener with the implementation above
        scanBut.setOnClickListener(this);
        // Register the onClick listener with the implementation above
        scanBut.setOnClickListener(this);
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        
        // Create a generic PendingIntent that will be deliver to this activity.
        // The NFC stack
        // will fill in the intent with the details of the discovered tag before
        // delivering to
        // this activity.
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        // Setup an intent filter for all MIME based dispatches
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        mFilters = new IntentFilter[] { ndef, };
        // Setup a tech list for all NfcF tags
        mTechLists = new String[][] { new String[] { MifareClassic.class.getName() } };
        Intent intent = getIntent();

        resolveIntent(intent);
    }

    void resolveIntent(Intent intent) {
        // Parse the intent
        String action = intent.getAction();
        String myUID = null;
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            // status_Data.setText("Discovered tag with intent: " + intent);
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            MifareClassic mfc = MifareClassic.get(tagFromIntent);
            byte[] data;
            try {
                mfc.connect();
                boolean auth = false;
                String cardData = null;
                status_Data.setText("Authenticating the Tag..");
                // Authenticating and reading Block 0 /Sector 1
                auth = mfc.authenticateSectorWithKeyA(0, MifareClassic.KEY_DEFAULT);
                if (auth) {
                    data = mfc.readBlock(0);
                    // cardData = getHexString(data, data.length);
                    cardData = getHexString(data, 4);
                    if (cardData != null) {
                        block_0_Data.setText(cardData);
                        myUID = cardData;
                    } else {
                        showAlert(EMPTY_BLOCK_0);
                    }
                } else {
                    auth = mfc.authenticateSectorWithKeyA(0, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY);
                    if (auth) {
                        data = mfc.readBlock(0);
                        // cardData = getHexString(data, data.length); fix it to
                        // UID size
                        cardData = getHexString(data, 4);
                        myUID = getHexString(data, 4);
                        if (cardData != null) {
                            block_0_Data.setText(cardData);
                        } else {
                            showAlert(EMPTY_BLOCK_0);
                        }
                    } else {
                        showAlert(AUTH);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
                showAlert(NETWORK);
            }
        } else {
            status_Data.setText("Online + Scan a tag");
        }

        imageDisp(myUID);
    }

    private void showAlert(int alertCase) {
        // prepare the alert box
        AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
        switch (alertCase) {

        case AUTH:// Card Authentication Error
            alertbox.setMessage("Authentication Failed on Block 0");
            break;
        case EMPTY_BLOCK_0: // Block 0 Empty
            alertbox.setMessage("Failed reading Block 0");
            break;
        case NETWORK: // Communication Error
            alertbox.setMessage("Tag reading error");
            break;
        }
        
        // set a positive/yes button and create a listener
        alertbox.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            // Save the data from the UI to the database - already done
            public void onClick(DialogInterface arg0, int arg1) {
                clearFields();
            }
        });

        alertbox.show();
    }

    public void imageDisp(String x) {
        if ( mHash.containsKey(x) ) {
            Bitmap bmp = BitmapFactory.decodeResource( getResources(), mHash.get(x) );
            BitmapDrawable bitmapDrawable = new BitmapDrawable(bmp);
            image.setImageDrawable(bitmapDrawable);
        }
    }

    @Override
    public void onClick(View v) {
        clearFields();
    }

    private static void clearFields() {
        block_0_Data.setText("");
        status_Data.setText("Ready for Scan");
    }

    public static String getHexString(byte[] raw, int len) {
        byte[] hex = new byte[2 * len];
        int index = 0;
        int pos = 0;

        for (byte b : raw) {
            if (pos >= len)
                break;

            pos++;
            int v = b & 0xFF;
            hex[index++] = HEX_CHAR_TABLE[v >>> 4];
            hex[index++] = HEX_CHAR_TABLE[v & 0xF];
        }

        return new String(hex);
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists);
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.i("Foreground dispatch", "Discovered tag with intent: " + intent);
        resolveIntent(intent);
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdapter.disableForegroundDispatch(this);
    }
}