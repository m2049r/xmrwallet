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
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.m2049r.xmrwallet.model.PendingTransaction;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.TxData;

public class SendFragment extends Fragment {
    static final String TAG = "GenerateFragment";

    EditText etAddress;
    EditText etPaymentId;
    EditText etAmount;
    Button bSweep;
    Spinner sMixin;
    Spinner sPriority;
    Button bPrepareSend;
    LinearLayout llConfirmSend;
    TextView tvTxAmount;
    TextView tvTxFee;
    TextView tvTxDust;
    Button bSend;

    final static int Mixins[] = {4, 6, 8, 10, 13}; // must match the layout XML
    final static PendingTransaction.Priority Priorities[] =
            {PendingTransaction.Priority.Priority_Low,
                    PendingTransaction.Priority.Priority_Medium,
                    PendingTransaction.Priority.Priority_High}; // must match the layout XML

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.send_fragment, container, false);

        sMixin = (Spinner) view.findViewById(R.id.sMixin);
        sPriority = (Spinner) view.findViewById(R.id.sPriority);
        etAddress = (EditText) view.findViewById(R.id.etAddress);
        etPaymentId = (EditText) view.findViewById(R.id.etPaymentId);
        etAmount = (EditText) view.findViewById(R.id.etAmount);
        bSweep = (Button) view.findViewById(R.id.bSweep);
        bPrepareSend = (Button) view.findViewById(R.id.bPrepareSend);

        llConfirmSend = (LinearLayout) view.findViewById(R.id.llConfirmSend);
        tvTxAmount = (TextView) view.findViewById(R.id.tvTxAmount);
        tvTxFee = (TextView) view.findViewById(R.id.tvTxFee);
        tvTxDust = (TextView) view.findViewById(R.id.tvTxDust);
        bSend = (Button) view.findViewById(R.id.bSend);

        etAddress.setRawInputType(InputType.TYPE_CLASS_TEXT);
        etPaymentId.setRawInputType(InputType.TYPE_CLASS_TEXT);

        etAddress.setText("9tDC52GsMjTNt4dpnRCwAF7ekVBkbkgkXGaMKTcSTpBhGpqkPX56jCNRydLq9oGjbbAQBsZhLfgmTKsntmxRd3TaJFYM2f8");
        boolean testnet = WalletManager.getInstance().isTestNet();
        if (!testnet) throw new IllegalStateException("Sending TX only on testnet. sorry.");

        Helper.showKeyboard(getActivity());
        etAddress.requestFocus();
        etAddress.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                    if (addressOk()) {
                        etPaymentId.requestFocus();
                    } // otherwise ignore
                    return true;
                }
                return false;
            }
        });

        etPaymentId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.showKeyboard(getActivity());
            }
        });
        etPaymentId.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                    if (paymentIdOk()) {
                        etAmount.requestFocus();
                    } // otherwise ignore
                    return true;
                }
                return false;
            }
        });

        etAmount.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    if (amountOk()) {
                        Helper.hideKeyboard(getActivity());
                    }
                    return true;
                }
                return false;
            }
        });

        bPrepareSend.setEnabled(true); // TODO need clever logic here
        bPrepareSend.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                Helper.hideKeyboard(getActivity());
                prepareSend();
            }
        });


        bSend.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                send();
            }
        });
        return view;
    }

    private boolean addressOk() {
        String address = etAddress.getText().toString();
        // TODO only accept address from the correct net
        return ((address.length() == 95) && ("49A".indexOf(address.charAt(0)) >= 0));
    }

    private boolean amountOk() {
        String amount = etAmount.getText().toString();
        // TODO decimal separator
        return ((amount.length() > 0) && (amount.matches("^[0-9]+([,.][0-9]+)?$")));
    }

    private boolean paymentIdOk() {
        String spendKey = etPaymentId.getText().toString();
        return ((spendKey.length() == 0) || ((spendKey.length() == 64) && (spendKey.matches("^[0-9a-fA-F]+$"))));
    }

    private void prepareSend() {
        String dst_addr = etAddress.getText().toString();
        String paymentId = etPaymentId.getText().toString();
        long amount = Wallet.getAmountFromString(etAmount.getText().toString());
        int mixin = Mixins[sMixin.getSelectedItemPosition()];
        int priorityIndex = sPriority.getSelectedItemPosition();
        PendingTransaction.Priority priority = Priorities[priorityIndex];
        //Log.d(TAG, dst_addr + "/" + paymentId + "/" + amount + "/" + mixin + "/" + priority.toString());
        TxData txData = new TxData(
                dst_addr,
                paymentId,
                amount,
                mixin,
                priority);

        sMixin.setEnabled(false);
        sPriority.setEnabled(false);
        etAddress.setEnabled(false);
        etPaymentId.setEnabled(false);
        etAmount.setEnabled(false);
        bSweep.setEnabled(false);
        bPrepareSend.setEnabled(false);

        activityCallback.onPrepareSend(txData);
    }

    private void send() {
        activityCallback.onSend();
    }

    SendFragment.Listener activityCallback;

    public interface Listener {
        void onPrepareSend(TxData data);

        void onSend();

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
        PendingTransaction.Status status = pendingTransaction.getStatus();
        if (status != PendingTransaction.Status.Status_Ok) {
            Log.d(TAG, "Wallet store failed: " + pendingTransaction.getErrorString());
        }
        /*
        Log.d(TAG, "transaction amount " + pendingTransaction.getAmount());
        Log.d(TAG, "transaction fee    " + pendingTransaction.getFee());
        Log.d(TAG, "transaction dust   " + pendingTransaction.getDust());
        Log.d(TAG, "transactions       " + pendingTransaction.getTxCount());
        */
        llConfirmSend.setVisibility(View.VISIBLE);
        tvTxAmount.setText(Wallet.getDisplayAmount(pendingTransaction.getAmount()));
        tvTxFee.setText(Wallet.getDisplayAmount(pendingTransaction.getFee()));
        tvTxDust.setText(Wallet.getDisplayAmount(pendingTransaction.getDust()));
        bSend.setEnabled(true);
    }
}
