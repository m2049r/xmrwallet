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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.data.PendingTx;
import com.m2049r.xmrwallet.data.TxDataBtc;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.OkHttpHelper;
import com.m2049r.xmrwallet.xmrto.XmrToException;
import com.m2049r.xmrwallet.xmrto.api.QueryOrderStatus;
import com.m2049r.xmrwallet.xmrto.api.XmrToApi;
import com.m2049r.xmrwallet.xmrto.api.XmrToCallback;
import com.m2049r.xmrwallet.xmrto.network.XmrToApiImpl;

import java.text.NumberFormat;
import java.util.Locale;

import timber.log.Timber;

public class SendBtcSuccessWizardFragment extends SendWizardFragment {

    public static SendBtcSuccessWizardFragment newInstance(SendSuccessWizardFragment.Listener listener) {
        SendBtcSuccessWizardFragment instance = new SendBtcSuccessWizardFragment();
        instance.setSendListener(listener);
        return instance;
    }

    SendSuccessWizardFragment.Listener sendListener;

    public SendBtcSuccessWizardFragment setSendListener(SendSuccessWizardFragment.Listener listener) {
        this.sendListener = listener;
        return this;
    }

    ImageButton bCopyTxId;
    private TextView tvTxId;
    private TextView tvTxAddress;
    private TextView tvTxPaymentId;
    private TextView tvTxAmount;
    private TextView tvTxFee;
    private TextView tvXmrToAmount;
    private TextView tvXmrToStatus;
    private ImageView ivXmrToStatus;
    private ImageView ivXmrToStatusBig;
    private ProgressBar pbXmrto;
    private TextView tvTxXmrToKey;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Timber.d("onCreateView() %s", (String.valueOf(savedInstanceState)));

        View view = inflater.inflate(
                R.layout.fragment_send_btc_success, container, false);

