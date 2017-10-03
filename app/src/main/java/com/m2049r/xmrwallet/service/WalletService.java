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

import android.app.Notification;
import android.app.PendingIntent;
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
import com.m2049r.xmrwallet.WalletActivity;
import com.m2049r.xmrwallet.model.PendingTransaction;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.model.WalletListener;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.TxData;

public class WalletService extends Service {
    public static boolean Running = false;

    final static String TAG = "WalletService";
    final static int NOTIFICATION_ID = 2049;

    public static final String REQUEST_WALLET = "wallet";
    public static final String REQUEST = "request";

    public static final String REQUEST_CMD_LOAD = "load";
    public static final String REQUEST_CMD_LOAD_PW = "walletPassword";

    public static final String REQUEST_CMD_STORE = "store";

    public static final String REQUEST_CMD_TX = "createTX";
    public static final String REQUEST_CMD_TX_DATA = "data";

    public static final String REQUEST_CMD_SWEEP = "sweepTX";

    public static final String REQUEST_CMD_SEND = "send";
    public static final String REQUEST_CMD_SEND_NOTES = "notes";

    public static final String REQUEST_CMD_SETNOTE = "setnote";
    public static final String REQUEST_CMD_SETNOTE_TX = "tx";
    public static final String REQUEST_CMD_SETNOTE_NOTES = "notes";

    public static final int START_SERVICE = 1;
    public static final int STOP_SERVICE = 2;

    private MyWalletListener listener = null;

    private class MyWalletListener implements WalletListener {
        boolean updated = true;

        void start() {
            Log.d(TAG, "MyWalletListener.start()");
            Wallet wallet = getWallet();
            if (wallet == null) throw new IllegalStateException("No wallet!");
            //acquireWakeLock();
            wallet.setListener(this);
            wallet.startRefresh();
        }

        void stop() {
            Log.d(TAG, "MyWalletListener.stop()");
            Wallet wallet = getWallet();
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
            Wallet wallet = getWallet();
            if (wallet == null) throw new IllegalStateException("No wallet!");
            // don't flood with an update for every block ...
            if (lastBlockTime < System.currentTimeMillis() - 2000) {
                Log.d(TAG, "newBlock() @" + height + " with observer " + observer);
                lastBlockTime = System.currentTimeMillis();
                if (observer != null) {
                    boolean fullRefresh = false;
                    updateDaemonState(wallet, wallet.isSynchronized() ? height : 0);
                    if (!wallet.isSynchronized()) {
                        updated = true;
                        // we want to see our transactions as they come in
                        wallet.getHistory().refresh();
                        int txCount = wallet.getHistory().getCount();
                        if (txCount > lastTxCount) {
                            // update the transaction list only if we have more than before
                            lastTxCount = txCount;
                            fullRefresh = true;
                        }
                    }
                    if (observer != null)
                        observer.onRefreshed(wallet, fullRefresh);
                }
            }
        }

        public void updated() {
            Log.d(TAG, "updated()");
            Wallet wallet = getWallet();
            if (wallet == null) throw new IllegalStateException("No wallet!");
            updated = true;
        }

