package com.m2049r.xmrwallet.service.shift.process;

import com.m2049r.xmrwallet.data.CryptoAmount;
import com.m2049r.xmrwallet.fragment.send.PreShifter;
import com.m2049r.xmrwallet.service.shift.ShiftCallback;
import com.m2049r.xmrwallet.service.shift.ShiftService;
import com.m2049r.xmrwallet.service.shift.api.QueryOrderParameters;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import timber.log.Timber;

@RequiredArgsConstructor
public class PreProcess implements PreShiftProcess {
    @Getter
    final private ShiftService service;
    final private PreShifter preshifter;

    @Override
    public void run() {
        getOrderParameters();
    }

    @Override
    public void restart() {
        run();
    }

    private void getOrderParameters() {
        Timber.d("getOrderParameters");
        if (!preshifter.isActive()) return;
        preshifter.showProgress();
        final CryptoAmount btcAmount = preshifter.getAmount();
        service.getShiftApi().queryOrderParameters(btcAmount, new ShiftCallback<QueryOrderParameters>() {
            @Override
            public void onSuccess(final QueryOrderParameters orderParameters) {
                if (!preshifter.isActive()) return;
                onOrderParametersReceived(orderParameters);
            }

            @Override
            public void onError(final Exception ex) {
                if (!preshifter.isActive()) return;
                preshifter.onOrderParametersError(ex);
            }
        });
    }

    private void onOrderParametersReceived(final QueryOrderParameters orderParameters) {
        Timber.d("onOrderParmsReceived %f", orderParameters.getPrice());
        preshifter.onOrderParametersReceived(orderParameters);
    }
}
