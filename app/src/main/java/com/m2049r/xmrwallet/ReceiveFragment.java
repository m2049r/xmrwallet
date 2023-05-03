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

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.transition.MaterialContainerTransform;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.m2049r.xmrwallet.data.BarcodeData;
import com.m2049r.xmrwallet.data.Crypto;
import com.m2049r.xmrwallet.data.Subaddress;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.ThemeHelper;
import com.m2049r.xmrwallet.widget.ExchangeView;
import com.m2049r.xmrwallet.widget.Toolbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import timber.log.Timber;

public class ReceiveFragment extends Fragment {

    private ProgressBar pbProgress;
    private TextView tvAddress;
    private TextInputLayout etNotes;
    private ExchangeView evAmount;
    private TextView tvQrCode;
    private ImageView ivQrCode;
    private ImageView ivQrCodeFull;
    private EditText etDummy;
    private ImageButton bCopyAddress;
    private MenuItem shareItem;

    private Wallet wallet = null;
    private boolean isMyWallet = false;

    public interface Listener {
        void setToolbarButton(int type);

        void setTitle(String title);

        void setSubtitle(String subtitle);

        void showSubaddresses(boolean managerMode);

        Subaddress getSelectedSubaddress();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_receive, container, false);

        pbProgress = view.findViewById(R.id.pbProgress);
        tvAddress = view.findViewById(R.id.tvAddress);
        etNotes = view.findViewById(R.id.etNotes);
        evAmount = view.findViewById(R.id.evAmount);
        ivQrCode = view.findViewById(R.id.qrCode);
        tvQrCode = view.findViewById(R.id.tvQrCode);
        ivQrCodeFull = view.findViewById(R.id.qrCodeFull);
        etDummy = view.findViewById(R.id.etDummy);
        bCopyAddress = view.findViewById(R.id.bCopyAddress);

        etDummy.setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        bCopyAddress.setOnClickListener(v -> copyAddress());

        evAmount.setOnNewAmountListener(xmr -> {
            Timber.d("new amount = %s", xmr);
            generateQr();
            if (shareRequested && (xmr != null)) share();
        });

        evAmount.setOnFailedExchangeListener(() -> {
            if (isAdded()) {
                clearQR();
                Toast.makeText(getActivity(), getString(R.string.message_exchange_failed), Toast.LENGTH_LONG).show();
            }
        });

