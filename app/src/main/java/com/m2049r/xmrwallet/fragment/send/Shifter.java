package com.m2049r.xmrwallet.fragment.send;

import com.m2049r.xmrwallet.service.shift.api.CreateOrder;
import com.m2049r.xmrwallet.service.shift.api.RequestQuote;

public interface Shifter {
    void invalidateShift();

    void onQuoteError(final Exception ex);

    void showQuoteError();

    void onQuoteReceived(RequestQuote quote);

    void onOrderCreated(CreateOrder order);

    void onOrderError(final Exception ex);

    boolean isActive();

    void showProgress(Shifter.Stage stage);

    enum Stage {
        X, A, B, C
    }
}
