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
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.data.TxData;
import com.m2049r.xmrwallet.data.UserNotes;
import com.m2049r.xmrwallet.model.PendingTransaction;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.util.Helper;

import timber.log.Timber;

public class SendConfirmWizardFragment extends SendWizardFragment implements SendConfirm {

    public static SendConfirmWizardFragment newInstance(Listener listener) {
        SendConfirmWizardFragment instance = new SendConfirmWizardFragment();
        instance.setSendListener(listener);
        return instance;
    }

    Listener sendListener;

    public SendConfirmWizardFragment setSendListener(Listener listener) {
        this.sendListener = listener;
        return this;
    }

    interface Listener {
        SendFragment.Listener getActivityCallback();

        TxData getTxData();

        void commitTransaction();

        void disposeTransaction();

        SendFragment.Mode getMode();
    }

    private TextView tvTxAddress;
    private TextView tvTxNotes;
    private TextView tvTxAmount;
    private TextView tvTxChange;
    private TextView tvTxFee;
    private TextView tvTxTotal;
    private View llProgress;
    private View bSend;
    private View llConfirmSend;
    private View pbProgressSend;
    private View llPocketChange;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Timber.d("onCreateView() %s", (String.valueOf(savedInstanceState)));

        View view = inflater.inflate(
                R.layout.fragment_send_confirm, container, false);

        tvTxAddress = view.findViewById(R.id.tvTxAddress);
        tvTxNotes = view.findViewById(R.id.tvTxNotes);
        tvTxAmount = view.findViewById(R.id.tvTxAmount);
        tvTxChange = view.findViewById(R.id.tvTxChange);
        tvTxFee = view.findViewById(R.id.tvTxFee);
        tvTxTotal = view.findViewById(R.id.tvTxTotal);

        llProgress = view.findViewById(R.id.llProgress);
        pbProgressSend = view.findViewById(R.id.pbProgressSend);
        llConfirmSend = view.findViewById(R.id.llConfirmSend);
        llPocketChange = view.findViewById(R.id.llPocketChange);

        bSend = view.findViewById(R.id.bSend);
        bSend.setEnabled(false);
        bSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Timber.d("bSend.setOnClickListener");
                bSend.setEnabled(false);
                preSend();
            }
        });
        return view;
    }

    boolean inProgress = false;

    public void hideProgress() {
        llProgress.setVisibility(View.INVISIBLE);
        inProgress = false;
    }

    public void showProgress() {
        llProgress.setVisibility(View.VISIBLE);
        inProgress = true;
    }

    PendingTransaction pendingTransaction = null;

    @Override
    // callback from wallet when PendingTransaction created
    public void transactionCreated(String txTag, PendingTransaction pendingTransaction) {
        // ignore txTag - the app flow ensures this is the correct tx
        // TODO: use the txTag
        hideProgress();
        if (isResumed) {
            this.pendingTransaction = pendingTransaction;
            refreshTransactionDetails();
        } else {
            sendListener.disposeTransaction();
        }
    }

    void send() {
        sendListener.commitTransaction();
        getActivity().runOnUiThread(() -> pbProgressSend.setVisibility(View.VISIBLE));
    }

    @Override
    public void sendFailed(String errorText) {
        pbProgressSend.setVisibility(View.INVISIBLE);
        showAlert(getString(R.string.send_create_tx_error_title), errorText);
    }

    @Override
    public void createTransactionFailed(String errorText) {
        hideProgress();
        showAlert(getString(R.string.send_create_tx_error_title), errorText);
    }

    private void showAlert(String title, String message) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setCancelable(true).
                setTitle(title).
                setMessage(message).
                create().
                show();
    }

    @Override
    public boolean onValidateFields() {
        return true;
    }

    private boolean isResumed = false;

    @Override
    public void onPauseFragment() {
        isResumed = false;
        pendingTransaction = null;
        sendListener.disposeTransaction();
        refreshTransactionDetails();
        super.onPauseFragment();
    }

    @Override
    public void onResumeFragment() {
        super.onResumeFragment();
        Timber.d("onResumeFragment()");
        Helper.hideKeyboard(getActivity());
        isResumed = true;

        final TxData txData = sendListener.getTxData();
        tvTxAddress.setText(txData.getDestination());
        UserNotes notes = sendListener.getTxData().getUserNotes();
        if ((notes != null) && (!notes.note.isEmpty())) {
            tvTxNotes.setText(notes.note);
        } else {
            tvTxNotes.setText("-");
        }
        refreshTransactionDetails();
        if ((pendingTransaction == null) && (!inProgress)) {
            showProgress();
            prepareSend(txData);
        }
    }

    void refreshTransactionDetails() {
        Timber.d("refreshTransactionDetails()");
        if (pendingTransaction != null) {
            llConfirmSend.setVisibility(View.VISIBLE);
            bSend.setEnabled(true);
            tvTxFee.setText(Wallet.getDisplayAmount(pendingTransaction.getFee()));
            if (getActivityCallback().isStreetMode()
                    && (sendListener.getTxData().getAmount() == Wallet.SWEEP_ALL)) {
                tvTxAmount.setText(getString(R.string.street_sweep_amount));
                tvTxTotal.setText(getString(R.string.street_sweep_amount));
            } else {
                tvTxAmount.setText(Wallet.getDisplayAmount(pendingTransaction.getNetAmount()));
                final long change = pendingTransaction.getPocketChange();
                if (change > 0) {
                    llPocketChange.setVisibility(View.VISIBLE);
                    tvTxChange.setText(Wallet.getDisplayAmount(change));
                } else {
                    llPocketChange.setVisibility(View.GONE);
                }
                tvTxTotal.setText(Wallet.getDisplayAmount(
                        pendingTransaction.getFee() + pendingTransaction.getAmount()));
            }
        } else {
            llConfirmSend.setVisibility(View.GONE);
            bSend.setEnabled(false);
        }
    }

    public void preSend() {
        Helper.promptPassword(getContext(), getActivityCallback().getWalletName(), false, new Helper.PasswordAction() {
            @Override
            public void act(String walletName, String password, boolean fingerprintUsed) {
                send();
            }

            public void fail(String walletName) {
                getActivity().runOnUiThread(() -> {
                    bSend.setEnabled(true); // allow to try again
                });
            }
        });
    }

    // creates a pending transaction and calls us back with transactionCreated()
    // or createTransactionFailed()
    void prepareSend(TxData txData) {
        getActivityCallback().onPrepareSend(null, txData);
    }

    SendFragment.Listener getActivityCallback() {
        return sendListener.getActivityCallback();
    }
}
