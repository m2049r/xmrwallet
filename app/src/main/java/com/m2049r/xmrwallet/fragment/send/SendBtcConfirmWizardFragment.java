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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.data.TxData;
import com.m2049r.xmrwallet.data.TxDataBtc;
import com.m2049r.xmrwallet.model.PendingTransaction;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.service.shift.ShiftCallback;
import com.m2049r.xmrwallet.service.shift.ShiftError;
import com.m2049r.xmrwallet.service.shift.ShiftException;
import com.m2049r.xmrwallet.service.shift.sideshift.api.CreateOrder;
import com.m2049r.xmrwallet.service.shift.sideshift.api.RequestQuote;
import com.m2049r.xmrwallet.service.shift.sideshift.api.SideShiftApi;
import com.m2049r.xmrwallet.service.shift.sideshift.network.SideShiftApiImpl;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.ServiceHelper;
import com.m2049r.xmrwallet.widget.SendProgressView;

import java.text.NumberFormat;
import java.util.Locale;

import timber.log.Timber;

public class SendBtcConfirmWizardFragment extends SendWizardFragment implements SendConfirm {
    public static SendBtcConfirmWizardFragment newInstance(SendConfirmWizardFragment.Listener listener) {
        SendBtcConfirmWizardFragment instance = new SendBtcConfirmWizardFragment();
        instance.setSendListener(listener);
        return instance;
    }

    SendConfirmWizardFragment.Listener sendListener;

    public void setSendListener(SendConfirmWizardFragment.Listener listener) {
        this.sendListener = listener;
    }

    private View llStageA;
    private SendProgressView evStageA;
    private View llStageB;
    private SendProgressView evStageB;
    private View llStageC;
    private SendProgressView evStageC;
    private TextView tvTxBtcAmount;
    private TextView tvTxBtcRate;
    private TextView tvTxBtcAddress;
    private TextView tvTxBtcAddressLabel;
    private TextView tvTxXmrToKey;
    private TextView tvTxFee;
    private TextView tvTxTotal;
    private View llConfirmSend;
    private Button bSend;
    private View pbProgressSend;
    private TextView tvTxChange;
    private View llPocketChange;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Timber.d("onCreateView(%s)", (String.valueOf(savedInstanceState)));

        View view = inflater.inflate(
                R.layout.fragment_send_btc_confirm, container, false);

        tvTxBtcAddress = view.findViewById(R.id.tvTxBtcAddress);
        tvTxBtcAddressLabel = view.findViewById(R.id.tvTxBtcAddressLabel);
        tvTxBtcAmount = view.findViewById(R.id.tvTxBtcAmount);
        tvTxBtcRate = view.findViewById(R.id.tvTxBtcRate);
        tvTxXmrToKey = view.findViewById(R.id.tvTxXmrToKey);

        tvTxFee = view.findViewById(R.id.tvTxFee);
        tvTxTotal = view.findViewById(R.id.tvTxTotal);
        tvTxChange = view.findViewById(R.id.tvTxChange);
        llPocketChange = view.findViewById(R.id.llPocketChange);

        llStageA = view.findViewById(R.id.llStageA);
        evStageA = view.findViewById(R.id.evStageA);
        llStageB = view.findViewById(R.id.llStageB);
        evStageB = view.findViewById(R.id.evStageB);
        llStageC = view.findViewById(R.id.llStageC);
        evStageC = view.findViewById(R.id.evStageC);

        tvTxXmrToKey.setOnClickListener(v -> {
            Helper.clipBoardCopy(getActivity(), getString(R.string.label_copy_xmrtokey), tvTxXmrToKey.getText().toString());
            Toast.makeText(getActivity(), getString(R.string.message_copy_xmrtokey), Toast.LENGTH_SHORT).show();
        });

        llConfirmSend = view.findViewById(R.id.llConfirmSend);
        pbProgressSend = view.findViewById(R.id.pbProgressSend);

