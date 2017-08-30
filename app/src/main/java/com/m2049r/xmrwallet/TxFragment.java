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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.m2049r.xmrwallet.model.TransactionInfo;
import com.m2049r.xmrwallet.model.Transfer;
import com.m2049r.xmrwallet.model.Wallet;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

public class TxFragment extends Fragment {
    static final String TAG = "TxFragment";

    static public final String ARG_INFO = "info";

    private final SimpleDateFormat TS_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    public TxFragment() {
        super();
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone(); //get the local time zone.
        TS_FORMATTER.setTimeZone(tz);
    }

    TextView tvTxTimestamp;
    TextView tvTxId;
    TextView tvTxKey;
    TextView tvDestination;
    TextView tvTxPaymentId;
    TextView tvTxBlockheight;
    TextView tvTxAmount;
    TextView tvTxFee;
    TextView tvTxTransfers;
    TextView etTxNotes;
    Button bCopy;
    Button bTxNotes;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.tx_fragment, container, false);

        tvTxTimestamp = (TextView) view.findViewById(R.id.tvTxTimestamp);
        tvTxId = (TextView) view.findViewById(R.id.tvTxId);
        tvTxKey = (TextView) view.findViewById(R.id.tvTxKey);
        tvDestination = (TextView) view.findViewById(R.id.tvDestination);
        tvTxPaymentId = (TextView) view.findViewById(R.id.tvTxPaymentId);
        tvTxBlockheight = (TextView) view.findViewById(R.id.tvTxBlockheight);
        tvTxAmount = (TextView) view.findViewById(R.id.tvTxAmount);
        tvTxFee = (TextView) view.findViewById(R.id.tvTxFee);
        tvTxTransfers = (TextView) view.findViewById(R.id.tvTxTransfers);
        etTxNotes = (TextView) view.findViewById(R.id.etTxNotes);
        bCopy = (Button) view.findViewById(R.id.bCopy);
        bTxNotes = (Button) view.findViewById(R.id.bTxNotes);

        etTxNotes.setRawInputType(InputType.TYPE_CLASS_TEXT);

        bCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard();
            }
        });

        bTxNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                info.notes = null; // force reload on next view
                bTxNotes.setEnabled(false);
                etTxNotes.setEnabled(false);
                activityCallback.onSetNote(info.hash, etTxNotes.getText().toString());
            }
        });

        Bundle args = getArguments();
        TransactionInfo info = args.getParcelable(ARG_INFO);
        show(info);
        return view;
    }

    public void onNotesSet(boolean reload) {
        bTxNotes.setEnabled(true);
        etTxNotes.setEnabled(true);
        if (reload) {
            loadNotes(this.info);
        }
    }

    void copyToClipboard() {
        if (this.info == null) return;
        StringBuffer sb = new StringBuffer();
        sb.append(getString(R.string.tx_address)).append(": ");
        sb.append(activityCallback.getWalletAddress()).append("\n");
        sb.append(getString(R.string.tx_id)).append(": ");
        sb.append(info.hash).append("\n");
        sb.append(getString(R.string.tx_key)).append(": ");
        sb.append(info.txKey.isEmpty() ? "-" : info.txKey).append("\n");
        sb.append(getString(R.string.tx_paymentId)).append(": ");
        sb.append(info.paymentId).append("\n");
        sb.append(getString(R.string.tx_amount)).append(": ");
        sb.append((info.direction == TransactionInfo.Direction.Direction_In ? "+" : "-"));
        sb.append(Wallet.getDisplayAmount(info.amount)).append("\n");
        sb.append(getString(R.string.tx_fee)).append(": ");
        sb.append(Wallet.getDisplayAmount(info.fee)).append("\n");
        sb.append(getString(R.string.tx_notes)).append(": ");
        String oneLineNotes = info.notes.replace("\n", " ; ");
        sb.append(oneLineNotes.isEmpty() ? "-" : oneLineNotes).append("\n");
        sb.append(getString(R.string.tx_timestamp)).append(": ");
        sb.append(TS_FORMATTER.format(new Date(info.timestamp * 1000))).append("\n");
        sb.append(getString(R.string.tx_blockheight)).append(": ");
        if (info.isFailed) {
            sb.append(getString(R.string.tx_failed)).append("\n");
        } else if (info.isPending) {
            sb.append(getString(R.string.tx_pending)).append("\n");
        } else {
            sb.append(info.blockheight).append("\n");
        }
        sb.append(getString(R.string.tx_transfers)).append(": ");
        if (info.transfers != null) {
            boolean comma = false;
            for (Transfer transfer : info.transfers) {
                if (comma) {
                    sb.append(", ");
                } else {
                    comma = true;
                }
                sb.append(transfer.address).append(": ");
                sb.append(Wallet.getDisplayAmount(transfer.amount));
            }
        } else {
            sb.append("-");
        }
        sb.append("\n");
        ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.tx_copy_label), sb.toString());
        clipboardManager.setPrimaryClip(clip);
        Toast.makeText(getActivity(), getString(R.string.tx_copy_message), Toast.LENGTH_SHORT).show();
        //Log.d(TAG, sb.toString());
    }

    TransactionInfo info = null;

    void loadNotes(TransactionInfo info) {
        if (info.notes == null) {
            info.notes = activityCallback.getTxNotes(info.hash);
            //Log.d(TAG, "NOTES:" + info.notes + ":");
        }
        etTxNotes.setText(info.notes);
    }

    private void show(TransactionInfo info) {
        if (info.txKey == null) {
            info.txKey = activityCallback.getTxKey(info.hash);
            //Log.d(TAG, "TXKEY:" + info.txKey + ":");
        }
        loadNotes(info);
        tvTxTimestamp.setText(TS_FORMATTER.format(new Date(info.timestamp * 1000)));
        tvTxId.setText(info.hash);
        tvTxKey.setText(info.txKey.isEmpty() ? "-" : info.txKey);
        tvTxPaymentId.setText(info.paymentId);
        if (info.isFailed) {
            tvTxBlockheight.setText(getString(R.string.tx_failed));
        } else if (info.isPending) {
            tvTxBlockheight.setText(getString(R.string.tx_pending));
        } else {
            tvTxBlockheight.setText("" + info.blockheight);
        }
        String sign = (info.direction == TransactionInfo.Direction.Direction_In ? "+" : "-");
        tvTxAmount.setText(sign + Wallet.getDisplayAmount(info.amount));
        tvTxFee.setText(Wallet.getDisplayAmount(info.fee));
        Set<String> destinations = new HashSet<>();
        StringBuffer sb = new StringBuffer();
        StringBuffer dstSb = new StringBuffer();
        if (info.transfers != null) {
            boolean newline = false;
            for (Transfer transfer : info.transfers) {
                destinations.add(transfer.address);
                if (newline) {
                    sb.append("\n");
                } else {
                    newline = true;
                }
                sb.append("[").append(transfer.address.substring(0, 6)).append("] ");
                sb.append(Wallet.getDisplayAmount(transfer.amount));
            }
            newline = false;
            for (String dst : destinations) {
                if (newline) {
                    dstSb.append("\n");
                } else {
                    newline = true;
                }
                dstSb.append(dst);
            }
        } else {
            sb.append("-");
            dstSb.append(info.direction == TransactionInfo.Direction.Direction_In ? activityCallback.getWalletAddress() : "-");
        }
        tvTxTransfers.setText(sb.toString());
        tvDestination.setText(dstSb.toString());
        this.info = info;
        bCopy.setEnabled(true);
    }

    TxFragment.Listener activityCallback;

    public interface Listener {
        String getWalletAddress();

        String getTxKey(String hash);

        String getTxNotes(String hash);

        void onSetNote(String txId, String notes);

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof TxFragment.Listener) {
            this.activityCallback = (TxFragment.Listener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement Listener");
        }
    }
}