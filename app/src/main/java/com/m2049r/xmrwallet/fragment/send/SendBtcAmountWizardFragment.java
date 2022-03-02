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
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.data.BarcodeData;
import com.m2049r.xmrwallet.data.TxDataBtc;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.service.shift.ShiftCallback;
import com.m2049r.xmrwallet.service.shift.ShiftError;
import com.m2049r.xmrwallet.service.shift.ShiftException;
import com.m2049r.xmrwallet.service.shift.sideshift.api.QueryOrderParameters;
import com.m2049r.xmrwallet.service.shift.sideshift.api.SideShiftApi;
import com.m2049r.xmrwallet.service.shift.sideshift.network.SideShiftApiImpl;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.ServiceHelper;
import com.m2049r.xmrwallet.widget.ExchangeOtherEditText;
import com.m2049r.xmrwallet.widget.SendProgressView;

import java.text.NumberFormat;
import java.util.Locale;

import timber.log.Timber;

public class SendBtcAmountWizardFragment extends SendWizardFragment {

    public static SendBtcAmountWizardFragment newInstance(SendAmountWizardFragment.Listener listener) {
        SendBtcAmountWizardFragment instance = new SendBtcAmountWizardFragment();
        instance.setSendListener(listener);
        return instance;
    }

    SendAmountWizardFragment.Listener sendListener;

    public SendBtcAmountWizardFragment setSendListener(SendAmountWizardFragment.Listener listener) {
        this.sendListener = listener;
        return this;
    }

    private TextView tvFunds;
    private ExchangeOtherEditText etAmount;

    private TextView tvXmrToParms;
    private SendProgressView evParams;
    private View llXmrToParms;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Timber.d("onCreateView() %s", (String.valueOf(savedInstanceState)));

        sendListener = (SendAmountWizardFragment.Listener) getParentFragment();

        View view = inflater.inflate(R.layout.fragment_send_btc_amount, container, false);

        tvFunds = view.findViewById(R.id.tvFunds);

        evParams = view.findViewById(R.id.evXmrToParms);
        llXmrToParms = view.findViewById(R.id.llXmrToParms);

        tvXmrToParms = view.findViewById(R.id.tvXmrToParms);

        etAmount = view.findViewById(R.id.etAmount);
        etAmount.requestFocus();

