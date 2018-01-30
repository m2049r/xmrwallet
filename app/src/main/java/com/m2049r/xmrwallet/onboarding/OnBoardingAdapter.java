package com.m2049r.xmrwallet.onboarding;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.m2049r.xmrwallet.R;

public class OnBoardingAdapter extends PagerAdapter {

    private final Context context;

    OnBoardingAdapter(final Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup collection, int position) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.view_onboarding, collection, false);
        final OnBoardingScreen onBoardingScreen = OnBoardingScreen.values()[position];

        final Drawable drawable = ContextCompat.getDrawable(context, onBoardingScreen.getDrawable());
        ((ImageView) view.findViewById(R.id.onboardingImage)).setImageDrawable(drawable);
        ((TextView) view.findViewById(R.id.onboardingTitle)).setText(onBoardingScreen.getTitle());
        ((TextView) view.findViewById(R.id.onboardingInformation)).setText(onBoardingScreen.getInformation());

        collection.addView(view);
        return view;
    }

    @Override
    public int getCount() {
        return OnBoardingScreen.values().length;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object view) {
        collection.removeView((View) view);
    }

    @Override
    public boolean isViewFromObject(@NonNull final View view, @NonNull final Object object) {
        return view == object;
    }
}
