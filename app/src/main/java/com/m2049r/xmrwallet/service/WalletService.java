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

package com.m2049r.xmrwallet.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.model.TransactionHistory;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.model.WalletListener;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.Helper;

public class WalletService extends Service {
    final static String TAG = "WalletService";

    public static final String REQUEST_WALLET = "wallet";
    public static final String REQUEST = "request";
    public static final String REQUEST_CMD_LOAD = "load";
    public static final String REQUEST_CMD_LOAD_PW = "walletPassword";
    public static final String REQUEST_CMD_STORE = "store";

    public static final int START_SERVICE = 1;
    public static final int STOP_SERVICE = 2;

    private MyWalletListener listener = null;

    private class MyWalletListener implements WalletListener {
        private Wallet wallet;
        boolean updated = true;

        Wallet getWallet() {
            return wallet;
        }

        MyWalletListener(Wallet aWallet) {
            if (aWallet == null) throw new IllegalArgumentException("Cannot open wallet!");
            this.wallet = aWallet;
        }

        void start() {
            Log.d(TAG, "MyWalletListener.start()");
            if (wallet == null) throw new IllegalStateException("No wallet!");
            //acquireWakeLock();
            wallet.setListener(this);
            wallet.startRefresh();
        }

        void stop() {
            Log.d(TAG, "MyWalletListener.stop()");
            if (wallet == null) throw new IllegalStateException("No wallet!");
            wallet.pauseRefresh();
            wallet.setListener(null);
            //releaseWakeLock();
        }

        // WalletListener callbacks
        public void moneySpent(String txId, long amount) {
            Log.d(TAG, "moneySpent() " + amount + " @ " + txId);
        }

        public void moneyReceived(String txId, long amount) {
            Log.d(TAG, "moneyReceived() " + amount + " @ " + txId);
        }

        public void unconfirmedMoneyReceived(String txId, long amount) {
            Log.d(TAG, "unconfirmedMoneyReceived() " + amount + " @ " + txId);
        }

        long lastBlockTime = 0;
        int lastTxCount = 0;

        public void newBlock(long height) {
            if (wallet == null) throw new IllegalStateException("No wallet!");
            // don't flood with an update for every block ...
            if (lastBlockTime < System.currentTimeMillis() - 2000) {
                Log.d(TAG, "newBlock() @" + height + " with observer " + observer);
                lastBlockTime = System.currentTimeMillis();
                if (observer != null) {
                    boolean fullRefresh = false;
                    updateDaemonState(wallet, wallet.isSynchronized() ? height : 0);
                    if (!wallet.isSynchronized()) {
                        // we want to see our transactions as they come in
                        wallet.getHistory().refresh();
                        int txCount = wallet.getHistory().getCount();
                        if (txCount > lastTxCount) {
                            lastTxCount = txCount;
                            fullRefresh = true;
                        }
                    }
                    observer.onRefreshed(wallet, fullRefresh);
                }
            }
        }

        public void updated() {
            Log.d(TAG, "updated() " + wallet.getBalance());
            if (wallet == null) throw new IllegalStateException("No wallet!");
            updated = true;
        }

        public void refreshed() {
            if (wallet == null) throw new IllegalStateException("No wallet!");
            Log.d(TAG, "refreshed() " + wallet.getName() + " " + wallet.getBalance() + " sync=" + wallet.isSynchronized() + " with observer " + observer);
            if (updated) {
                if (observer != null) {
                    updateDaemonState(wallet, 0);
                    TransactionHistory history = wallet.getHistory();
                    history.refresh();
                    observer.onRefreshed(wallet, true);
                    updated = false;
                }
            }
        }
    }

    private long lastDaemonStatusUpdate = 0;
    private long daemonHeight = 0;
    private Wallet.ConnectionStatus connectionStatus = Wallet.ConnectionStatus.ConnectionStatus_Disconnected;
    private static final long STATUS_UPDATE_INTERVAL = 120000; // 120s (blocktime)

