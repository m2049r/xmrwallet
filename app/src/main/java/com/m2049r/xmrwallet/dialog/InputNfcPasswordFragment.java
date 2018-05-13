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

import android.nfc.*;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.tech.*;
import android.nfc.tech.Ndef;
import android.widget.Toast;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;

public class InputNfcPasswordFragment extends DialogFragment {

    private String walletName = null;


    static final String TAG = "InputNfcPasswordFragment";

    private TextInputLayout etWalletPassword;

    private static InputNfcPasswordFragment fragment = null;

//    protected NfcAdapter nfcAdapter;
//    protected PendingIntent pendingIntent;
//    protected IntentFilter[] mFilters;
//    protected String[][] mTechLists;

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
        //initNfc();
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
//            Strength strength = zxcvbn.measure(password);
//            int msg;
//            double guessesLog10 = strength.getGuessesLog10();
//            if (guessesLog10 < 10)
//                msg = R.string.password_weak;
//            else if (guessesLog10 < 11)
//                msg = R.string.password_fair;
//            else if (guessesLog10 < 12)
//                msg = R.string.password_good;
//            else if (guessesLog10 < 13)
//                msg = R.string.password_strong;
//            else
//                msg = R.string.password_very_strong;
//            etWalletPassword.setError(getResources().getString(msg));
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

//    protected void initNfc() {
//
//        nfcAdapter = NfcAdapter.getDefaultAdapter(this.getContext());
//        ifNFCSupport();
//        // 将被调用的Intent，用于重复被Intent触发后将要执行的跳转
//        pendingIntent = PendingIntent.getActivity(this.getContext(), 0,
//                new Intent(this.getContext(), getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
//        mTechLists = new String[][] { new String[] { NfcA.class.getName() }, new String[] { NfcF.class.getName() },
//                new String[] { NfcB.class.getName() }, new String[] { NfcV.class.getName() } };// 允许扫描的标签类型
//
//
//    }

    /**
     * 检测工作,判断设备的NFC支持情况
     *
     * @return
     */
//    private boolean ifNFCSupport() {
//        if (nfcAdapter == null) {
//            Toast.makeText(this.getContext(), "NO NFC FOUND!", Toast.LENGTH_SHORT).show();
//            return false;
//        }
//        if (nfcAdapter != null && !nfcAdapter.isEnabled()) {
//            Toast.makeText(this.getContext(), "Please open NFC first!", Toast.LENGTH_SHORT).show();
//            return false;
//        }
//        return true;
//    }

//    @Override
//    public void onResume() {
//        super.onResume();
//
//        nfcAdapter.enableForegroundDispatch(this.getActivity(), pendingIntent, mFilters, mTechLists);
//
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        nfcAdapter.disableForegroundDispatch(this.getActivity());
//    }

//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())||
//                NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
//            readOrWriteTag(intent);
//        }
//
//    }
}
