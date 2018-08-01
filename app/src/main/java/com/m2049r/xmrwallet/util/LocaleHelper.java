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
    public static final int COMPARED_RESOURCE_ID = R.string.language;

    private static final String PREFERRED_LOCALE_KEY = "preferred_locale";
    private static Locale SYSTEM_DEFAULT_LOCALE = Locale.getDefault();

    public static ArrayList<Locale> getAvailableLocales(Context context) {
        ArrayList<Locale> locales = new ArrayList<>();
        HashSet<String> localizedStrings = new HashSet<>();

        for (String localeName : context.getAssets().getLocales()) {
            Locale locale = Locale.forLanguageTag(localeName);
            String localizedString = getLocaleString(context, locale, COMPARED_RESOURCE_ID);

            if (localizedStrings.add(localizedString)) {
                locales.add(locale);
            }
        }

        return locales;
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

    public static String getLocaleString(Context context, Locale locale, int resId) {
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration).getString(resId);
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
