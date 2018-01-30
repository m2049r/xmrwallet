package com.m2049r.xmrwallet.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;

import com.m2049r.xmrwallet.LoginActivity;
import com.m2049r.xmrwallet.R;

public class OnBoardingActivity extends AppCompatActivity {

    private ViewPager pager;
    private OnBoardingAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_boarding);

        findViewById(R.id.buttonSkip).setOnClickListener(v -> finishOnboarding());
        pager = findViewById(R.id.pager);
        pagerAdapter = new OnBoardingAdapter(getApplicationContext());
        pager.setAdapter(pagerAdapter);
        int pixels = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,16, getResources().getDisplayMetrics());
        pager.setPageMargin(pixels);

        final TabLayout onboardingTabLayout = findViewById(R.id.tabLayout);
        onboardingTabLayout.setupWithViewPager(pager, true);

        findViewById(R.id.buttonNext).setOnClickListener(v -> {
            final int item = pager.getCurrentItem();
            if(item + 1 >= pagerAdapter.getCount()) {
                finishOnboarding();
            } else {
                pager.setCurrentItem(item + 1);
            }
        });

    }

    private void finishOnboarding() {
        OnBoardingManager.setOnBoardingShown(getApplicationContext());
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

}
