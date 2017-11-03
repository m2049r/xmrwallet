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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.m2049r.xmrwallet.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class BetterHelpFragment extends DialogFragment {
    static final String TAG = "BetterHelpFragment";
    private static final String HELP_ID = "HELP_ID";

    public static BetterHelpFragment newInstance(int helpResourceId) {
        BetterHelpFragment fragment = new BetterHelpFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(HELP_ID, helpResourceId);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static void displayHelp(FragmentManager fm, int helpResourceId) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(TAG);
        if (prev != null) {
            ft.remove(prev);
        }

        DialogFragment helpFragment = BetterHelpFragment.newInstance(helpResourceId);
        helpFragment.show(ft, TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_better_help, null);

        int helpId = 0;
        Bundle arguments = getArguments();
        if (arguments != null) {
            helpId = arguments.getInt(HELP_ID);
        }
        if (helpId > 0)
            ((TextView) view.findViewById(R.id.tvHelp)).setText(Html.fromHtml(getString(helpId)));

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