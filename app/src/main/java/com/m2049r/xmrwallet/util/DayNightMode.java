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

import androidx.appcompat.app.AppCompatDelegate;

public enum DayNightMode {
    // order must match R.array.daynight_themes
    AUTO(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    DAY(AppCompatDelegate.MODE_NIGHT_NO),
    NIGHT(AppCompatDelegate.MODE_NIGHT_YES),
    UNKNOWN(AppCompatDelegate.MODE_NIGHT_UNSPECIFIED);

    final private int nightMode;

    DayNightMode(int nightMode) {
        this.nightMode = nightMode;
    }

    public int getNightMode() {
        return nightMode;
    }

    static public DayNightMode getValue(int nightMode) {
        for (DayNightMode mode : DayNightMode.values()) {
            if (mode.nightMode == nightMode)
                return mode;
        }
        return UNKNOWN;
    }
}
