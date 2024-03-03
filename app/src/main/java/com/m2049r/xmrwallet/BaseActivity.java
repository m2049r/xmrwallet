/*
 * Copyright (c) 2017-2020 m2049r et al.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m2049r.xmrwallet;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.CallSuper;
import androidx.fragment.app.FragmentActivity;

import com.m2049r.xmrwallet.data.BarcodeData;
import com.m2049r.xmrwallet.dialog.ProgressDialog;
import com.m2049r.xmrwallet.ledger.Ledger;
import com.m2049r.xmrwallet.ledger.LedgerProgressDialog;

import timber.log.Timber;

public class BaseActivity extends SecureActivity
        implements GenerateReviewFragment.ProgressListener, SubaddressFragment.ProgressListener {

    ProgressDialog progressDialog = null;

    private static class SimpleProgressDialog extends ProgressDialog {

        SimpleProgressDialog(Context context, int msgId) {
            super(context);
            setCancelable(false);
            setMessage(context.getString(msgId));
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
            handler.postDelayed(() -> {
                if (progressDialog != null) {
                    progressDialog.show();
                    disableBackPressed();
                }
            }, delayMillis);
        } else {
            progressDialog.show();
            disableBackPressed();
        }
    }

    @Override
    public void showLedgerProgressDialog(int mode) {
        dismissProgressDialog(); // just in case
        progressDialog = new LedgerProgressDialog(BaseActivity.this, mode);
        Ledger.setListener((Ledger.Listener) progressDialog);
        progressDialog.show();
        disableBackPressed();
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
        enableBackPressed();
    }

    OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            // no going back
        }
    };

    public void disableBackPressed() {
        backPressedCallback.setEnabled(true);
    }

    public void enableBackPressed() {
        backPressedCallback.setEnabled(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
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
}
