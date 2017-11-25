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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.data.PendingTx;
import com.m2049r.xmrwallet.data.TxData;

import timber.log.Timber;

public class SendSuccessWizardFragment extends SendWizardFragment {

    public static SendSuccessWizardFragment newInstance(Listener listener) {
        SendSuccessWizardFragment instance = new SendSuccessWizardFragment();
        instance.setSendListener(listener);
        return instance;
    }

    Listener sendListener;

    public SendSuccessWizardFragment setSendListener(Listener listener) {
        this.sendListener = listener;
        return this;
    }

    interface Listener {
        String getNotes();

        TxData getTxData();

        PendingTx getCommittedTx();

        void enableDone();
    }

    ImageButton bCopyAddress;
    private TextView tvTxId;
    private TextView tvTxAddress;
    private TextView tvTxPaymentId;
    private TextView tvTxNotes;
    private TextView tvTxAmount;
    private TextView tvTxFee;
    private TextView tvTxTotal;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Timber.d("onCreateView() %s", (String.valueOf(savedInstanceState)));

        View view = inflater.inflate(
                R.layout.fragment_send_success, container, false);

        bCopyAddress = (ImageButton) view.findViewById(R.id.bCopyAddress);
        bCopyAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyAddress();
            }
        });

        tvTxId = (TextView) view.findViewById(R.id.tvTxId);
        tvTxAddress = (TextView) view.findViewById(R.id.tvTxAddress);
        tvTxPaymentId = (TextView) view.findViewById(R.id.tvTxPaymentId);
        tvTxNotes = (TextView) view.findViewById(R.id.tvTxNotes);
        tvTxAmount = ((TextView) view.findViewById(R.id.tvTxAmount));
        tvTxFee = (TextView) view.findViewById(R.id.tvTxFee);
        tvTxTotal = (TextView) view.findViewById(R.id.tvTxTotal);

        return view;
    }

    @Override
    public boolean onValidateFields() {
        return true;
    }

    @Override
    public void onPauseFragment() {
        super.onPauseFragment();
    }

    @Override
    public void onResumeFragment() {
        super.onResumeFragment();
        Timber.d("onResumeFragment()");
        Helper.hideKeyboard(getActivity());

        final TxData txData = sendListener.getTxData();
        tvTxAddress.setText(txData.getDestinationAddress());
        String paymentId = txData.getPaymentId();
        if ((paymentId != null) && (!paymentId.isEmpty())) {
            tvTxPaymentId.setText(txData.getPaymentId());
        } else {
            tvTxPaymentId.setText("-");
        }
        String notes = sendListener.getNotes();
        if ((notes != null) && (!notes.isEmpty())) {
            tvTxNotes.setText(sendListener.getNotes());
        } else {
            tvTxNotes.setText("-");
        }

        final PendingTx committedTx = sendListener.getCommittedTx();
        if (committedTx != null) {
            tvTxId.setText(committedTx.txId);
            bCopyAddress.setEnabled(true);
            bCopyAddress.setImageResource(R.drawable.ic_content_copy_black_24dp);
            tvTxAmount.setText(Wallet.getDisplayAmount(committedTx.amount));
            tvTxFee.setText(Wallet.getDisplayAmount(committedTx.fee));
            //tvTxDust.setText(Wallet.getDisplayAmount(pendingTransaction.getDust()));
            tvTxTotal.setText(Wallet.getDisplayAmount(
                    committedTx.fee + committedTx.amount));
        }
        sendListener.enableDone();
    }

    void copyAddress() {
        Helper.clipBoardCopy(getActivity(), getString(R.string.label_send_txid), tvTxId.getText().toString());
        Toast.makeText(getActivity(), getString(R.string.message_copy_address), Toast.LENGTH_SHORT).show();
    }

}
