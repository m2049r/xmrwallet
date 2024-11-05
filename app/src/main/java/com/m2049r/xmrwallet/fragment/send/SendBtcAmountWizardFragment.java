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
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.data.BarcodeData;
import com.m2049r.xmrwallet.data.CryptoAmount;
import com.m2049r.xmrwallet.data.TxData;
import com.m2049r.xmrwallet.data.TxDataBtc;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.service.shift.ShiftError;
import com.m2049r.xmrwallet.service.shift.ShiftException;
import com.m2049r.xmrwallet.service.shift.ShiftService;
import com.m2049r.xmrwallet.service.shift.api.QueryOrderParameters;
import com.m2049r.xmrwallet.service.shift.process.PreShiftProcess;
import com.m2049r.xmrwallet.util.AmountHelper;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.widget.ExchangeOtherEditText;
import com.m2049r.xmrwallet.widget.SendProgressView;

import timber.log.Timber;

public class SendBtcAmountWizardFragment extends SendWizardFragment implements PreShifter, ExchangeOtherEditText.Listener {

    public static SendBtcAmountWizardFragment newInstance(SendAmountWizardFragment.Listener listener) {
        return new SendBtcAmountWizardFragment(listener);
    }

    private SendBtcAmountWizardFragment(@NonNull SendAmountWizardFragment.Listener listener) {
        super();
        sendListener = listener;
    }

    private final SendAmountWizardFragment.Listener sendListener;

    private TextView tvFunds;
    private ExchangeOtherEditText etAmount;

    private TextView tvXmrToParms;
    private SendProgressView evParams;
    private View llXmrToParms;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Timber.d("onCreateView() %s", (String.valueOf(savedInstanceState)));

        View view = inflater.inflate(R.layout.fragment_send_btc_amount, container, false);

        tvFunds = view.findViewById(R.id.tvFunds);

        evParams = view.findViewById(R.id.evXmrToParms);
        llXmrToParms = view.findViewById(R.id.llXmrToParms);

        tvXmrToParms = view.findViewById(R.id.tvXmrToParms);

