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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.PagerAdapter;

import com.m2049r.xmrwallet.R;

import timber.log.Timber;

public class OnBoardingAdapter extends PagerAdapter {

    interface Listener {
        void setAgreeClicked(int position, boolean isChecked);

        boolean isAgreeClicked(int position);

        void setButtonState(int position);
    }

    private final Context context;
    private Listener listener;

    OnBoardingAdapter(final Context context, final Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup collection, int position) {
        LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.view_onboarding, collection, false);
        final OnBoardingScreen onBoardingScreen = OnBoardingScreen.values()[position];

        final Drawable drawable = ContextCompat.getDrawable(context, onBoardingScreen.getDrawable());
        ((ImageView) view.findViewById(R.id.onboardingImage)).setImageDrawable(drawable);
        ((TextView) view.findViewById(R.id.onboardingTitle)).setText(onBoardingScreen.getTitle());
        ((TextView) view.findViewById(R.id.onboardingInformation)).setText(onBoardingScreen.getInformation());
        if (onBoardingScreen.isMustAgree()) {
            final CheckBox agree = ((CheckBox) view.findViewById(R.id.onboardingAgree));
            agree.setVisibility(View.VISIBLE);
            agree.setChecked(listener.isAgreeClicked(position));
            agree.setOnClickListener(v -> {
                listener.setAgreeClicked(position, ((CheckBox) v).isChecked());
            });
        }
        collection.addView(view);
        return view;
    }

    @Override
    public int getCount() {
        return OnBoardingScreen.values().length;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object view) {
        Timber.d("destroy " + position);
        collection.removeView((View) view);
    }

    @Override
    public boolean isViewFromObject(@NonNull final View view, @NonNull final Object object) {
        return view == object;
    }
}