        bSend = view.findViewById(R.id.bSend);
        bSend.setEnabled(false);

        bSend.setOnClickListener(v -> {
            Timber.d("bSend.setOnClickListener");
            bSend.setEnabled(false);
            preSend();
        });

        return view;
    }

    int inProgress = 0;
    final static int STAGE_X = 0;
    final static int STAGE_A = 1;
    final static int STAGE_B = 2;
    final static int STAGE_C = 3;

    private void showProgress(int stage, String progressText) {
        Timber.d("showProgress(%d)", stage);
        inProgress = stage;
        switch (stage) {
            case STAGE_A:
                evStageA.showProgress(progressText);
                break;
            case STAGE_B:
                evStageB.showProgress(progressText);
                break;
            case STAGE_C:
                evStageC.showProgress(progressText);
                break;
            default:
                throw new IllegalStateException("unknown stage " + stage);
        }
    }

    public void hideProgress() {
        Timber.d("hideProgress(%d)", inProgress);
        switch (inProgress) {
            case STAGE_A:
                evStageA.hideProgress();
                llStageA.setVisibility(View.VISIBLE);
                break;
            case STAGE_B:
                evStageB.hideProgress();
                llStageB.setVisibility(View.VISIBLE);
                break;
            case STAGE_C:
                evStageC.hideProgress();
                llStageC.setVisibility(View.VISIBLE);
                break;
            default:
                throw new IllegalStateException("unknown stage " + inProgress);
        }
        inProgress = STAGE_X;
    }

    public void showStageError(String code, String message, String solution) {
        switch (inProgress) {
            case STAGE_A:
                evStageA.showMessage(code, message, solution);
                break;
            case STAGE_B:
                evStageB.showMessage(code, message, solution);
                break;
            case STAGE_C:
                evStageC.showMessage(code, message, solution);
                break;
            default:
                throw new IllegalStateException("unknown stage");
        }
        inProgress = STAGE_X;
    }

    PendingTransaction pendingTransaction = null;

    void send() {
        Timber.d("SEND @%d", sendCountdown);
        if (sendCountdown <= 0) {
            Timber.i("User waited too long in password dialog.");
            Toast.makeText(getContext(), getString(R.string.send_xmrto_timeout), Toast.LENGTH_SHORT).show();
            return;
        }
        sendListener.getTxData().getUserNotes().setXmrtoOrder(xmrtoOrder); // note the transaction in the TX notes
        ((TxDataBtc) sendListener.getTxData()).setXmrtoOrderId(xmrtoOrder.getOrderId()); // remember the order id for later
        // TODO make method in TxDataBtc to set both of the above in one go
        sendListener.commitTransaction();
        getActivity().runOnUiThread(() -> pbProgressSend.setVisibility(View.VISIBLE));
    }

    @Override
    public void sendFailed(String error) {
        pbProgressSend.setVisibility(View.INVISIBLE);
        Toast.makeText(getContext(), getString(R.string.status_transaction_failed, error), Toast.LENGTH_LONG).show();
    }

    @Override
    // callback from wallet when PendingTransaction created (started by prepareSend() here
    public void transactionCreated(final String txTag, final PendingTransaction pendingTransaction) {
        if (isResumed
                && (inProgress == STAGE_C)
                && (xmrtoOrder != null)
                && (xmrtoOrder.getOrderId().equals(txTag))) {
            this.pendingTransaction = pendingTransaction;
            getView().post(() -> {
                hideProgress();
                tvTxFee.setText(Wallet.getDisplayAmount(pendingTransaction.getFee()));
                tvTxTotal.setText(Wallet.getDisplayAmount(
                        pendingTransaction.getFee() + pendingTransaction.getAmount()));
                final long change = pendingTransaction.getPocketChange();
                if (change > 0) {
                    llPocketChange.setVisibility(View.VISIBLE);
                    tvTxChange.setText(Wallet.getDisplayAmount(change));
                } else {
                    llPocketChange.setVisibility(View.GONE);
                }
                updateSendButton();
            });
        } else {
            this.pendingTransaction = null;
            sendListener.disposeTransaction();
        }
    }

    @Override
    public void createTransactionFailed(String errorText) {
        Timber.e("CREATE TX FAILED");
        if (pendingTransaction != null) {
            throw new IllegalStateException("pendingTransaction is not null");
        }
        showStageError(getString(R.string.send_create_tx_error_title),
                errorText,
                getString(R.string.text_noretry_monero));
    }

    @Override
    public boolean onValidateFields() {
        return true;
    }

    private boolean isResumed = false;

    @Override
    public void onPauseFragment() {
        isResumed = false;
        stopSendTimer();
        sendListener.disposeTransaction();
        pendingTransaction = null;
        inProgress = STAGE_X;
        updateSendButton();
        super.onPauseFragment();
    }

    @Override
    public void onResumeFragment() {
        super.onResumeFragment();
        Timber.d("onResumeFragment()");
        if (sendListener.getMode() != SendFragment.Mode.BTC) {
            throw new IllegalStateException("Mode is not BTC!");
        }
        if (!((TxDataBtc) sendListener.getTxData()).getBtcSymbol().toLowerCase().equals(ServiceHelper.ASSET))
            throw new IllegalStateException("Asset Symbol is wrong!");
        Helper.hideKeyboard(getActivity());
        llStageA.setVisibility(View.INVISIBLE);
        evStageA.hideProgress();
        llStageB.setVisibility(View.INVISIBLE);
        evStageB.hideProgress();
        llStageC.setVisibility(View.INVISIBLE);
        evStageC.hideProgress();
        isResumed = true;
        if ((pendingTransaction == null) && (inProgress == STAGE_X)) {
            stageA();
        } // otherwise just sit there blank
        // TODO: don't sit there blank - can this happen? should we just die?
    }

    private int sendCountdown = 0;
    private static final int XMRTO_COUNTDOWN_STEP = 1; // 1 second

    Runnable updateRunnable = null;

    void startSendTimer(int timeout) {
        Timber.d("startSendTimer()");
        sendCountdown = timeout;
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded())
                    return;
                Timber.d("updateTimer()");
                if (sendCountdown <= 0) {
                    bSend.setEnabled(false);
                    sendCountdown = 0;
                    Toast.makeText(getContext(), getString(R.string.send_xmrto_timeout), Toast.LENGTH_SHORT).show();
                }
                int minutes = sendCountdown / 60;
                int seconds = sendCountdown % 60;
                String t = String.format("%d:%02d", minutes, seconds);
                bSend.setText(getString(R.string.send_send_timed_label, t));
                if (sendCountdown > 0) {
                    sendCountdown -= XMRTO_COUNTDOWN_STEP;
                    getView().postDelayed(this, XMRTO_COUNTDOWN_STEP * 1000);
                }
            }
        };
        getView().post(updateRunnable);
    }

    void stopSendTimer() {
        getView().removeCallbacks(updateRunnable);
    }

    void updateSendButton() {
        Timber.d("updateSendButton()");
        if (pendingTransaction != null) {
            llConfirmSend.setVisibility(View.VISIBLE);
            bSend.setEnabled(sendCountdown > 0);
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
                    bSend.setEnabled(sendCountdown > 0); // allow to try again
                });
            }
        });
    }

    // creates a pending transaction and calls us back with transactionCreated()
    // or createTransactionFailed()
    void prepareSend() {
        if (!isResumed) return;
        if ((xmrtoOrder == null)) {
            throw new IllegalStateException("xmrtoOrder is null");
        }
        showProgress(3, getString(R.string.label_send_progress_create_tx));
        final TxData txData = sendListener.getTxData();
        txData.setDestination(xmrtoOrder.getXmrAddress());
        txData.setAmount(xmrtoOrder.getXmrAmount());
        getActivityCallback().onPrepareSend(xmrtoOrder.getOrderId(), txData);
    }

    SendFragment.Listener getActivityCallback() {
        return sendListener.getActivityCallback();
    }

    private RequestQuote xmrtoQuote = null;

    private void processStageA(final RequestQuote requestQuote) {
        Timber.d("processCreateOrder %s", requestQuote.getId());
        TxDataBtc txDataBtc = (TxDataBtc) sendListener.getTxData();
        // verify the BTC amount is correct
        if (requestQuote.getBtcAmount() != txDataBtc.getBtcAmount()) {
            Timber.d("Failed to get quote");
            getView().post(() -> showStageError(ShiftError.Error.SERVICE.toString(),
                    getString(R.string.shift_noquote),
                    getString(R.string.shift_checkamount)));
            return; // just stop for now
        }
        xmrtoQuote = requestQuote;
        txDataBtc.setAmount(xmrtoQuote.getXmrAmount());
        getView().post(() -> {
            // show data from the actual quote as that is what is used to
            NumberFormat df = NumberFormat.getInstance(Locale.US);
            df.setMaximumFractionDigits(12);
            final String btcAmount = df.format(xmrtoQuote.getBtcAmount());
            final String xmrAmountTotal = df.format(xmrtoQuote.getXmrAmount());
            tvTxBtcAmount.setText(getString(R.string.text_send_btc_amount,
                    btcAmount, xmrAmountTotal, txDataBtc.getBtcSymbol()));
            final String xmrPriceBtc = df.format(xmrtoQuote.getPrice());
            tvTxBtcRate.setText(getString(R.string.text_send_btc_rate, xmrPriceBtc, txDataBtc.getBtcSymbol()));
            hideProgress();
        });
        stageB(requestQuote.getId());
    }

    private void processStageAError(final Exception ex) {
        Timber.e("processStageAError %s", ex.getLocalizedMessage());
        getView().post(() -> {
            if (ex instanceof ShiftException) {
                ShiftException xmrEx = (ShiftException) ex;
                ShiftError xmrErr = xmrEx.getError();
                if (xmrErr != null) {
                    if (xmrErr.isRetryable()) {
                        showStageError(xmrErr.getErrorType().toString(), xmrErr.getErrorMsg(),
                                getString(R.string.text_retry));
                        evStageA.setOnClickListener(v -> {
                            evStageA.setOnClickListener(null);
                            stageA();
                        });
                    } else {
                        showStageError(xmrErr.getErrorType().toString(), xmrErr.getErrorMsg(),
                                getString(R.string.text_noretry));
                    }
                } else {
                    showStageError(getString(R.string.label_generic_xmrto_error),
                            getString(R.string.text_generic_xmrto_error, xmrEx.getCode()),
                            getString(R.string.text_noretry));
                }
            } else {
                evStageA.showMessage(getString(R.string.label_generic_xmrto_error),
                        ex.getLocalizedMessage(),
                        getString(R.string.text_noretry));
            }
        });
    }

    private void stageA() {
        if (!isResumed) return;
        Timber.d("Request Quote");
        xmrtoQuote = null;
        xmrtoOrder = null;
        showProgress(1, getString(R.string.label_send_progress_xmrto_create));
        TxDataBtc txDataBtc = (TxDataBtc) sendListener.getTxData();

        ShiftCallback<RequestQuote> callback = new ShiftCallback<RequestQuote>() {
            @Override
            public void onSuccess(RequestQuote requestQuote) {
                if (!isResumed) return;
                if (xmrtoQuote != null) {
                    Timber.w("another ongoing request quote request");
                    return;
                }
                processStageA(requestQuote);
            }

            @Override
            public void onError(Exception ex) {
                if (!isResumed) return;
                if (xmrtoQuote != null) {
                    Timber.w("another ongoing request quote request");
                    return;
                }
                processStageAError(ex);
            }
        };

        getXmrToApi().requestQuote(txDataBtc.getBtcAmount(), callback);
    }

    private CreateOrder xmrtoOrder = null;

    private void processStageB(final CreateOrder order) {
        Timber.d("processCreateOrder %s for %s", order.getOrderId(), order.getQuoteId());
        TxDataBtc txDataBtc = (TxDataBtc) sendListener.getTxData();
        // verify amount & destination
        if ((order.getBtcAmount() != txDataBtc.getBtcAmount())
                || (!txDataBtc.validateAddress(order.getBtcAddress()))) {
            throw new IllegalStateException("Order does not fulfill quote!"); // something is terribly wrong - die
        }
        xmrtoOrder = order;
        getView().post(() -> {
            tvTxXmrToKey.setText(order.getOrderId());
            tvTxBtcAddress.setText(order.getBtcAddress());
            tvTxBtcAddressLabel.setText(getString(R.string.label_send_btc_address, txDataBtc.getBtcSymbol()));
            hideProgress();
            Timber.d("Expires @ %s", order.getExpiresAt().toString());
            final int timeout = (int) (order.getExpiresAt().getTime() - order.getCreatedAt().getTime()) / 1000 - 60; // -1 minute buffer
            startSendTimer(timeout);
            prepareSend();
        });
    }

    private void processStageBError(final Exception ex) {
        Timber.e("processCreateOrderError %s", ex.getLocalizedMessage());
        getView().post(() -> {
            if (ex instanceof ShiftException) {
                ShiftException xmrEx = (ShiftException) ex;
                ShiftError xmrErr = xmrEx.getError();
                if (xmrErr != null) {
                    if (xmrErr.isRetryable()) {
                        showStageError(xmrErr.getErrorType().toString(), xmrErr.getErrorMsg(),
                                getString(R.string.text_retry));
                        evStageB.setOnClickListener(v -> {
                            evStageB.setOnClickListener(null);
                            stageB(xmrtoOrder.getOrderId());
                        });
                    } else {
                        showStageError(xmrErr.getErrorType().toString(), xmrErr.getErrorMsg(),
                                getString(R.string.text_noretry));
                    }
                } else {
                    showStageError(getString(R.string.label_generic_xmrto_error),
                            getString(R.string.text_generic_xmrto_error, xmrEx.getCode()),
                            getString(R.string.text_noretry));
                }
            } else {
                evStageB.showMessage(getString(R.string.label_generic_xmrto_error),
                        ex.getLocalizedMessage(),
                        getString(R.string.text_noretry));
            }
        });
    }

    private void stageB(final String quoteId) {
        Timber.d("createOrder(%s)", quoteId);
        if (!isResumed) return;
        final String btcAddress = ((TxDataBtc) sendListener.getTxData()).getBtcAddress();
        getView().post(() -> {
            xmrtoOrder = null;
            showProgress(2, getString(R.string.label_send_progress_xmrto_query));
            getXmrToApi().createOrder(quoteId, btcAddress, new ShiftCallback<CreateOrder>() {
                @Override
                public void onSuccess(CreateOrder order) {
                    if (!isResumed) return;
                    if (xmrtoQuote == null) return;
                    if (!order.getQuoteId().equals(xmrtoQuote.getId())) {
                        Timber.d("Quote ID does not match");
                        // ignore (we got a response to a stale request)
                        return;
                    }
                    if (xmrtoOrder != null)
                        throw new IllegalStateException("xmrtoOrder must be null here!");
                    processStageB(order);
                }

                @Override
                public void onError(Exception ex) {
                    if (!isResumed) return;
                    processStageBError(ex);
                }
            });
        });
    }

    private SideShiftApi xmrToApi = null;

    private SideShiftApi getXmrToApi() {
        if (xmrToApi == null) {
            synchronized (this) {
                if (xmrToApi == null) {
                    xmrToApi = new SideShiftApiImpl(ServiceHelper.getXmrToBaseUrl());
                }
            }
        }
        return xmrToApi;
    }
}
