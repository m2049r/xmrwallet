/*
 * Copyright (c) 2017 m2049r
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

package com.m2049r.xmrwallet;

import static android.view.WindowManager.LayoutParams;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.LocaleHelper;

import java.util.Locale;

public abstract class SecureActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Helper.preventScreenshot()) {
            getWindow().setFlags(LayoutParams.FLAG_SECURE, LayoutParams.FLAG_SECURE);
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        applyOverrideConfiguration(new Configuration());
    }

    @Override
    public void applyOverrideConfiguration(Configuration newConfig) {
        super.applyOverrideConfiguration(updateConfigurationIfSupported(newConfig));
    }

    private Configuration updateConfigurationIfSupported(Configuration config) {
        // Configuration.getLocales is added after 24 and Configuration.locale is deprecated in 24
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!config.getLocales().isEmpty()) {
                return config;
            }
        } else {
            if (config.locale != null) {
                return config;
            }
        }

        Locale locale = LocaleHelper.getPreferredLocale(this);
        if (locale != null) {
            config.setLocale(locale);
        }
        return config;
    }
}
