package com.m2049r.xmrwallet.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
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
        HashSet<String> localizedStrings = new HashSet<>();
        int compareResId = R.string.menu_about;

        // Application default locale
        locales.add(Locale.ENGLISH);
        localizedStrings.add(getLocaleString(context, Locale.ENGLISH, compareResId));

        // Locale with region
        locales.add(Locale.CHINA);
        localizedStrings.add(getLocaleString(context, Locale.CHINA, compareResId));

        locales.add(Locale.TAIWAN);
        localizedStrings.add(getLocaleString(context, Locale.TAIWAN, compareResId));

        // Enumerate supported locales and add translated ones
        for (String localeName : context.getAssets().getLocales()) {
            Locale locale = Locale.forLanguageTag(localeName);
            String localizedString = getLocaleString(context, locale, compareResId);

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
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();

        configuration.setLocale(newLocale);
        configuration.setLayoutDirection(newLocale);
        return context.createConfigurationContext(configuration);
    }

    public static void updateSystemDefaultLocale(Locale locale) {
        SYSTEM_DEFAULT_LOCALE = locale;
    }

    private static String getLocaleString(Context context, Locale locale, int resId) {
        Configuration configuration = context.getResources().getConfiguration();
        configuration.locale = locale;

        Resources localizedResources = new Resources(context.getAssets(),
                context.getResources().getDisplayMetrics(), configuration);
        return localizedResources.getString(resId);
    }

    private static String getPreferredLocale(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREFERRED_LOCALE_KEY, "");
    }

    private static void setPreferredLocale(Context context, String locale) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(PREFERRED_LOCALE_KEY, locale).apply();
    }
}
