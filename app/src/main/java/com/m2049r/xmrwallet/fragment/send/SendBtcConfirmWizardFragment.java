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

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.m2049r.xmrwallet.BuildConfig;
import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.data.TxData;
import com.m2049r.xmrwallet.data.TxDataBtc;
import com.m2049r.xmrwallet.model.PendingTransaction;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.OkHttpHelper;
import com.m2049r.xmrwallet.widget.SendProgressView;
import com.m2049r.xmrwallet.xmrto.XmrToError;
import com.m2049r.xmrwallet.xmrto.XmrToException;
import com.m2049r.xmrwallet.xmrto.api.CreateOrder;
import com.m2049r.xmrwallet.xmrto.api.QueryOrderStatus;
import com.m2049r.xmrwallet.xmrto.api.XmrToApi;
import com.m2049r.xmrwallet.xmrto.api.XmrToCallback;
import com.m2049r.xmrwallet.xmrto.network.XmrToApiImpl;

import java.text.NumberFormat;
import java.util.Locale;

import timber.log.Timber;

public class SendBtcConfirmWizardFragment extends SendWizardFragment implements SendConfirm {
    private final int QUERY_INTERVAL = 500;//ms

    public static SendBtcConfirmWizardFragment newInstance(SendConfirmWizardFragment.Listener listener) {
        SendBtcConfirmWizardFragment instance = new SendBtcConfirmWizardFragment();
        instance.setSendListener(listener);
        return instance;
    }

    SendConfirmWizardFragment.Listener sendListener;

