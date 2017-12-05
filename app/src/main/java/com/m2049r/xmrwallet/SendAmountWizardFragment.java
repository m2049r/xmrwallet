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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.m2049r.xmrwallet.data.BarcodeData;
import com.m2049r.xmrwallet.widget.ExchangeTextView;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.widget.NumberPadView;

import timber.log.Timber;

public class SendAmountWizardFragment extends SendWizardFragment {

    public static SendAmountWizardFragment newInstance(Listener listener) {
        SendAmountWizardFragment instance = new SendAmountWizardFragment();
        instance.setSendListener(listener);
        return instance;
    }

    Listener sendListener;

    public SendAmountWizardFragment setSendListener(Listener listener) {
        this.sendListener = listener;
        return this;
    }

    interface Listener {
        SendFragment.Listener getActivityCallback();

        void setAmount(final long amount);

        BarcodeData popBarcodeData();
    }

    private TextView tvFunds;
    private ExchangeTextView evAmount;
    //private Button bSendAll;
    private NumberPadView numberPad;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Timber.d("onCreateView() %s", (String.valueOf(savedInstanceState)));

        sendListener = (Listener) getParentFragment();

        View view = inflater.inflate(R.layout.fragment_send_amount, container, false);

        tvFunds = (TextView) view.findViewById(R.id.tvFunds);

        evAmount = (ExchangeTextView) view.findViewById(R.id.evAmount);
        numberPad = (NumberPadView) view.findViewById(R.id.numberPad);
        numberPad.setListener(evAmount);

        /*
        bSendAll = (Button) view.findViewById(R.id.bSendAll);
        bSendAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: send all - figure out how to display this
            }
        });
*/

        Helper.hideKeyboard(getActivity());

        return view;
    }


    @Override
    public boolean onValidateFields() {
        if (!evAmount.validate(maxFunds)) {
            return false;
        }

        if (sendListener != null) {
            String xmr = evAmount.getAmount();
            if (xmr != null) {
                sendListener.setAmount(Wallet.getAmountFromString(xmr));
            } else {
                sendListener.setAmount(0L);
            }
        }
        return true;
    }

    double maxFunds = 0;

    @Override
    public void onResumeFragment() {
        super.onResumeFragment();
        Timber.d("onResumeFragment()");
        Helper.hideKeyboard(getActivity());
        final long funds = getTotalFunds();
        maxFunds = 1.0 * funds / 1000000000000L;
        tvFunds.setText(getString(R.string.send_available,
                Wallet.getDisplayAmount(funds)));
        // getAmount is null if exchange is in progress
        if ((evAmount.getAmount() != null) && evAmount.getAmount().isEmpty()) {
            final BarcodeData data = sendListener.popBarcodeData();
            if ((data != null) && (data.amount > 0)) {
                evAmount.setAmount(Wallet.getDisplayAmount(data.amount));
            }
        }
    }

    long getTotalFunds() {
        return sendListener.getActivityCallback().getTotalFunds();
    }
}