        bCopyTxId = view.findViewById(R.id.bCopyTxId);
        bCopyTxId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.clipBoardCopy(getActivity(), getString(R.string.label_send_txid), tvTxId.getText().toString());
                Toast.makeText(getActivity(), getString(R.string.message_copy_txid), Toast.LENGTH_SHORT).show();
            }
        });

        tvXmrToAmount = view.findViewById(R.id.tvXmrToAmount);
        tvXmrToStatus = view.findViewById(R.id.tvXmrToStatus);
        ivXmrToStatus = view.findViewById(R.id.ivXmrToStatus);
        ivXmrToStatusBig = view.findViewById(R.id.ivXmrToStatusBig);

        tvTxId = view.findViewById(R.id.tvTxId);
        tvTxAddress = view.findViewById(R.id.tvTxAddress);
        tvTxPaymentId = view.findViewById(R.id.tvTxPaymentId);
        tvTxAmount = view.findViewById(R.id.tvTxAmount);
        tvTxFee = view.findViewById(R.id.tvTxFee);

        pbXmrto = view.findViewById(R.id.pbXmrto);
        pbXmrto.getIndeterminateDrawable().setColorFilter(0x61000000, android.graphics.PorterDuff.Mode.MULTIPLY);

        tvTxXmrToKey = view.findViewById(R.id.tvTxXmrToKey);
        tvTxXmrToKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.clipBoardCopy(getActivity(), getString(R.string.label_copy_xmrtokey), tvTxXmrToKey.getText().toString());
                Toast.makeText(getActivity(), getString(R.string.message_copy_xmrtokey), Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    @Override
    public boolean onValidateFields() {
        return true;
    }

    private boolean isResumed = false;

    @Override
    public void onPauseFragment() {
        isResumed = false;
        super.onPauseFragment();
    }

    TxDataBtc btcData = null;

    @Override
    public void onResumeFragment() {
        super.onResumeFragment();
        Timber.d("onResumeFragment()");
        Helper.hideKeyboard(getActivity());
        isResumed = true;

        btcData = (TxDataBtc) sendListener.getTxData();
        tvTxAddress.setText(btcData.getDestinationAddress());
        String paymentId = btcData.getPaymentId();
        if ((paymentId != null) && (!paymentId.isEmpty())) {
            tvTxPaymentId.setText(btcData.getPaymentId());
        } else {
            tvTxPaymentId.setText("-");
        }

        final PendingTx committedTx = sendListener.getCommittedTx();
        if (committedTx != null) {
            tvTxId.setText(committedTx.txId);
            bCopyTxId.setEnabled(true);
            bCopyTxId.setImageResource(R.drawable.ic_content_copy_black_24dp);
            tvTxAmount.setText(getString(R.string.send_amount, Helper.getDisplayAmount(committedTx.amount)));
            tvTxFee.setText(getString(R.string.send_fee, Helper.getDisplayAmount(committedTx.fee)));
            if (btcData != null) {
                NumberFormat df = NumberFormat.getInstance(Locale.US);
                df.setMaximumFractionDigits(12);
                String btcAmount = df.format(btcData.getBtcAmount());
                tvXmrToAmount.setText(getString(R.string.info_send_xmrto_success_btc, btcAmount));
                //TODO         btcData.getBtcAddress();
                tvTxXmrToKey.setText(btcData.getXmrtoUuid());
                queryOrder();
            } else {
                throw new IllegalStateException("btcData is null");
            }
        }
        sendListener.enableDone();
    }

    private final int QUERY_INTERVAL = 1000; // ms

    private void processQueryOrder(final QueryOrderStatus status) {
        Timber.d("processQueryOrder %s for %s", status.getState().toString(), status.getUuid());
        if (!btcData.getXmrtoUuid().equals(status.getUuid()))
            throw new IllegalStateException("UUIDs do not match!");
        if (isResumed && (getView() != null))
            getView().post(new Runnable() {
                @Override
                public void run() {
                    showXmrToStatus(status);
                    if (!status.isTerminal()) {
                        getView().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                queryOrder();
                            }
                        }, QUERY_INTERVAL);
                    }
                }
            });
    }

    private void queryOrder() {
        Timber.d("queryOrder(%s)", btcData.getXmrtoUuid());
        if (!isResumed) return;
        getXmrToApi().queryOrderStatus(btcData.getXmrtoUuid(), new XmrToCallback<QueryOrderStatus>() {
            @Override
            public void onSuccess(QueryOrderStatus status) {
                if (!isAdded()) return;
                processQueryOrder(status);
            }

            @Override
            public void onError(final Exception ex) {
                if (!isResumed) return;
                Timber.e(ex);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (ex instanceof XmrToException) {
                            Toast.makeText(getActivity(), ((XmrToException) ex).getError().getErrorMsg(), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getActivity(), ex.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    private int statusResource = 0;

    void showXmrToStatus(final QueryOrderStatus status) {
        if (status.isError()) {
            tvXmrToStatus.setText(getString(R.string.info_send_xmrto_error, status.toString()));
            statusResource = R.drawable.ic_error_red_24dp;
            pbXmrto.getIndeterminateDrawable().setColorFilter(0xff8b0000, android.graphics.PorterDuff.Mode.MULTIPLY);
        } else if (status.isSent()) {
            tvXmrToStatus.setText(getString(R.string.info_send_xmrto_sent));
            statusResource = R.drawable.ic_success_green_24dp;
            pbXmrto.getIndeterminateDrawable().setColorFilter(0xFF417505, android.graphics.PorterDuff.Mode.MULTIPLY);
        } else if (status.isPending()) {
            if (status.isPaid()) {
                tvXmrToStatus.setText(getString(R.string.info_send_xmrto_paid));
            } else {
                tvXmrToStatus.setText(getString(R.string.info_send_xmrto_unpaid));
            }
            statusResource = R.drawable.ic_pending_orange_24dp;
            pbXmrto.getIndeterminateDrawable().setColorFilter(0xFFFF6105, android.graphics.PorterDuff.Mode.MULTIPLY);
        } else {
            throw new IllegalStateException("status is broken: " + status.toString());
        }
        ivXmrToStatus.setImageResource(statusResource);
        if (status.isTerminal()) {
            pbXmrto.setVisibility(View.INVISIBLE);
            ivXmrToStatus.setVisibility(View.GONE);
            ivXmrToStatusBig.setImageResource(statusResource);
            ivXmrToStatusBig.setVisibility(View.VISIBLE);
        }
    }

    private XmrToApi xmrToApi = null;

    private final XmrToApi getXmrToApi() {
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
