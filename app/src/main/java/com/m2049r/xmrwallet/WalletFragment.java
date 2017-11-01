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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.m2049r.xmrwallet.layout.AsyncExchangeRate;
import com.m2049r.xmrwallet.layout.Toolbar;
import com.m2049r.xmrwallet.layout.TransactionInfoAdapter;
import com.m2049r.xmrwallet.model.TransactionInfo;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.util.Helper;

import java.text.NumberFormat;
import java.util.List;

public class WalletFragment extends Fragment
        implements TransactionInfoAdapter.OnInteractionListener,
        AsyncExchangeRate.Listener {
    private static final String TAG = "WalletFragment";
    private TransactionInfoAdapter adapter;
    private NumberFormat formatter = NumberFormat.getInstance();

    private FrameLayout flExchange;
    private TextView tvBalance;
    private TextView tvUnconfirmedAmount;
    private TextView tvProgress;
    private ImageView ivSynced;
    private ProgressBar pbProgress;
    private Button bReceive;
    private Button bSend;

    private Spinner sCurrency;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (activityCallback.hasWallet())
            inflater.inflate(R.menu.wallet_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        flExchange = (FrameLayout) view.findViewById(R.id.flExchange);
        ((ProgressBar) view.findViewById(R.id.pbExchange)).getIndeterminateDrawable().
                setColorFilter(getResources().getColor(R.color.trafficGray),
                        android.graphics.PorterDuff.Mode.MULTIPLY);

        tvProgress = (TextView) view.findViewById(R.id.tvProgress);
        pbProgress = (ProgressBar) view.findViewById(R.id.pbProgress);
        tvBalance = (TextView) view.findViewById(R.id.tvBalance);
        tvBalance.setText(Helper.getDisplayAmount(0));
        tvUnconfirmedAmount = (TextView) view.findViewById(R.id.tvUnconfirmedAmount);
        tvUnconfirmedAmount.setText(getResources().getString(R.string.xmr_unconfirmed_amount, Helper.getDisplayAmount(0)));
        ivSynced = (ImageView) view.findViewById(R.id.ivSynced);

        sCurrency = (Spinner) view.findViewById(R.id.sCurrency);
        sCurrency.setAdapter(ArrayAdapter.createFromResource(getContext(), R.array.currency, R.layout.item_spinner));

        bSend = (Button) view.findViewById(R.id.bSend);
        bReceive = (Button) view.findViewById(R.id.bReceive);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);

        this.adapter = new TransactionInfoAdapter(getActivity(), this);
        recyclerView.setAdapter(adapter);

        bSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activityCallback.onSendRequest();
            }
        });
        bReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activityCallback.onWalletReceive();
            }
        });

        sCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                refreshBalance();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // nothing (yet?)
            }
        });


        if (activityCallback.isSynced()) {
            onSynced();
        }

        activityCallback.forceUpdate();

        return view;
    }

    String balanceCurrency = "XMR";
    double balanceRate = 1.0;

    void refreshBalance() {
        if (sCurrency.getSelectedItemPosition() == 0) { // XMR
            double amountXmr = Double.parseDouble(Wallet.getDisplayAmount(unlockedBalance)); // assume this cannot fail!
            tvBalance.setText(Helper.getFormattedAmount(amountXmr, true));
        } else { // not XMR
            String currency = (String) sCurrency.getSelectedItem();
            if (!currency.equals(balanceCurrency) || (balanceRate <= 0)) {
                showExchanging();
                new AsyncExchangeRate(this).execute("XMR", currency);
            } else {
                exchange("XMR", balanceCurrency, balanceRate);
            }
        }
    }

    boolean isExchanging = false;

    void showExchanging() {
        isExchanging = true;
        tvBalance.setVisibility(View.GONE);
        flExchange.setVisibility(View.VISIBLE);
    }

    void hideExchanging() {
        isExchanging = false;
        tvBalance.setVisibility(View.VISIBLE);
        flExchange.setVisibility(View.GONE);
    }

    // Callbacks from AsyncExchangeRate

    // callback from AsyncExchangeRate when it can't get exchange rate
    public void exchangeFailed() {
        sCurrency.setSelection(0, true); // default to XMR
        double amountXmr = Double.parseDouble(Wallet.getDisplayAmount(unlockedBalance)); // assume this cannot fail!
        tvBalance.setText(Helper.getFormattedAmount(amountXmr, true));
        hideExchanging();
    }

    void updateBalance() {
        if (isExchanging) return; // wait for exchange to finish - it will fire this itself then.
        // at this point selection is XMR in case of error
        String displayB;
        double amountA = Double.parseDouble(Wallet.getDisplayAmount(unlockedBalance)); // assume this cannot fail!
        if (!"XMR".equals(balanceCurrency)) { // not XMR
            double amountB = amountA * balanceRate;
            displayB = Helper.getFormattedAmount(amountB, false);
        } else { // XMR
            displayB = Helper.getFormattedAmount(amountA, true);
        }
        tvBalance.setText(displayB);
    }

    // callback from AsyncExchangeRate when we have a rate
    public void exchange(String currencyA, String currencyB, double rate) {
        hideExchanging();
        if (!"XMR".equals(currencyA)) {
            Log.e(TAG, "Not XMR");
            sCurrency.setSelection(0, true);
            balanceCurrency = "XMR";
            balanceRate = 1.0;
        } else {
            int spinnerPosition = ((ArrayAdapter) sCurrency.getAdapter()).getPosition(currencyB);
            if (spinnerPosition < 0) { // requested currency not in list
                Log.e(TAG, "Requested currency not in list " + currencyB);
                sCurrency.setSelection(0, true);
            } else {
                sCurrency.setSelection(spinnerPosition, true);
            }
            balanceCurrency = currencyB;
            balanceRate = rate;
        }
        updateBalance();
    }

    // Callbacks from TransactionInfoAdapter
    @Override
    public void onInteraction(final View view, final TransactionInfo infoItem) {
        activityCallback.onTxDetailsRequest(infoItem);
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

    public void onSynced() {
        if (!activityCallback.isWatchOnly()) {
            bSend.setVisibility(View.VISIBLE);
            bSend.setEnabled(true);
        }
    }

    public void setProgressText(final String text) {
        tvProgress.setText(text);
    }

    public void onProgress(final String text) {
        if (text != null) {
            setProgressText(text);
            pbProgress.setVisibility(View.VISIBLE);
        } else {
            pbProgress.setVisibility(View.INVISIBLE);
            setProgressText(getString(R.string.status_working));
            onProgress(-1);
        }
    }

    public void onProgress(final int n) {
        if (n >= 0) {
            pbProgress.setIndeterminate(false);
            pbProgress.setProgress(n);
        } else {
            pbProgress.setIndeterminate(true);
        }
    }

    void setActivityTitle(Wallet wallet) {
        if (wallet == null) return;
        walletTitle = wallet.getName();
        String watchOnly = (wallet.isWatchOnly() ? getString(R.string.label_watchonly) : "");
        walletSubtitle = wallet.getAddress().substring(0, 16) + "…" + watchOnly;
        activityCallback.setTitle(walletTitle, walletSubtitle);
        Log.d(TAG, "wallet title is " + walletTitle);
    }

    private long firstBlock = 0;
    private String walletTitle = null;
    private String walletSubtitle = null;
    private long unlockedBalance = 0;

    private void updateStatus(Wallet wallet) {
        if (!isAdded()) return;
        Log.d(TAG, "updateStatus()");
        if (walletTitle == null) {
            setActivityTitle(wallet);
            onProgress(100); // of loading
        }
        long balance = wallet.getBalance();
        unlockedBalance = wallet.getUnlockedBalance();
        refreshBalance();
        double amountXmr = Double.parseDouble(Helper.getDisplayAmount(balance - unlockedBalance)); // assume this cannot fail!
        String unconfirmed = Helper.getFormattedAmount(amountXmr, true);
        tvUnconfirmedAmount.setText(getResources().getString(R.string.xmr_unconfirmed_amount, unconfirmed));
        //tvUnconfirmedAmount.setText(getResources().getString(R.string.xmr_unconfirmed_amount,
        //        Helper.getDisplayAmount(balance - unlockedBalance, Helper.DISPLAY_DIGITS_SHORT)));
        String sync = "";
        if (!activityCallback.hasBoundService())
            throw new IllegalStateException("WalletService not bound.");
        Wallet.ConnectionStatus daemonConnected = activityCallback.getConnectionStatus();
        if (daemonConnected == Wallet.ConnectionStatus.ConnectionStatus_Connected) {
            long daemonHeight = activityCallback.getDaemonHeight();
            if (!wallet.isSynchronized()) {
                long n = daemonHeight - wallet.getBlockChainHeight();
                sync = getString(R.string.status_syncing) + " " + formatter.format(n) + " " + getString(R.string.status_remaining);
                if (firstBlock == 0) {
                    firstBlock = wallet.getBlockChainHeight();
                }
                int x = 100 - Math.round(100f * n / (1f * daemonHeight - firstBlock));
                //onProgress(getString(R.string.status_syncing) + " " + sync);
                if (x == 0) x = -1;
                onProgress(x);
                ivSynced.setVisibility(View.GONE);
            } else {
                sync = getString(R.string.status_synced) + formatter.format(wallet.getBlockChainHeight());
                ivSynced.setVisibility(View.VISIBLE);
            }
        }
        setProgressText(sync);
        // TODO show connected status somewhere
    }

    Listener activityCallback;

    // Container Activity must implement this interface
    public interface Listener {
        boolean hasBoundService();

        void forceUpdate();

        Wallet.ConnectionStatus getConnectionStatus();

        long getDaemonHeight(); //mBoundService.getDaemonHeight();

        void onSendRequest();

        void onTxDetailsRequest(TransactionInfo info);

        boolean isSynced();

        boolean isWatchOnly();

        String getTxKey(String txId);

        void onWalletReceive();

        boolean hasWallet();

        void setToolbarButton(int type);

        void setTitle(String title, String subtitle);

        void setSubtitle(String subtitle);
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

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        activityCallback.setTitle(walletTitle, walletSubtitle);
        activityCallback.setToolbarButton(Toolbar.BUTTON_CLOSE);
    }
}
