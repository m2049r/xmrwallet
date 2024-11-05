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

import androidx.annotation.NonNull;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.data.TxData;
import com.m2049r.xmrwallet.data.TxDataBtc;
import com.m2049r.xmrwallet.model.PendingTransaction;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.service.shift.ShiftError;
import com.m2049r.xmrwallet.service.shift.ShiftException;
import com.m2049r.xmrwallet.service.shift.ShiftService;
import com.m2049r.xmrwallet.service.shift.api.CreateOrder;
import com.m2049r.xmrwallet.service.shift.api.RequestQuote;
import com.m2049r.xmrwallet.service.shift.process.ShiftProcess;
import com.m2049r.xmrwallet.util.AmountHelper;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.widget.SendProgressView;

import java.util.Locale;

import timber.log.Timber;

public class SendBtcConfirmWizardFragment extends SendWizardFragment implements SendConfirm, Shifter {

    public static SendBtcConfirmWizardFragment newInstance(SendConfirmWizardFragment.Listener listener) {
        SendBtcConfirmWizardFragment instance = new SendBtcConfirmWizardFragment();
        instance.sendListener = listener;
        return instance;
    }

    private SendConfirmWizardFragment.Listener sendListener;

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

    private TextView tvTxXmrToKeyLabel;
    private TextView tvTxXmrToInfo;

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

        tvTxXmrToKeyLabel = view.findViewById(R.id.tvTxXmrToKeyLabel);
        tvTxXmrToInfo = view.findViewById(R.id.tvTxXmrToInfo);


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
            Helper.clipBoardCopy(requireActivity(), getString(R.string.label_copy_xmrtokey), tvTxXmrToKey.getText().toString());
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

    @NonNull
    Shifter.Stage inProgress = Stage.X;

    @Override
    public void showProgress(@NonNull Shifter.Stage stage) {
        Timber.d("showProgress(%s)", stage);
        requireView().post(() -> {
            switch (stage) {
                case A:
                    evStageA.showProgress(getString(R.string.label_send_progress_xmrto_create));
                    break;
                case B:
                    evStageB.showProgress(getString(R.string.label_send_progress_xmrto_query));
                    break;
                case C:
                    evStageC.showProgress(getString(R.string.label_send_progress_create_tx));
                    break;
                default:
                    throw new IllegalArgumentException("invalid stage " + stage);
            }
            inProgress = stage;
        });
    }

    public void hideProgress() {
        Timber.d("hideProgress(%s)", inProgress);
        switch (inProgress) {
            case A:
                evStageA.hideProgress();
                llStageA.setVisibility(View.VISIBLE);
                break;
            case B:
                evStageB.hideProgress();
                llStageA.setVisibility(View.VISIBLE); // show Stage A info when B is ready
                llStageB.setVisibility(View.VISIBLE);
                break;
            case C:
                evStageC.hideProgress();
                llStageC.setVisibility(View.VISIBLE);
                break;
        }
        inProgress = Stage.X;
    }

    private void showErrorMessage(String code, String message, String solution) {
        switch (inProgress) {
            case A:
                evStageA.showMessage(code, message, solution);
                break;
            case B:
                evStageB.showMessage(code, message, solution);
                break;
            case C:
                evStageC.showMessage(code, message, solution);
                break;
            default:
                throw new IllegalStateException("invalid stage");
        }
        inProgress = Stage.X;
    }

    public void showQuoteError() {
        showErrorMessage(ShiftError.Type.SERVICE.toString(),
                getString(R.string.shift_noquote),
                getString(R.string.shift_checkamount));
    }


    PendingTransaction pendingTransaction = null;

    void send() {
        Timber.d("SEND @%d", sendCountdown);
        if (sendCountdown <= 0) {
            Timber.i("User waited too long in password dialog.");
            Toast.makeText(getContext(), getString(R.string.send_xmrto_timeout), Toast.LENGTH_SHORT).show();
            return;
        }
        sendListener.commitTransaction();
        requireActivity().runOnUiThread(() -> pbProgressSend.setVisibility(View.VISIBLE));
    }

    @Override
    public void sendFailed(String error) {
        pbProgressSend.setVisibility(View.INVISIBLE);
        Toast.makeText(getContext(), getString(R.string.status_transaction_failed, error), Toast.LENGTH_LONG).show();
    }

    private String orderId = null;