        return view;
    }

    @Override
    public boolean onValidateFields() {
        Timber.i(maxBtc + "/" + minBtc);
        if (!etAmount.validate(maxBtc, minBtc)) {
            return false;
        }
        if (orderParameters == null) {
            return false; // this should never happen
        }
        if (sendListener != null) {
            TxDataBtc txDataBtc = (TxDataBtc) sendListener.getTxData();
            String btcString = etAmount.getNativeAmount();
            if (btcString != null) {
                try {
                    double btc = Double.parseDouble(btcString);
                    Timber.d("setBtcAmount %f", btc);
                    txDataBtc.setBtcAmount(btc);
                    txDataBtc.setAmount(btc / orderParameters.getPrice());
                } catch (NumberFormatException ex) {
                    Timber.d(ex.getLocalizedMessage());
                    txDataBtc.setBtcAmount(0);
                }
            } else {
                txDataBtc.setBtcAmount(0);
            }
        }
        return true;
    }

    double maxBtc = 0;
    double minBtc = 0;

    @Override
    public void onPauseFragment() {
        llXmrToParms.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onResumeFragment() {
        super.onResumeFragment();
        Timber.d("onResumeFragment()");
        final String btcSymbol = ((TxDataBtc) sendListener.getTxData()).getBtcSymbol();
        if (!btcSymbol.toLowerCase().equals(ServiceHelper.ASSET))
            throw new IllegalStateException("Asset Symbol is wrong!");
        final long funds = getTotalFunds();
        if (!sendListener.getActivityCallback().isStreetMode()) {
            tvFunds.setText(getString(R.string.send_available,
                    Wallet.getDisplayAmount(funds)));
            //TODO
        } else {
            tvFunds.setText(getString(R.string.send_available,
                    getString(R.string.unknown_amount)));
        }
        etAmount.setAmount("");
        final BarcodeData data = sendListener.popBarcodeData();
        if (data != null) {
            if (data.amount != null) {
                etAmount.setAmount(data.amount);
            }
        }
        etAmount.setBaseCurrency(btcSymbol);
        callXmrTo();
    }

    long getTotalFunds() {
        return sendListener.getActivityCallback().getTotalFunds();
    }

    private QueryOrderParameters orderParameters = null;

    private void processOrderParms(final QueryOrderParameters orderParameters) {
        this.orderParameters = orderParameters;
        getView().post(() -> {
            final double price = orderParameters.getPrice();
            etAmount.setExchangeRate(1 / price);
            maxBtc = price * orderParameters.getUpperLimit();
            minBtc = price * orderParameters.getLowerLimit();
            Timber.d("minBtc=%f / maxBtc=%f", minBtc, maxBtc);
            NumberFormat df = NumberFormat.getInstance(Locale.US);
            df.setMaximumFractionDigits(6);
            String min = df.format(minBtc);
            String max = df.format(maxBtc);
            String rate = df.format(price);
            final TxDataBtc txDataBtc = (TxDataBtc) sendListener.getTxData();
            Spanned xmrParmText = Html.fromHtml(getString(R.string.info_send_xmrto_parms,
                    min, max, rate, txDataBtc.getBtcSymbol()));
            tvXmrToParms.setText(xmrParmText);

            final long funds = getTotalFunds();
            double availableXmr = 1.0 * funds / Helper.ONE_XMR;

            String availBtcString;
            String availXmrString;
            if (!sendListener.getActivityCallback().isStreetMode()) {
                availBtcString = df.format(availableXmr * price);
                availXmrString = df.format(availableXmr);
            } else {
                availBtcString = getString(R.string.unknown_amount);
                availXmrString = availBtcString;
            }
            tvFunds.setText(getString(R.string.send_available_btc,
                    availXmrString,
                    availBtcString,
                    ((TxDataBtc) sendListener.getTxData()).getBtcSymbol()));
            llXmrToParms.setVisibility(View.VISIBLE);
            evParams.hideProgress();
        });
    }

    private void processOrderParmsError(final Exception ex) {
        etAmount.setExchangeRate(0);
        orderParameters = null;
        maxBtc = 0;
        minBtc = 0;
        Timber.e(ex);
        getView().post(() -> {
            if (ex instanceof ShiftException) {
                ShiftException xmrEx = (ShiftException) ex;
                ShiftError xmrErr = xmrEx.getError();
                if (xmrErr != null) {
                    if (xmrErr.isRetryable()) {
                        evParams.showMessage(xmrErr.getErrorType().toString(), xmrErr.getErrorMsg(),
                                getString(R.string.text_retry));
                        evParams.setOnClickListener(v -> {
                            evParams.setOnClickListener(null);
                            callXmrTo();
                        });
                    } else {
                        evParams.showMessage(xmrErr.getErrorType().toString(), xmrErr.getErrorMsg(),
                                getString(R.string.text_noretry));
                    }
                } else {
                    evParams.showMessage(getString(R.string.label_generic_xmrto_error),
                            getString(R.string.text_generic_xmrto_error, xmrEx.getCode()),
                            getString(R.string.text_noretry));
                }
            } else {
                evParams.showMessage(getString(R.string.label_generic_xmrto_error),
                        ex.getLocalizedMessage(),
                        getString(R.string.text_noretry));
            }
        });
    }

    private void callXmrTo() {
        evParams.showProgress(getString(R.string.label_send_progress_queryparms));
        getXmrToApi().queryOrderParameters(new ShiftCallback<QueryOrderParameters>() {
            @Override
            public void onSuccess(final QueryOrderParameters orderParameters) {
                processOrderParms(orderParameters);
            }

            @Override
            public void onError(final Exception e) {
                processOrderParmsError(e);
            }
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