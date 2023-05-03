/*
 * Copyright (c) 2018-2020 EarlOfEgo, m2049r
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

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.m2049r.xmrwallet.onboarding.OnBoardingActivity;
import com.m2049r.xmrwallet.onboarding.OnBoardingManager;

public class MainActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (OnBoardingManager.shouldShowOnBoarding(getApplicationContext())) {
            startActivity(new Intent(this, OnBoardingActivity.class));
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }
}
