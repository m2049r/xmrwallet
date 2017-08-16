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
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.m2049r.xmrwallet.model.WalletManager;

import java.text.NumberFormat;

// TODO: somehow show which net we are generating for

public class GenerateReviewFragment extends Fragment {
    static final String TAG = "GenerateReviewFragment";

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

        tvWalletName = (TextView) view.findViewById(R.id.tvWalletName);
        tvWalletPassword = (TextView) view.findViewById(R.id.tvWalletPassword);
        tvWalletAddress = (TextView) view.findViewById(R.id.tvWalletAddress);
        tvWalletViewKey = (TextView) view.findViewById(R.id.tvWalletViewKey);
        tvWalletSpendKey = (TextView) view.findViewById(R.id.tvWalletSpendKey);
        tvWalletMnemonic = (TextView) view.findViewById(R.id.tvWalletMnemonic);

        bAccept = (Button) view.findViewById(R.id.bAccept);

        boolean testnet = WalletManager.getInstance().isTestNet();
        tvWalletMnemonic.setTextIsSelectable(testnet);

        bAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptWallet();
            }
        });

        showDetails();
        return view;
    }

    private void acceptWallet() {
        String name = tvWalletName.getText().toString();
        String password = tvWalletPassword.getText().toString();
        bAccept.setEnabled(false);
        activityCallback.onAccept(name, password);
    }

    public void showDetails() {
        Bundle b = getArguments();
        String name = b.getString("name");
        String password = b.getString("password");
        String address = b.getString("address");
        String seed = b.getString("seed");
        String view = b.getString("viewkey");
        String spend = b.getString("spendkey");
        long height = b.getLong("restoreHeight");
        tvWalletName.setText(name);
        tvWalletPassword.setText(password);
        tvWalletAddress.setText(address);
        tvWalletMnemonic.setText(seed);
        tvWalletViewKey.setText(view);
        tvWalletSpendKey.setText(spend);
        NumberFormat formatter = NumberFormat.getInstance();
        bAccept.setEnabled(true);
    }

    GenerateReviewFragment.Listener activityCallback;

    public interface Listener {
        void onAccept(String name, String password);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof GenerateReviewFragment.Listener) {
            this.activityCallback = (GenerateReviewFragment.Listener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement Listener");
        }
    }
}
