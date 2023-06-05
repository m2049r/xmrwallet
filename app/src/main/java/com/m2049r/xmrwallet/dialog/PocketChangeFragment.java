/*
 * Copyright (c) 2023 m2049r
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

package com.m2049r.xmrwallet.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.util.Helper;

public class PocketChangeFragment extends DialogFragment implements Slider.OnChangeListener {
    static final String TAG = "PocketChangeFragment";
    static final String ENABLED = "enabled";
    static final String TICK = "tick";

    public static PocketChangeFragment newInstance(boolean enabled, int tick) {
        PocketChangeFragment fragment = new PocketChangeFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ENABLED, enabled ? 1 : 0);
        bundle.putInt(TICK, tick);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static void display(FragmentManager fm, @NonNull Wallet.PocketChangeSetting setting) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        PocketChangeFragment.newInstance(setting.isEnabled(), getTick(setting.getAmount())).show(ft, TAG);
    }

    SwitchMaterial switchPocketChange;
    Slider slider;
    TextView tvProgressLabel;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_pocketchange_setting, null);
        boolean enabled = false;
        int progress = 0;
        Bundle arguments = getArguments();
        if (arguments != null) {
            enabled = arguments.getInt(ENABLED) > 0;
            progress = arguments.getInt(TICK);
        }

        final View llAmount = view.findViewById(R.id.llAmount);
        switchPocketChange = view.findViewById(R.id.switchPocketChange);
        switchPocketChange.setOnCheckedChangeListener((buttonView, isChecked) -> llAmount.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE));
        slider = view.findViewById(R.id.seekbar);
        slider.addOnChangeListener(this);
        switchPocketChange.setChecked(enabled);
        tvProgressLabel = view.findViewById(R.id.seekbar_value);
        slider.setValue(progress);
        llAmount.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        onValueChange(slider, slider.getValue(), false);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity())
                .setView(view)
                .setPositiveButton(R.string.label_apply,
                        (dialog, whichButton) -> {
                            final FragmentActivity activity = getActivity();
                            if (activity instanceof Listener) {
                                ((Listener) activity).setPocketChange(Wallet.PocketChangeSetting.of(switchPocketChange.isChecked(), getAmount()));
                            }
                        }
                );
        return builder.create();
    }

    private long getAmount() {
        return Wallet.getAmountFromDouble(getAmount((int) slider.getValue()));
    }

    private static final double[] AMOUNTS = {0.1, 0.2, 0.3, 0.5, 0.8, 1.3};

    private static double getAmount(int i) {
        return AMOUNTS[i];
    }

    // find the closest amount we have
    private static int getTick(long amount) {
        int enabled = amount > 0 ? 1 : -1;
        amount = Math.abs(amount);
        double lastDiff = Double.MAX_VALUE;
        for (int i = 0; i < AMOUNTS.length; i++) {
            final double diff = Math.abs(Helper.ONE_XMR * AMOUNTS[i] - amount);
            if (lastDiff < diff) return i - 1;
            lastDiff = diff;
        }
        return enabled * (AMOUNTS.length - 1);
    }

    @Override
    public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
        tvProgressLabel.setText(getString(R.string.pocketchange_amount, getAmount((int) value)));
    }

    public interface Listener {
        void setPocketChange(Wallet.PocketChangeSetting setting);
    }
}