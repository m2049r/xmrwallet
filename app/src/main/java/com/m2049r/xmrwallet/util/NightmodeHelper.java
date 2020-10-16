package com.m2049r.xmrwallet.util;

import android.content.Context;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatDelegate;

public class NightmodeHelper {
    private static final String PREFERRED_NIGHTMODE_KEY = "preferred_nightmode";

    public static DayNightMode getPreferredNightmode(Context context) {
        return DayNightMode.valueOf(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREFERRED_NIGHTMODE_KEY, "UNKNOWN"));
    }

    public static void setPreferredNightmode(Context context) {
        final DayNightMode mode = DayNightMode.valueOf(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREFERRED_NIGHTMODE_KEY, "UNKNOWN"));
        if (mode == DayNightMode.UNKNOWN) setAndSavePreferredNightmode(context, DayNightMode.AUTO);
        setNightMode(mode);
    }

    public static void setAndSavePreferredNightmode(Context context, DayNightMode mode) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(PREFERRED_NIGHTMODE_KEY, mode.name()).apply();
        setNightMode(mode);
    }

    public static void setNightMode(DayNightMode mode) {
        AppCompatDelegate.setDefaultNightMode(mode.getNightMode());
    }
}
