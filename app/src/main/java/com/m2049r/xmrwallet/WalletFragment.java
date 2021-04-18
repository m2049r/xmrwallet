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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.github.brnunes.swipeablerecyclerview.SwipeableRecyclerViewTouchListener;
import com.google.android.material.transition.MaterialElevationScale;
import com.m2049r.xmrwallet.layout.TransactionInfoAdapter;
import com.m2049r.xmrwallet.model.TransactionInfo;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeApi;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeCallback;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeRate;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.ServiceHelper;
import com.m2049r.xmrwallet.widget.Toolbar;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import timber.log.Timber;

public class WalletFragment extends Fragment
        implements TransactionInfoAdapter.OnInteractionListener {
    private TransactionInfoAdapter adapter;
    private final NumberFormat formatter = NumberFormat.getInstance();

    private TextView tvStreetView;
    private LinearLayout llBalance;
    private FrameLayout flExchange;
    private TextView tvBalance;
    private TextView tvUnconfirmedAmount;
    private TextView tvProgress;
    private ImageView ivSynced;
    private ProgressBar pbProgress;
    private Button bReceive;
    private Button bSend;
    private ImageView ivStreetGunther;
    private Drawable streetGunther = null;
    RecyclerView txlist;

    private Spinner sCurrency;

    private final List<String> dismissedTransactions = new ArrayList<>();

    public void resetDismissedTransactions() {
        dismissedTransactions.clear();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (activityCallback.hasWallet())
            inflater.inflate(R.menu.wallet_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        ivStreetGunther = view.findViewById(R.id.ivStreetGunther);
        tvStreetView = view.findViewById(R.id.tvStreetView);
        llBalance = view.findViewById(R.id.llBalance);
        flExchange = view.findViewById(R.id.flExchange);
        ((ProgressBar) view.findViewById(R.id.pbExchange)).getIndeterminateDrawable().
                setColorFilter(getResources().getColor(R.color.progress_circle),
                        android.graphics.PorterDuff.Mode.MULTIPLY);

        tvProgress = view.findViewById(R.id.tvProgress);
        pbProgress = view.findViewById(R.id.pbProgress);
        tvBalance = view.findViewById(R.id.tvBalance);
        showBalance(Helper.getDisplayAmount(0));
        tvUnconfirmedAmount = view.findViewById(R.id.tvUnconfirmedAmount);
        showUnconfirmed(0);
        ivSynced = view.findViewById(R.id.ivSynced);

        sCurrency = view.findViewById(R.id.sCurrency);
        List<String> currencies = new ArrayList<>();
        currencies.add(Helper.BASE_CRYPTO);
        if (Helper.SHOW_EXCHANGERATES)
            currencies.addAll(Arrays.asList(getResources().getStringArray(R.array.currency)));
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(Objects.requireNonNull(getContext()), R.layout.item_spinner_balance, currencies);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sCurrency.setAdapter(spinnerAdapter);

        bSend = view.findViewById(R.id.bSend);
        bReceive = view.findViewById(R.id.bReceive);

        txlist = view.findViewById(R.id.list);
        adapter = new TransactionInfoAdapter(getActivity(), this);
        txlist.setAdapter(adapter);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                if ((positionStart == 0) && (txlist.computeVerticalScrollOffset() == 0))
                    txlist.scrollToPosition(positionStart);
            }
        });

        txlist.addOnItemTouchListener(
                new SwipeableRecyclerViewTouchListener(txlist,
                        new SwipeableRecyclerViewTouchListener.SwipeListener() {
                            @Override
                            public boolean canSwipeLeft(int position) {
                                return activityCallback.isStreetMode();
                            }

                            @Override
                            public boolean canSwipeRight(int position) {
                                return activityCallback.isStreetMode();
                            }

                            @Override
                            public void onDismissedBySwipeLeft(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    dismissedTransactions.add(adapter.getItem(position).hash);
                                    adapter.removeItem(position);
                                }
                            }

                            @Override
                            public void onDismissedBySwipeRight(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    dismissedTransactions.add(adapter.getItem(position).hash);
                                    adapter.removeItem(position);
                                }
                            }
                        }));

        bSend.setOnClickListener(v -> activityCallback.onSendRequest(v));
        bReceive.setOnClickListener(v -> activityCallback.onWalletReceive(v));

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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        postponeEnterTransition();
        view.getViewTreeObserver().addOnPreDrawListener(() -> {
            startPostponedEnterTransition();
            return true;
        });
    }

    void showBalance(String balance) {
        tvBalance.setText(balance);
        final boolean streetMode = activityCallback.isStreetMode();
        if (!streetMode) {
            llBalance.setVisibility(View.VISIBLE);
            tvStreetView.setVisibility(View.INVISIBLE);
        } else {
            llBalance.setVisibility(View.INVISIBLE);
            tvStreetView.setVisibility(View.VISIBLE);
        }
        setStreetModeBackground(streetMode);
    }

    void showUnconfirmed(double unconfirmedAmount) {
        if (!activityCallback.isStreetMode()) {
            String unconfirmed = Helper.getFormattedAmount(unconfirmedAmount, true);
            tvUnconfirmedAmount.setText(getResources().getString(R.string.xmr_unconfirmed_amount, unconfirmed));
        } else {
            tvUnconfirmedAmount.setText(null);
        }
    }

    void updateBalance() {
        if (isExchanging) return; // wait for exchange to finish - it will fire this itself then.
        // at this point selection is XMR in case of error
        String displayB;
        double amountA = Helper.getDecimalAmount(unlockedBalance).doubleValue();
        if (!Helper.BASE_CRYPTO.equals(balanceCurrency)) { // not XMR
            double amountB = amountA * balanceRate;
            displayB = Helper.getFormattedAmount(amountB, false);
        } else { // XMR
            displayB = Helper.getFormattedAmount(amountA, true);
        }
        showBalance(displayB);
    }

    String balanceCurrency = Helper.BASE_CRYPTO;
    double balanceRate = 1.0;

    private final ExchangeApi exchangeApi = ServiceHelper.getExchangeApi();

    void refreshBalance() {
        double unconfirmedXmr = Helper.getDecimalAmount(balance - unlockedBalance).doubleValue();
        showUnconfirmed(unconfirmedXmr);
        if (sCurrency.getSelectedItemPosition() == 0) { // XMR
            double amountXmr = Helper.getDecimalAmount(unlockedBalance).doubleValue();
            showBalance(Helper.getFormattedAmount(amountXmr, true));
        } else { // not XMR
            String currency = (String) sCurrency.getSelectedItem();
            Timber.d(currency);
            if (!currency.equals(balanceCurrency) || (balanceRate <= 0)) {
                showExchanging();
                exchangeApi.queryExchangeRate(Helper.BASE_CRYPTO, currency,
                        new ExchangeCallback() {
                            @Override
                            public void onSuccess(final ExchangeRate exchangeRate) {
                                if (isAdded())
                                    new Handler(Looper.getMainLooper()).post(() -> exchange(exchangeRate));
                            }

                            @Override
                            public void onError(final Exception e) {
                                Timber.e(e.getLocalizedMessage());
                                if (isAdded())
                                    new Handler(Looper.getMainLooper()).post(() -> exchangeFailed());
                            }
                        });
            } else {
                updateBalance();
            }
        }
    }

    boolean isExchanging = false;

    void showExchanging() {
        isExchanging = true;
        tvBalance.setVisibility(View.GONE);
        flExchange.setVisibility(View.VISIBLE);
        sCurrency.setEnabled(false);
    }

    void hideExchanging() {
        isExchanging = false;
        tvBalance.setVisibility(View.VISIBLE);
        flExchange.setVisibility(View.GONE);
        sCurrency.setEnabled(true);
    }

    public void exchangeFailed() {
        sCurrency.setSelection(0, true); // default to XMR
        double amountXmr = Helper.getDecimalAmount(unlockedBalance).doubleValue();
        showBalance(Helper.getFormattedAmount(amountXmr, true));
        hideExchanging();
    }

    public void exchange(final ExchangeRate exchangeRate) {
        hideExchanging();
        if (!Helper.BASE_CRYPTO.equals(exchangeRate.getBaseCurrency())) {
            Timber.e("Not XMR");
            sCurrency.setSelection(0, true);
            balanceCurrency = Helper.BASE_CRYPTO;
            balanceRate = 1.0;
        } else {
            int spinnerPosition = ((ArrayAdapter) sCurrency.getAdapter()).getPosition(exchangeRate.getQuoteCurrency());
            if (spinnerPosition < 0) { // requested currency not in list
                Timber.e("Requested currency not in list %s", exchangeRate.getQuoteCurrency());
                sCurrency.setSelection(0, true);
            } else {
                sCurrency.setSelection(spinnerPosition, true);
            }
            balanceCurrency = exchangeRate.getQuoteCurrency();
            balanceRate = exchangeRate.getRate();
        }
        updateBalance();
    }

    // Callbacks from TransactionInfoAdapter
    @Override
    public void onInteraction(final View view, final TransactionInfo infoItem) {
        final MaterialElevationScale exitTransition = new MaterialElevationScale(false);
        exitTransition.setDuration(getResources().getInteger(R.integer.tx_item_transition_duration));
        setExitTransition(exitTransition);
        final MaterialElevationScale reenterTransition = new MaterialElevationScale(true);
        reenterTransition.setDuration(getResources().getInteger(R.integer.tx_item_transition_duration));
        setReenterTransition(reenterTransition);

        activityCallback.onTxDetailsRequest(view, infoItem);
    }

    // called from activity


    // if account index has changed scroll to top?
    private int accountIndex = 0;

    public void onRefreshed(final Wallet wallet, final boolean full) {
        Timber.d("onRefreshed(%b)", full);

        if (full) {
            List<TransactionInfo> list = new ArrayList<>();
            final long streetHeight = activityCallback.getStreetModeHeight();
            Timber.d("StreetHeight=%d", streetHeight);
            for (TransactionInfo info : wallet.getHistory().getAll()) {
                Timber.d("TxHeight=%d, Label=%s", info.blockheight, info.subaddressLabel);
                if ((info.isPending || (info.blockheight >= streetHeight))
                        && !dismissedTransactions.contains(info.hash))
                    list.add(info);
            }
            adapter.setInfos(list);
            if (accountIndex != wallet.getAccountIndex()) {
                accountIndex = wallet.getAccountIndex();
                txlist.scrollToPosition(0);
            }
        }
        updateStatus(wallet);
    }

    public void onSynced() {
        if (!activityCallback.isWatchOnly()) {
            bSend.setVisibility(View.VISIBLE);
            bSend.setEnabled(true);
        }
        if (isVisible()) enableAccountsList(true); //otherwise it is enabled in onResume()
    }

    public void unsync() {
        if (!activityCallback.isWatchOnly()) {
            bSend.setVisibility(View.INVISIBLE);
            bSend.setEnabled(false);
        }
        if (isVisible()) enableAccountsList(false); //otherwise it is enabled in onResume()
        firstBlock = 0;
    }

    boolean walletLoaded = false;

    public void onLoaded() {
        walletLoaded = true;
        showReceive();
    }

    private void showReceive() {
        if (walletLoaded) {
            bReceive.setVisibility(View.VISIBLE);
            bReceive.setEnabled(true);
        }
    }

    private String syncText = null;

    public void setProgress(final String text) {
        syncText = text;
        tvProgress.setText(text);
    }

    private int syncProgress = -1;

    public void setProgress(final int n) {
        syncProgress = n;
        if (n > 100) {
            pbProgress.setIndeterminate(true);
            pbProgress.setVisibility(View.VISIBLE);
        } else if (n >= 0) {
            pbProgress.setIndeterminate(false);
            pbProgress.setProgress(n);
            pbProgress.setVisibility(View.VISIBLE);
        } else { // <0
            pbProgress.setVisibility(View.INVISIBLE);
        }
    }

    void setActivityTitle(Wallet wallet) {
        if (wallet == null) return;
        walletTitle = wallet.getName();
        walletSubtitle = wallet.getAccountLabel();
        activityCallback.setTitle(walletTitle, walletSubtitle);
        Timber.d("wallet title is %s", walletTitle);
    }

    private long firstBlock = 0;
    private String walletTitle = null;
    private String walletSubtitle = null;
    private long unlockedBalance = 0;
    private long balance = 0;

    private int accountIdx = -1;

    private void updateStatus(Wallet wallet) {
        if (!isAdded()) return;
        Timber.d("updateStatus()");
        if ((walletTitle == null) || (accountIdx != wallet.getAccountIndex())) {
            accountIdx = wallet.getAccountIndex();
            setActivityTitle(wallet);
        }
        balance = wallet.getBalance();
        unlockedBalance = wallet.getUnlockedBalance();
        refreshBalance();
        String sync;
        if (!activityCallback.hasBoundService())
            throw new IllegalStateException("WalletService not bound.");
        Wallet.ConnectionStatus daemonConnected = activityCallback.getConnectionStatus();
        if (daemonConnected == Wallet.ConnectionStatus.ConnectionStatus_Connected) {
            if (!wallet.isSynchronized()) {
                long daemonHeight = activityCallback.getDaemonHeight();
                long walletHeight = wallet.getBlockChainHeight();
                long n = daemonHeight - walletHeight;
                sync = getString(R.string.status_syncing) + " " + formatter.format(n) + " " + getString(R.string.status_remaining);
                if (firstBlock == 0) {
                    firstBlock = walletHeight;
                }
                int x = 100 - Math.round(100f * n / (1f * daemonHeight - firstBlock));
                if (x == 0) x = 101; // indeterminate
                setProgress(x);
                ivSynced.setVisibility(View.GONE);
            } else {
                sync = getString(R.string.status_synced) + " " + formatter.format(wallet.getBlockChainHeight());
                ivSynced.setVisibility(View.VISIBLE);
            }
        } else {
            sync = getString(R.string.status_wallet_connecting);
            setProgress(101);
        }
        setProgress(sync);
        // TODO show connected status somewhere
    }

    Listener activityCallback;

    // Container Activity must implement this interface
    public interface Listener {
        boolean hasBoundService();

        void forceUpdate();

        Wallet.ConnectionStatus getConnectionStatus();

        long getDaemonHeight(); //mBoundService.getDaemonHeight();

        void onSendRequest(View view);

        void onTxDetailsRequest(View view, TransactionInfo info);

        boolean isSynced();

        boolean isStreetMode();

        long getStreetModeHeight();

        boolean isWatchOnly();

        String getTxKey(String txId);

        void onWalletReceive(View view);

        boolean hasWallet();

        Wallet getWallet();

        void setToolbarButton(int type);

        void setTitle(String title, String subtitle);

        void setSubtitle(String subtitle);
    }

    @Override
    public void onAttach(@NonNull Context context) {
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
        setExitTransition(null);
        setReenterTransition(null);
        Timber.d("onResume()");
        activityCallback.setTitle(walletTitle, walletSubtitle);
        activityCallback.setToolbarButton(Toolbar.BUTTON_NONE);
        setProgress(syncProgress);
        setProgress(syncText);
        showReceive();
        if (activityCallback.isSynced()) enableAccountsList(true);
    }

    @Override
    public void onPause() {
        enableAccountsList(false);
        super.onPause();
    }

    public interface DrawerLocker {
        void setDrawerEnabled(boolean enabled);
    }

    private void enableAccountsList(boolean enable) {
        if (activityCallback instanceof DrawerLocker) {
            ((DrawerLocker) activityCallback).setDrawerEnabled(enable);
        }
    }

    public void setStreetModeBackground(boolean enable) {
        //TODO figure out why gunther disappears on return from send although he is still set
        if (enable) {
            if (streetGunther == null)
                streetGunther = ContextCompat.getDrawable(Objects.requireNonNull(getContext()), R.drawable.ic_gunther_streetmode);
            ivStreetGunther.setImageDrawable(streetGunther);
        } else
            ivStreetGunther.setImageDrawable(null);
    }
}
