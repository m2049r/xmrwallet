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

package com.m2049r.xmrwallet.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.m2049r.xmrwallet.R;

public class SendProgressView extends LinearLayout {

    public SendProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeViews(context);
    }

    public SendProgressView(Context context,
                            AttributeSet attrs,
                            int defStyle) {
        super(context, attrs, defStyle);
        initializeViews(context);
    }

    private void initializeViews(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_send_progress, this);
    }


    View pbProgress;
    View llMessage;
    TextView tvCode;
    TextView tvMessage;
    TextView tvSolution;

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        pbProgress = findViewById(R.id.pbProgress);
        llMessage = findViewById(R.id.llMessage);
        tvCode = findViewById(R.id.tvCode);
        tvMessage = findViewById(R.id.tvMessage);
        tvSolution = findViewById(R.id.tvSolution);
    }

    public void showProgress(String progressText) {
        pbProgress.setVisibility(VISIBLE);
        tvCode.setVisibility(INVISIBLE);
        tvMessage.setText(progressText);
        llMessage.setVisibility(VISIBLE);
        tvSolution.setVisibility(INVISIBLE);
    }

    public void hideProgress() {
        pbProgress.setVisibility(INVISIBLE);
        llMessage.setVisibility(INVISIBLE);
    }

    public void showMessage(String code, String message, String solution) {
        tvCode.setText(code);
        tvMessage.setText(message);
        tvSolution.setText(solution);
        tvCode.setVisibility(VISIBLE);
        llMessage.setVisibility(VISIBLE);
        tvSolution.setVisibility(VISIBLE);
        pbProgress.setVisibility(INVISIBLE);
    }
}