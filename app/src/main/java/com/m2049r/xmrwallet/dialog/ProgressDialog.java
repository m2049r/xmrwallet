package com.m2049r.xmrwallet.dialog;

/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (C) 2018 m2049r
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.util.Helper;

import java.util.Locale;

import timber.log.Timber;

public class ProgressDialog extends AlertDialog {

    private ProgressBar pbBar;

    private TextView tvMessage;

    private TextView tvProgress;

    private View rlProgressBar, pbCircle;

    static private final String PROGRESS_FORMAT = "%1d/%2d";

    private CharSequence message;
    private int maxValue, progressValue;
    private boolean indeterminate = true;

    public ProgressDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_ledger_progress, null);
        pbCircle = view.findViewById(R.id.pbCircle);
        tvMessage = view.findViewById(R.id.tvMessage);
        rlProgressBar = view.findViewById(R.id.rlProgressBar);
        pbBar = view.findViewById(R.id.pbBar);
        tvProgress = view.findViewById(R.id.tvProgress);
        setView(view);
        setIndeterminate(indeterminate);
        if (maxValue > 0) {
            setMax(maxValue);
        }
        if (progressValue > 0) {
            setProgress(progressValue);
        }
        if (message != null) {
            Timber.d("msg=%s", message);
            setMessage(message);
        }

        super.onCreate(savedInstanceState);

        if (Helper.preventScreenshot()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    public void setProgress(int value, int max) {
        progressValue = value;
        maxValue = max;
        if (pbBar != null) {
            pbBar.setProgress(value);
            pbBar.setMax(max);
            tvProgress.setText(String.format(Locale.getDefault(), PROGRESS_FORMAT, value, maxValue));
        }
    }

    public void setProgress(int value) {
        progressValue = value;
        if (pbBar != null) {
            pbBar.setProgress(value);
            tvProgress.setText(String.format(Locale.getDefault(), PROGRESS_FORMAT, value, maxValue));
        }
    }

    public void setMax(int max) {
        maxValue = max;
        if (pbBar != null) {
            pbBar.setMax(max);
        }
    }

    public void setIndeterminate(boolean indeterminate) {
        if (this.indeterminate != indeterminate) {
            if (rlProgressBar != null) {
                if (indeterminate) {
                    pbCircle.setVisibility(View.VISIBLE);
                    rlProgressBar.setVisibility(View.GONE);
                } else {
                    pbCircle.setVisibility(View.GONE);
                    rlProgressBar.setVisibility(View.VISIBLE);
                }
            }
            this.indeterminate = indeterminate;
        }
    }

    @Override
    public void setMessage(CharSequence message) {
        this.message = message;
        if (tvMessage != null) {
            tvMessage.setText(message);
        }
    }
}
