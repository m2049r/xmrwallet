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

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import com.m2049r.xmrwallet.data.TxData;
import com.m2049r.xmrwallet.model.PendingTransaction;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.util.Helper;

import timber.log.Timber;

public class SendConfirmWizardFragment extends SendWizardFragment {

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

        String getNotes();

        void commitTransaction();

        void disposeTransaction();
    }

    private TextView tvTxAddress;
    private TextView tvTxPaymentId;
    private TextView tvTxNotes;
    private TextView tvTxAmount;
    private TextView tvTxFee;
    private TextView tvTxTotal;
    private View llProgress;
    private View bSend;
    private View llConfirmSend;
    private View pbProgressSend;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Timber.d("onCreateView() %s", (String.valueOf(savedInstanceState)));

        View view = inflater.inflate(
                R.layout.fragment_send_confirm, container, false);

        tvTxAddress = (TextView) view.findViewById(R.id.tvTxAddress);
        tvTxPaymentId = (TextView) view.findViewById(R.id.tvTxPaymentId);
        tvTxNotes = (TextView) view.findViewById(R.id.tvTxNotes);
        tvTxAmount = ((TextView) view.findViewById(R.id.tvTxAmount));
        tvTxFee = (TextView) view.findViewById(R.id.tvTxFee);
        tvTxTotal = (TextView) view.findViewById(R.id.tvTxTotal);

        llProgress = view.findViewById(R.id.llProgress);
        pbProgressSend = view.findViewById(R.id.pbProgressSend);
        llConfirmSend = view.findViewById(R.id.llConfirmSend);

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

    // callback from wallet when PendingTransaction created
    void transactionCreated(PendingTransaction pendingTransaction) {
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
        pbProgressSend.setVisibility(View.VISIBLE);
    }

    void sendFailed() {
        pbProgressSend.setVisibility(View.INVISIBLE);
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
            tvTxAmount.setText(Wallet.getDisplayAmount(pendingTransaction.getAmount()));
            tvTxFee.setText(Wallet.getDisplayAmount(pendingTransaction.getFee()));
            //tvTxDust.setText(Wallet.getDisplayAmount(pendingTransaction.getDust()));
            tvTxTotal.setText(Wallet.getDisplayAmount(
                    pendingTransaction.getFee() + pendingTransaction.getAmount()));
        } else {
            llConfirmSend.setVisibility(View.GONE);
            bSend.setEnabled(false);
        }
    }

    public void preSend() {
        final Activity activity = getActivity();
        View promptsView = getLayoutInflater().inflate(R.layout.prompt_password, null);
        android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(activity);
        alertDialogBuilder.setView(promptsView);

        final TextInputLayout etPassword = (TextInputLayout) promptsView.findViewById(R.id.etPassword);
        etPassword.setHint(getString(R.string.prompt_send_password));

        etPassword.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (etPassword.getError() != null) {
                    etPassword.setError(null);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
            }
        });

        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String pass = etPassword.getEditText().getText().toString();
                        if (getActivityCallback().verifyWalletPassword(pass)) {
                            dialog.dismiss();
                            Helper.hideKeyboardAlways(activity);
                            send();
                        } else {
                            etPassword.setError(getString(R.string.bad_password));
                        }
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Helper.hideKeyboardAlways(activity);
                                dialog.cancel();
                                bSend.setEnabled(true); // allow to try again
                            }
                        });

        final android.app.AlertDialog passwordDialog = alertDialogBuilder.create();
        passwordDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button button = ((android.app.AlertDialog) dialog).getButton(android.app.AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String pass = etPassword.getEditText().getText().toString();
                        if (getActivityCallback().verifyWalletPassword(pass)) {
                            Helper.hideKeyboardAlways(activity);
                            passwordDialog.dismiss();
                            send();
                        } else {
                            etPassword.setError(getString(R.string.bad_password));
                        }
                    }
                });
            }
        });

        Helper.showKeyboard(passwordDialog);

        // accept keyboard "ok"
        etPassword.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    String pass = etPassword.getEditText().getText().toString();
                    if (getActivityCallback().verifyWalletPassword(pass)) {
                        Helper.hideKeyboardAlways(activity);
                        passwordDialog.dismiss();
                        send();
                    } else {
                        etPassword.setError(getString(R.string.bad_password));
                    }
                    return true;
                }
                return false;
            }
        });
        passwordDialog.show();
    }

    void prepareSend(TxData txData) {
        getActivityCallback().onPrepareSend(txData);
    }

    SendFragment.Listener getActivityCallback() {
        return sendListener.getActivityCallback();
    }
}
