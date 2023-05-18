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
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.WalletActivity;
import com.m2049r.xmrwallet.data.TxData;
import com.m2049r.xmrwallet.model.PendingTransaction;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.model.WalletListener;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.LocaleHelper;
import com.m2049r.xmrwallet.util.NetCipherHelper;

import timber.log.Timber;

public class WalletService extends Service {
    public static boolean Running = false;

    final static int NOTIFICATION_ID = 2049;
    final static String CHANNEL_ID = "m_service";

    public static final String REQUEST_WALLET = "wallet";
    public static final String REQUEST = "request";

    public static final String REQUEST_CMD_LOAD = "load";
    public static final String REQUEST_CMD_LOAD_PW = "walletPassword";

    public static final String REQUEST_CMD_STORE = "store";

    public static final String REQUEST_CMD_TX = "createTX";
    public static final String REQUEST_CMD_TX_DATA = "data";
    public static final String REQUEST_CMD_TX_TAG = "tag";

    public static final String REQUEST_CMD_SWEEP = "sweepTX";

    public static final String REQUEST_CMD_SEND = "send";
    public static final String REQUEST_CMD_SEND_NOTES = "notes";

    public static final int START_SERVICE = 1;
    public static final int STOP_SERVICE = 2;

    private MyWalletListener listener = null;

    private class MyWalletListener implements WalletListener {
        boolean updated = true;

        void start() {
            Timber.d("MyWalletListener.start()");
            Wallet wallet = getWallet();
            if (wallet == null) throw new IllegalStateException("No wallet!");
            wallet.setListener(this);
            wallet.startRefresh();
        }

        void stop() {
            Timber.d("MyWalletListener.stop()");
            Wallet wallet = getWallet();
            if (wallet == null) throw new IllegalStateException("No wallet!");
            wallet.pauseRefresh();
            wallet.setListener(null);
        }

        // WalletListener callbacks
        public void moneySpent(String txId, long amount) {
            Timber.d("moneySpent() %d @ %s", amount, txId);
        }

        public void moneyReceived(String txId, long amount) {
            Timber.d("moneyReceived() %d @ %s", amount, txId);
        }

        public void unconfirmedMoneyReceived(String txId, long amount) {
            Timber.d("unconfirmedMoneyReceived() %d @ %s", amount, txId);
        }

        private long lastBlockTime = 0;
        private int lastTxCount = 0;