    @Override
    // callback from wallet when PendingTransaction created (started by prepareSend() here
    public void transactionCreated(final String txTag, final PendingTransaction pendingTransaction) {
        if (isResumed
                && (inProgress == Stage.C)
                && (orderId != null)
                && (orderId.equals(txTag))) {
            this.pendingTransaction = pendingTransaction;
            requireView().post(() -> {
                Timber.d("transactionCreated");
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
        showErrorMessage(getString(R.string.send_create_tx_error_title),
                errorText,
                getString(R.string.text_noretry_monero));
    }

    private boolean isResumed = false;

    @Override
    public void onPauseFragment() {
        isResumed = false;
        shiftProcess = null;
        stopSendTimer();
        sendListener.disposeTransaction();
        pendingTransaction = null;
        inProgress = Stage.X;
        //TODO: maybe reset the progress messages
        updateSendButton();
        super.onPauseFragment();
    }

    private TxDataBtc getTxData() {
        final TxData txData = sendListener.getTxData();
        if (txData instanceof TxDataBtc) {
            return (TxDataBtc) txData;
        } else {
            throw new IllegalStateException("TxData not BTC");
        }
    }

    @Override
    public void onResumeFragment() {
        super.onResumeFragment();
        Timber.d("onResumeFragment()");
        if (sendListener.getMode() != SendFragment.Mode.BTC) {
            throw new IllegalStateException("Mode is not BTC!");
        }
        final String btcSymbol = getTxData().getBtcSymbol();
        if (!btcSymbol.equalsIgnoreCase(ShiftService.ASSET.getSymbol()))
            throw new IllegalStateException("Asset Symbol is wrong (" + btcSymbol + "!=" + ShiftService.ASSET.getSymbol() + ")");
        Helper.hideKeyboard(getActivity());
        llStageA.setVisibility(View.INVISIBLE);
        evStageA.hideProgress();
        llStageB.setVisibility(View.INVISIBLE);
        evStageB.hideProgress();
        llStageC.setVisibility(View.INVISIBLE);
        evStageC.hideProgress();

        if (shiftProcess != null) throw new IllegalStateException("shiftProcess not null");
        shiftProcess = getTxData().getShiftService().createProcess(this);
        tvTxXmrToKeyLabel.setText(getString(R.string.label_send_btc_xmrto_key, shiftProcess.getService().getLabel()));
        tvTxXmrToKeyLabel.setCompoundDrawablesRelativeWithIntrinsicBounds(shiftProcess.getService().getIconId(), 0, 0, 0);
        tvTxXmrToInfo.setText(getString(R.string.label_send_btc_xmrto_info, shiftProcess.getService().getLabel()));

        isResumed = true;
        if ((pendingTransaction == null) && (inProgress == Stage.X)) {
            Timber.d("Starting ShiftProcess");
            shiftProcess.run(getTxData());
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
                String t = String.format(Locale.US, "%d:%02d", minutes, seconds);
                bSend.setText(getString(R.string.send_send_timed_label, t));
                if (sendCountdown > 0) {
                    sendCountdown -= XMRTO_COUNTDOWN_STEP;
                    requireView().postDelayed(this, XMRTO_COUNTDOWN_STEP * 1000);
                }
            }
        };
        requireView().post(updateRunnable);
    }

    void stopSendTimer() {
        requireView().removeCallbacks(updateRunnable);
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
                requireActivity().runOnUiThread(() -> {
                    bSend.setEnabled(sendCountdown > 0); // allow to try again
                });
            }
        });
    }

    // creates a pending transaction and calls us back with transactionCreated()
    // or createTransactionFailed()
    public void prepareSend(CreateOrder order) {
        if (!isResumed) return;
        if ((order == null)) {
            throw new IllegalStateException("order is null");
        }
        showProgress(Stage.C);
        orderId = order.getOrderId();
        final TxDataBtc txData = getTxData();
        txData.setDestination(order.getXmrAddress());
        txData.setAmount(order.getXmrAmount());
        txData.getUserNotes().setXmrtoOrder(order); // note the transaction in the TX notes
        txData.setXmrtoOrderId(order.getOrderId()); // remember the order id for later
        txData.setXmrtoQueryOrderToken(order.getQueryOrderId()); // remember the order id for later
        getActivityCallback().onPrepareSend(order.getOrderId(), txData);
    }

    SendFragment.Listener getActivityCallback() {
        return sendListener.getActivityCallback();
    }

    private ShiftProcess shiftProcess;

    public void showQuote(double btcAmount, double xmrAmount, double price) {
        final String symbol = getTxData().getBtcSymbol();
        tvTxBtcAmount.setText(getString(R.string.text_send_btc_amount,
                AmountHelper.format(btcAmount), AmountHelper.format(xmrAmount), symbol));
        tvTxBtcRate.setText(getString(R.string.text_send_btc_rate, AmountHelper.format(price), symbol));
    }

    // Shifter
    public void onQuoteReceived(RequestQuote quote) {
        requireView().post(() -> {
            Timber.d("onQuoteReceived");
            showQuote(quote.getBtcAmount(), quote.getXmrAmount(), quote.getPrice());
            hideProgress();
        });
    }

    public void onQuoteError(final Exception ex) {
        Timber.e("processStageAError %s", ex.getLocalizedMessage());
        requireView().post(() -> {
            if (ex instanceof ShiftException) {
                ShiftException xmrEx = (ShiftException) ex;
                ShiftError xmrErr = xmrEx.getError();
                if (xmrErr != null) {
                    if (xmrErr.isRetryable()) {
                        showErrorMessage(xmrErr.getType().toString(), xmrErr.getErrorMsg(),
                                getString(R.string.text_retry));
                        evStageA.setOnClickListener(v -> {
                            evStageA.setOnClickListener(null);
                            shiftProcess.restart();
                        });
                    } else {
                        showErrorMessage(xmrErr.getType().toString(), xmrErr.getErrorMsg(),
                                getString(R.string.text_noretry, getTxData().getShiftService().getLabel()));
                    }
                } else {
                    showErrorMessage(getString(R.string.label_generic_xmrto_error),
                            getString(R.string.text_generic_xmrto_error, xmrEx.getCode()),
                            getString(R.string.text_noretry, getTxData().getShiftService().getLabel()));
                }
            } else {
                evStageA.showMessage(getString(R.string.label_generic_xmrto_error),
                        ex.getLocalizedMessage(),
                        getString(R.string.text_noretry, getTxData().getShiftService().getLabel()));
            }
        });
    }

    @Override
    public boolean isActive() {
        return isResumed;
    }

    public void onOrderCreated(CreateOrder order) {
        requireView().post(() -> {
            showQuote(order.getBtcAmount(), order.getXmrAmount(), order.getBtcAmount() / order.getXmrAmount());
            tvTxXmrToKey.setText(order.getOrderId());
            tvTxBtcAddress.setText(order.getBtcAddress());
            tvTxBtcAddressLabel.setText(getString(R.string.label_send_btc_address, order.getBtcCurrency()));
            Timber.d("onOrderCreated");
            hideProgress();
            Timber.d("Expires @ %s", order.getExpiresAt().toString());
            final int timeout = (int) (order.getExpiresAt().getTime() - order.getCreatedAt().getTime()) / 1000 - 60; // -1 minute buffer
            startSendTimer(timeout);
            prepareSend(order);
        });
    }

    public void onOrderError(final Exception ex) {
        Timber.e("onOrderError %s", ex.getLocalizedMessage());
        requireView().post(() -> {
            if (ex instanceof ShiftException) {
                ShiftException xmrEx = (ShiftException) ex;
                ShiftError xmrErr = xmrEx.getError();
                if (xmrErr != null) {
                    if (xmrErr.isRetryable()) {
                        showErrorMessage(xmrErr.getType().toString(), xmrErr.getErrorMsg(),
                                getString(R.string.text_retry));
                        evStageB.setOnClickListener(v -> {
                            evStageB.setOnClickListener(null);
                            shiftProcess.retryCreateOrder();
                        });
                    } else {
                        showErrorMessage(xmrErr.getType().toString(), xmrErr.getErrorMsg(), null);
                    }
                } else {
                    showErrorMessage(getString(R.string.label_generic_xmrto_error),
                            getString(R.string.text_generic_xmrto_error, xmrEx.getCode()),
                            getString(R.string.text_noretry, getTxData().getShiftService().getLabel()));
                }
            } else {
                evStageB.showMessage(getString(R.string.label_generic_xmrto_error),
                        ex.getLocalizedMessage(),
                        getString(R.string.text_noretry, getTxData().getShiftService().getLabel()));
            }
        });
    }

    @Override
    public void invalidateShift() {
        orderId = null;
    }
}
