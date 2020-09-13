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
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;

import com.m2049r.xmrwallet.LoginActivity;
import com.m2049r.xmrwallet.R;

public class OnBoardingActivity extends AppCompatActivity implements OnBoardingAdapter.Listener {

    private ViewPager pager;
    private OnBoardingAdapter pagerAdapter;
    private int mustAgreePages = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_boarding);

        final View nextButton = findViewById(R.id.buttonNext);

        pager = findViewById(R.id.pager);
        pagerAdapter = new OnBoardingAdapter(getApplicationContext(), this);
        pager.setAdapter(pagerAdapter);
        int pixels = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        pager.setPageMargin(pixels);
        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int i) {
                setButtonState();
            }
        });

        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        if (pagerAdapter.getCount() > 1) {
            tabLayout.setupWithViewPager(pager, true);
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

        for (int i = 0; i < OnBoardingScreen.values().length; i++) {
            if (OnBoardingScreen.values()[i].isMustAgree()) mustAgreePages++;
        }
    }

    private void finishOnboarding() {
        OnBoardingManager.setOnBoardingShown(getApplicationContext());
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    int agreeCounter = 0;
    boolean agreed[] = new boolean[OnBoardingScreen.values().length];

    @Override
    public void setAgreeClicked(int position, boolean isChecked) {
        if (isChecked) {
            agreeCounter++;
        } else {
            agreeCounter--;
        }
        agreed[position] = isChecked;
        setButtonState();
    }

    @Override
    public boolean isAgreeClicked(int position) {
        return agreed[position];
    }

    @Override
    public void setButtonState() {
        if (pager.getCurrentItem() + 1 == pagerAdapter.getCount()) { // last page
            findViewById(R.id.buttonNext).setEnabled(mustAgreePages == agreeCounter);
        } else {
            findViewById(R.id.buttonNext).setEnabled(true);
        }
    }
}
