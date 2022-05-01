package com.m2049r.xmrwallet;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.StyleRes;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.m2049r.xmrwallet.dialog.AboutFragment;
import com.m2049r.xmrwallet.dialog.CreditsFragment;
import com.m2049r.xmrwallet.dialog.PrivacyFragment;
import com.m2049r.xmrwallet.util.DayNightMode;
import com.m2049r.xmrwallet.util.LocaleHelper;
import com.m2049r.xmrwallet.util.NightmodeHelper;
import com.m2049r.xmrwallet.util.ThemeHelper;
import com.m2049r.xmrwallet.widget.Toolbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import timber.log.Timber;

public class SettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        findPreference(getString(R.string.about_info)).setOnPreferenceClickListener(preference -> {
            AboutFragment.display(getParentFragmentManager());
            return true;
        });
        findPreference(getString(R.string.privacy_info)).setOnPreferenceClickListener(preference -> {
            PrivacyFragment.display(getParentFragmentManager());
            return true;
        });
        findPreference(getString(R.string.credits_info)).setOnPreferenceClickListener(preference -> {
            CreditsFragment.display(getParentFragmentManager());
            return true;
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.preferred_locale))) {
            activity.recreate();
        } else if (key.equals(getString(R.string.preferred_nightmode))) {
            NightmodeHelper.setNightMode(DayNightMode.valueOf(sharedPreferences.getString(key, "AUTO")));
        } else if (key.equals(getString(R.string.preferred_theme))) {
            ThemeHelper.setTheme((Activity) activity, sharedPreferences.getString(key, "Classic"));
            activity.recreate();
        }
    }

    private SettingsFragment.Listener activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof SettingsFragment.Listener) {
            activity = (SettingsFragment.Listener) context;
        } else {
            throw new ClassCastException(context + " must implement Listener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume()");
        activity.setSubtitle(getString(R.string.menu_settings));
        activity.setToolbarButton(Toolbar.BUTTON_BACK);
        populateLanguages();
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public interface Listener {
        void setToolbarButton(int type);

        void setSubtitle(String title);

        void recreate();

        void setTheme(@StyleRes final int resId);
    }

    public void populateLanguages() {
        ListPreference language = findPreference(getString(R.string.preferred_locale));
        assert language != null;

        final ArrayList<Locale> availableLocales = LocaleHelper.getAvailableLocales(requireContext());
        Collections.sort(availableLocales, (locale1, locale2) -> {
            String localeString1 = LocaleHelper.getDisplayName(locale1, true);
            String localeString2 = LocaleHelper.getDisplayName(locale2, true);
            return localeString1.compareTo(localeString2);
        });

        String[] localeDisplayNames = new String[1 + availableLocales.size()];
        localeDisplayNames[0] = getString(R.string.language_system_default);
        for (int i = 1; i < localeDisplayNames.length; i++) {
            localeDisplayNames[i] = LocaleHelper.getDisplayName(availableLocales.get(i - 1), true);
        }
        language.setEntries(localeDisplayNames);

        String[] languageTags = new String[1 + availableLocales.size()];
        languageTags[0] = "";
        for (int i = 1; i < languageTags.length; i++) {
            languageTags[i] = availableLocales.get(i - 1).toLanguageTag();
        }
        language.setEntryValues(languageTags);
    }
}