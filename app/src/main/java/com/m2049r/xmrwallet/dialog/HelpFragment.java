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
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.m2049r.xmrwallet.R;

public class HelpFragment extends DialogFragment {
    static final String TAG = "HelpFragment";
    private static final String HELP_ID = "HELP_ID";

    public static HelpFragment newInstance(int helpResourceId) {
        HelpFragment fragment = new HelpFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(HELP_ID, helpResourceId);
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

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_help, null);

        int helpId = 0;
        Bundle arguments = getArguments();
        if (arguments != null) {
            helpId = arguments.getInt(HELP_ID);
        }
        if (helpId > 0)
            ((TextView) view.findViewById(R.id.tvHelp)).setText(Html.fromHtml(getString(helpId)));

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity())
                .setView(view)
                .setNegativeButton(R.string.help_ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
        return builder.create();
    }
}