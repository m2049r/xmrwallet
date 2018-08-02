package com.m2049r.xmrwallet.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

import com.m2049r.xmrwallet.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

public class LocaleHelper {
    private static final String PREFERRED_LOCALE_KEY = "preferred_locale";
    private static Locale SYSTEM_DEFAULT_LOCALE = Locale.getDefault();

    public static ArrayList<Locale> getAvailableLocales(Context context) {
        ArrayList<Locale> locales = new ArrayList<>();
        String[] availableLocales = context.getString(R.string.available_locales).split(",");

        for (String localeName : availableLocales) {
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

    public static String getLocale(Context context) {
        return getPreferredLocale(context);
    }

    public static Context setLocale(Context context, String locale) {
        setPreferredLocale(context, locale);

        Locale newLocale = (locale.isEmpty()) ? SYSTEM_DEFAULT_LOCALE : Locale.forLanguageTag(locale);
        Configuration configuration = context.getResources().getConfiguration();

        Locale.setDefault(newLocale);

        configuration.setLocale(newLocale);
        configuration.setLayoutDirection(newLocale);

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

    private static String getPreferredLocale(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREFERRED_LOCALE_KEY, "");
    }

    @SuppressLint("ApplySharedPref")
    private static void setPreferredLocale(Context context, String locale) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(PREFERRED_LOCALE_KEY, locale).commit();
    }
}
