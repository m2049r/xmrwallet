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

package com.m2049r.xmrwallet;

import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.m2049r.xmrwallet.model.PendingTransaction;
import com.m2049r.xmrwallet.util.Helper;

import timber.log.Timber;

public class SendSettingsWizardFragment extends SendWizardFragment {

    public static SendSettingsWizardFragment newInstance(Listener listener) {
        SendSettingsWizardFragment instance = new SendSettingsWizardFragment();
        instance.setSendListener(listener);
        return instance;
    }

    Listener sendListener;

    public SendSettingsWizardFragment setSendListener(Listener listener) {
        this.sendListener = listener;
        return this;
    }

    interface Listener {

        void setPriority(final PendingTransaction.Priority priority);

        void setMixin(final int mixin);

        void setNotes(final String notes);
    }

    final static int Mixins[] = {4, 7, 12, 25}; // must match the layout XML
    final static PendingTransaction.Priority Priorities[] =
            {PendingTransaction.Priority.Priority_Default,
                    PendingTransaction.Priority.Priority_Low,
                    PendingTransaction.Priority.Priority_Medium,
                    PendingTransaction.Priority.Priority_High}; // must match the layout XML

    private Spinner sMixin;
    private Spinner sPriority;
    private EditText etNotes;
    private EditText etDummy;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Timber.d("onCreateView() %s", (String.valueOf(savedInstanceState)));

        View view = inflater.inflate(
                R.layout.fragment_send_settings, container, false);

        sMixin = (Spinner) view.findViewById(R.id.sMixin);
        sPriority = (Spinner) view.findViewById(R.id.sPriority);

        etNotes = (EditText) view.findViewById(R.id.etNotes);
        etNotes.setRawInputType(InputType.TYPE_CLASS_TEXT);
        etNotes.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    etDummy.requestFocus();
                    Helper.hideKeyboard(getActivity());
                    return true;
                }
                return false;
            }
        });

        etDummy = (EditText) view.findViewById(R.id.etDummy);
        etDummy.setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        return view;
    }

    @Override
    public boolean onValidateFields() {
        if (sendListener != null) {
            int mixin = Mixins[sMixin.getSelectedItemPosition()];
            int priorityIndex = sPriority.getSelectedItemPosition();
            PendingTransaction.Priority priority = Priorities[priorityIndex];
            sendListener.setPriority(priority);
            sendListener.setMixin(mixin);
            String notes = etNotes.getText().toString();
            sendListener.setNotes(notes);
        }
        return true;
    }

    @Override
    public void onResumeFragment() {
        super.onResumeFragment();
        Timber.d("onResumeFragment()");
        Helper.hideKeyboard(getActivity());
        etDummy.requestFocus();
    }
}
