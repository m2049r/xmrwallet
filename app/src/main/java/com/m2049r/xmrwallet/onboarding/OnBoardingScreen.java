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

package com.m2049r.xmrwallet.onboarding;

import com.m2049r.xmrwallet.R;

enum OnBoardingScreen {
    WELCOME(R.string.onboarding_welcome_title, R.string.onboarding_welcome_information, R.drawable.ic_onboarding_welcome, false),
    SEED(R.string.onboarding_seed_title, R.string.onboarding_seed_information, R.drawable.ic_onboarding_seed, true),
    FPSEND(R.string.onboarding_fpsend_title, R.string.onboarding_fpsend_information, R.drawable.ic_onboarding_fingerprint, false),
    NODES(R.string.onboarding_nodes_title, R.string.onboarding_nodes_information, R.drawable.ic_onboarding_nodes, false);

    private final int title;
    private final int information;
    private final int drawable;
    private boolean mustAgree;

    OnBoardingScreen(final int title, final int information, final int drawable, final boolean mustAgree) {
        this.title = title;
        this.information = information;
        this.drawable = drawable;
        this.mustAgree = mustAgree;
    }

    public int getTitle() {
        return title;
    }

    public int getInformation() {
        return information;
    }

    public int getDrawable() {
        return drawable;
    }

    public boolean isMustAgree() {
        return mustAgree;
    }

    public boolean setMustAgree(boolean mustAgree) {
        return this.mustAgree = mustAgree;
    }
}
