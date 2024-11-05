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

import android.content.Intent;
import android.graphics.Paint;
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
import com.m2049r.xmrwallet.data.Crypto;
import com.m2049r.xmrwallet.data.PendingTx;
import com.m2049r.xmrwallet.data.TxData;
import com.m2049r.xmrwallet.data.TxDataBtc;
import com.m2049r.xmrwallet.service.shift.ShiftCallback;
import com.m2049r.xmrwallet.service.shift.ShiftException;
import com.m2049r.xmrwallet.service.shift.ShiftService;
import com.m2049r.xmrwallet.service.shift.api.QueryOrderStatus;
import com.m2049r.xmrwallet.service.shift.api.ShiftApi;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.ThemeHelper;

import java.net.SocketTimeoutException;
import java.text.NumberFormat;
import java.util.Locale;

import timber.log.Timber;

public class SendBtcSuccessWizardFragment extends SendWizardFragment {

    public static SendBtcSuccessWizardFragment newInstance(SendSuccessWizardFragment.Listener listener) {
        SendBtcSuccessWizardFragment instance = new SendBtcSuccessWizardFragment();
        instance.sendListener = listener;
        return instance;
    }

    private SendSuccessWizardFragment.Listener sendListener;

    ImageButton bCopyTxId;
    private TextView tvTxId;
    private TextView tvTxAddress;
    private TextView tvTxAmount;
    private TextView tvTxFee;
    private TextView tvXmrToAmount;
    private ImageView ivXmrToIcon;
    private TextView tvXmrToStatus;
    private ImageView ivXmrToStatus;
    private ImageView ivXmrToStatusBig;
    private ProgressBar pbXmrto;
    private TextView tvTxXmrToKey;
    private TextView tvXmrToSupport;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Timber.d("onCreateView() %s", (String.valueOf(savedInstanceState)));
        final ShiftService shiftService = getTxData().getShiftService();

        View view = inflater.inflate(
                R.layout.fragment_send_btc_success, container, false);

        bCopyTxId = view.findViewById(R.id.bCopyTxId);
        bCopyTxId.setEnabled(false);
        bCopyTxId.setOnClickListener(v -> {
            Helper.clipBoardCopy(requireActivity(), getString(R.string.label_send_txid), tvTxId.getText().toString());
            Toast.makeText(getActivity(), getString(R.string.message_copy_txid), Toast.LENGTH_SHORT).show();
        });

        tvXmrToAmount = view.findViewById(R.id.tvXmrToAmount);
        ivXmrToIcon = view.findViewById(R.id.ivXmrToIcon);
        tvXmrToStatus = view.findViewById(R.id.tvXmrToStatus);
        ivXmrToStatus = view.findViewById(R.id.ivXmrToStatus);
        ivXmrToStatusBig = view.findViewById(R.id.ivXmrToStatusBig);

        tvTxId = view.findViewById(R.id.tvTxId);
        tvTxAddress = view.findViewById(R.id.tvTxAddress);
        tvTxAmount = view.findViewById(R.id.tvTxAmount);
        tvTxFee = view.findViewById(R.id.tvTxFee);

        pbXmrto = view.findViewById(R.id.pbXmrto);
        pbXmrto.getIndeterminateDrawable().setColorFilter(0x61000000, android.graphics.PorterDuff.Mode.MULTIPLY);

        final TextView tvXmrToLabel = view.findViewById(R.id.tvXmrToLabel);
        tvXmrToLabel.setText(getString(R.string.info_send_xmrto_success_order_label, shiftService.getLabel()));

        tvTxXmrToKey = view.findViewById(R.id.tvTxXmrToKey);
        tvTxXmrToKey.setOnClickListener(v -> {
            Helper.clipBoardCopy(requireActivity(), getString(R.string.label_copy_xmrtokey), tvTxXmrToKey.getText().toString());
            Toast.makeText(getActivity(), getString(R.string.message_copy_xmrtokey), Toast.LENGTH_SHORT).show();
        });

        tvXmrToSupport = view.findViewById(R.id.tvXmrToSupport);
        tvXmrToSupport.setPaintFlags(tvXmrToSupport.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvXmrToSupport.setText(getString(R.string.label_send_btc_xmrto_info, shiftService.getLabel()));

        return view;
    }

    private boolean isResumed = false;

    @Override
    public void onPauseFragment() {
        isResumed = false;
        super.onPauseFragment();
    }