    private void updateDaemonState(Wallet wallet, long height) {
        long t = System.currentTimeMillis();
        if (height > 0) { // if we get a height, we are connected
            daemonHeight = height;
            connectionStatus = Wallet.ConnectionStatus.ConnectionStatus_Connected;
            lastDaemonStatusUpdate = t;
        } else {
            if (t - lastDaemonStatusUpdate > STATUS_UPDATE_INTERVAL) {
                lastDaemonStatusUpdate = t;
                // these calls really connect to the daemon - wasting time
                daemonHeight = wallet.getDaemonBlockChainHeight();
                if (daemonHeight > 0) {
                    // if we get a valid height, the obviously we are connected
                    connectionStatus = Wallet.ConnectionStatus.ConnectionStatus_Connected;
                } else {
                    // TODO: or connectionStatus = wallet.getConnectionStatus(); ?
                    connectionStatus = Wallet.ConnectionStatus.ConnectionStatus_Disconnected;
                }
            }
        }
        //Log.d(TAG, "updated daemon status: " + daemonHeight + "/" + connectionStatus.toString());
    }

    public long getDaemonHeight() {
        return this.daemonHeight;
    }

    public Wallet.ConnectionStatus getConnectionStatus() {
        return this.connectionStatus;
    }

    /////////////////////////////////////////////
    // communication back to client (activity) //
    /////////////////////////////////////////////
    // NB: This allows for only one observer, i.e. only a single activity bound here

    private Observer observer = null;

    public void setObserver(Observer anObserver) {
        observer = anObserver;
        Log.d(TAG, "setObserver " + observer);
    }

    public interface Observer {
        void onRefreshed(Wallet wallet, boolean full);

        void onProgress(String text);

        void onProgress(int n);

        void onWalletStored();
    }

    String progressText = null;
    int progressValue = -1;

    private void showProgress(String text) {
        progressText = text;
        if (observer != null) {
            observer.onProgress(text);
        }
    }

    private void showProgress(int n) {
        progressValue = n;
        if (observer != null) {
            observer.onProgress(n);
        }
    }

    public String getProgressText() {
        return progressText;
    }

    public int getProgressValue() {
        return progressValue;
    }

    //
    public Wallet getWallet() {
        if (listener == null) throw new IllegalStateException("no listener");
        return listener.getWallet();
    }

    /////////////////////////////////////////////
    /////////////////////////////////////////////

