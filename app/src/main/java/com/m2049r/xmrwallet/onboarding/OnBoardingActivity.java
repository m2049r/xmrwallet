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

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.m2049r.xmrwallet.LoginActivity;
import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.util.KeyStoreHelper;

public class OnBoardingActivity extends AppCompatActivity implements OnBoardingAdapter.Listener {

    private OnBoardingViewPager pager;
    private OnBoardingAdapter pagerAdapter;
    private Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_boarding);

        nextButton = findViewById(R.id.buttonNext);

        pager = findViewById(R.id.pager);
        pagerAdapter = new OnBoardingAdapter(this, this);
        pager.setAdapter(pagerAdapter);
        int pixels = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        pager.setPageMargin(pixels);
        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                setButtonState(position);
            }
        });

        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        if (pagerAdapter.getCount() > 1) {
            tabLayout.setupWithViewPager(pager, true);
            LinearLayout tabStrip = ((LinearLayout) tabLayout.getChildAt(0));
            for (int i = 0; i < tabStrip.getChildCount(); i++) {
                tabStrip.getChildAt(i).setClickable(false);
            }
        } else {
            tabLayout.setVisibility(View.GONE);
        }

        nextButton.setOnClickListener(v -> {
            final int item = pager.getCurrentItem();
            if (item + 1 >= pagerAdapter.getCount()) {
                finishOnboarding();
            } else {
                pager.setCurrentItem(item + 1);
            }
        });

        // let old users who have fingerprint wallets already agree for fingerprint sending
        OnBoardingScreen.FPSEND.setMustAgree(KeyStoreHelper.hasStoredPasswords(this));

        for (int i = 0; i < OnBoardingScreen.values().length; i++) {
            agreed[i] = !OnBoardingScreen.values()[i].isMustAgree();
        }

        setButtonState(0);
    }

    private void finishOnboarding() {
        nextButton.setEnabled(false);
        OnBoardingManager.setOnBoardingShown(getApplicationContext());
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    boolean[] agreed = new boolean[OnBoardingScreen.values().length];

    @Override
    public void setAgreeClicked(int position, boolean isChecked) {
        agreed[position] = isChecked;
        setButtonState(position);
    }

    @Override
    public boolean isAgreeClicked(int position) {
        return agreed[position];
    }

    @Override
    public void setButtonState(int position) {
        nextButton.setEnabled(agreed[position]);
        if (nextButton.isEnabled())
            pager.setAllowedSwipeDirection(OnBoardingViewPager.SwipeDirection.ALL);
        else
            pager.setAllowedSwipeDirection(OnBoardingViewPager.SwipeDirection.LEFT);
        if (pager.getCurrentItem() + 1 == pagerAdapter.getCount()) { // last page
            nextButton.setText(R.string.onboarding_button_ready);
        } else {
            nextButton.setText(R.string.onboarding_button_next);
        }
    }
}
