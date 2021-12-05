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

package com.m2049r.xmrwallet.dialog;

import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.util.NetCipherHelper;

public class HelpFragment extends DialogFragment {
    static final String TAG = "HelpFragment";
    private static final String HELP_ID = "HELP_ID";
    private static final String TOR_BUTTON = "TOR";

    public static HelpFragment newInstance(int helpResourceId) {
        HelpFragment fragment = new HelpFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(HELP_ID, helpResourceId);
        // a hack for the tor button
        if (helpResourceId == R.string.help_tor)
            bundle.putInt(TOR_BUTTON, 7);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static void display(FragmentManager fm, int helpResourceId) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(TAG);
        if (prev != null) {
            ft.remove(prev);
        }

        HelpFragment.newInstance(helpResourceId).show(ft, TAG);
    }

    private Spanned getHtml(String html, double textSize) {
        final Html.ImageGetter imageGetter = source -> {
            final int imageId = getResources().getIdentifier(source.replace("/", ""), "drawable", requireActivity().getPackageName());
            // Don't die if we don't find the image - use a heart instead
            final Drawable drawable = ContextCompat.getDrawable(requireActivity(), imageId > 0 ? imageId : R.drawable.ic_favorite_24dp);
            final double f = textSize / drawable.getIntrinsicHeight();
            drawable.setBounds(0, 0, (int) (f * drawable.getIntrinsicWidth()), (int) textSize);
            return drawable;
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY, imageGetter, null);
        } else {
            return Html.fromHtml(html, imageGetter, null);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_help, null);

        int helpId = 0;
        boolean torButton = false;
        Bundle arguments = getArguments();
        if (arguments != null) {
            helpId = arguments.getInt(HELP_ID);
            torButton = arguments.getInt(TOR_BUTTON) > 0;
        }
        final TextView helpTv = view.findViewById(R.id.tvHelp);
        if (helpId > 0)
            helpTv.setText(getHtml(getString(helpId), helpTv.getTextSize()));

        MaterialAlertDialogBuilder builder =
                new MaterialAlertDialogBuilder(requireActivity())
                        .setView(view);
        if (torButton) {
            builder.setNegativeButton(R.string.help_nok,
                    (dialog, id) -> dialog.dismiss())
                    .setPositiveButton(R.string.help_getorbot,
                            (dialog, id) -> {
                                dialog.dismiss();
                                NetCipherHelper.getInstance().installOrbot(requireActivity());
                            });
        } else {
            builder.setNegativeButton(R.string.help_ok,
                    (dialog, id) -> dialog.dismiss());
        }
        return builder.create();
    }
}