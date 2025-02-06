/*
 * Copyright (c) 2024 m2049r
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

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.preference.PreferenceManager;

import com.m2049r.xmrwallet.R;

public class StickyFiatHelper {

    public static String getPreferredFiatSymbol(Context ctx) {
        if (PreferenceManager.getDefaultSharedPreferences(ctx)
                .getBoolean(ctx.getString(R.string.preferred_stickyfiat), false)) {
            return PreferenceManager.getDefaultSharedPreferences(ctx)
                    .getString(ctx.getString(R.string.preferred_stickyfiat_pref), Helper.BASE_CRYPTO);
        }
        return null;
    }

    public static void setPreferredFiatSymbol(Context ctx, String symbol) {
        if (PreferenceManager.getDefaultSharedPreferences(ctx)
                .getBoolean(ctx.getString(R.string.preferred_stickyfiat), false)) {
            PreferenceManager.getDefaultSharedPreferences(ctx).edit()
                    .putString(ctx.getString(R.string.preferred_stickyfiat_pref), symbol).apply();
        }
    }


    public static void setPreferredCurrencyPosition(Spinner spinner) {
        String stickyFiat = StickyFiatHelper.getPreferredFiatSymbol(spinner.getContext());
        if (stickyFiat != null) {
            StickyFiatHelper.setCurrencyPosition(spinner, stickyFiat);
        }
    }

    public static void setCurrencyPosition(Spinner spinner, String symbol) {
        @SuppressWarnings("unchecked")
        int spinnerPosition = ((ArrayAdapter<String>) spinner.getAdapter()).getPosition(symbol);
        spinner.setSelection(Math.max(spinnerPosition, 0), true);
    }
}
