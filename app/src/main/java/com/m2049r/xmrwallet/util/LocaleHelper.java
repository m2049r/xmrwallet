/*
 * Copyright (c) 2018-2020 m2049r et al.
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
import android.content.res.Configuration;

import androidx.preference.PreferenceManager;

import com.m2049r.xmrwallet.R;

import java.util.ArrayList;
import java.util.Locale;

public class LocaleHelper {
    private static Locale SYSTEM_DEFAULT_LOCALE = Locale.getDefault();

    public static ArrayList<Locale> getAvailableLocales(Context context) {
        ArrayList<Locale> locales = new ArrayList<>();
        // R.string.available_locales gets generated in build.gradle by enumerating values-* folders
        String[] availableLocales = context.getString(R.string.available_locales).split(",");

        for (String localeName : availableLocales) {
            if (!localeName.startsWith("night") && !localeName.matches("v[0-9]+"))
                locales.add(Locale.forLanguageTag(localeName));
        }

        return locales;
    }

    public static String getDisplayName(Locale locale, boolean sentenceCase) {
        String displayName = locale.getDisplayName(locale);

        if (sentenceCase) {
            displayName = toSentenceCase(displayName, locale);
        }

        return displayName;
    }

    public static Context setPreferredLocale(Context context) {
        return setLocale(context, getPreferredLanguageTag(context));
    }

    public static Context setAndSaveLocale(Context context, String langaugeTag) {
        savePreferredLangaugeTag(context, langaugeTag);
        return setLocale(context, langaugeTag);
    }

    private static Context setLocale(Context context, String languageTag) {
        Locale locale = (languageTag.isEmpty()) ? SYSTEM_DEFAULT_LOCALE : Locale.forLanguageTag(languageTag);
        Locale.setDefault(locale);

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);

        return context.createConfigurationContext(configuration);
    }

    public static void updateSystemDefaultLocale(Locale locale) {
        SYSTEM_DEFAULT_LOCALE = locale;
    }

    private static String toSentenceCase(String str, Locale locale) {
        if (str.isEmpty()) {
            return str;
        }

        int firstCodePointLen = str.offsetByCodePoints(0, 1);
        return str.substring(0, firstCodePointLen).toUpperCase(locale)
                + str.substring(firstCodePointLen);
    }

    public static Locale getPreferredLocale(Context context) {
        String languageTag = getPreferredLanguageTag(context);
        return languageTag.isEmpty() ? SYSTEM_DEFAULT_LOCALE : Locale.forLanguageTag(languageTag);
    }

    public static String getPreferredLanguageTag(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString("preferred_locale", "");
        // cannot access getString here as it's done BEFORE string locale is set
    }

    @SuppressLint("ApplySharedPref")
    private static void savePreferredLangaugeTag(Context context, String locale) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(context.getString(R.string.preferred_locale), locale).commit();
    }
}
