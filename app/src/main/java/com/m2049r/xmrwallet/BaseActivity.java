package com.m2049r.xmrwallet;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.m2049r.xmrwallet.data.BarcodeData;
import com.m2049r.xmrwallet.dialog.ProgressDialog;
import com.m2049r.xmrwallet.fragment.send.SendFragment;
import com.m2049r.xmrwallet.ledger.Ledger;
import com.m2049r.xmrwallet.ledger.LedgerProgressDialog;

import java.io.IOException;

import timber.log.Timber;

public class BaseActivity extends SecureActivity implements GenerateReviewFragment.ProgressListener {

    ProgressDialog progressDialog = null;

    private class SimpleProgressDialog extends ProgressDialog {

        SimpleProgressDialog(Context context, int msgId) {
            super(context);
            setCancelable(false);
            setMessage(context.getString(msgId));
        }

        @Override
        public void onBackPressed() {
            // prevent back button
        }
    }

    @Override
    public void showProgressDialog(int msgId) {
        showProgressDialog(msgId, 250); // don't show dialog for fast operations
    }

    public void showProgressDialog(int msgId, long delayMillis) {
        dismissProgressDialog(); // just in case
        progressDialog = new SimpleProgressDialog(BaseActivity.this, msgId);
        if (delayMillis > 0) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    if (progressDialog != null) progressDialog.show();
                }
            }, delayMillis);
        } else {
            progressDialog.show();
        }
    }

    @Override
    public void showLedgerProgressDialog(int mode) {
        dismissProgressDialog(); // just in case
        progressDialog = new LedgerProgressDialog(BaseActivity.this, mode);
        Ledger.setListener((Ledger.Listener) progressDialog);
        progressDialog.show();
    }

    @Override
    public void dismissProgressDialog() {
        if (progressDialog == null) return; // nothing to do
        if (progressDialog instanceof Ledger.Listener) {
            Ledger.unsetListener((Ledger.Listener) progressDialog);
        }
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = null;
    }

    static final int RELEASE_WAKE_LOCK_DELAY = 5000; // millisconds

    private PowerManager.WakeLock wl = null;

    void acquireWakeLock() {
        if ((wl != null) && wl.isHeld()) return;
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, getString(R.string.app_name));
        try {
            wl.acquire();
            Timber.d("WakeLock acquired");
        } catch (SecurityException ex) {
            Timber.w("WakeLock NOT acquired: %s", ex.getLocalizedMessage());
            wl = null;
        }
    }

    void releaseWakeLock(int delayMillis) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                releaseWakeLock();
            }
        }, delayMillis);
    }

    void releaseWakeLock() {
        if ((wl == null) || !wl.isHeld()) return;
        wl.release();
        wl = null;
        Timber.d("WakeLock released");
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initNfc();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, null, null);
            // intercept all techs so we can tell the user their tag is no good
        }
    }

    @Override
    protected void onPause() {
        Timber.d("onPause()");
        if (nfcAdapter != null)
            nfcAdapter.disableForegroundDispatch(this);
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processNfcIntent(intent);
    }

    // NFC stuff
    private NfcAdapter nfcAdapter;
    private PendingIntent nfcPendingIntent;

    public void initNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) // no NFC support
            return;
        nfcPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                0);
    }

    private void processNfcIntent(Intent intent) {
        String action = intent.getAction();
        Timber.d("ACTION=%s", action);
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                Toast.makeText(this, getString(R.string.nfc_tag_unsupported), Toast.LENGTH_LONG).show();
                return;
            }

            Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (f instanceof ReceiveFragment) {
                // We want to write a Tag from the ReceiveFragment
                BarcodeData bc = ((ReceiveFragment) f).getBarcodeData();
                if (bc != null) {
                    new AsyncWriteTag(ndef, bc.getUri()).execute();
                } // else wallet is not loaded yet or receive is otherwise not ready - ignore
            } else if (f instanceof SendFragment) {
                // We want to read a Tag for the SendFragment
                NdefMessage ndefMessage = ndef.getCachedNdefMessage();
                if (ndefMessage == null) {
                    Toast.makeText(this, getString(R.string.nfc_tag_read_undef), Toast.LENGTH_LONG).show();
                    return;
                }
                NdefRecord firstRecord = ndefMessage.getRecords()[0];
                Uri uri = firstRecord.toUri(); // we insist on the first record
                if (uri == null) {
                    Toast.makeText(this, getString(R.string.nfc_tag_read_undef), Toast.LENGTH_LONG).show();
                } else {
                    BarcodeData bc = BarcodeData.fromQrCode(uri.toString());
                    if (bc == null)
                        Toast.makeText(this, getString(R.string.nfc_tag_read_undef), Toast.LENGTH_LONG).show();
                    else
                        onUriScanned(bc);
                }
            }
        }
    }

    // this gets called only if we get data
    @CallSuper
    void onUriScanned(BarcodeData barcodeData) {
        // do nothing by default yet
    }

    private BarcodeData barcodeData = null;

    private BarcodeData popBarcodeData() {
        BarcodeData popped = barcodeData;
        barcodeData = null;
        return popped;
    }

    private class AsyncWriteTag extends AsyncTask<Void, Void, Boolean> {

        Ndef ndef;
        Uri uri;
        String errorMessage = null;

        AsyncWriteTag(Ndef ndef, Uri uri) {
            this.ndef = ndef;
            this.uri = uri;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog(R.string.progress_nfc_write);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (params.length != 0) return false;
            try {
                writeNdef(ndef, uri);
                return true;
            } catch (IOException | FormatException ex) {
                Timber.e(ex);
            } catch (IllegalArgumentException ex) {
                errorMessage = ex.getMessage();
                Timber.d(errorMessage);
            } finally {
                try {
                    ndef.close();
                } catch (IOException ex) {
                    Timber.e(ex);
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (isDestroyed()) {
                return;
            }
            dismissProgressDialog();
            if (!result) {
                if (errorMessage != null)
                    Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(getApplicationContext(), getString(R.string.nfc_write_failed), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.nfc_write_successful), Toast.LENGTH_SHORT).show();
            }
        }
    }

    void writeNdef(Ndef ndef, Uri uri) throws IOException, FormatException {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) return; // no NFC support here

        NdefRecord recordNFC = NdefRecord.createUri(uri);
        NdefMessage message = new NdefMessage(recordNFC);
        ndef.connect();
        int tagSize = ndef.getMaxSize();
        int msgSize = message.getByteArrayLength();
        Timber.d("tagSize=%d, msgSIze=%d, uriSize=%d", tagSize, msgSize, uri.toString().length());
        if (tagSize < msgSize)
            throw new IllegalArgumentException(getString(R.string.nfc_tag_size, tagSize, msgSize));
        ndef.writeNdefMessage(message);
    }
}
