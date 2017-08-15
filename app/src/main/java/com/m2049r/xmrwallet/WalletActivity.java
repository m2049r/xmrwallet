/*
 * Copyright (c) 2017 m2049r
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

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.service.WalletService;

public class WalletActivity extends Activity implements WalletFragment.WalletFragmentListener,
        WalletService.Observer {
    private static final String TAG = "WalletActivity";

    static final int MIN_DAEMON_VERSION = 65544;

    public static final String REQUEST_ID = "id";
    public static final String REQUEST_PW = "pw";

    private boolean synced = false;

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
        this.synced = false; // init syncing logic
    }

    private void startWalletService() {
        acquireWakeLock();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String walletId = extras.getString(REQUEST_ID);
            String walletPassword = extras.getString(REQUEST_PW);
            connectWalletService(walletId, walletPassword);
        } else {
            throw new IllegalStateException("No extras passed! Panic!");
        }
    }

    private void stopWalletService() {
        releaseWakeLock();
        disconnectWalletService();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop()");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        stopWalletService();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallet_activity);

        Fragment walletFragment = new WalletFragment();
        getFragmentManager().beginTransaction()
                .add(R.id.fragment_container, walletFragment).commit();
        Log.d(TAG, "fragment added");

        // TODO do stuff with savedInstanceState ?
        if (savedInstanceState != null) {
            return;
        }

        startWalletService();
        Log.d(TAG, "onCreate() done.");
    }


    Wallet getWallet() {
        if (mBoundService == null) throw new IllegalStateException("WalletService not bound.");
        return mBoundService.getWallet();
    }

    private WalletService mBoundService = null;
    private boolean mIsBound = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((WalletService.WalletServiceBinder) service).getService();
            //Log.d(TAG, "setting observer of " + mBoundService);
            mBoundService.setObserver(WalletActivity.this);
            updateProgress();
            //TODO show current progress (eg. if the service is already busy saving last wallet)
            Log.d(TAG, "CONNECTED");
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
            setTitle(getString(R.string.wallet_activity_name));
            Log.d(TAG, "DISCONNECTED");
        }
    };

    void connectWalletService(String walletName, String walletPassword) {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        Intent intent = new Intent(getApplicationContext(), WalletService.class);
        intent.putExtra(WalletService.REQUEST_WALLET, walletName);
        intent.putExtra(WalletService.REQUEST, WalletService.REQUEST_CMD_LOAD);
        intent.putExtra(WalletService.REQUEST_CMD_LOAD_PW, walletPassword);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        Log.d(TAG, "BOUND");
    }

    void disconnectWalletService() {
        if (mIsBound) {
            // Detach our existing connection.
            mBoundService.setObserver(null);
            unbindService(mConnection);
            mIsBound = false;
            Log.d(TAG, "UNBOUND");
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        //saveWallet(); //TODO: do it here if we really need to ...
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }

    private PowerManager.WakeLock wl = null;

    void acquireWakeLock() {
        if ((wl != null) && wl.isHeld()) return;
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, getString(R.string.app_name));
        try {
            wl.acquire();
            Log.d(TAG, "WakeLock acquired");
        } catch (SecurityException ex) {
            Log.d(TAG, "WakeLock NOT acquired");
            Log.d(TAG, ex.getLocalizedMessage());
            wl = null;
        }
    }

    public void releaseWakeLock() {
        if ((wl == null) || !wl.isHeld()) return;
        wl.release();
        wl = null;
        Log.d(TAG, "WakeLock released");
    }

    public void saveWallet() {
        if (mIsBound) { // no point in talking to unbound service
            Intent intent = new Intent(getApplicationContext(), WalletService.class);
            intent.putExtra(WalletService.REQUEST, WalletService.REQUEST_CMD_STORE);
            startService(intent);
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), getString(R.string.status_wallet_unloading), Toast.LENGTH_LONG).show();
                }
            });
            Log.d(TAG, "STORE request sent");
        } else {
            Log.e(TAG, "Service not bound");
        }
    }

    //////////////////////////////////////////
    // WalletFragment.WalletFragmentListener
    //////////////////////////////////////////

    @Override
    public boolean hasBoundService() {
        return mBoundService != null;
    }

    @Override
    public Wallet.ConnectionStatus getConnectionStatus() {
        return mBoundService.getConnectionStatus();
    }

    @Override
    public long getDaemonHeight() {
        return mBoundService.getDaemonHeight();
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
    }

    ///////////////////////////
    // WalletService.Observer
    ///////////////////////////
    @Override
    public void onRefreshed(final Wallet wallet, final boolean full) {
        Log.d(TAG, "onRefreshed()");
        if (wallet.isSynchronized()) {
            releaseWakeLock(); // the idea is to stay awake until synced
            if (!synced) {
                onProgress(null);
                saveWallet(); // save on first sync
                synced = true;
            }
        }
        // TODO check which fragment is loaded
        final WalletFragment walletFragment = (WalletFragment)
                getFragmentManager().findFragmentById(R.id.fragment_container);
        runOnUiThread(new Runnable() {
            public void run() {
                walletFragment.onRefreshed(wallet, full);
            }
        });
    }

    @Override
    public void onProgress(final String text) {
        //Log.d(TAG, "PROGRESS: " + text);
        // TODO check which fragment is loaded
        final WalletFragment walletFragment = (WalletFragment)
                getFragmentManager().findFragmentById(R.id.fragment_container);
        runOnUiThread(new Runnable() {
            public void run() {
                walletFragment.onProgress(text);
            }
        });
    }

    @Override
    public void onProgress(final int n) {
        // TODO check which fragment is loaded
        final WalletFragment walletFragment = (WalletFragment)
                getFragmentManager().findFragmentById(R.id.fragment_container);
        runOnUiThread(new Runnable() {
            public void run() {
                walletFragment.onProgress(n);
            }
        });
    }

    private void updateProgress() {
        // TODO maybe show real state of WalletService (like "still closing previous wallet")
        if (hasBoundService()) {
            onProgress(mBoundService.getProgressText());
            onProgress(mBoundService.getProgressValue());
        }
    }
}
