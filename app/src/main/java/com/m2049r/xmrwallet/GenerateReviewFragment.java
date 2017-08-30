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
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.service.MoneroHandlerThread;

import java.io.File;

public class GenerateReviewFragment extends Fragment {
    static final String TAG = "GenerateReviewFragment";
    static final public String VIEW_DETAILS = "details";
    static final public String VIEW_ACCEPT = "accept";
    static final public String VIEW_WALLET = "wallet";

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

        Bundle b = getArguments();
        String type = b.getString("type");
        if (!type.equals(VIEW_WALLET)) {
            String name = b.getString("name");
            String password = b.getString("password");
            tvWalletName.setText(new File(name).getName());
            show(name, password, type);
        } else {
                show(walletCallback.getWallet(), null, type);
        }
        return view;
    }

    private void acceptWallet() {
        String name = tvWalletName.getText().toString();
        String password = tvWalletPassword.getText().toString();
        bAccept.setEnabled(false);
        acceptCallback.onAccept(name, password);
    }

    private void show(final String walletPath, final String password, final String type) {
        new Thread(null,
                new Runnable() {
                    @Override
                    public void run() {
                        final Wallet wallet = WalletManager.getInstance().openWallet(walletPath, password);
                        getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                show(wallet, password, type);
                                wallet.close();
                            }
                        });
                    }
                }
                , "DetailsReview", MoneroHandlerThread.THREAD_STACK_SIZE).start();
    }

    private void show(final Wallet wallet, final String password, final String type) {
        if (type.equals(GenerateReviewFragment.VIEW_ACCEPT)) {
            tvWalletPassword.setText(password);
            bAccept.setVisibility(View.VISIBLE);
            bAccept.setEnabled(true);
        }
        tvWalletName.setText(wallet.getName());
        tvWalletAddress.setText(wallet.getAddress());
        tvWalletMnemonic.setText(wallet.getSeed());
        tvWalletViewKey.setText(wallet.getSecretViewKey());
        String spend = wallet.isWatchOnly() ? "" : "not available - use seed for recovery";
        if (spend.length() > 0) { //TODO should be == 64, but spendkey is not in the API yet
            tvWalletSpendKey.setText(spend);
        } else {
            tvWalletSpendKey.setText(getString(R.string.generate_wallet_watchonly));
        }
        hideProgress();
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
