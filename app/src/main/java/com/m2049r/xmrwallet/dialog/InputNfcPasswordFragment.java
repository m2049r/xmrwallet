package com.m2049r.xmrwallet.dialog;


import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.m2049r.xmrwallet.R;
import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;

public class InputNfcPasswordFragment extends DialogFragment {

    private String walletName = null;


    static final String TAG = "InputNfcPasswordFragment";

    private TextInputLayout etWalletPassword;

    private static InputNfcPasswordFragment fragment = null;


    public static InputNfcPasswordFragment getInstance() {
        if(fragment ==null){
            fragment = new InputNfcPasswordFragment();
        }

        return fragment;
    }

    public static void display(FragmentManager fm) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        InputNfcPasswordFragment.getInstance().show(ft, TAG);
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_input_nfc_password, null);
        etWalletPassword = (TextInputLayout) view.findViewById(R.id.etWalletPassword);
        etWalletPassword.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                checkPassword();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        etWalletPassword.requestFocus();
        ImageView nfcAnimationImage = (ImageView) view.findViewById(R.id.NfcAnimation);
        nfcAnimationImage.setImageResource(R.drawable.nfc_signal);
        AnimationDrawable nfcAnimation = (AnimationDrawable) nfcAnimationImage.getDrawable();
        nfcAnimation.setOneShot(false);
        nfcAnimation.start();


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        initZxcvbn();
        return builder.create();
    }
    Zxcvbn zxcvbn = new Zxcvbn();
    // initialize zxcvbn engine in background thread
    private void initZxcvbn() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                zxcvbn.measure("");
            }
        }).start();
    }

    private void checkPassword() {
        String password = etWalletPassword.getEditText().getText().toString();
        if (!password.isEmpty()) {
            Strength strength = zxcvbn.measure(password);
            int msg;
            double guessesLog10 = strength.getGuessesLog10();
            if (guessesLog10 < 10)
                msg = R.string.password_weak;
            else if (guessesLog10 < 11)
                msg = R.string.password_fair;
            else if (guessesLog10 < 12)
                msg = R.string.password_good;
            else if (guessesLog10 < 13)
                msg = R.string.password_strong;
            else
                msg = R.string.password_very_strong;
            etWalletPassword.setError(getResources().getString(msg));
        } else {
            etWalletPassword.setError(null);
        }
    }

    //get input nfc password
    public String getPassword(){
        String password = etWalletPassword.getEditText().getText().toString();
        return password;
    }

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }
}
