package com.m2049r.xmrwallet.util;

import androidx.appcompat.app.AppCompatDelegate;

public enum DayNightMode {
    // order must match R.array.daynight_themes
    AUTO(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    DAY(AppCompatDelegate.MODE_NIGHT_NO),
    NIGHT(AppCompatDelegate.MODE_NIGHT_YES),
    UNKNOWN(AppCompatDelegate.MODE_NIGHT_UNSPECIFIED);

    final private int nightMode;

    DayNightMode(int nightMode) {
        this.nightMode = nightMode;
    }

    public int getNightMode() {
        return nightMode;
    }

    static public DayNightMode getValue(int nightMode) {
        for (DayNightMode mode : DayNightMode.values()) {
            if (mode.nightMode == nightMode)
                return mode;
        }
        return UNKNOWN;
    }
}
