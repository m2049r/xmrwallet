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
import com.m2049r.xmrwallet.BuildConfig;
import com.m2049r.xmrwallet.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import timber.log.Timber;

public class AboutFragment extends DialogFragment {
    static final String TAG = "AboutFragment";

    public static AboutFragment newInstance() {
        return new AboutFragment();
    }

    public static void display(FragmentManager fm) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(TAG);
        if (prev != null) {
            ft.remove(prev);
        }

        AboutFragment.newInstance().show(ft, TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_about, null);
        ((TextView) view.findViewById(R.id.tvHelp)).setText(Html.fromHtml(getLicencesHtml()));
        ((TextView) view.findViewById(R.id.tvVersion)).setText(getString(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity())
                .setView(view)
                .setNegativeButton(R.string.about_close,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
        return builder.create();
    }

    private String getLicencesHtml() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getContext().getAssets().open("licenses.html"), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                sb.append(line);
            return sb.toString();
        } catch (IOException ex) {
            Timber.e(ex);
            return ex.getLocalizedMessage();
        }
    }
}