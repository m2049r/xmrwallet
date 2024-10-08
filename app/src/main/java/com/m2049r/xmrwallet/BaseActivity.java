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

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

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

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
            return networkCapabilities != null && (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }

    static private final int REQUEST_CODE_BLUETOOTH_PERMISSIONS = 32423;

    void btPermissionGranted() {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                btPermissionGranted();
            } // else onResume() takes care of trying again
        }
    }

    private void showBtPermissionsDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(R.string.bluetooth_permissions);
        alertDialogBuilder.setPositiveButton(R.string.bluetooth_permissions_ok,
                (dialog, which) -> requestBtPermissions());
        alertDialogBuilder.setCancelable(false);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void showAppInfoDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(R.string.bluetooth_permissions);
        alertDialogBuilder.setPositiveButton(R.string.bluetooth_permissions_settings, (dialog, which) -> {
            Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });
        alertDialogBuilder.setNegativeButton(R.string.bluetooth_permissions_cancel, (dialog, which) -> {
            finish();
        });
        alertDialogBuilder.setCancelable(false);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void requestBtPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_SCAN}, REQUEST_CODE_BLUETOOTH_PERMISSIONS);
    }

    private boolean firstCheck = true;

    void checkBtPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if ((ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) ||
                    (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
                if (shouldShowRequestPermissionRationale(android.Manifest.permission.BLUETOOTH_SCAN) || shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT)) {
                    showBtPermissionsDialog();
                } else {
                    if (firstCheck) {
                        requestBtPermissions();
                    } else {
                        showAppInfoDialog();
                    }
                }
            } else {
                btPermissionGranted();
            }
            firstCheck = false;
        } else {
            btPermissionGranted();
        }
    }
}
