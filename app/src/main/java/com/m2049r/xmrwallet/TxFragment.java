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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.transition.MaterialContainerTransform;
import com.google.android.material.transition.MaterialElevationScale;
import com.m2049r.xmrwallet.data.Subaddress;
import com.m2049r.xmrwallet.data.UserNotes;
import com.m2049r.xmrwallet.model.TransactionInfo;
import com.m2049r.xmrwallet.model.Transfer;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.ThemeHelper;
import com.m2049r.xmrwallet.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import timber.log.Timber;

public class TxFragment extends Fragment {

    static public final String ARG_INFO = "info";

    private final SimpleDateFormat TS_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    public TxFragment() {
        super();
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone(); //get the local time zone.
        TS_FORMATTER.setTimeZone(tz);
    }

    private TextView tvAccount;
    private TextView tvAddress;
    private TextView tvTxTimestamp;
    private TextView tvTxId;
    private TextView tvTxKey;
    private TextView tvDestination;
    private TextView tvTxPaymentId;
    private TextView tvTxBlockheight;
    private TextView tvTxAmount;
    private TextView tvTxFee;
    private TextView tvTxTransfers;
    private TextView etTxNotes;

    // XMRTO stuff
    private View cvXmrTo;
    private TextView tvTxXmrToKey;
    private TextView tvDestinationBtc;
    private TextView tvTxAmountBtc;
    private TextView tvXmrToSupport;
    private TextView tvXmrToKeyLabel;
    private ImageView tvXmrToLogo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tx_info, container, false);

        final MaterialElevationScale exitTransition = new MaterialElevationScale(false);
        exitTransition.setDuration(getResources().getInteger(R.integer.tx_item_transition_duration));
        setExitTransition(exitTransition);
        final MaterialElevationScale reenterTransition = new MaterialElevationScale(true);
        reenterTransition.setDuration(getResources().getInteger(R.integer.tx_item_transition_duration));
        setReenterTransition(reenterTransition);

        cvXmrTo = view.findViewById(R.id.cvXmrTo);
        tvTxXmrToKey = view.findViewById(R.id.tvTxXmrToKey);
        tvDestinationBtc = view.findViewById(R.id.tvDestinationBtc);
        tvTxAmountBtc = view.findViewById(R.id.tvTxAmountBtc);
        tvXmrToSupport = view.findViewById(R.id.tvXmrToSupport);
        tvXmrToKeyLabel = view.findViewById(R.id.tvXmrToKeyLabel);
        tvXmrToLogo = view.findViewById(R.id.tvXmrToLogo);

        tvAccount = view.findViewById(R.id.tvAccount);
        tvAddress = view.findViewById(R.id.tvAddress);
        tvTxTimestamp = view.findViewById(R.id.tvTxTimestamp);
        tvTxId = view.findViewById(R.id.tvTxId);
        tvTxKey = view.findViewById(R.id.tvTxKey);
        tvDestination = view.findViewById(R.id.tvDestination);
        tvTxPaymentId = view.findViewById(R.id.tvTxPaymentId);
        tvTxBlockheight = view.findViewById(R.id.tvTxBlockheight);
        tvTxAmount = view.findViewById(R.id.tvTxAmount);
        tvTxFee = view.findViewById(R.id.tvTxFee);
        tvTxTransfers = view.findViewById(R.id.tvTxTransfers);
        etTxNotes = view.findViewById(R.id.etTxNotes);

        etTxNotes.setRawInputType(InputType.TYPE_CLASS_TEXT);

        tvTxXmrToKey.setOnClickListener(v -> {
            Helper.clipBoardCopy(getActivity(), getString(R.string.label_copy_xmrtokey), tvTxXmrToKey.getText().toString());
            Toast.makeText(getActivity(), getString(R.string.message_copy_xmrtokey), Toast.LENGTH_SHORT).show();
        });

        info = getArguments().getParcelable(ARG_INFO);
        show();
        return view;
    }

    void shareTxInfo() {
        if (this.info == null) return;
        StringBuffer sb = new StringBuffer();

        sb.append(getString(R.string.tx_timestamp)).append(":\n");
        sb.append(TS_FORMATTER.format(new Date(info.timestamp * 1000))).append("\n\n");

        sb.append(getString(R.string.tx_amount)).append(":\n");
        sb.append((info.direction == TransactionInfo.Direction.Direction_In ? "+" : "-"));
        sb.append(Wallet.getDisplayAmount(info.amount)).append("\n");
        sb.append(getString(R.string.tx_fee)).append(":\n");
        sb.append(Wallet.getDisplayAmount(info.fee)).append("\n\n");

        sb.append(getString(R.string.tx_notes)).append(":\n");
        String oneLineNotes = info.notes.replace("\n", " ; ");
        sb.append(oneLineNotes.isEmpty() ? "-" : oneLineNotes).append("\n\n");

        sb.append(getString(R.string.tx_destination)).append(":\n");
        sb.append(tvDestination.getText()).append("\n\n");

        sb.append(getString(R.string.tx_paymentId)).append(":\n");
        sb.append(info.paymentId).append("\n\n");

        sb.append(getString(R.string.tx_id)).append(":\n");
        sb.append(info.hash).append("\n");
        sb.append(getString(R.string.tx_key)).append(":\n");
        sb.append(info.txKey.isEmpty() ? "-" : info.txKey).append("\n\n");

        sb.append(getString(R.string.tx_blockheight)).append(":\n");
        if (info.isFailed) {
            sb.append(getString(R.string.tx_failed)).append("\n");
        } else if (info.isPending) {
            sb.append(getString(R.string.tx_pending)).append("\n");
        } else {
            sb.append(info.blockheight).append("\n");
        }
        sb.append("\n");

        sb.append(getString(R.string.tx_transfers)).append(":\n");
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
        sb.append("\n\n");

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, null));
    }

    TransactionInfo info = null;
    UserNotes userNotes = null;

    void loadNotes() {
        if ((userNotes == null) || (info.notes == null)) {
            info.notes = activityCallback.getTxNotes(info.hash);
        }
        userNotes = new UserNotes(info.notes);
        etTxNotes.setText(userNotes.note);
    }

    private void setTxColour(int clr) {
        tvTxAmount.setTextColor(clr);
        tvTxFee.setTextColor(clr);
    }

    private void showSubaddressLabel() {
        final Subaddress subaddress = activityCallback.getWalletSubaddress(info.accountIndex, info.addressIndex);
        final Context ctx = getContext();
        Spanned label = Html.fromHtml(ctx.getString(R.string.tx_account_formatted,
                info.accountIndex, info.addressIndex,
                Integer.toHexString(ContextCompat.getColor(ctx, R.color.monerujoGreen) & 0xFFFFFF),
                Integer.toHexString(ContextCompat.getColor(ctx, R.color.monerujoBackground) & 0xFFFFFF),
                subaddress.getDisplayLabel()));
        tvAccount.setText(label);
        tvAccount.setOnClickListener(v -> activityCallback.showSubaddress(v, info.addressIndex));
    }

    private void show() {
        if (info.txKey == null) {
            info.txKey = activityCallback.getTxKey(info.hash);
        }
        if (info.address == null) {
            info.address = activityCallback.getTxAddress(info.accountIndex, info.addressIndex);
        }
        loadNotes();

        showSubaddressLabel();
        tvAddress.setText(info.address);

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

        long realAmount = info.amount;
        tvTxAmount.setText(sign + Wallet.getDisplayAmount(realAmount));

        if ((info.fee > 0)) {
            String fee = Wallet.getDisplayAmount(info.fee);
            tvTxFee.setText(getString(R.string.tx_list_fee, fee));
        } else {
            tvTxFee.setText(null);
            tvTxFee.setVisibility(View.GONE);
        }

        if (info.isFailed) {
            tvTxAmount.setText(getString(R.string.tx_list_amount_failed, Wallet.getDisplayAmount(info.amount)));
            tvTxFee.setText(getString(R.string.tx_list_failed_text));
            setTxColour(ContextCompat.getColor(getContext(), R.color.tx_failed));
        } else if (info.isPending) {
            setTxColour(ContextCompat.getColor(getContext(), R.color.tx_pending));
        } else if (info.direction == TransactionInfo.Direction.Direction_In) {
            setTxColour(ContextCompat.getColor(getContext(), R.color.tx_plus));
        } else {
            setTxColour(ContextCompat.getColor(getContext(), R.color.tx_minus));
        }
        Set<String> destinations = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        StringBuilder dstSb = new StringBuilder();
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
            dstSb.append(info.direction == TransactionInfo.Direction.Direction_In ?
                    activityCallback.getWalletSubaddress(info.accountIndex, info.addressIndex).getAddress() :
                    "-");
        }
        tvTxTransfers.setText(sb.toString());
        tvDestination.setText(dstSb.toString());
        showBtcInfo();
    }

    @SuppressLint("SetTextI18n")
    void showBtcInfo() {
        if (userNotes.xmrtoKey != null) {
            cvXmrTo.setVisibility(View.VISIBLE);
            String key = userNotes.xmrtoKey;
            if ("xmrto".equals(userNotes.xmrtoTag)) { // legacy xmr.to service :(
                key = "xmrto-" + key;
            }
            tvTxXmrToKey.setText(key);
            tvDestinationBtc.setText(userNotes.xmrtoDestination);
            tvTxAmountBtc.setText(userNotes.xmrtoAmount + " " + userNotes.xmrtoCurrency);
            switch (userNotes.xmrtoTag) {
                case "xmrto":
                    tvXmrToSupport.setVisibility(View.GONE);
                    tvXmrToKeyLabel.setVisibility(View.INVISIBLE);
                    tvXmrToLogo.setImageResource(R.drawable.ic_xmrto_logo);
                    break;
                case "side": // defaults in layout - just add underline
                    tvXmrToSupport.setPaintFlags(tvXmrToSupport.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                    tvXmrToSupport.setOnClickListener(v -> {
                        Uri uri = Uri.parse("https://sideshift.ai/orders/" + userNotes.xmrtoKey);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                    });
                    break;
                default:
                    tvXmrToSupport.setVisibility(View.GONE);
                    tvXmrToKeyLabel.setVisibility(View.INVISIBLE);
                    tvXmrToLogo.setVisibility(View.GONE);
            }
        } else {
            cvXmrTo.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        final MaterialContainerTransform transform = new MaterialContainerTransform();
        transform.setDrawingViewId(R.id.fragment_container);
        transform.setDuration(getResources().getInteger(R.integer.tx_item_transition_duration));
        transform.setAllContainerColors(ThemeHelper.getThemedColor(getContext(), android.R.attr.colorBackground));
        setSharedElementEnterTransition(transform);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.tx_info_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    Listener activityCallback;

    public interface Listener {
        Subaddress getWalletSubaddress(int accountIndex, int subaddressIndex);

        String getTxKey(String hash);

        String getTxNotes(String hash);

        boolean setTxNotes(String txId, String txNotes);

        String getTxAddress(int major, int minor);

        void setToolbarButton(int type);

        void setSubtitle(String subtitle);

        void showSubaddress(View view, final int subaddressIndex);

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

    @Override
    public void onPause() {
        if (!etTxNotes.getText().toString().equals(userNotes.note)) { // notes have changed
            // save them
            userNotes.setNote(etTxNotes.getText().toString());
            info.notes = userNotes.txNotes;
            activityCallback.setTxNotes(info.hash, info.notes);
        }
        Helper.hideKeyboard(getActivity());
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume()");
        activityCallback.setSubtitle(getString(R.string.tx_title));
        activityCallback.setToolbarButton(Toolbar.BUTTON_BACK);
        showSubaddressLabel();
    }
}