    private Looper mServiceLooper;
    private WalletService.ServiceHandler mServiceHandler;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "Handling " + msg.arg2);
            switch (msg.arg2) {
                case START_SERVICE: {
                    Bundle extras = msg.getData();
                    String cmd = extras.getString(REQUEST, null);
                    if (cmd.equals(REQUEST_CMD_LOAD)) {
                        String walletId = extras.getString(REQUEST_WALLET, null);
                        String walletPw = extras.getString(REQUEST_CMD_LOAD_PW, null);
                        Log.d(TAG, "LOAD wallet " + walletId);// + ":" + walletPw);
                        if (walletId != null) {
                            showProgress(getString(R.string.status_wallet_loading));
                            showProgress(10);
                            start(walletId, walletPw); // TODO What if this fails?
                        }
                    } else if (cmd.equals(REQUEST_CMD_STORE)) {
                        Wallet myWallet = getWallet();
                        Log.d(TAG, "storing wallet: " + myWallet.getName());
                        getWallet().store();
                        Log.d(TAG, "wallet stored: " + myWallet.getName());
                        if (observer != null) observer.onWalletStored();
                    }
                }
                break;
                case STOP_SERVICE:
                    stop();
                    break;
                default:
                    Log.e(TAG, "UNKNOWN " + msg.arg2);
            }
        }
    }

    @Override
    public void onCreate() {
        //mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //showNotification();

        // We are using a HandlerThread and a Looper to avoid loading and closing
        // concurrency
        MoneroHandlerThread thread = new MoneroHandlerThread("WalletService",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new WalletService.ServiceHandler(mServiceLooper);

        Log.d(TAG, "Service created");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        // Cancel the persistent notification.
        //mNM.cancel(NOTIFICATION);
        if (this.listener != null) {
            Log.w(TAG, "onDestroy() with active listener");
            // no need to stop() here because the wallet closing should have been triggered
            // through onUnbind() already
        }
    }

    public class WalletServiceBinder extends Binder {
        public WalletService getService() {
            return WalletService.this;
        }
    }

    private final IBinder mBinder = new WalletServiceBinder();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // when the activity satrts the service, it expects to start it for a new wallet
        // the service is possibly still occupied with saving the last opened wallet
        // so we queue the open request
        // this should not matter since the old activity is not getting updates
        // and the new one is not listening yet (although it will be bound)
        Log.d(TAG, "onStartCommand()");
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg2 = START_SERVICE;
        msg.setData(intent.getExtras());
        mServiceHandler.sendMessage(msg);
        //Log.d(TAG, "onStartCommand() message sent");
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Very first client binds
        Log.d(TAG, "onBind()");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind()");
        // All clients have unbound with unbindService()
        Message msg = mServiceHandler.obtainMessage();
        msg.arg2 = STOP_SERVICE;
        mServiceHandler.sendMessage(msg);
        Log.d(TAG, "onUnbind() message sent");
        return true; // true is important so that onUnbind is also called next time
    }

    private void start(String walletName, String walletPassword) {
        // if there is an listener it is always started / syncing
        Log.d(TAG, "start()");
        showProgress(getString(R.string.status_wallet_loading));
        showProgress(10);
        if (listener == null) {
            Log.d(TAG, "start() loadWallet");
            Wallet aWallet = loadWallet(walletName, walletPassword);
            listener = new MyWalletListener(aWallet);
            listener.start();
            showProgress(100);
        }
        showProgress(getString(R.string.status_wallet_connecting));
        showProgress(-1);
        // if we try to refresh the history here we get occasional segfaults!
        // doesnt matter since we update as soon as we get a new block anyway
        Log.d(TAG, "start() done");
    }

    public void stop() {
        Log.d(TAG, "stop()");
        setObserver(null); // in case it was not reset already
        if (listener != null) {
            listener.stop();
            Wallet myWallet = getWallet();
//            if (!myWallet.isSynchronized()) { // save only if NOT synced (to continue later)
//                Log.d(TAG, "stop() saving");
//                myWallet.store();
//            }
            Log.d(TAG, "stop() closing");
            myWallet.close();
            Log.d(TAG, "stop() closed");
            listener = null;
        }
        stopSelf();
        // TODO ensure the Looper & thread actually stop and go away?
    }

    private Wallet loadWallet(String walletName, String walletPassword) {
        String path = Helper.getWalletPath(getApplicationContext(), walletName);
        //Log.d(TAG, "open wallet " + path);
        Wallet wallet = openWallet(walletName, walletPassword);
        //Log.d(TAG, "wallet opened: " + wallet);
        if (wallet != null) {
            //Log.d(TAG, wallet.getStatus().toString());
            Log.d(TAG, "Using daemon " + WalletManager.getInstance().getDaemonAddress());
            showProgress(55);
            wallet.init(0);
            showProgress(90);
            Log.d(TAG, wallet.getConnectionStatus().toString());
        }
        return wallet;
    }

    private Wallet openWallet(String walletName, String walletPassword) {
        String path = Helper.getWalletPath(getApplicationContext(), walletName);
        showProgress(20);
        Wallet wallet = null;
        WalletManager walletMgr = WalletManager.getInstance();
        Log.d(TAG, "WalletManager testnet=" + walletMgr.isTestNet());
        showProgress(30);
        if (walletMgr.walletExists(path)) {
            Log.d(TAG, "open wallet " + path);
            wallet = walletMgr.openWallet(path, walletPassword);
            showProgress(60);
            Log.d(TAG, "wallet opened");
            Wallet.Status status = wallet.getStatus();
            Log.d(TAG, "wallet status is " + status);
            if (status != Wallet.Status.Status_Ok) {
                Log.d(TAG, "wallet status is " + status);
                WalletManager.getInstance().close(wallet); // TODO close() failed?
                wallet = null;
                // TODO what do we do with the progress??
                // TODO tell the activity this failed
            }
        }
        return wallet;
    }
}
