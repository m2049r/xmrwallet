package com.m2049r.xmrwallet.service.shift.process;

import com.m2049r.xmrwallet.data.Crypto;
import com.m2049r.xmrwallet.data.TxDataBtc;
import com.m2049r.xmrwallet.fragment.send.Shifter;
import com.m2049r.xmrwallet.service.shift.ShiftCallback;
import com.m2049r.xmrwallet.service.shift.ShiftService;
import com.m2049r.xmrwallet.service.shift.ShiftType;
import com.m2049r.xmrwallet.service.shift.api.CreateOrder;
import com.m2049r.xmrwallet.service.shift.api.RequestQuote;

import java.util.Date;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import timber.log.Timber;

@RequiredArgsConstructor
public class Process implements ShiftProcess {
    @Getter
    final private ShiftService service;
    final private Shifter shifter;
    private TxDataBtc txDataBtc;
    private RequestQuote quote = null;
    private CreateOrder order = null;

    @Override
    public void run(TxDataBtc txData) {
        txDataBtc = txData;
        switch (service.getType()) {
            case TWOSTEP:
                getQuote();
                break;
            case ONESTEP:
                quote = new RequestQuote() {
                    @Override
                    public double getBtcAmount() {
                        return txDataBtc.getBtcAmount();
                    }

                    @Override
                    public double getXmrAmount() {
                        return txDataBtc.getXmrAmount();
                    }

                    @Override
                    public String getId() {
                        return null;
                    }

                    @Override
                    public Date getCreatedAt() {
                        return null;
                    }

                    @Override
                    public Date getExpiresAt() {
                        return null;
                    }

                    @Override
                    public double getPrice() {
                        return 0;
                    }

                    @Override
                    public ShiftType getType() {
                        return (txDataBtc.getShiftAmount().getCrypto() == Crypto.XMR) ? ShiftType.FLOAT : ShiftType.FIXED;
                    }
                };
                createOrder();
                break;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void restart() {
        run(txDataBtc);
    }

    private void getQuote() {
        shifter.invalidateShift();
        if (!shifter.isActive()) return;
        Timber.d("Request Quote");
        quote = null;
        order = null;
        shifter.showProgress(Shifter.Stage.A);

        ShiftCallback<RequestQuote> callback = new ShiftCallback<RequestQuote>() {
            @Override
            public void onSuccess(RequestQuote requestQuote) {
                if (!shifter.isActive()) return;
                if (quote != null) {
                    Timber.w("another ongoing request quote request");
                    return;
                }
                onQuoteReceived(requestQuote);
            }

            @Override
            public void onError(Exception ex) {
                if (!shifter.isActive()) return;
                if (quote != null) {
                    Timber.w("another ongoing request quote request");
                    return;
                }
                shifter.onQuoteError(ex);
            }
        };
        service.getShiftApi().requestQuote(txDataBtc.getBtcAddress(), txDataBtc.getShiftAmount(), callback);
    }

    private void onQuoteReceived(final RequestQuote quote) {
        Timber.d("onQuoteReceived %s", quote.getId());
        // verify the shift is correct
        if (!txDataBtc.validate(quote)) {
            Timber.d("Failed to get quote");
            shifter.showQuoteError();
            return; // just stop for now
        }
        this.quote = quote;
        txDataBtc.setAmount(this.quote.getXmrAmount());
        shifter.onQuoteReceived(this.quote);
        createOrder();
    }

    @Override
    public void retryCreateOrder() {
        createOrder();
    }

    private void createOrder() {
        Timber.d("createOrder(%s)", quote.getId());
        if (!shifter.isActive()) return;
        final String btcAddress = txDataBtc.getBtcAddress();
        order = null;
        shifter.showProgress(Shifter.Stage.B);
        service.getShiftApi().createOrder(quote, btcAddress, new ShiftCallback<CreateOrder>() {
            @Override
            public void onSuccess(final CreateOrder order) {
                if (!shifter.isActive()) return;
                if (quote == null) return;
                if ((quote.getId() != null) && !order.getQuoteId().equals(quote.getId())) {
                    Timber.d("Quote ID does not match");
                    // ignore (we got a response to a stale request)
                    return;
                }
                if (Process.this.order != null)
                    throw new IllegalStateException("order must be null here!");
                onOrderReceived(order);
            }

            @Override
            public void onError(final Exception ex) {
                if (!shifter.isActive()) return;
                shifter.onOrderError(ex);
            }
        });
    }

    private void onOrderReceived(final CreateOrder order) {
        Timber.d("onOrderReceived %s for %s", order.getOrderId(), order.getQuoteId());
        // verify amount & destination
        if (!order.getBtcCurrency().equalsIgnoreCase(txDataBtc.getBtcSymbol()))
            throw new IllegalStateException("Destination Currency is wrong: " + order.getBtcCurrency()); // something is terribly wrong - die
        if ((order.getType() == ShiftType.FIXED) && (order.getBtcAmount() != txDataBtc.getShiftAmount().getAmount()))
            throw new IllegalStateException("Destination Amount is wrong: " + order.getBtcAmount()); // something is terribly wrong - die
        if ((order.getType() == ShiftType.FLOAT) && (order.getXmrAmount() != txDataBtc.getShiftAmount().getAmount()))
            throw new IllegalStateException("Source Amount is wrong: " + order.getXmrAmount()); // something is terribly wrong - die
        if (!txDataBtc.validateAddress(order.getBtcAddress())) {
            throw new IllegalStateException("Destination address is wrong: " + order.getBtcAddress()); // something is terribly wrong - die
        }
        this.order = order;
        shifter.onOrderCreated(order);
    }
}
