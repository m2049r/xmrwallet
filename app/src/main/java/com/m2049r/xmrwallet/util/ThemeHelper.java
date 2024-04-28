/*
 * Copyright (c) 2019 m2049r
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

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;

import androidx.preference.PreferenceManager;

import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.m2049r.xmrwallet.R;

public class ThemeHelper {
    static public int getThemedResourceId(Context ctx, int attrId) {
        final TypedValue typedValue = new TypedValue();
        if (ctx.getTheme().resolveAttribute(attrId, typedValue, true))
            return typedValue.resourceId;
        else
            return 0;
    }

    @ColorInt
    static public int getThemedColor(Context ctx, int attrId) {
        final TypedValue typedValue = new TypedValue();
        if (ctx.getTheme().resolveAttribute(attrId, typedValue, true))
            return typedValue.data;
        else
            return Color.BLACK;
    }

    public static void setTheme(@NonNull Activity activity, @NonNull String theme) {
        switch (theme) {
            case "Classic":
                activity.setTheme(R.style.MyMaterialThemeClassic);
                break;
            case "Oled":
                activity.setTheme(R.style.MyMaterialThemeOled);
                break;
        }
    }

    public static void setPreferred(Activity activity) {
        final String theme = PreferenceManager.getDefaultSharedPreferences(activity)
                .getString(activity.getString(R.string.preferred_theme), "Classic");
        setTheme(activity, theme);
    }

}