        final EditText notesEdit = etNotes.getEditText();
        notesEdit.setRawInputType(InputType.TYPE_CLASS_TEXT);
        notesEdit.setOnEditorActionListener((v, actionId, event) -> {
            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN))
                    || (actionId == EditorInfo.IME_ACTION_DONE)) {
                generateQr();
                return true;
            }
            return false;
        });
        notesEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearQR();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        tvAddress.setOnClickListener(v -> {
            listenerCallback.showSubaddresses(false);
        });

        view.findViewById(R.id.cvQrCode).setOnClickListener(v -> {
            Helper.hideKeyboard(getActivity());
            etDummy.requestFocus();
            if (qrValid) {
                ivQrCodeFull.setImageBitmap(((BitmapDrawable) ivQrCode.getDrawable()).getBitmap());
                ivQrCodeFull.setVisibility(View.VISIBLE);
            } else {
                evAmount.doExchange();
            }
        });

        ivQrCodeFull.setOnClickListener(v -> {
            ivQrCodeFull.setImageBitmap(null);
            ivQrCodeFull.setVisibility(View.GONE);
        });

        showProgress();
        clearQR();

        if (getActivity() instanceof GenerateReviewFragment.ListenerWithWallet) {
            wallet = ((GenerateReviewFragment.ListenerWithWallet) getActivity()).getWallet();
            show();
        } else {
            throw new IllegalStateException("no wallet info");
        }

        return view;
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

    private boolean shareRequested = false;

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.receive_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);

        shareItem = menu.findItem(R.id.menu_item_share);
        shareItem.setOnMenuItemClickListener(item -> {
            if (shareRequested) return true;
            shareRequested = true;
            if (!qrValid) {
                evAmount.doExchange();
            } else {
                share();
            }
            return true;
        });
    }

    private void share() {
        shareRequested = false;
        if (saveQrCode()) {
            final Intent sendIntent = getSendIntent();
            if (sendIntent != null)
                startActivity(Intent.createChooser(sendIntent, null));
        } else {
            Toast.makeText(getActivity(), getString(R.string.message_qr_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean saveQrCode() {
        if (!qrValid) throw new IllegalStateException("trying to save null qr code!");

        File cachePath = new File(getActivity().getCacheDir(), "images");
        if (!cachePath.exists())
            if (!cachePath.mkdirs()) throw new IllegalStateException("cannot create images folder");
        File png = new File(cachePath, "QR.png");
        try {
            FileOutputStream stream = new FileOutputStream(png);
            Bitmap qrBitmap = ((BitmapDrawable) ivQrCode.getDrawable()).getBitmap();
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();
            return true;
        } catch (IOException ex) {
            Timber.e(ex);
            // make sure we don't share an old qr code
            if (!png.delete()) throw new IllegalStateException("cannot delete old qr code");
            // if we manage to delete it, the URI points to nothing and the user gets a toast with the error
        }
        return false;
    }

    private Intent getSendIntent() {
        File imagePath = new File(requireActivity().getCacheDir(), "images");
        File png = new File(imagePath, "QR.png");
        Uri contentUri = FileProvider.getUriForFile(requireActivity(), BuildConfig.APPLICATION_ID + ".fileprovider", png);
        if (contentUri != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
            shareIntent.setTypeAndNormalize("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            if (bcData != null)
                shareIntent.putExtra(Intent.EXTRA_TEXT, bcData.getUriString());
            return shareIntent;
        }
        return null;
    }

    void copyAddress() {
        Helper.clipBoardCopy(requireActivity(), getString(R.string.label_copy_address), subaddress.getAddress());
        Toast.makeText(getActivity(), getString(R.string.message_copy_address), Toast.LENGTH_SHORT).show();
    }

    private boolean qrValid = false;

    void clearQR() {
        if (qrValid) {
            ivQrCode.setImageBitmap(null);
            qrValid = false;
            if (isLoaded)
                tvQrCode.setVisibility(View.VISIBLE);
        }
    }

    void setQR(Bitmap qr) {
        ivQrCode.setImageBitmap(qr);
        qrValid = true;
        tvQrCode.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume()");
        listenerCallback.setToolbarButton(Toolbar.BUTTON_BACK);
        if (wallet != null) {
            listenerCallback.setTitle(wallet.getName());
            listenerCallback.setSubtitle(wallet.getAccountLabel());
            setNewSubaddress();
        } else {
            listenerCallback.setSubtitle(getString(R.string.status_wallet_loading));
            clearQR();
        }
    }

    private boolean isLoaded = false;

    private void show() {
        Timber.d("name=%s", wallet.getName());
        isLoaded = true;
        hideProgress();
    }

    public BarcodeData getBarcodeData() {
        if (qrValid)
            return bcData;
        else
            return null;
    }

    private BarcodeData bcData = null;

    private void generateQr() {
        Timber.d("GENQR");
        String address = subaddress.getAddress();
        String notes = etNotes.getEditText().getText().toString();
        String xmrAmount = evAmount.getAmount();
        Timber.d("%s/%s/%s", xmrAmount, notes, address);
        if ((xmrAmount == null) || !Wallet.isAddressValid(address)) {
            clearQR();
            Timber.d("CLEARQR");
            return;
        }
        bcData = new BarcodeData(Crypto.XMR, address, notes, xmrAmount);
        int size = Math.max(ivQrCode.getWidth(), ivQrCode.getHeight());
        Bitmap qr = generate(bcData.getUriString(), size, size);
        if (qr != null) {
            setQR(qr);
            Timber.d("SETQR");
            etDummy.requestFocus();
            Helper.hideKeyboard(getActivity());
        }
    }

    public Bitmap generate(String text, int width, int height) {
        if ((width <= 0) || (height <= 0)) return null;
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        try {
            BitMatrix bitMatrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints);
            int[] pixels = new int[width * height];
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    if (bitMatrix.get(j, i)) {
                        pixels[i * width + j] = 0x00000000;
                    } else {
                        pixels[i * height + j] = 0xffffffff;
                    }
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.RGB_565);
            bitmap = addLogo(bitmap);
            return bitmap;
        } catch (WriterException ex) {
            Timber.e(ex);
        }
        return null;
    }

    private Bitmap addLogo(Bitmap qrBitmap) {
        // addume logo & qrcode are both square
        Bitmap logo = getMoneroLogo();
        final int qrSize = qrBitmap.getWidth();
        final int logoSize = logo.getWidth();

        Bitmap logoBitmap = Bitmap.createBitmap(qrSize, qrSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(logoBitmap);
        canvas.drawBitmap(qrBitmap, 0, 0, null);
        canvas.save();
        final float sx = 0.2f * qrSize / logoSize;
        canvas.scale(sx, sx, qrSize / 2f, qrSize / 2f);
        canvas.drawBitmap(logo, (qrSize - logoSize) / 2f, (qrSize - logoSize) / 2f, null);
        canvas.restore();
        return logoBitmap;
    }

    private Bitmap logo = null;

    private Bitmap getMoneroLogo() {
        if (logo == null) {
            logo = Helper.getBitmap(getContext(), R.drawable.ic_monerujo_qr);
        }
        return logo;
    }

    public void showProgress() {
        pbProgress.setVisibility(View.VISIBLE);
    }

    public void hideProgress() {
        pbProgress.setVisibility(View.GONE);
    }

    Listener listenerCallback = null;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            this.listenerCallback = (Listener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement Listener");
        }
    }

    @Override
    public void onPause() {
        Timber.d("onPause()");
        Helper.hideKeyboard(getActivity());
        super.onPause();
    }

    @Override
    public void onDetach() {
        Timber.d("onDetach()");
        if ((wallet != null) && (isMyWallet)) {
            wallet.close();
            wallet = null;
            isMyWallet = false;
        }
        super.onDetach();
    }

    private Subaddress subaddress = null;

    void setNewSubaddress() {
        final Subaddress newSubaddress = listenerCallback.getSelectedSubaddress();
        if (!Objects.equals(subaddress, newSubaddress)) {
            final Runnable resetSize = () -> tvAddress.animate().setDuration(125).scaleX(1).scaleY(1).start();
            tvAddress.animate().alpha(1).setDuration(125)
                    .scaleX(1.2f).scaleY(1.2f)
                    .withEndAction(resetSize).start();
        }
        subaddress = newSubaddress;
        final Context context = getContext();
        Spanned label = Html.fromHtml(context.getString(R.string.receive_subaddress,
                Integer.toHexString(ThemeHelper.getThemedColor(context, R.attr.positiveColor) & 0xFFFFFF),
                Integer.toHexString(ThemeHelper.getThemedColor(context, android.R.attr.colorBackground) & 0xFFFFFF),
                subaddress.getDisplayLabel(), subaddress.getAddress()));
        tvAddress.setText(label);
        generateQr();
    }
}
