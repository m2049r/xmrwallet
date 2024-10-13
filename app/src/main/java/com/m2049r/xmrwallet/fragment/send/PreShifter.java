package com.m2049r.xmrwallet.fragment.send;

import com.m2049r.xmrwallet.data.CryptoAmount;
import com.m2049r.xmrwallet.service.shift.api.QueryOrderParameters;

public interface PreShifter {
    CryptoAmount getAmount();

    void onOrderParametersError(final Exception ex);

    void onOrderParametersReceived(final QueryOrderParameters orderParameters);

    boolean isActive();

    void showProgress();
}