        public void newBlock(long height) {
            final Wallet wallet = getWallet();
            if (wallet == null) throw new IllegalStateException("No wallet!");
            // don't flood with an update for every block ...
            if (lastBlockTime < System.currentTimeMillis() - 2000) {
                lastBlockTime = System.currentTimeMillis();
                Timber.d("newBlock() @ %d with observer %s", height, observer);
                if (observer != null) {
                    boolean fullRefresh = false;
                    updateDaemonState(wallet, wallet.isSynchronized() ? height : 0);
                    if (!wallet.isSynchronized()) {
                        updated = true;
                        // we want to see our transactions as they come in
                        wallet.refreshHistory();
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
            Timber.d("updated()");
            Wallet wallet = getWallet();
            if (wallet == null) throw new IllegalStateException("No wallet!");
            updated = true;
        }

        public void refreshed() { // this means it's synced
            Timber.d("refreshed()");
            final Wallet wallet = getWallet();
            if (wallet == null) throw new IllegalStateException("No wallet!");
            wallet.setSynchronized();
            if (updated) {
                updateDaemonState(wallet, wallet.getBlockChainHeight());
                wallet.refreshHistory();
                if (observer != null) {
                    updated = !observer.onRefreshed(wallet, true);
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
        Timber.d("setObserver %s", observer);
    }

    public interface Observer {
        boolean onRefreshed(Wallet wallet, boolean full);

        void onProgress(String text);

        void onProgress(int n);

        void onWalletStored(boolean success);

        void onTransactionCreated(String tag, PendingTransaction pendingTransaction);

        void onTransactionSent(String txid);

        void onSendTransactionFailed(String error);

        void onWalletStarted(Wallet.Status walletStatus);

        void onWalletOpen(Wallet.Device device);
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

    private WalletService.ServiceHandler mServiceHandler;

    private boolean errorState = false;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Timber.d("Handling %s", msg.arg2);
            if (errorState) {
                Timber.i("In error state.");
                // also, we have already stopped ourselves
                return;
            }
            switch (msg.arg2) {
                case START_SERVICE: {
                    Bundle extras = msg.getData();
                    String cmd = extras.getString(REQUEST, null);
                    switch (cmd) {
                        case REQUEST_CMD_LOAD:
                            String walletId = extras.getString(REQUEST_WALLET, null);
                            String walletPw = extras.getString(REQUEST_CMD_LOAD_PW, null);
                            Timber.d("LOAD wallet %s", walletId);
                            if (walletId != null) {
                                showProgress(getString(R.string.status_wallet_loading));
                                showProgress(10);
                                Wallet.Status walletStatus = start(walletId, walletPw);
                                if (observer != null) observer.onWalletStarted(walletStatus);
                                if ((walletStatus == null) || !walletStatus.isOk()) {
                                    errorState = true;
                                    stop();
                                }
                            }
                            break;
                        case REQUEST_CMD_STORE: {
                            Wallet myWallet = getWallet();
                            if (myWallet == null) break;
                            Timber.d("STORE wallet: %s", myWallet.getName());
                            boolean rc = myWallet.store();
                            Timber.d("wallet stored: %s with rc=%b", myWallet.getName(), rc);
                            if (!rc) {
                                Timber.w("Wallet store failed: %s", myWallet.getStatus().getErrorString());
                            }
                            if (observer != null) observer.onWalletStored(rc);
                            break;
                        }
                        case REQUEST_CMD_TX: {
                            Wallet myWallet = getWallet();
                            if (myWallet == null) break;
                            Timber.d("CREATE TX for wallet: %s", myWallet.getName());
                            myWallet.disposePendingTransaction(); // remove any old pending tx

                            TxData txData = extras.getParcelable(REQUEST_CMD_TX_DATA);
                            String txTag = extras.getString(REQUEST_CMD_TX_TAG);
                            assert txData != null;
                            txData.createPocketChange(myWallet);
                            PendingTransaction pendingTransaction = myWallet.createTransaction(txData);
                            PendingTransaction.Status status = pendingTransaction.getStatus();
                            if (status != PendingTransaction.Status.Status_Ok) {
                                Timber.w("Create Transaction failed: %s", pendingTransaction.getErrorString());
                            }
                            if (observer != null) {
                                observer.onTransactionCreated(txTag, pendingTransaction);
                            } else {
                                myWallet.disposePendingTransaction();
                            }
                            break;
                        }
                        case REQUEST_CMD_SWEEP: {
                            Wallet myWallet = getWallet();
                            if (myWallet == null) break;
                            Timber.d("SWEEP TX for wallet: %s", myWallet.getName());
                            myWallet.disposePendingTransaction(); // remove any old pending tx

                            String txTag = extras.getString(REQUEST_CMD_TX_TAG);
                            PendingTransaction pendingTransaction = myWallet.createSweepUnmixableTransaction();
                            PendingTransaction.Status status = pendingTransaction.getStatus();
                            Timber.d("transaction status %s", status);
                            if (status != PendingTransaction.Status.Status_Ok) {
                                Timber.w("Create Transaction failed: %s", pendingTransaction.getErrorString());
                            }
                            if (observer != null) {
                                observer.onTransactionCreated(txTag, pendingTransaction);
                            } else {
                                myWallet.disposePendingTransaction();
                            }
                            break;
                        }
                        case REQUEST_CMD_SEND: {
                            Wallet myWallet = getWallet();
                            if (myWallet == null) break;
                            Timber.d("SEND TX for wallet: %s", myWallet.getName());
                            PendingTransaction pendingTransaction = myWallet.getPendingTransaction();
                            if (pendingTransaction == null) {
                                throw new IllegalArgumentException("PendingTransaction is null"); // die
                            }
                            if (pendingTransaction.getStatus() != PendingTransaction.Status.Status_Ok) {
                                Timber.e("PendingTransaction is %s", pendingTransaction.getStatus());
                                final String error = pendingTransaction.getErrorString();
                                myWallet.disposePendingTransaction(); // it's broken anyway
                                if (observer != null) observer.onSendTransactionFailed(error);
                                return;
                            }
                            final String txid = pendingTransaction.getFirstTxId(); // tx ids vanish after commit()!

                            boolean success = pendingTransaction.commit("", true);
                            if (success) {
                                myWallet.disposePendingTransaction();
                                if (observer != null) observer.onTransactionSent(txid);
                                String notes = extras.getString(REQUEST_CMD_SEND_NOTES);
                                if ((notes != null) && (!notes.isEmpty())) {
                                    myWallet.setUserNote(txid, notes);
                                }
                                boolean rc = myWallet.store();
                                Timber.d("wallet stored: %s with rc=%b", myWallet.getName(), rc);
                                if (!rc) {
                                    Timber.w("Wallet store failed: %s", myWallet.getStatus().getErrorString());
                                }
                                if (observer != null) observer.onWalletStored(rc);
                                listener.updated = true;
                            } else {
                                final String error = pendingTransaction.getErrorString();
                                myWallet.disposePendingTransaction();
                                if (observer != null) observer.onSendTransactionFailed(error);
                                return;
                            }
                            break;
                        }
                    }
                }
                break;
                case STOP_SERVICE:
                    stop();
                    break;
                default:
                    Timber.e("UNKNOWN %s", msg.arg2);
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
        final Looper serviceLooper = thread.getLooper();
        mServiceHandler = new WalletService.ServiceHandler(serviceLooper);

        Timber.d("Service created");
    }

    @Override
    public void onDestroy() {
        Timber.d("onDestroy()");
        if (this.listener != null) {
            Timber.w("onDestroy() with active listener");
            // no need to stop() here because the wallet closing should have been triggered
            // through onUnbind() already
        }
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(LocaleHelper.setPreferredLocale(context));
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
        Timber.d("onStartCommand()");
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
        Timber.d("onBind()");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Timber.d("onUnbind()");
        // All clients have unbound with unbindService()
        Message msg = mServiceHandler.obtainMessage();
        msg.arg2 = STOP_SERVICE;
        mServiceHandler.sendMessage(msg);
        Timber.d("onUnbind() message sent");
        return true; // true is important so that onUnbind is also called next time
    }

    @Nullable
    private Wallet.Status start(String walletName, String walletPassword) {
        Timber.d("start()");
        startNotfication();
        showProgress(getString(R.string.status_wallet_loading));
        showProgress(10);
        if (listener == null) {
            Timber.d("start() loadWallet");
            Wallet aWallet = loadWallet(walletName, walletPassword);
            if (aWallet == null) return null;
            Wallet.Status walletStatus = aWallet.getFullStatus();
            if (!walletStatus.isOk()) {
                aWallet.close();
                return walletStatus;
            }
            listener = new MyWalletListener();
            listener.start();
            showProgress(100);
        }
        showProgress(getString(R.string.status_wallet_connecting));
        showProgress(101);
        // if we try to refresh the history here we get occasional segfaults!
        // doesnt matter since we update as soon as we get a new block anyway
        Timber.d("start() done");
        return getWallet().getFullStatus();
    }

    public void stop() {
        Timber.d("stop()");
        setObserver(null); // in case it was not reset already
        if (listener != null) {
            listener.stop();
            Wallet myWallet = getWallet();
            Timber.d("stop() closing");
            myWallet.close();
            Timber.d("stop() closed");
            listener = null;
        }
        stopForeground(true);
        stopSelf();
        Running = false;
    }

    private Wallet loadWallet(String walletName, String walletPassword) {
        Wallet wallet = openWallet(walletName, walletPassword);
        if (wallet != null) {
            Timber.d("Using daemon %s", WalletManager.getInstance().getDaemonAddress());
            showProgress(55);
            wallet.init(0);
            wallet.setProxy(NetCipherHelper.getProxy());
            showProgress(90);
        }
        return wallet;
    }

    private Wallet openWallet(String walletName, String walletPassword) {
        String path = Helper.getWalletFile(getApplicationContext(), walletName).getAbsolutePath();
        showProgress(20);
        Wallet wallet = null;
        WalletManager walletMgr = WalletManager.getInstance();
        Timber.d("WalletManager network=%s", walletMgr.getNetworkType().name());
        showProgress(30);
        if (walletMgr.walletExists(path)) {
            Timber.d("open wallet %s", path);
            Wallet.Device device = WalletManager.getInstance().queryWalletDevice(path + ".keys", walletPassword);
            Timber.d("device is %s", device.toString());
            if (observer != null) observer.onWalletOpen(device);
            wallet = walletMgr.openWallet(path, walletPassword);
            showProgress(60);
            Timber.d("wallet opened");
            Wallet.Status walletStatus = wallet.getStatus();
            if (!walletStatus.isOk()) {
                Timber.d("wallet status is %s", walletStatus);
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
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel() : "";
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle(getString(R.string.service_description))
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_monerujo)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.service_description),
                NotificationManager.IMPORTANCE_LOW);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return CHANNEL_ID;
    }
}
