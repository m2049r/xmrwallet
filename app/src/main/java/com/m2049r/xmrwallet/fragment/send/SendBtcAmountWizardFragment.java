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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.data.BarcodeData;
import com.m2049r.xmrwallet.data.TxDataBtc;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.OkHttpHelper;
import com.m2049r.xmrwallet.widget.ExchangeBtcTextView;
import com.m2049r.xmrwallet.widget.NumberPadView;
import com.m2049r.xmrwallet.widget.SendProgressView;
import com.m2049r.xmrwallet.xmrto.XmrToError;
import com.m2049r.xmrwallet.xmrto.XmrToException;
import com.m2049r.xmrwallet.xmrto.api.QueryOrderParameters;
import com.m2049r.xmrwallet.xmrto.api.XmrToApi;
import com.m2049r.xmrwallet.xmrto.api.XmrToCallback;
import com.m2049r.xmrwallet.xmrto.network.XmrToApiImpl;

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
    private ExchangeBtcTextView evAmount;
    private NumberPadView numberPad;

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

        evAmount = view.findViewById(R.id.evAmount);
        numberPad = view.findViewById(R.id.numberPad);
        numberPad.setListener(evAmount);

        Helper.hideKeyboard(getActivity());

        return view;
    }


    @Override
    public boolean onValidateFields() {
        if (!evAmount.validate(maxBtc, minBtc)) {
            return false;
        }
        if (sendListener != null) {
            TxDataBtc txDataBtc = (TxDataBtc) sendListener.getTxData();
            String btcString = evAmount.getAmount();
            if (btcString != null) {
                try {
                    double btc = Double.parseDouble(btcString);
                    Timber.d("setAmount %f", btc);
                    txDataBtc.setBtcAmount(btc);
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

    private void setBip70Mode() {
        TxDataBtc txDataBtc = (TxDataBtc) sendListener.getTxData();
        if (txDataBtc.getBip70() != null) {
            numberPad.setVisibility(View.INVISIBLE);
        } else {
            numberPad.setVisibility(View.VISIBLE);
        }
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
        Helper.hideKeyboard(getActivity());
        final long funds = getTotalFunds();
        if (!sendListener.getActivityCallback().isStreetMode()) {
            tvFunds.setText(getString(R.string.send_available,
                    Wallet.getDisplayAmount(funds)));
        } else {
            tvFunds.setText(getString(R.string.send_available,
                    getString(R.string.unknown_amount)));
        }
        final BarcodeData data = sendListener.popBarcodeData();
        if (data != null) {
            if (data.amount != null) {
                evAmount.setAmount(data.amount);
            }
        }
        setBip70Mode();
        callXmrTo();
    }

    long getTotalFunds() {
        return sendListener.getActivityCallback().getTotalFunds();
    }

    private QueryOrderParameters orderParameters = null;

    private void processOrderParms(final QueryOrderParameters orderParameters) {
        this.orderParameters = orderParameters;
        getView().post(new Runnable() {
            @Override
            public void run() {
                evAmount.setRate(1.0d / orderParameters.getPrice());
                NumberFormat df = NumberFormat.getInstance(Locale.US);
                df.setMaximumFractionDigits(6);
                String min = df.format(orderParameters.getLowerLimit());
                String max = df.format(orderParameters.getUpperLimit());
                String rate = df.format(orderParameters.getPrice());
                Spanned xmrParmText = Html.fromHtml(getString(R.string.info_send_xmrto_parms, min, max, rate));
                if (orderParameters.isZeroConfEnabled()) {
                    String zeroConf = df.format(orderParameters.getZeroConfMaxAmount());
                    Spanned zeroConfText = Html.fromHtml(getString(R.string.info_send_xmrto_zeroconf, zeroConf));
                    xmrParmText = (Spanned) TextUtils.concat(xmrParmText, " ", zeroConfText);
                }
                tvXmrToParms.setText(xmrParmText);
                maxBtc = orderParameters.getUpperLimit();
                minBtc = orderParameters.getLowerLimit();
                Timber.d("minBtc=%f / maxBtc=%f", minBtc, maxBtc);

                final long funds = getTotalFunds();
                double availableXmr = 1.0 * funds / 1000000000000L;
                maxBtc = Math.min(maxBtc, availableXmr * orderParameters.getPrice());

                String availBtcString;
                String availXmrString;
                if (!sendListener.getActivityCallback().isStreetMode()) {
                    availBtcString = df.format(availableXmr * orderParameters.getPrice());
                    availXmrString = df.format(availableXmr);
                } else {
                    availBtcString = getString(R.string.unknown_amount);
                    availXmrString = availBtcString;
                }
                tvFunds.setText(getString(R.string.send_available_btc,
                        availXmrString,
                        availBtcString));
                llXmrToParms.setVisibility(View.VISIBLE);
                evParams.hideProgress();
            }
        });
    }

    private void processOrderParmsError(final Exception ex) {
        evAmount.setRate(0);
        orderParameters = null;
        maxBtc = 0;
        minBtc = 0;
        Timber.e(ex);
        getView().post(new Runnable() {
            @Override
            public void run() {
                if (ex instanceof XmrToException) {
                    XmrToException xmrEx = (XmrToException) ex;
                    XmrToError xmrErr = xmrEx.getError();
                    if (xmrErr != null) {
                        if (xmrErr.isRetryable()) {
                            evParams.showMessage(xmrErr.getErrorId().toString(), xmrErr.getErrorMsg(),
                                    getString(R.string.text_retry));
                            evParams.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    evParams.setOnClickListener(null);
                                    callXmrTo();
                                }
                            });
                        } else {
                            evParams.showMessage(xmrErr.getErrorId().toString(), xmrErr.getErrorMsg(),
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
            }
        });
    }

    private void callXmrTo() {
        evParams.showProgress(getString(R.string.label_send_progress_queryparms));
        getXmrToApi().queryOrderParameters(new XmrToCallback<QueryOrderParameters>() {
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