        public void refreshed() {
            Log.d(TAG, "refreshed()");
            Wallet wallet = getWallet();
            if (wallet == null) throw new IllegalStateException("No wallet!");
            if (updated) {
                if (observer != null) {
                    updateDaemonState(wallet, 0);
                    wallet.getHistory().refresh();
                    if (observer != null) {
                        updated = !observer.onRefreshed(wallet, true);
                    }
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
                    // if we get a valid height, then obviously we are connected
                    connectionStatus = Wallet.ConnectionStatus.ConnectionStatus_Connected;
                } else {
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
        boolean onRefreshed(Wallet wallet, boolean full);

        void onProgress(String text);

        void onProgress(int n);

        void onWalletStored(boolean success);

        void onCreatedTransaction(PendingTransaction pendingTransaction);

        void onSentTransaction(boolean success);

        void onSetNotes(boolean success);

        void onWalletStarted(boolean success);
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
        return WalletManager.getInstance().getWallet();
    }

    /////////////////////////////////////////////
    /////////////////////////////////////////////

    private Looper mServiceLooper;
    private WalletService.ServiceHandler mServiceHandler;

    private boolean errorState = false;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "Handling " + msg.arg2);
            if (errorState) {
                Log.i(TAG, "In error state.");
                // also, we have already stopped ourselves
                return;
            }
            switch (msg.arg2) {
                case START_SERVICE: {
                    Bundle extras = msg.getData();
                    String cmd = extras.getString(REQUEST, null);
                    if (cmd.equals(REQUEST_CMD_LOAD)) {
                        String walletId = extras.getString(REQUEST_WALLET, null);
                        String walletPw = extras.getString(REQUEST_CMD_LOAD_PW, null);
                        Log.d(TAG, "LOAD wallet " + walletId);
                        if (walletId != null) {
                            showProgress(getString(R.string.status_wallet_loading));
                            showProgress(10);
                            boolean success = start(walletId, walletPw);
                            if (observer != null) observer.onWalletStarted(success);
                            if (!success) {
                                errorState = true;
                                stop();
                            }
                        }
                    } else if (cmd.equals(REQUEST_CMD_STORE)) {
                        Wallet myWallet = getWallet();
                        Log.d(TAG, "STORE wallet: " + myWallet.getName());
                        boolean rc = myWallet.store();
                        Log.d(TAG, "wallet stored: " + myWallet.getName() + " with rc=" + rc);
                        if (!rc) {
                            Log.d(TAG, "Wallet store failed: " + myWallet.getErrorString());
                        }
                        if (observer != null) observer.onWalletStored(rc);
                    } else if (cmd.equals(REQUEST_CMD_TX)) {
                        Wallet myWallet = getWallet();
                        Log.d(TAG, "CREATE TX for wallet: " + myWallet.getName());
                        TxData txData = extras.getParcelable(REQUEST_CMD_TX_DATA);
                        PendingTransaction pendingTransaction = myWallet.createTransaction(
                                txData.dst_addr, txData.paymentId, txData.amount, txData.mixin, txData.priority);
                        PendingTransaction.Status status = pendingTransaction.getStatus();
                        Log.d(TAG, "transaction status " + status);
                        if (status != PendingTransaction.Status.Status_Ok) {
                            Log.d(TAG, "Create Transaction failed: " + pendingTransaction.getErrorString());
                        }
                        if (observer != null) {
                            observer.onCreatedTransaction(pendingTransaction);
                        } else {
                            myWallet.disposePendingTransaction();
                        }
                    } else if (cmd.equals(REQUEST_CMD_SWEEP)) {
                        Wallet myWallet = getWallet();
                        Log.d(TAG, "SWEEP TX for wallet: " + myWallet.getName());
                        PendingTransaction pendingTransaction = myWallet.createSweepUnmixableTransaction();
                        PendingTransaction.Status status = pendingTransaction.getStatus();
                        Log.d(TAG, "transaction status " + status);
                        if (status != PendingTransaction.Status.Status_Ok) {
                            Log.d(TAG, "Create Transaction failed: " + pendingTransaction.getErrorString());
                        }
                        if (observer != null) {
                            observer.onCreatedTransaction(pendingTransaction);
                        } else {
                            myWallet.disposePendingTransaction();
                        }
                    } else if (cmd.equals(REQUEST_CMD_SEND)) {
                        Wallet myWallet = getWallet();
                        Log.d(TAG, "SEND TX for wallet: " + myWallet.getName());
                        PendingTransaction pendingTransaction = myWallet.getPendingTransaction();
                        if (pendingTransaction.getStatus() != PendingTransaction.Status.Status_Ok) {
                            Log.e(TAG, "PendingTransaction is " + pendingTransaction.getStatus());
                            myWallet.disposePendingTransaction(); // it's broken anyway
                            if (observer != null) observer.onSentTransaction(false);
                            return;
                        }
                        String txid = pendingTransaction.getFirstTxId();
                        boolean success = pendingTransaction.commit("", true);
                        myWallet.disposePendingTransaction();
                        if (observer != null) observer.onSentTransaction(success);
                        if (success) {
                            String notes = extras.getString(REQUEST_CMD_SEND_NOTES);
                            if ((notes != null) && (!notes.isEmpty())) {
                                myWallet.setUserNote(txid, notes);
                            }
                            boolean rc = myWallet.store();
                            Log.d(TAG, "wallet stored: " + myWallet.getName() + " with rc=" + rc);
                            if (!rc) {
                                Log.d(TAG, "Wallet store failed: " + myWallet.getErrorString());
                            }
                            if (observer != null) observer.onWalletStored(rc);
                            listener.updated = true;
                        }
                    } else if (cmd.equals(REQUEST_CMD_SETNOTE)) {
                        Wallet myWallet = getWallet();
                        Log.d(TAG, "SET NOTE for wallet: " + myWallet.getName());
                        String txId = extras.getString(REQUEST_CMD_SETNOTE_TX);
                        String notes = extras.getString(REQUEST_CMD_SETNOTE_NOTES);
                        if ((txId != null) && (notes != null)) {
                            boolean success = myWallet.setUserNote(txId, notes);
                            if (!success) {
                                Log.e(TAG, myWallet.getErrorString());
                            }
                            if (observer != null) observer.onSetNotes(success);
                            if (success) {
                                boolean rc = myWallet.store();
                                Log.d(TAG, "wallet stored: " + myWallet.getName() + " with rc=" + rc);
                                if (!rc) {
                                    Log.d(TAG, "Wallet store failed: " + myWallet.getErrorString());
                                }
                                if (observer != null) observer.onWalletStored(rc);
                            }
                        }
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
        Running = true;
        // when the activity starts the service, it expects to start it for a new wallet
        // the service is possibly still occupied with saving the last opened wallet
        // so we queue the open request
        // this should not matter since the old activity is not getting updates
        // and the new one is not listening yet (although it will be bound)
        Log.d(TAG, "onStartCommand()");
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg2 = START_SERVICE;
        if (intent != null) {
            msg.setData(intent.getExtras());
            mServiceHandler.sendMessage(msg);
            return START_STICKY;
        } else {
            // process restart - don't do anything - let system kill it again
            stop();
            return START_NOT_STICKY;
        }
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

    private boolean start(String walletName, String walletPassword) {
        Log.d(TAG, "start()");
        startNotfication();
        showProgress(getString(R.string.status_wallet_loading));
        showProgress(10);
        if (listener == null) {
            Log.d(TAG, "start() loadWallet");
            Wallet aWallet = loadWallet(walletName, walletPassword);
            if ((aWallet == null) || (aWallet.getConnectionStatus() != Wallet.ConnectionStatus.ConnectionStatus_Connected)) {
                if (aWallet != null) aWallet.close();
                return false;
            }
            listener = new MyWalletListener();
            listener.start();
            showProgress(100);
        }
        showProgress(getString(R.string.status_wallet_connecting));
        showProgress(-1);
        // if we try to refresh the history here we get occasional segfaults!
        // doesnt matter since we update as soon as we get a new block anyway
        Log.d(TAG, "start() done");
        return true;
    }

    public void stop() {
        Log.d(TAG, "stop()");
        setObserver(null); // in case it was not reset already
        if (listener != null) {
            listener.stop();
            Wallet myWallet = getWallet();
            Log.d(TAG, "stop() closing");
            myWallet.close();
            Log.d(TAG, "stop() closed");
            listener = null;
        }
        stopForeground(true);
        stopSelf();
        Running = false;
    }

    private Wallet loadWallet(String walletName, String walletPassword) {
        //String path = Helper.getWalletPath(getApplicationContext(), walletName);
        //Log.d(TAG, "open wallet " + path);
        Wallet wallet = openWallet(walletName, walletPassword);
        //Log.d(TAG, "wallet opened: " + wallet);
        if (wallet != null) {
            //Log.d(TAG, wallet.getStatus().toString());
            Log.d(TAG, "Using daemon " + WalletManager.getInstance().getDaemonAddress());
            showProgress(55);
            wallet.init(0);
            showProgress(90);
            //Log.d(TAG, wallet.getConnectionStatus().toString());
        }
        return wallet;
    }

    private Wallet openWallet(String walletName, String walletPassword) {
        String path = Helper.getWalletFile(getApplicationContext(), walletName).getAbsolutePath();
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
                // this crashes in MyWalletListener(Wallet aWallet) as wallet == null
            }
        }
        return wallet;
    }

    private void startNotfication() {
        Intent notificationIntent = new Intent(this, WalletActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new Notification.Builder(this)
                .setContentTitle(getString(R.string.service_description))
                .setSmallIcon(R.drawable.ic_monerujo)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }
}
