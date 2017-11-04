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
import android.widget.TextView;
import android.widget.Toast;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.util.Helper;

public class DonationFragment extends DialogFragment {
    static final String TAG = "DonationFragment";

    public static DonationFragment newInstance() {
        return new DonationFragment();
    }

    public static void display(FragmentManager fm) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(TAG);
        if (prev != null) {
            ft.remove(prev);
        }

        DonationFragment.newInstance().show(ft, TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_donation, null);

        ((TextView) view.findViewById(R.id.tvCredits)).setText(Html.fromHtml(getString(R.string.donation_credits)));

        (view.findViewById(R.id.bCopyAddress)).
                setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Helper.clipBoardCopy(getActivity(), getString(R.string.label_copy_address),
                                ((TextView) view.findViewById(R.id.tvWalletAddress)).getText().toString());
                        Toast.makeText(getActivity(), getString(R.string.message_copy_address), Toast.LENGTH_SHORT).show();
                    }
                });

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