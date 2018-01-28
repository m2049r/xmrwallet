package com.m2049r.xmrwallet.onboarding;


import com.m2049r.xmrwallet.R;

enum OnBoardingScreen {
    SECURITY(R.string.onboarding_security_title, R.string.onboarding_security_information, R.drawable.ic_onboarding_security),
    SYNCING(R.string.onboarding_syncing_title, R.string.onboarding_syncing_information, R.drawable.ic_syncing_security),
    SEED_WORD(R.string.onboarding_seed_title, R.string.onboarding_seed_information, R.drawable.ic_seed_security);


    private final int title;
    private final int information;
    private final int drawable;

    OnBoardingScreen(final int title, final int information, final int drawable) {
        this.title = title;
        this.information = information;
        this.drawable = drawable;
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
}