    private TxDataBtc getTxData() {
        final TxData txData = sendListener.getTxData();
        if (txData == null) throw new IllegalStateException("TxDataBtc is null");
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
        Helper.hideKeyboard(getActivity());
        isResumed = true;

        final TxDataBtc txData = getTxData();
        tvTxAddress.setText(txData.getDestination());

        final PendingTx committedTx = sendListener.getCommittedTx();
        if (committedTx != null) {
            tvTxId.setText(committedTx.txId);
            bCopyTxId.setEnabled(true);
            tvTxAmount.setText(getString(R.string.send_amount, Helper.getDisplayAmount(committedTx.amount)));
            tvTxFee.setText(getString(R.string.send_fee, Helper.getDisplayAmount(committedTx.fee)));
            NumberFormat df = NumberFormat.getInstance(Locale.US);
            df.setMaximumFractionDigits(12);
            String btcAmount = df.format(txData.getBtcAmount());
            tvXmrToAmount.setText(getString(R.string.info_send_xmrto_success_btc, btcAmount, txData.getBtcSymbol()));
            //TODO         btcData.getBtcAddress();
            tvTxXmrToKey.setText(txData.getXmrtoOrderId());
            final Crypto crypto = Crypto.withSymbol(txData.getBtcSymbol());
            assert crypto != null;
            ivXmrToIcon.setImageResource(crypto.getIconEnabledId());
            tvXmrToSupport.setOnClickListener(v -> {
                startActivity(new Intent(Intent.ACTION_VIEW, txData.getShiftService().getShiftApi().getQueryOrderUri(txData.getXmrtoOrderId())));
            });
            queryOrder();
        }
        sendListener.enableDone();
    }

    private void processQueryOrder(final QueryOrderStatus status) {
        Timber.d("processQueryOrder %s for %s", status.getStatus().toString(), status.getOrderId());
        if (!getTxData().getXmrtoOrderId().equals(status.getOrderId()))
            throw new IllegalStateException("UUIDs do not match!");
        if (isResumed && (getView() != null))
            getView().post(() -> {
                showXmrToStatus(status);
                if (!status.isTerminal()) {
                    getView().postDelayed(this::queryOrder, ShiftApi.QUERY_INTERVAL);
                }
            });
    }

    private void queryOrder() {
        final TxDataBtc btcData = getTxData();
        Timber.d("queryOrder(%s)", btcData.getXmrtoOrderId());
        if (!isResumed) return;
        btcData.getShiftService().getShiftApi().queryOrderStatus(btcData.getXmrtoQueryOrderToken(), new ShiftCallback<QueryOrderStatus>() {
            @Override
            public void onSuccess(QueryOrderStatus status) {
                if (!isAdded()) return;
                processQueryOrder(status);
            }

            @Override
            public void onError(final Exception ex) {
                if (!isResumed) return;
                Timber.w(ex);
                if (ex instanceof SocketTimeoutException) {
                    // try again
                    if (isResumed && (getView() != null))
                        getView().post(() -> {
                            getView().postDelayed(SendBtcSuccessWizardFragment.this::queryOrder, ShiftApi.QUERY_INTERVAL);
                        });
                } else {
                    requireActivity().runOnUiThread(() -> {
                        if (ex instanceof ShiftException) {
                            Toast.makeText(getActivity(), ((ShiftException) ex).getErrorMessage(), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getActivity(), ex.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    void showXmrToStatus(final QueryOrderStatus status) {
        int statusResource = 0;
        final TxDataBtc txData = getTxData();
        if (status.isError()) {
            tvXmrToStatus.setText(getString(R.string.info_send_xmrto_error, txData.getShiftService().getLabel(), status.toString()));
            statusResource = R.drawable.ic_error_red_24dp;
            pbXmrto.getIndeterminateDrawable().setColorFilter(
                    ThemeHelper.getThemedColor(requireContext(), R.attr.negativeColor),
                    android.graphics.PorterDuff.Mode.MULTIPLY);
        } else if (status.isSent() || status.isPaid()) {
            tvXmrToStatus.setText(getString(R.string.info_send_xmrto_sent, txData.getBtcSymbol()));
            statusResource = R.drawable.ic_success;
            pbXmrto.getIndeterminateDrawable().setColorFilter(
                    ThemeHelper.getThemedColor(requireContext(), R.attr.positiveColor),
                    android.graphics.PorterDuff.Mode.MULTIPLY);
        } else if (status.isWaiting()) {
            tvXmrToStatus.setText(getString(R.string.info_send_xmrto_unpaid));
            statusResource = R.drawable.ic_pending;
            pbXmrto.getIndeterminateDrawable().setColorFilter(
                    ThemeHelper.getThemedColor(requireContext(), R.attr.neutralColor),
                    android.graphics.PorterDuff.Mode.MULTIPLY);
        } else if (status.isPending()) {
            tvXmrToStatus.setText(getString(R.string.info_send_xmrto_paid));
            statusResource = R.drawable.ic_pending;
            pbXmrto.getIndeterminateDrawable().setColorFilter(
                    ThemeHelper.getThemedColor(requireContext(), R.attr.neutralColor),
                    android.graphics.PorterDuff.Mode.MULTIPLY);
        } else {
            throw new IllegalStateException("status is broken: " + status);
        }
        ivXmrToStatus.setImageResource(statusResource);
        if (status.isTerminal()) {
            pbXmrto.setVisibility(View.INVISIBLE);
            ivXmrToIcon.setVisibility(View.GONE);
            ivXmrToStatus.setVisibility(View.GONE);
            ivXmrToStatusBig.setImageResource(statusResource);
            ivXmrToStatusBig.setVisibility(View.VISIBLE);
        }
    }
}
