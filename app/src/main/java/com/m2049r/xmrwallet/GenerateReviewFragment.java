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
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.MoneroThreadPoolExecutor;

public class GenerateReviewFragment extends Fragment {
    static final String TAG = "GenerateReviewFragment";
    static final public String VIEW_TYPE_DETAILS = "details";
    static final public String VIEW_TYPE_ACCEPT = "accept";
    static final public String VIEW_TYPE_WALLET = "wallet";

    ProgressBar pbProgress;
    TextView tvWalletName;
    TextView tvWalletPassword;
    TextView tvWalletAddress;
    TextView tvWalletMnemonic;
    TextView tvWalletViewKey;
    TextView tvWalletSpendKey;
    Button bAccept;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.gen_review_fragment, container, false);

        pbProgress = (ProgressBar) view.findViewById(R.id.pbProgress);
        tvWalletName = (TextView) view.findViewById(R.id.tvWalletName);
        tvWalletPassword = (TextView) view.findViewById(R.id.tvWalletPassword);
        tvWalletAddress = (TextView) view.findViewById(R.id.tvWalletAddress);
        tvWalletViewKey = (TextView) view.findViewById(R.id.tvWalletViewKey);
        tvWalletSpendKey = (TextView) view.findViewById(R.id.tvWalletSpendKey);
        tvWalletMnemonic = (TextView) view.findViewById(R.id.tvWalletMnemonic);

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

        showProgress();

        Bundle args = getArguments();
        String path = args.getString("path");
        String password = args.getString("password");
        String type = args.getString("type");
        new AsyncShow().executeOnExecutor(MoneroThreadPoolExecutor.MONERO_THREAD_POOL_EXECUTOR,
                path, password, type);
        return view;
    }

    private void acceptWallet() {
        String name = tvWalletName.getText().toString();
        String password = tvWalletPassword.getText().toString();
        bAccept.setEnabled(false);
        acceptCallback.onAccept(name, password);
    }

    private class AsyncShow extends AsyncTask<String, Void, Boolean> {
        String type;
        String password;

        String name;
        String address;
        String seed;
        String viewKey;
        String spendKey;
        boolean isWatchOnly;

        @Override
        protected Boolean doInBackground(String... params) {
            if (params.length != 3) return false;
            String walletPath = params[0];
            password = params[1];
            type = params[2];

            Wallet wallet;
            boolean closeWallet;
            if (type.equals(GenerateReviewFragment.VIEW_TYPE_WALLET)) {
                wallet = GenerateReviewFragment.this.walletCallback.getWallet();
                closeWallet = false;
            } else {
                wallet = WalletManager.getInstance().openWallet(walletPath, password);
                closeWallet = true;
            }
            if (wallet.getStatus() != Wallet.Status.Status_Ok) return false;
            name = wallet.getName();
            address = wallet.getAddress();
            seed = wallet.getSeed();
            viewKey = wallet.getSecretViewKey();
            spendKey = isWatchOnly ? getActivity().getString(R.string.watchonly_label) : wallet.getSecretSpendKey();
            isWatchOnly = wallet.isWatchOnly();
            if (closeWallet) wallet.close();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (result) {
                if (type.equals(GenerateReviewFragment.VIEW_TYPE_ACCEPT)) {
                    tvWalletPassword.setText(password);
                    bAccept.setVisibility(View.VISIBLE);
                    bAccept.setEnabled(true);
                }
                tvWalletName.setText(name);
                tvWalletAddress.setText(address);
                tvWalletMnemonic.setText(seed);
                tvWalletViewKey.setText(viewKey);
                tvWalletSpendKey.setText(spendKey);
            }
            hideProgress();
        }
    }

    GenerateReviewFragment.Listener acceptCallback = null;
    GenerateReviewFragment.ListenerWithWallet walletCallback = null;

    public interface Listener {
        void onAccept(String name, String password);
    }

    public interface ListenerWithWallet {
        Wallet getWallet();

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof GenerateReviewFragment.Listener) {
            this.acceptCallback = (GenerateReviewFragment.Listener) context;
        } else if (context instanceof GenerateReviewFragment.ListenerWithWallet) {
            this.walletCallback = (GenerateReviewFragment.ListenerWithWallet) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement Listener");
        }
    }

    public void showProgress() {
        pbProgress.setIndeterminate(true);
        pbProgress.setVisibility(View.VISIBLE);
    }

    public void hideProgress() {
        pbProgress.setVisibility(View.INVISIBLE);
    }
}
