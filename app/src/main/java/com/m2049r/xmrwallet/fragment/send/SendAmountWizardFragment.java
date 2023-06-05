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

package com.m2049r.xmrwallet.fragment.send;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.data.BarcodeData;
import com.m2049r.xmrwallet.data.TxData;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.widget.ExchangeEditText;

import timber.log.Timber;

public class SendAmountWizardFragment extends SendWizardFragment {

    public static SendAmountWizardFragment newInstance(Listener listener) {
        SendAmountWizardFragment instance = new SendAmountWizardFragment();
        instance.setSendListener(listener);
        return instance;
    }

    Listener sendListener;

    public void setSendListener(Listener listener) {
        this.sendListener = listener;
    }

    interface Listener {
        SendFragment.Listener getActivityCallback();

        TxData getTxData();

        BarcodeData popBarcodeData();
    }

    private TextView tvFunds;
    private ExchangeEditText etAmount;
    private View rlSweep;
    private ImageButton ibSweep;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Timber.d("onCreateView() %s", (String.valueOf(savedInstanceState)));

        sendListener = (Listener) getParentFragment();

        View view = inflater.inflate(R.layout.fragment_send_amount, container, false);

        tvFunds = view.findViewById(R.id.tvFunds);
        etAmount = view.findViewById(R.id.etAmount);
        rlSweep = view.findViewById(R.id.rlSweep);

        view.findViewById(R.id.ivSweep).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sweepAll(false);
            }
        });

        ibSweep = view.findViewById(R.id.ibSweep);

        ibSweep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sweepAll(true);
            }
        });

        etAmount.requestFocus();
        return view;
    }

    private boolean spendAllMode = false;

    private void sweepAll(boolean spendAllMode) {
        if (spendAllMode) {
            ibSweep.setVisibility(View.INVISIBLE);
            etAmount.setVisibility(View.GONE);
            rlSweep.setVisibility(View.VISIBLE);
        } else {
            ibSweep.setVisibility(View.VISIBLE);
            etAmount.setVisibility(View.VISIBLE);
            rlSweep.setVisibility(View.GONE);
        }
        this.spendAllMode = spendAllMode;
    }

    @Override
    public boolean onValidateFields() {
        if (spendAllMode) {
            if (sendListener != null) {
                sendListener.getTxData().setAmount(Wallet.SWEEP_ALL);
            }
        } else {
            if (!etAmount.validate(maxFunds, 0)) {
                return false;
            }

            if (sendListener != null) {
                String xmr = etAmount.getNativeAmount();
                if (xmr != null) {
                    sendListener.getTxData().setAmount(Wallet.getAmountFromString(xmr));
                } else {
                    sendListener.getTxData().setAmount(0L);
                }
            }
        }
        return true;
    }

    double maxFunds = 0;

    @Override
    public void onResumeFragment() {
        super.onResumeFragment();
        Timber.d("onResumeFragment()");
        Helper.showKeyboard(getActivity());
        final long funds = getTotalFunds();
        maxFunds = 1.0 * funds / Helper.ONE_XMR;
        if (!sendListener.getActivityCallback().isStreetMode()) {
            tvFunds.setText(getString(R.string.send_available,
                    Wallet.getDisplayAmount(funds)));
        } else {
            tvFunds.setText(getString(R.string.send_available,
                    getString(R.string.unknown_amount)));
        }
        final BarcodeData data = sendListener.popBarcodeData();
        if ((data != null) && (data.amount != null)) {
            etAmount.setAmount(data.amount);
        }
    }

    long getTotalFunds() {
        return sendListener.getActivityCallback().getTotalFunds();
    }
}