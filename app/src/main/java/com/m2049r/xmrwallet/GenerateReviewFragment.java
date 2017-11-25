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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.m2049r.xmrwallet.widget.Toolbar;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.MoneroThreadPoolExecutor;

import timber.log.Timber;

public class GenerateReviewFragment extends Fragment {
    static final public String VIEW_TYPE_DETAILS = "details";
    static final public String VIEW_TYPE_ACCEPT = "accept";
    static final public String VIEW_TYPE_WALLET = "wallet";

    ScrollView scrollview;

    ProgressBar pbProgress;
    TextView tvWalletName;
    TextView tvWalletPassword;
    TextView tvWalletAddress;
    TextView tvWalletMnemonic;
    TextView tvWalletViewKey;
    TextView tvWalletSpendKey;
    ImageButton bCopyAddress;
    LinearLayout llAdvancedInfo;
    Button bAdvancedInfo;
    Button bAccept;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_review, container, false);

        scrollview = (ScrollView) view.findViewById(R.id.scrollview);
        pbProgress = (ProgressBar) view.findViewById(R.id.pbProgress);
        tvWalletName = (TextView) view.findViewById(R.id.tvWalletName);
        tvWalletPassword = (TextView) view.findViewById(R.id.tvWalletPassword);
        tvWalletAddress = (TextView) view.findViewById(R.id.tvWalletAddress);
        tvWalletViewKey = (TextView) view.findViewById(R.id.tvWalletViewKey);
        tvWalletSpendKey = (TextView) view.findViewById(R.id.tvWalletSpendKey);
        tvWalletMnemonic = (TextView) view.findViewById(R.id.tvWalletMnemonic);
        bCopyAddress = (ImageButton) view.findViewById(R.id.bCopyAddress);
        bAdvancedInfo = (Button) view.findViewById(R.id.bAdvancedInfo);
        llAdvancedInfo = (LinearLayout) view.findViewById(R.id.llAdvancedInfo);

        bAccept = (Button) view.findViewById(R.id.bAccept);

        boolean testnet = WalletManager.getInstance().isTestNet();
        tvWalletMnemonic.setTextIsSelectable(testnet);
        tvWalletSpendKey.setTextIsSelectable(testnet);

        bAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptWallet();
            }
        });
        view.findViewById(R.id.bCopyViewKey).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyViewKey();
            }
        });
        bCopyAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyAddress();
            }
        });
        view.findViewById(R.id.bAdvancedInfo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAdvancedInfo();
            }
        });

        showProgress();

        Bundle args = getArguments();
        String path = args.getString("path");
        String password = args.getString("password");
        this.type = args.getString("type");
        new AsyncShow().executeOnExecutor(MoneroThreadPoolExecutor.MONERO_THREAD_POOL_EXECUTOR,
                path, password);
        return view;
    }

    void copyViewKey() {
        Helper.clipBoardCopy(getActivity(), getString(R.string.label_copy_viewkey), tvWalletViewKey.getText().toString());
        Toast.makeText(getActivity(), getString(R.string.message_copy_viewkey), Toast.LENGTH_SHORT).show();
    }

    void copyAddress() {
        Helper.clipBoardCopy(getActivity(), getString(R.string.label_copy_address), tvWalletAddress.getText().toString());
        Toast.makeText(getActivity(), getString(R.string.message_copy_address), Toast.LENGTH_SHORT).show();
    }

    void nocopy() {
        Toast.makeText(getActivity(), getString(R.string.message_nocopy), Toast.LENGTH_SHORT).show();
    }

    void showAdvancedInfo() {
        llAdvancedInfo.setVisibility(View.VISIBLE);
        bAdvancedInfo.setVisibility(View.GONE);
        scrollview.post(new Runnable() {
            @Override
            public void run() {
                scrollview.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    String type;

    private void acceptWallet() {
        String name = tvWalletName.getText().toString();
        String password = tvWalletPassword.getText().toString();
        bAccept.setEnabled(false);
        acceptCallback.onAccept(name, password);
    }

    private class AsyncShow extends AsyncTask<String, Void, Boolean> {
        String password;

        String name;
        String address;
        String seed;
        String viewKey;
        String spendKey;
        boolean isWatchOnly;
        Wallet.Status status;

        @Override
        protected Boolean doInBackground(String... params) {
            if (params.length != 2) return false;
            String walletPath = params[0];
            password = params[1];

            Wallet wallet;
            boolean closeWallet;
            if (type.equals(GenerateReviewFragment.VIEW_TYPE_WALLET)) {
                wallet = GenerateReviewFragment.this.walletCallback.getWallet();
                closeWallet = false;
            } else {
                wallet = WalletManager.getInstance().openWallet(walletPath, password);
                closeWallet = true;
            }
            name = wallet.getName();
            status = wallet.getStatus();
            if (status != Wallet.Status.Status_Ok) {
                if (closeWallet) wallet.close();
                return false;
            }

            address = wallet.getAddress();
            seed = wallet.getSeed();
            viewKey = wallet.getSecretViewKey();
            spendKey = isWatchOnly ? getActivity().getString(R.string.label_watchonly) : wallet.getSecretSpendKey();
            isWatchOnly = wallet.isWatchOnly();
            if (closeWallet) wallet.close();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (!isAdded()) return; // never mind
            tvWalletName.setText(name);
            if (result) {
                if (type.equals(GenerateReviewFragment.VIEW_TYPE_ACCEPT)) {
                    tvWalletPassword.setText(password);
                    bAccept.setVisibility(View.VISIBLE);
                    bAccept.setEnabled(true);
                }
                tvWalletAddress.setText(address);
                tvWalletMnemonic.setText(seed);
                tvWalletViewKey.setText(viewKey);
                tvWalletSpendKey.setText(spendKey);
                bAdvancedInfo.setVisibility(View.VISIBLE);
                bCopyAddress.setEnabled(true);
                bCopyAddress.setImageResource(R.drawable.ic_content_copy_black_24dp);
                activityCallback.setTitle(name, getString(R.string.details_title));
                activityCallback.setToolbarButton(
                        GenerateReviewFragment.VIEW_TYPE_ACCEPT.equals(type) ? Toolbar.BUTTON_NONE : Toolbar.BUTTON_BACK);
            } else {
                // TODO show proper error message and/or end the fragment?
                tvWalletAddress.setText(status.toString());
                tvWalletMnemonic.setText(status.toString());
                tvWalletViewKey.setText(status.toString());
                tvWalletSpendKey.setText(status.toString());
            }
            hideProgress();
        }
    }

    Listener activityCallback = null;
    AcceptListener acceptCallback = null;
    ListenerWithWallet walletCallback = null;

    public interface Listener {
        void setTitle(String title, String subtitle);

        void setToolbarButton(int type);
    }

    public interface AcceptListener {
        void onAccept(String name, String password);
    }

    public interface ListenerWithWallet {
        Wallet getWallet();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            this.activityCallback = (Listener) context;
        }
        if (context instanceof AcceptListener) {
            this.acceptCallback = (AcceptListener) context;
        }
        if (context instanceof ListenerWithWallet) {
            this.walletCallback = (ListenerWithWallet) context;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume()");
        String name = tvWalletName.getText().toString();
        if (name.isEmpty()) name = null;
        activityCallback.setTitle(name, getString(R.string.details_title));
        activityCallback.setToolbarButton(
                GenerateReviewFragment.VIEW_TYPE_ACCEPT.equals(type) ? Toolbar.BUTTON_NONE : Toolbar.BUTTON_BACK);
    }

    public void showProgress() {
        pbProgress.setVisibility(View.VISIBLE);
    }

    public void hideProgress() {
        pbProgress.setVisibility(View.GONE);
    }

    boolean backOk() {
        return !type.equals(GenerateReviewFragment.VIEW_TYPE_ACCEPT);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.wallet_details_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
}
