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
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.m2049r.xmrwallet.layout.ExchangeView;
import com.m2049r.xmrwallet.layout.Toolbar;
import com.m2049r.xmrwallet.model.PendingTransaction;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.BarcodeData;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.TxData;

import timber.log.Timber;

public class SendFragment extends Fragment {

    private EditText etDummy;

    private ScrollView scrollview;

    private TextInputLayout etAddress;
    private TextInputLayout etPaymentId;

    private ExchangeView evAmount;

    private Button bScan;
    private Spinner sMixin;
    private Spinner sPriority;
    private Button bPrepareSend;
    private Button bDispose;
    private Button bPaymentId;
    private LinearLayout llConfirmSend;
    private TextView tvTxAmount;
    private TextView tvTxFee;
    //TextView tvTxDust;
    private TextView tvTxTotal;
    private EditText etNotes;
    private Button bSend;
    private Button bReallySend;
    private ProgressBar pbProgress;

    final static int Mixins[] = {4, 7, 12, 25}; // must match the layout XML
    final static PendingTransaction.Priority Priorities[] =
            {PendingTransaction.Priority.Priority_Default,
                    PendingTransaction.Priority.Priority_Low,
                    PendingTransaction.Priority.Priority_Medium,
                    PendingTransaction.Priority.Priority_High}; // must match the layout XML

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_send, container, false);

        etDummy = (EditText) view.findViewById(R.id.etDummy);
        etDummy.setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        scrollview = (ScrollView) view.findViewById(R.id.scrollview);

        sMixin = (Spinner) view.findViewById(R.id.sMixin);
        sPriority = (Spinner) view.findViewById(R.id.sPriority);
        etAddress = (TextInputLayout) view.findViewById(R.id.etAddress);
        etPaymentId = (TextInputLayout) view.findViewById(R.id.etPaymentId);
        evAmount = (ExchangeView) view.findViewById(R.id.evAmount);

        bScan = (Button) view.findViewById(R.id.bScan);
        bPrepareSend = (Button) view.findViewById(R.id.bPrepareSend);
        bPaymentId = (Button) view.findViewById(R.id.bPaymentId);
        bDispose = (Button) view.findViewById(R.id.bDispose);

        llConfirmSend = (LinearLayout) view.findViewById(R.id.llConfirmSend);
        tvTxAmount = (TextView) view.findViewById(R.id.tvTxAmount);
        tvTxFee = (TextView) view.findViewById(R.id.tvTxFee);
        //tvTxDust = (TextView) view.findViewById(R.id.tvTxDust);
        tvTxTotal = (TextView) view.findViewById(R.id.tvTxTotal);
        etNotes = (EditText) view.findViewById(R.id.etNotes);
        bSend = (Button) view.findViewById(R.id.bSend);
        bReallySend = (Button) view.findViewById(R.id.bReallySend);

        pbProgress = (ProgressBar) view.findViewById(R.id.pbProgress);

        etAddress.getEditText().setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etPaymentId.getEditText().setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etNotes.setRawInputType(InputType.TYPE_CLASS_TEXT);

        Helper.showKeyboard(getActivity());
        etAddress.getEditText().requestFocus();
        etAddress.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                    if (checkAddress()) {
                        evAmount.focus();
                    } // otherwise ignore
                    return true;
                }
                return false;
            }
        });

        etAddress.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                etAddress.setError(null);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        etPaymentId.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    if (checkPaymentId()) {
                        etDummy.requestFocus();
                        Helper.hideKeyboard(getActivity());
                    }
                    return true;
                }
                return false;
            }
        });

        etPaymentId.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                etPaymentId.setError(null);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        bPrepareSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkAddress() && checkAmountWithError() && checkPaymentId()) {
                    Helper.hideKeyboard(getActivity());
                    prepareSend();
                }
            }
        });

        bDispose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activityCallback.onDisposeRequest();
                enableEdit();
            }
        });

        bScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activityCallback.onScanAddress();
            }
        });

        bPaymentId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etPaymentId.getEditText().setText((Wallet.generatePaymentId()));
                etPaymentId.getEditText().setSelection(etPaymentId.getEditText().getText().length());
            }
        });

        etNotes.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    Helper.hideKeyboard(getActivity());
                    return true;
                }
                return false;
            }
        });

        bSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bSend.setEnabled(false);
                boolean testnet = WalletManager.getInstance().isTestNet();
                if (testnet) {
                    send();
                } else {
                    etNotes.setEnabled(false);
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            bReallySend.setVisibility(View.VISIBLE);
                            bReallySend.setEnabled(true);
                            scrollview.post(new Runnable() {
                                @Override
                                public void run() {
                                    scrollview.fullScroll(ScrollView.FOCUS_DOWN);
                                }
                            });
                        }
                    }, 1000);
                }
            }
        });

        bReallySend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bReallySend.setEnabled(false);
                send();
            }
        });

        sMixin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parentView, View selectedItemView, int position, long id) {
                parentView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isAdded())
                            ((TextView) parentView.getChildAt(0)).setTextColor(getResources().getColor(R.color.moneroGray));
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // nothing (yet?)
            }
        });
        sPriority.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parentView, View selectedItemView, int position, long id) {
                parentView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isAdded())
                            ((TextView) parentView.getChildAt(0)).setTextColor(getResources().getColor(R.color.moneroGray));
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // nothing (yet?)
            }
        });


        etDummy.requestFocus();
        Helper.hideKeyboard(getActivity());

        return view;
    }

    private boolean checkAddressNoError() {
        String address = etAddress.getEditText().getText().toString();
        return Wallet.isAddressValid(address, WalletManager.getInstance().isTestNet());
    }

    private boolean checkAddress() {
        boolean ok = checkAddressNoError();
        if (!ok) {
            etAddress.setError(getString(R.string.send_qr_address_invalid));
        } else {
            etAddress.setError(null);
        }
        return ok;
    }

    private boolean checkAmount() {
        String xmr = evAmount.getAmount();
        return (xmr != null) && (Wallet.getAmountFromString(xmr) > 0);
    }

    private boolean checkAmountWithError() {
        boolean ok = checkAmount();
        if (!ok) {
            evAmount.setError(getString(R.string.receive_amount_empty));
        } else {
            evAmount.setError(null);
        }
        return ok;
    }

    private boolean checkPaymentId() {
        String paymentId = etPaymentId.getEditText().getText().toString();
        boolean ok = paymentId.isEmpty() || Wallet.isPaymentIdValid(paymentId);
        if (!ok) {
            etPaymentId.setError(getString(R.string.receive_paymentid_invalid));
        } else {
            etPaymentId.setError(null);
        }
        return ok;
    }

    private void prepareSend() {
        etDummy.requestFocus();
        disableEdit();
        String dst_addr = etAddress.getEditText().getText().toString();
        String paymentId = etPaymentId.getEditText().getText().toString();
        long amount = Wallet.getAmountFromString(evAmount.getAmount());
        int mixin = Mixins[sMixin.getSelectedItemPosition()];
        int priorityIndex = sPriority.getSelectedItemPosition();
        PendingTransaction.Priority priority = Priorities[priorityIndex];
        Timber.d("%s/%s/%d/%d/%s", dst_addr, paymentId, amount, mixin, priority.toString());
        TxData txData = new TxData(
                dst_addr,
                paymentId,
                amount,
                mixin,
                priority);
        showProgress();
        activityCallback.onPrepareSend(txData);
    }

    private void disableEdit() {
        sMixin.setEnabled(false);
        sPriority.setEnabled(false);
        etAddress.getEditText().setEnabled(false);
        etPaymentId.getEditText().setEnabled(false);
        evAmount.enable(false);
        bScan.setEnabled(false);
        bPaymentId.setEnabled(false);
        bPrepareSend.setEnabled(false);
        bPrepareSend.setVisibility(View.GONE);
    }

    private void enableEdit() {
        sMixin.setEnabled(true);
        sPriority.setEnabled(true);
        etAddress.getEditText().setEnabled(true);
        etPaymentId.getEditText().setEnabled(true);
        evAmount.enable(true);
        bScan.setEnabled(true);
        bPaymentId.setEnabled(true);
        bPrepareSend.setEnabled(true);
        bPrepareSend.setVisibility(View.VISIBLE);

        llConfirmSend.setVisibility(View.GONE);
        etNotes.setEnabled(true);
        bSend.setEnabled(false);
        bReallySend.setVisibility(View.GONE);
        bReallySend.setEnabled(false);
        etDummy.requestFocus();
    }

    private void send() {
        etNotes.setEnabled(false);
        etDummy.requestFocus();
        String notes = etNotes.getText().toString();
        activityCallback.onSend(notes);
    }

    Listener activityCallback;

    public interface Listener {
        void onPrepareSend(TxData data);

        void onSend(String notes);

        String getWalletAddress();

        String getWalletName();

        void onDisposeRequest();

        void onScanAddress();

        BarcodeData popScannedData();

        void setSubtitle(String subtitle);

        void setToolbarButton(int type);
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume");
        activityCallback.setToolbarButton(Toolbar.BUTTON_BACK);
        activityCallback.setSubtitle(getString(R.string.send_title));
        BarcodeData data = activityCallback.popScannedData();
        if (data != null) {
            Timber.d("GOT DATA");
            String scannedAddress = data.address;
            if (scannedAddress != null) {
                etAddress.getEditText().setText(scannedAddress);
                checkAddress();
            } else {
                etAddress.getEditText().getText().clear();
                etAddress.setError(null);
            }
            String scannedPaymenId = data.paymentId;
            if (scannedPaymenId != null) {
                etPaymentId.getEditText().setText(scannedPaymenId);
                checkPaymentId();
            } else {
                etPaymentId.getEditText().getText().clear();
                etPaymentId.setError(null);
            }
            if (data.amount > 0) {
                String scannedAmount = Helper.getDisplayAmount(data.amount);
                evAmount.setAmount(scannedAmount);
            } else {
                evAmount.setAmount("");
            }
        }
        if ((data != null) && (data.amount <= 0)) {
            evAmount.focus();
        } else {
            etDummy.requestFocus();
            Helper.hideKeyboard(getActivity());
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof SendFragment.Listener) {
            this.activityCallback = (SendFragment.Listener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement Listener");
        }
    }

    public void onCreatedTransaction(PendingTransaction pendingTransaction) {
        hideProgress();
        if (pendingTransaction == null) {
            enableEdit();
            return;
        }
        llConfirmSend.setVisibility(View.VISIBLE);
        bSend.setEnabled(true);
        tvTxAmount.setText(Wallet.getDisplayAmount(pendingTransaction.getAmount()));
        tvTxFee.setText(Wallet.getDisplayAmount(pendingTransaction.getFee()));
        //tvTxDust.setText(Wallet.getDisplayAmount(pendingTransaction.getDust()));
        tvTxTotal.setText(Wallet.getDisplayAmount(
                pendingTransaction.getFee() + pendingTransaction.getAmount()));
        scrollview.post(new Runnable() {
            @Override
            public void run() {
                scrollview.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    public void onCreatedTransactionFailed(String errorText) {
        hideProgress();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.send_error_title));
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                enableEdit();
            }
        });
        builder.setMessage(errorText);
        builder.setCancelable(false);
        builder.create().show();
    }

    public void showProgress() {
        pbProgress.setIndeterminate(true);
        pbProgress.setVisibility(View.VISIBLE);
    }

    public void hideProgress() {
        pbProgress.setVisibility(View.GONE);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.send_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
}
