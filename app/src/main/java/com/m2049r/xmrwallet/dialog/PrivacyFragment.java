/*
 * Copyright (c) 2017 m2049r
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

/**
 * Based on work by Adam Speakman http://speakman.net.nz
 */

package com.m2049r.xmrwallet.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.util.Helper;

public class PrivacyFragment extends DialogFragment {
    static final String TAG = "PrivacyFragment";
    private static final String FRAGMENT_TAG = "com.m2049r.xmrwalelt.dialog.PrivacyFragment";

    /**
     * Creates a new instance of LicensesFragment with no Close button.
     *
     * @return A new licenses fragment.
     */
    public static PrivacyFragment newInstance() {
        return new PrivacyFragment();
    }

    /**
     * Builds and displays a licenses fragment with no Close button. Requires
     * "/res/raw/licenses.html" and "/res/layout/licenses_fragment.xml" to be
     * present.
     *
     * @param fm A fragment manager instance used to display this LicensesFragment.
     */
    public static void display(FragmentManager fm) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(FRAGMENT_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = PrivacyFragment.newInstance();
        newFragment.show(ft, FRAGMENT_TAG);
    }

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_privacy_policy, null);

        ((TextView) view.findViewById(R.id.tvCredits)).setText(Html.fromHtml(getString(R.string.privacy_policy)));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setNegativeButton(R.string.about_close,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        return builder.create();
    }
}