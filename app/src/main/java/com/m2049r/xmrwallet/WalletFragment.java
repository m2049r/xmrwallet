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

import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.m2049r.xmrwallet.layout.TransactionInfoAdapter;
import com.m2049r.xmrwallet.model.TransactionInfo;
import com.m2049r.xmrwallet.model.Wallet;

import java.util.List;

public class WalletFragment extends Fragment implements TransactionInfoAdapter.OnInteractionListener {
    private static final String TAG = "WalletFragment";
    private TransactionInfoAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.wallet_fragment, container, false);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecoration);

        this.adapter = new TransactionInfoAdapter(this);
        recyclerView.setAdapter(adapter);

        activityCallback.setTitle(getString(R.string.status_wallet_loading));

        return view;
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

    // called from activity
    public void onRefreshed(final Wallet wallet, final boolean full) {
        Log.d(TAG, "onRefreshed()");
        if (full) {
            List<TransactionInfo> list = wallet.getHistory().getAll();
            adapter.setInfos(list);
            adapter.notifyDataSetChanged();
        }
        updateStatus(wallet);
    }

    public void onProgress(final String text) {
        TextView progressText = (TextView) getView().findViewById(R.id.tvProgress);
        if (text != null) {
            progressText.setText(text);
            showProgress(); //TODO optimize this
        } else {
            hideProgress();
            progressText.setText(getString(R.string.status_working));
            onProgress(-1);
        }
    }

    public void onProgress(final int n) {
        ProgressBar progress = (ProgressBar) getView().findViewById(R.id.pbProgress);
        if (n >= 0) {
            progress.setIndeterminate(false);
            progress.setProgress(n);
        } else {
            progress.setIndeterminate(true);
        }
    }

    public void showProgress() {
        LinearLayout llProgress = (LinearLayout) getView().findViewById(R.id.llProgress);
        llProgress.setVisibility(View.VISIBLE);
    }

    public void hideProgress() {
        LinearLayout llProgress = (LinearLayout) getView().findViewById(R.id.llProgress);
        llProgress.setVisibility(View.GONE);
    }

    String setActivityTitle(Wallet wallet) {
        if (wallet == null) return null;
        String shortName = wallet.getName();
        if (shortName.length() > 16) {
            shortName = shortName.substring(0, 14) + "...";
        }
        String title = "[" + wallet.getAddress().substring(0, 6) + "] " + shortName;
        activityCallback.setTitle(title);
        Log.d(TAG, "wallet title is " + title);
        return title;
    }


    private long firstBlock = 0;
    private String walletTitle = null;

    private void updateStatus(Wallet wallet) {
        Log.d(TAG, "updateStatus()");
        if (walletTitle == null) {
            walletTitle = setActivityTitle(wallet);
            onProgress(100); // of loading
        }
        final TextView balanceView = (TextView) getView().findViewById(R.id.tvBalance);
        final TextView unlockedView = (TextView) getView().findViewById(R.id.tvUnlockedBalance);
        final TextView syncProgressView = (TextView) getView().findViewById(R.id.tvBlockHeightProgress);
        final TextView connectionStatusView = (TextView) getView().findViewById(R.id.tvConnectionStatus);
        balanceView.setText(Wallet.getDisplayAmount(wallet.getBalance()));
        unlockedView.setText(Wallet.getDisplayAmount(wallet.getUnlockedBalance()));
        String sync = "";
        if (!activityCallback.hasBoundService())
            throw new IllegalStateException("WalletService not bound.");
        Wallet.ConnectionStatus daemonConnected = activityCallback.getConnectionStatus();
        if (daemonConnected == Wallet.ConnectionStatus.ConnectionStatus_Connected) {
            long daemonHeight = activityCallback.getDaemonHeight();
            if (!wallet.isSynchronized()) {
                long n = daemonHeight - wallet.getBlockChainHeight();
                sync = n + " " + getString(R.string.status_remaining);
                if (firstBlock == 0) {
                    firstBlock = wallet.getBlockChainHeight();
                }
                int x = 100 - Math.round(100f * n / (1f * daemonHeight - firstBlock));
                onProgress(getString(R.string.status_syncing) + " " + sync);
                if (x == 0) x = -1;
                onProgress(x);
            } else {
                sync = getString(R.string.status_synced) + ": " + wallet.getBlockChainHeight();
            }
        }
        String net = (wallet.isTestNet() ? getString(R.string.connect_testnet) : getString(R.string.connect_mainnet));
        syncProgressView.setText(sync);
        connectionStatusView.setText(net + " " + daemonConnected.toString().substring(17));
    }

    Listener activityCallback;

    // Container Activity must implement this interface
    public interface Listener {
        boolean hasBoundService();

        Wallet.ConnectionStatus getConnectionStatus();

        long getDaemonHeight(); //mBoundService.getDaemonHeight();

        void setTitle(String title);

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            this.activityCallback = (Listener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement Listener");
        }
    }
}
