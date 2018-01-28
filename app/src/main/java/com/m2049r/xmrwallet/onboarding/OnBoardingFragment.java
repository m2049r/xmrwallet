package com.m2049r.xmrwallet.onboarding;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.m2049r.xmrwallet.R;

public class OnBoardingFragment extends Fragment {
    private static final String BUNDLE_ARGS_NUMBER = "BUNDLE_ARGS_NUMBER";

    public static OnBoardingFragment newInstance(final int number) {

        Bundle args = new Bundle();
        args.putInt(BUNDLE_ARGS_NUMBER, number);
        OnBoardingFragment fragment = new OnBoardingFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container,
            @Nullable final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_onboarding, container, false);
        final int number = getArguments().getInt(BUNDLE_ARGS_NUMBER);
        final OnBoardingScreen onBoardingScreen = OnBoardingScreen.values()[number];
//        ((ImageView) view.findViewById(R.id.onboardingImage)).setImageDrawable(onBoardingScreen.getDrawable());
        ((TextView) view.findViewById(R.id.onboardingTitle)).setText(onBoardingScreen.getTitle());
        ((TextView) view.findViewById(R.id.onboardingInformation)).setText(onBoardingScreen.getInformation());
        return view;
    }
}
