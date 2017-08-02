/**
 * Copyright (c) 2017 m2049r
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m2049r.xmrwallet;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.m2049r.xmrwallet.layout.TransactionInfoAdapter;
import com.m2049r.xmrwallet.model.TransactionInfo;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.service.WalletService;

import java.util.List;

public class WalletActivity extends AppCompatActivity
        implements TransactionInfoAdapter.OnInteractionListener, WalletService.Observer {
    private static final String TAG = "WalletActivity";

    public static final String REQUEST_ID = "id";
    public static final String REQUEST_PW = "pw";

    TransactionInfoAdapter adapter;

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
        acquireWakeLock();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String walletId = extras.getString(REQUEST_ID);
            String walletPassword = extras.getString(REQUEST_PW);
            connectWalletService(walletId, walletPassword);
        } else {
            throw new IllegalStateException("No extras passed! Panic!");
        }
        showProgress();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                onProgress(10); // look like we are working!
            }
        }, 250);
        //Log.d(TAG, "onStart() done.");
    }

    private String title = null;

    void setActivityTitle(Wallet wallet) {
        if ((wallet == null) || (title != null)) return;
        String shortName = wallet.getName();
        if (shortName.length() > 16) {
            shortName = shortName.substring(0, 14) + "...";
        }
        this.title = "[" + wallet.getAddress().substring(0, 6) + "] " + shortName;
        setTitle(this.title);
        onProgress(100);
        Log.d(TAG, "wallet title is " + this.title);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop()");
        releaseWakeLock();
        disconnectWalletService();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallet_activity);

        // TODO do stuff with savedInstanceState
        if (savedInstanceState != null) {
            return;
        }
        //Log.d(TAG, "no savedInstanceState");

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list);

        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecoration);

        this.adapter = new TransactionInfoAdapter(this);
        recyclerView.setAdapter(adapter);

        setTitle(getString(R.string.status_wallet_loading));
        //Log.d(TAG, "onCreate() done.");
    }

    private long firstBlock = 0;
    private boolean synced = false;

    private void updateStatus(Wallet wallet) {
        setActivityTitle(wallet);
        final TextView balanceView = (TextView) findViewById(R.id.tvBalance);
        final TextView unlockedView = (TextView) findViewById(R.id.tvUnlockedBalance);
        final TextView syncProgressView = (TextView) findViewById(R.id.tvBlockHeightProgress);
        final TextView connectionStatusView = (TextView) findViewById(R.id.tvConnectionStatus);

        //Wallet wallet = getWallet();
        balanceView.setText(Wallet.getDisplayAmount(wallet.getBalance()));
        unlockedView.setText(Wallet.getDisplayAmount(wallet.getUnlockedBalance()));
        String sync = "";
        if (wallet.getConnectionStatus() == Wallet.ConnectionStatus.ConnectionStatus_Connected) {
            if (!wallet.isSynchronized()) {
                long n = wallet.getDaemonBlockChainHeight() - wallet.getBlockChainHeight();
                sync = n + " " + getString(R.string.status_remaining);
                if (firstBlock == 0) {
                    firstBlock = wallet.getBlockChainHeight();
                }
                int x = 100 - Math.round(100f * n / (1f * wallet.getDaemonBlockChainHeight() - firstBlock));
                //Log.d(TAG, n + "/" + (wallet.getDaemonBlockChainHeight() - firstBlock));
                onProgress(getString(R.string.status_syncing) + " " + sync);
                onProgress(x);
            } else {
                sync = getString(R.string.status_synced) + ": " + wallet.getBlockChainHeight();
                if (!synced) {
                    hideProgress();
                    synced = true;
                }
            }
        }
        String t = (wallet.isTestNet() ? getString(R.string.connect_testnet) : getString(R.string.connect_mainnet));
        syncProgressView.setText(sync);
        connectionStatusView.setText(t + " " + wallet.getConnectionStatus().toString().substring(17));
    }

    @Override
    public void onRefreshed(final Wallet wallet, final boolean full) {
        Log.d(TAG, "onRefreshed()");
        if (wallet.isSynchronized()) {
            releaseWakeLock(); // the idea is to stay awake until synced
        }
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
            Toast.makeText(getApplicationContext(), getString(R.string.status_wallet_unloading), Toast.LENGTH_LONG).show();
            Log.d(TAG, "UNBOUND");
        }
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

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
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

    void releaseWakeLock() {
        if ((wl == null) || !wl.isHeld()) return;
        wl.release();
        wl = null;
        Log.d(TAG, "WakeLock released");
    }

    private void showProgress() {
        runOnUiThread(new Runnable() {
            public void run() {
                LinearLayout llProgress = (LinearLayout) findViewById(R.id.llProgress);
                llProgress.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideProgress() {
        runOnUiThread(new Runnable() {
            public void run() {
                LinearLayout llProgress = (LinearLayout) findViewById(R.id.llProgress);
                llProgress.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onProgress(final String text) {
        runOnUiThread(new Runnable() {
            public void run() {
                TextView progressText = (TextView) findViewById(R.id.tvProgress);
                progressText.setText(text);
            }
        });
    }

    @Override
    public void onProgress(final int n) {
        runOnUiThread(new Runnable() {
            public void run() {
                ProgressBar progress = (ProgressBar) findViewById(R.id.pbProgress);
                progress.setProgress(n);
            }
        });
    }
}
