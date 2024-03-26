/*
 * Copyright (c) 2020 m2049r
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

package com.m2049r.xmrwallet.util;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatDelegate;

import com.m2049r.xmrwallet.R;

public class NightmodeHelper {
    public static DayNightMode getPreferredNightmode(Context context) {
        return DayNightMode.valueOf(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.preferred_nightmode), "UNKNOWN"));
    }

    public static void setPreferredNightmode(Context context) {
        final DayNightMode mode = DayNightMode.valueOf(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.preferred_nightmode), "UNKNOWN"));
        if (mode == DayNightMode.UNKNOWN)
            setAndSavePreferredNightmode(context, DayNightMode.AUTO);
        else
            setNightMode(mode);
    }

    public static void setAndSavePreferredNightmode(Context context, DayNightMode mode) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(context.getString(R.string.preferred_nightmode), mode.name()).apply();
        setNightMode(mode);
    }

    @SuppressLint("WrongConstant")
    public static void setNightMode(DayNightMode mode) {
        AppCompatDelegate.setDefaultNightMode(mode.getNightMode());
    }
}