    public SendBtcConfirmWizardFragment setSendListener(SendConfirmWizardFragment.Listener listener) {
        this.sendListener = listener;
        return this;
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
    private TextView tvTxXmrToKey;
    private TextView tvTxFee;
    private TextView tvTxTotal;
    private View llConfirmSend;
    private Button bSend;
    private View pbProgressSend;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Timber.d("onCreateView(%s)", (String.valueOf(savedInstanceState)));

        View view = inflater.inflate(
                R.layout.fragment_send_btc_confirm, container, false);

        tvTxBtcAddress = view.findViewById(R.id.tvTxBtcAddress);
        tvTxBtcAmount = view.findViewById(R.id.tvTxBtcAmount);
        tvTxBtcRate = view.findViewById(R.id.tvTxBtcRate);
        tvTxXmrToKey = view.findViewById(R.id.tvTxXmrToKey);

        tvTxFee = view.findViewById(R.id.tvTxFee);
        tvTxTotal = view.findViewById(R.id.tvTxTotal);


        llStageA = view.findViewById(R.id.llStageA);
        evStageA = view.findViewById(R.id.evStageA);
        llStageB = view.findViewById(R.id.llStageB);
        evStageB = view.findViewById(R.id.evStageB);
        llStageC = view.findViewById(R.id.llStageC);
        evStageC = view.findViewById(R.id.evStageC);

        tvTxXmrToKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.clipBoardCopy(getActivity(), getString(R.string.label_copy_xmrtokey), tvTxXmrToKey.getText().toString());
                Toast.makeText(getActivity(), getString(R.string.message_copy_xmrtokey), Toast.LENGTH_SHORT).show();
            }
        });

        llConfirmSend = view.findViewById(R.id.llConfirmSend);
        pbProgressSend = view.findViewById(R.id.pbProgressSend);

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
        sendListener.getTxData().getUserNotes().setXmrtoStatus(xmrtoStatus);
        ((TxDataBtc) sendListener.getTxData()).setXmrtoUuid(xmrtoStatus.getUuid());
        // TODO make method in TxDataBtc to set both of the above in one go
        sendListener.commitTransaction();
        pbProgressSend.setVisibility(View.VISIBLE);
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
                && (xmrtoStatus != null)
                && (xmrtoStatus.isCreated()
                && (xmrtoStatus.getUuid().equals(txTag)))) {
            this.pendingTransaction = pendingTransaction;
            getView().post(new Runnable() {
                @Override
                public void run() {
                    hideProgress();
                    tvTxFee.setText(Wallet.getDisplayAmount(pendingTransaction.getFee()));
                    tvTxTotal.setText(Wallet.getDisplayAmount(
                            pendingTransaction.getFee() + pendingTransaction.getAmount()));
                    updateSendButton();
                }
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
        Helper.hideKeyboard(getActivity());
        llStageA.setVisibility(View.INVISIBLE);
        evStageA.hideProgress();
        llStageB.setVisibility(View.INVISIBLE);
        evStageB.hideProgress();
        llStageC.setVisibility(View.INVISIBLE);
        evStageC.hideProgress();
        isResumed = true;
        if ((pendingTransaction == null) && (inProgress == STAGE_X)) {
            createOrder();
        } // otherwise just sit there blank
        // TODO: don't sit there blank - can this happen? should we just die?
    }

    private int sendCountdown = 0;
    private static final int XMRTO_COUNTDOWN = 10 * 60; // 10 minutes
    private static final int XMRTO_COUNTDOWN_STEP = 1; // 1 second

    Runnable updateRunnable = null;

    void startSendTimer() {
        Timber.d("startSendTimer()");
        sendCountdown = XMRTO_COUNTDOWN;
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
        final Activity activity = getActivity();
        View promptsView = getLayoutInflater().inflate(R.layout.prompt_password, null);
        android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(activity);
        alertDialogBuilder.setView(promptsView);

        final TextInputLayout etPassword = promptsView.findViewById(R.id.etPassword);
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
                .setPositiveButton(getString(R.string.label_ok), new DialogInterface.OnClickListener() {
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
                .setNegativeButton(getString(R.string.label_cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Helper.hideKeyboardAlways(activity);
                                dialog.cancel();
                                bSend.setEnabled(sendCountdown > 0); // allow to try again
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
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN))
                        || (actionId == EditorInfo.IME_ACTION_DONE)) {
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

        if (Helper.preventScreenshot()) {
            passwordDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }

        passwordDialog.show();
    }

    // creates a pending transaction and calls us back with transactionCreated()
    // or createTransactionFailed()
    void prepareSend() {
        if (!isResumed) return;
        if ((xmrtoStatus == null)) {
            throw new IllegalStateException("xmrtoStatus is null");
        }
        if ((!xmrtoStatus.isCreated())) {
            throw new IllegalStateException("order is not created");
        }
        showProgress(3, getString(R.string.label_send_progress_create_tx));
        TxData txData = sendListener.getTxData();
        txData.setDestinationAddress(xmrtoStatus.getXmrReceivingAddress());
        txData.setPaymentId(xmrtoStatus.getXmrRequiredPaymentIdShort());
        txData.setAmount(Wallet.getAmountFromDouble(xmrtoStatus.getXmrAmountTotal()));
        getActivityCallback().onPrepareSend(xmrtoStatus.getUuid(), txData);
    }

    SendFragment.Listener getActivityCallback() {
        return sendListener.getActivityCallback();
    }

    private CreateOrder xmrtoOrder = null;

    private void processCreateOrder(final CreateOrder createOrder) {
        Timber.d("processCreateOrder %s", createOrder.getUuid());
        xmrtoOrder = createOrder;
        if (QueryOrderStatus.State.TO_BE_CREATED.toString().equals(createOrder.getState())) {
            getView().post(new Runnable() {
                @Override
                public void run() {
                    tvTxXmrToKey.setText(createOrder.getUuid());
                    tvTxBtcAddress.setText(createOrder.getBtcDestAddress());
                    hideProgress();
                }
            });
            queryOrder(createOrder.getUuid());
        } else {
            throw new IllegalStateException("Create Order is not TO_BE_CREATED");
        }
    }

    private void processCreateOrderError(final Exception ex) {
        Timber.e("processCreateOrderError %s", ex.getLocalizedMessage());
        getView().post(new Runnable() {
            @Override
            public void run() {
                if (ex instanceof XmrToException) {
                    XmrToException xmrEx = (XmrToException) ex;
                    XmrToError xmrErr = xmrEx.getError();
                    if (xmrErr != null) {
                        if (xmrErr.isRetryable()) {
                            showStageError(xmrErr.getErrorId().toString(), xmrErr.getErrorMsg(),
                                    getString(R.string.text_retry));
                            evStageA.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    evStageA.setOnClickListener(null);
                                    createOrder();
                                }
                            });
                        } else {
                            showStageError(xmrErr.getErrorId().toString(), xmrErr.getErrorMsg(),
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
            }
        });
    }

    private void createOrder() {
        if (!isResumed) return;
        Timber.d("createOrder");
        xmrtoOrder = null;
        xmrtoStatus = null;
        showProgress(1, getString(R.string.label_send_progress_xmrto_create));
        TxDataBtc txDataBtc = (TxDataBtc) sendListener.getTxData();

        XmrToCallback<CreateOrder> callback = new XmrToCallback<CreateOrder>() {
            @Override
            public void onSuccess(CreateOrder createOrder) {
                if (!isResumed) return;
                if (xmrtoOrder != null) {
                    Timber.w("another ongoing create order request");
                    return;
                }
                processCreateOrder(createOrder);
            }

            @Override
            public void onError(Exception ex) {
                if (!isResumed) return;
                if (xmrtoOrder != null) {
                    Timber.w("another ongoing create order request");
                    return;
                }
                processCreateOrderError(ex);
            }
        };

        if (txDataBtc.getBip70() != null) {
            getXmrToApi().createOrder(txDataBtc.getBip70(), callback);
        } else {
            getXmrToApi().createOrder(txDataBtc.getBtcAmount(), txDataBtc.getBtcAddress(), callback);
        }
    }

    private QueryOrderStatus xmrtoStatus = null;

    private void processQueryOrder(final QueryOrderStatus status) {
        Timber.d("processQueryOrder %s for %s", status.getState().toString(), status.getUuid());
        xmrtoStatus = status;
        if (status.isCreated()) {
            getView().post(new Runnable() {
                @Override
                public void run() {
                    NumberFormat df = NumberFormat.getInstance(Locale.US);
                    df.setMaximumFractionDigits(12);
                    String btcAmount = df.format(status.getBtcAmount());
                    String xmrAmountTotal = df.format(status.getXmrAmountTotal());
                    tvTxBtcAmount.setText(getString(R.string.text_send_btc_amount, btcAmount, xmrAmountTotal));
                    String xmrPriceBtc = df.format(status.getXmrPriceBtc());
                    tvTxBtcRate.setText(getString(R.string.text_send_btc_rate, xmrPriceBtc));

                    double calcRate = status.getBtcAmount() / status.getXmrPriceBtc();
                    Timber.i("Rates: %f / %f", calcRate, status.getXmrPriceBtc());

                    tvTxBtcAddress.setText(status.getBtcDestAddress()); // TODO test if this is different?

                    Timber.i("Expires @ %s, in %s seconds", status.getExpiresAt().toString(), status.getSecondsTillTimeout());

                    Timber.i("Status = %s", status.getState().toString());
                    tvTxXmrToKey.setText(status.getUuid());

                    Timber.d("AmountRemaining=%f, XmrAmountTotal=%f", status.getXmrAmountRemaining(), status.getXmrAmountTotal());
                    hideProgress();
                    startSendTimer();
                    prepareSend();
                }
            });
        } else {
            Timber.d("try again!");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    queryOrder(status.getUuid());
                }
            }, QUERY_INTERVAL);
        }
    }

    Handler handler = new Handler();

    private void processQueryOrderError(final Exception ex) {
        Timber.e("processQueryOrderError %s", ex.getLocalizedMessage());
        getView().post(new Runnable() {
            @Override
            public void run() {
                if (ex instanceof XmrToException) {
                    XmrToException xmrEx = (XmrToException) ex;
                    XmrToError xmrErr = xmrEx.getError();
                    if (xmrErr != null) {
                        if (xmrErr.isRetryable()) {
                            showStageError(xmrErr.getErrorId().toString(), xmrErr.getErrorMsg(),
                                    getString(R.string.text_retry));
                            evStageB.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    evStageB.setOnClickListener(null);
                                    queryOrder(xmrtoOrder.getUuid());
                                }
                            });
                        } else {
                            showStageError(xmrErr.getErrorId().toString(), xmrErr.getErrorMsg(),
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
            }
        });
    }

    private void queryOrder(final String uuid) {
        Timber.d("queryOrder(%s)", uuid);
        if (!isResumed) return;
        getView().post(new Runnable() {
            @Override
            public void run() {
                xmrtoStatus = null;
                showProgress(2, getString(R.string.label_send_progress_xmrto_query));
                getXmrToApi().queryOrderStatus(uuid, new XmrToCallback<QueryOrderStatus>() {
                    @Override
                    public void onSuccess(QueryOrderStatus status) {
                        if (!isResumed) return;
                        if (xmrtoOrder == null) return;
                        if (!status.getUuid().equals(xmrtoOrder.getUuid())) {
                            Timber.d("Query UUID does not match");
                            // ignore (we got a response to a stale request)
                            return;
                        }
                        if (xmrtoStatus != null)
                            throw new IllegalStateException("xmrtoStatus must be null here!");
                        processQueryOrder(status);
                    }

                    @Override
                    public void onError(Exception ex) {
                        if (!isResumed) return;
                        processQueryOrderError(ex);
                    }
                });
            }
        });
    }

    private XmrToApi xmrToApi = null;

    private XmrToApi getXmrToApi() {
        if (xmrToApi == null) {
            synchronized (this) {
                if (xmrToApi == null) {
                    xmrToApi = new XmrToApiImpl(OkHttpHelper.getOkHttpClient(),
                            Helper.getXmrToBaseUrl());
                }
            }
        }
        return xmrToApi;
    }
}