        ((ImageView) view.findViewById(R.id.shiftIcon)).setImageResource(service.getIconId());

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
        final TxDataBtc txDataBtc = (TxDataBtc) sendListener.getTxData();
        txDataBtc.setShiftAmount(etAmount.getPrimaryAmount());
        return true;
    }

    private double maxBtc = 0;
    private double minBtc = 0;

    @Override
    public void onPauseFragment() {
        super.onPauseFragment();
        llXmrToParms.setVisibility(View.INVISIBLE);
        etAmount.setListener(null);
    }

    @Override
    public void onResumeFragment() {
        super.onResumeFragment();
        Timber.d("onResumeFragment()");
        final String btcSymbol = ((TxDataBtc) sendListener.getTxData()).getBtcSymbol();
        if (!btcSymbol.equalsIgnoreCase(ShiftService.ASSET.getSymbol()))
            throw new IllegalStateException("Asset Symbol is wrong (" + btcSymbol + "!=" + ShiftService.ASSET.getSymbol() + ")");
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
        etAmount.setListener(this);
        final BarcodeData data = sendListener.popBarcodeData();
        if (data != null) {
            if (data.getAmount() != null) {
                etAmount.setAmount(data.getAmount());
            }
        }
        etAmount.setBaseCurrency(btcSymbol);
        updateShift();
    }

    long getTotalFunds() {
        return sendListener.getActivityCallback().getTotalFunds();
    }

    private final ShiftService service = ShiftService.DEFAULT;
    private final PreShiftProcess preShiftProcess = service.createPreProcess(this);

    private QueryOrderParameters orderParameters = null;

    private void reset() {
        orderParameters = null;
        maxBtc = 0;
        minBtc = 0;
        etAmount.setExchangeRate(0);
    }

    private void updateShift() {
        reset();
        getTxData().setShiftService(service);
        llXmrToParms.setVisibility(View.INVISIBLE);
        preShiftProcess.run();
    }

    private TxDataBtc getTxData() {
        final TxData txData = sendListener.getTxData();
        if (txData instanceof TxDataBtc) {
            return (TxDataBtc) txData;
        } else {
            throw new IllegalStateException("TxData not BTC");
        }
    }

    private boolean isValid() {
        return orderParameters != null;
    }

    @Override
    public CryptoAmount getAmount() { // of BTC
        return etAmount.getPrimaryAmount();
    }

    public double getLowerLimit() {
        if (!isValid()) throw new IllegalStateException();
        return orderParameters.getLowerLimit();
    }

    public double getPrice() {
        if (!isValid()) throw new IllegalStateException();
        return orderParameters.getPrice();
    }

    public double getUpperLimit() {
        if (!isValid()) throw new IllegalStateException();
        return orderParameters.getUpperLimit();
    }

    @Override
    public void onOrderParametersError(final Exception ex) {
        reset();
        Timber.e(ex);
        requireView().post(() -> {
            if (ex instanceof ShiftException) {
                ShiftException xmrEx = (ShiftException) ex;
                ShiftError xmrErr = xmrEx.getError();
                if (xmrErr != null) {
                    if (xmrErr.isRetryable()) {
                        evParams.showMessage(xmrErr.getType().toString(), xmrErr.getErrorMsg(),
                                getString(R.string.text_retry));
                        evParams.setOnClickListener(v -> {
                            evParams.setOnClickListener(null);
                            updateShift();
                        });
                    } else {
                        evParams.showMessage(xmrErr.getType().toString(), xmrErr.getErrorMsg(),
                                getString(R.string.text_noretry, getTxData().getShiftService().getLabel()));
                    }
                } else {
                    evParams.showMessage(getString(R.string.label_generic_xmrto_error),
                            getString(R.string.text_generic_xmrto_error, xmrEx.getCode()),
                            getString(R.string.text_noretry, getTxData().getShiftService().getLabel()));
                }
            } else {
                evParams.showMessage(getString(R.string.label_generic_xmrto_error),
                        ex.getLocalizedMessage(),
                        getString(R.string.text_noretry, getTxData().getShiftService().getLabel()));
            }
        });
    }

    @Override
    public void onOrderParametersReceived(QueryOrderParameters orderParameters) {
        final double price = orderParameters.getPrice();
        maxBtc = price * orderParameters.getUpperLimit();
        minBtc = price * orderParameters.getLowerLimit();
        this.orderParameters = orderParameters;
        requireView().post(() -> {
            etAmount.setExchangeRate(1 / price);
            Timber.d("minBtc=%f / maxBtc=%f", minBtc, maxBtc);
            final String min = AmountHelper.format_6(minBtc);
            final String max = AmountHelper.format_6(maxBtc);
            final String rate = AmountHelper.format_6(price);
            final TxDataBtc txDataBtc = (TxDataBtc) sendListener.getTxData();
            final Spanned xmrParmText = Html.fromHtml(getString(R.string.info_send_xmrto_parms,
                    min, max, rate, txDataBtc.getBtcSymbol(), service.getLabel()));
            tvXmrToParms.setText(xmrParmText);

            final long funds = getTotalFunds();
            final double availableXmr = 1.0 * funds / Helper.ONE_XMR;

            String availBtcString;
            String availXmrString;
            if (!sendListener.getActivityCallback().isStreetMode()) {
                availBtcString = AmountHelper.format_6(availableXmr * price);
                availXmrString = AmountHelper.format_6(availableXmr);
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

    @Override
    public boolean isActive() {
        return true;
    } // TODO Test what happens if we swtich away while querying

    @Override
    public void showProgress() {
        evParams.showProgress(getString(R.string.label_send_progress_queryparms));
    }

    long lastRequest = 0;
    final static long EXCHANGE_TIME = 750; //ms
    final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onExchangeRequested() {
        final long now = System.currentTimeMillis();
        lastRequest = now;
        handler.postDelayed(() -> {
            if (now == lastRequest) { // otherwise we are superseded
                updateShift();
            }
        }, EXCHANGE_TIME);
    }
}