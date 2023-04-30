/*
 * Copyright (c) 2018-2020 EarlOfEgo, m2049r
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

    public static void clearOnBoardingShown(final Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        sharedPreferences.edit().remove(ONBOARDING_SHOWN).apply();
    }

    private static SharedPreferences getSharedPreferences(final Context context) {
        return context.getSharedPreferences(PREFS_ONBOARDING, Context.MODE_PRIVATE);
    }
}
