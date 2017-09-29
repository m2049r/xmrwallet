/**
 * Copyright 2013 Adam Speakman, m2049r
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.m2049r.xmrwallet.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Based on LicensesFragment by Adam Speakman on 24/09/13.
 * http://speakman.net.nz
 */
public class HelpFragment extends DialogFragment {
    private static final String FRAGMENT_TAG = "com.m2049r.xmrwallet.dialog.HelpFragment";
    private static final String HELP_ID = "HELP_ID";

    private AsyncTask<Void, Void, String> loader;

    public static HelpFragment newInstance(int helpResourceId) {
        HelpFragment fragment = new HelpFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(HELP_ID, helpResourceId);
        fragment.setArguments(bundle);

        return fragment;
    }

    /**
     * @param fm A fragment manager instance used to display this LicensesFragment.
     */
    public static void displayHelp(FragmentManager fm, int helpResourceId) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(FRAGMENT_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = HelpFragment.newInstance(helpResourceId);
        newFragment.show(ft, FRAGMENT_TAG);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        int helpId = 0;
        Bundle arguments = getArguments();
        if (arguments != null) {
            helpId = arguments.getInt(HELP_ID);
        }
        if (helpId > 0)
            loadHelp(helpId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (loader != null) {
            loader.cancel(true);
        }
    }

    private WebView webView;
    private ProgressBar progress;

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View content = LayoutInflater.from(getActivity()).inflate(R.layout.help_fragment, null);
        webView = (WebView) content.findViewById(R.id.helpFragmentWebView);
        progress = (ProgressBar) content.findViewById(R.id.helpFragmentProgress);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setView(content);
        builder.setNegativeButton(R.string.about_close,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        return builder.create();
    }

    private void loadHelp(final int helpResourceId) {
        // Load asynchronously in case of a very large file.
        loader = new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... params) {
                InputStream rawResource = getActivity().getResources().openRawResource(helpResourceId);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(rawResource));

                String line;
                StringBuilder sb = new StringBuilder();

                try {
                    while ((line = bufferedReader.readLine()) != null) {
                        sb.append(line);
                        sb.append("\n");
                    }
                    bufferedReader.close();
                } catch (IOException e) {
                    // TODO You may want to include some logging here.
                }

                return sb.toString();
            }

            @Override
            protected void onPostExecute(String licensesBody) {
                super.onPostExecute(licensesBody);
                if (getActivity() == null || isCancelled()) {
                    return;
                }
                progress.setVisibility(View.INVISIBLE);
                webView.setVisibility(View.VISIBLE);
                webView.loadDataWithBaseURL(null, licensesBody, "text/html", "utf-8", null);
                loader = null;
            }

        }.execute();
    }
}