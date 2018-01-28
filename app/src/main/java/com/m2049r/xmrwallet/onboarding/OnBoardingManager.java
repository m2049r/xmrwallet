package com.m2049r.xmrwallet.onboarding;


import android.content.Context;
import android.content.SharedPreferences;

import java.util.Date;

import timber.log.Timber;

public class OnBoardingManager {

    private static final String PREFS_ONBOARDING = "PREFS_ONBOARDING";
    private static final String ONBOARDING_SHOWN = "ONBOARDING_SHOWN";

    public static boolean shouldShowOnBoarding(final Context context) {
        return !getSharedPreferences(context).contains(ONBOARDING_SHOWN);
    }

    public static void setOnBoardingShown(final Context context) {
        Timber.d("Set onboarding shown.");
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        sharedPreferences.edit().putLong(ONBOARDING_SHOWN, new Date().getTime()).apply();
    }

    private static SharedPreferences getSharedPreferences(final Context context) {
        return context.getSharedPreferences(PREFS_ONBOARDING, Context.MODE_PRIVATE);
    }

}
