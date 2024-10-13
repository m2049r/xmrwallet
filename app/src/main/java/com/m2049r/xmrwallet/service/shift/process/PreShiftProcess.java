package com.m2049r.xmrwallet.service.shift.process;

import com.m2049r.xmrwallet.service.shift.ShiftService;

public interface PreShiftProcess {
    ShiftService getService();

    void run();

    void restart();
}