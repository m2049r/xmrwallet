package com.m2049r.xmrwallet;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.m2049r.xmrwallet.onboarding.OnBoardingActivity;
import com.m2049r.xmrwallet.onboarding.OnBoardingManager;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(OnBoardingManager.shouldShowOnBoarding(getApplicationContext())) {
            startActivity(new Intent(this, OnBoardingActivity.class));
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }
}
