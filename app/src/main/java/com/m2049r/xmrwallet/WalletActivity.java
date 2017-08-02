package com.m2049r.xmrwallet;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.m2049r.xmrwallet.layout.TransactionInfoAdapter;
import com.m2049r.xmrwallet.model.TransactionInfo;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.model.WalletListener;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.Helper;

import java.io.File;
import java.util.List;

public class WalletActivity extends AppCompatActivity implements TransactionInfoAdapter.OnInteractionListener {
    private static final String TAG = "WalletActivity";

    TransactionInfoAdapter adapter;

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        Toast.makeText(this, getString(R.string.status_wallet_unloading), Toast.LENGTH_LONG).show();
        if (walletControl != null) {
            walletControl.stop();
            walletControl.destroy();
            walletControl = null;
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        Toast.makeText(this, getString(R.string.status_wallet_loading), Toast.LENGTH_LONG).show();
        load();
        super.onResume();
        setActivityTitle();
        if (walletControl != null) {
            updateStatus(walletControl.wallet);
        }
    }

    private String walletName = null;
    private String walletPassword = null;
    private boolean testnet = true;
    private String daemon = null;

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            this.walletName = extras.getString("wallet");
            this.walletPassword = extras.getString("password");
            this.daemon = extras.getString("daemon", "localhost:28081");
            this.testnet = extras.getBoolean("testnet", true);
        } else {
            throw new IllegalStateException("No extras passed! Panic!");
        }
        Log.d(TAG, "onStart() done.");
    }

    WalletControl walletControl;

    void load() {
        if (walletControl == null) {
            Log.d(TAG, "load wallet");
            Wallet aWallet = getOrCreateTestWallet();
            walletControl = new WalletControl(aWallet);
            walletControl.start();
            Log.d(TAG, "control started");
        }
    }

    void setActivityTitle() {
        if (walletControl != null) {
            String shortName = new File(walletControl.wallet.getPath()).getName();
            if (shortName.length() > 16) {
                shortName = shortName.substring(0, 14) + "...";
            }
            setTitle("[" + walletControl.wallet.getAddress().substring(0, 6) + "] " + shortName);
        } else {
            setTitle(getString(R.string.prompt_problems));
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop()");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        setContentView(R.layout.wallet_activity);

        // TODO do stuff with savedInstanceState
        if (savedInstanceState != null) {
            return;
        }

        Log.d(TAG, "no savedInstanceState");

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list);

        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecoration);

        this.adapter = new TransactionInfoAdapter(this);
        recyclerView.setAdapter(adapter);

        setTitle(getString(R.string.status_wallet_loading));

        Log.d(TAG, "onCreate() done.");
    }

    void updateStatus(Wallet wallet) {
        final TextView balanceView = (TextView) findViewById(R.id.tvBalance);
        final TextView unlockedView = (TextView) findViewById(R.id.tvUnlockedBalance);
        final TextView syncProgressView = (TextView) findViewById(R.id.tvBlockHeightProgress);
        final TextView connectionStatusView = (TextView) findViewById(R.id.tvConnectionStatus);

        balanceView.setText(Wallet.getDisplayAmount(wallet.getBalance()));
        unlockedView.setText(Wallet.getDisplayAmount(wallet.getUnlockedBalance()));
        String sync = "";
        if (wallet.getConnectionStatus() == Wallet.ConnectionStatus.ConnectionStatus_Connected) {
            if (!wallet.isSynchronized()) {
                long n = wallet.getDaemonBlockChainHeight() - wallet.getBlockChainHeight();
                sync = n + " " + getString(R.string.status_remaining);
            } else {
                sync = getString(R.string.status_synced) + ": " + wallet.getBlockChainHeight();
            }
        }
        String t = (wallet.isTestNet() ? getString(R.string.connect_testnet) : getString(R.string.connect_mainnet));
        syncProgressView.setText(sync);
        connectionStatusView.setText(t + " " + wallet.getConnectionStatus().toString().substring(17));
    }

    public void onRefreshed(final Wallet wallet, final boolean full) {
        Log.d(TAG, "onRefreshed()");
        runOnUiThread(new Runnable() {
            public void run() {
                if (full) {
                    List<TransactionInfo> list = wallet.getHistory().getAll();
                    adapter.setInfos(list);
                    adapter.notifyDataSetChanged();
                }
                updateStatus(wallet);
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "NEW INTENT");
        // and ignore it
    }

    // Callbacks from TransactionInfoAdapter
    @Override
    public void onInteraction(final View view, final TransactionInfo infoItem) {
        final Context ctx = view.getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("Transaction details");

        builder.setNegativeButton("Copy TX ID", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ClipboardManager clipboardManager = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("TX", infoItem.getHash());
                clipboardManager.setPrimaryClip(clip);
            }
        });

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.setMessage("TX ID: " + infoItem.getHash() +
                "\nPayment ID: " + infoItem.getPaymentId() +
                "\nBlockHeight: " + infoItem.getBlockHeight() +
                "\nAmount: " + Wallet.getDisplayAmount(infoItem.getAmount()) +
                "\nFee: " + Wallet.getDisplayAmount(infoItem.getFee()));
        AlertDialog alert1 = builder.create();
        alert1.show();
    }

    private class WalletControl implements WalletListener {
        private Wallet wallet;
        boolean updated = true;

        WalletControl(Wallet aWallet) {
            if (aWallet == null) throw new IllegalArgumentException("Cannot open wallet!");
            this.wallet = aWallet;
        }

        private void start() {
            Log.d(TAG, "start()");
            if (wallet == null) throw new IllegalStateException("No wallet!");
            acquireWakeLock();
            wallet.setListener(this);
            wallet.startRefresh();
        }

        private void stop() {
            Log.d(TAG, "stop()");
            if (wallet == null) throw new IllegalStateException("No wallet!");
            wallet.pauseRefresh();
            wallet.setListener(null);
            releaseWakeLock();
        }

        private void destroy() {
            if (wallet == null) throw new IllegalStateException("No wallet!");
            Log.d(TAG, "closing");
            wallet.close();
            Log.d(TAG, "closed");
            wallet = null;
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

        void releaseWakeLock() {
            if ((wl == null) || !wl.isHeld()) return;
            wl.release();
            wl = null;
            Log.d(TAG, "WakeLock released");
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////
        ///   WalletListener callbacks
        ////////////////////////////////////////////////////////////////////////////////////////////////
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

        public void newBlock(long height) {
            if (lastBlockTime < System.currentTimeMillis() - 2000) {
                Log.d(TAG, "newBlock() " + height);
                lastBlockTime = System.currentTimeMillis();
                onRefreshed(wallet, false);
            }
        }

        public void updated() {
            Log.d(TAG, "updated() " + wallet.getBalance());
            updated = true;
        }

        public void refreshed() {
            Log.d(TAG, "refreshed() " + wallet.getBalance() + " sync=" + wallet.isSynchronized());
            if (wallet.isSynchronized()) {
                releaseWakeLock(); // the idea is to stay awake until synced
            }
            if (updated) {
                wallet.getHistory().refresh();
                onRefreshed(wallet, true);
                updated = false;
            } else {
                onRefreshed(wallet, false);
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private Wallet getOrCreateTestWallet() {
        String path = Helper.getWalletPath(getApplicationContext(), this.walletName);
        WalletManager.getInstance().setDaemonAddress(this.daemon);
        Log.d(TAG, "prewallet " + path);
        Wallet wallet = getWallet(path, this.walletPassword, this.testnet);
        if (wallet == null) {
            Log.d(TAG, "creating wallet ...");
            wallet = createTestWallet(path, this.walletPassword, this.testnet);
        }
        Log.d(TAG, "postwallet " + wallet);
        if (wallet != null) {
            Log.d(TAG, wallet.getStatus().toString());
            Log.d(TAG, "Using daemon " + this.daemon);
            wallet.init(this.daemon, 0);
            Log.d(TAG, wallet.getConnectionStatus().toString());
        }
        return wallet;
    }

    private Wallet getWallet(String path, String password, boolean testnet) {
        Wallet wallet = null;
        WalletManager walletMgr = WalletManager.getInstance();
        Log.d(TAG, "got WalletManager testnet=" + testnet);
        if (walletMgr.walletExists(path)) {
            Log.d(TAG, "open wallet " + path);
            wallet = walletMgr.openWallet(path, password, testnet);
            Log.d(TAG, "opened wallet");
            Wallet.Status status = wallet.getStatus();
            Log.d(TAG, "wallet status is " + status);
            if (status != Wallet.Status.Status_Ok) {
                Log.d(TAG, "wallet status is " + status);
                wallet.close();
                wallet = null;
            }
        }
        return wallet;
    }

    private Wallet createTestWallet(String path, String password, boolean testnet) {
        long restoreHeight = 0;
        String seed = "camp feline inflamed memoir afloat eight alerts females " +
                "gutter cogs menu waveform gather tawny judge gusts " +
                "yahoo doctor females biscuit alchemy reef agony austere camp";
        WalletManager walletMgr = WalletManager.getInstance();
        Wallet wallet = walletMgr.recoveryWallet(path, seed, testnet, restoreHeight);
        wallet.setPassword(password);
        return wallet;
    }
}
