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

// based on from https://stackoverflow.com/a/45325876 (which did not work for me)

package com.m2049r.xmrwallet.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;

public class CTextInputLayout extends TextInputLayout {
    public CTextInputLayout(Context context) {
        super(context);
    }

    public CTextInputLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CTextInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public int getBaseline() {
        EditText editText = getEditText();
        return editText.getBaseline() - (getMeasuredHeight() - editText.getMeasuredHeight());
    }
}