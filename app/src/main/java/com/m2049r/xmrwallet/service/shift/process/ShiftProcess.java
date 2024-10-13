package com.m2049r.xmrwallet.service.shift.process;

import com.m2049r.xmrwallet.data.TxDataBtc;
import com.m2049r.xmrwallet.service.shift.ShiftService;

public interface ShiftProcess {
    ShiftService getService();

    void run(TxDataBtc txData);

    void restart();

    void retryCreateOrder();
}