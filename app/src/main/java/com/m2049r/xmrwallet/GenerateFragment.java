package com.m2049r.xmrwallet;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class GenerateFragment extends Fragment {

    EditText etWalletName;
    EditText etWalletPassword;
    Button bGenerate;
    LinearLayout llAccept;
    TextView tvWalletMnemonic;
    Button bAccept;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.gen_fragment, container, false);

        etWalletName = (EditText) view.findViewById(R.id.etWalletName);
        etWalletPassword = (EditText) view.findViewById(R.id.etWalletPassword);
        bGenerate = (Button) view.findViewById(R.id.bGenerate);
        llAccept = (LinearLayout) view.findViewById(R.id.llAccept);
        tvWalletMnemonic = (TextView) view.findViewById(R.id.tvWalletMnemonic);
        bAccept = (Button) view.findViewById(R.id.bAccept);

        etWalletName.requestFocus();
        etWalletName.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                setGenerateEnabled();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        etWalletName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(etWalletName, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        etWalletName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    etWalletPassword.requestFocus();
                    return false;
                }
                return false;
            }
        });

        etWalletPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(etWalletPassword, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        etWalletPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                    generateWallet();
                    return false;
                }
                return false;
            }
        });

        bGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 // TODO make keyboard go away
                generateWallet();
            }
        });

        bAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptWallet();
            }
        });

        bAccept.setEnabled(false);

        return view;
    }

    private void generateWallet() {
        String name = etWalletName.getText().toString();
        if (name.length() == 0) return;
        File walletFile = new File(activityCallback.getStorageRoot(), name + ".keys");
        if (walletFile.exists()) {
            Toast.makeText(getActivity(), getString(R.string.generate_wallet_exists), Toast.LENGTH_LONG).show();
            etWalletName.requestFocus();
            return;
        }
        String password = etWalletPassword.getText().toString();
        bGenerate.setEnabled(false);
        activityCallback.onGenerate(name, password);
    }

    private void acceptWallet() {
        String name = etWalletName.getText().toString();
        String password = etWalletPassword.getText().toString();
        bAccept.setEnabled(false);
        activityCallback.onAccept(name, password);
    }

    private void setGenerateEnabled() {
        bGenerate.setEnabled(etWalletName.length() > 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        setGenerateEnabled();
        etWalletName.requestFocus();
        InputMethodManager imgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imgr.showSoftInput(etWalletName, InputMethodManager.SHOW_IMPLICIT);
    }

    public void showMnemonic(String mnemonic) {
        setGenerateEnabled();
        if (mnemonic.length() > 0) {
            tvWalletMnemonic.setText(mnemonic);
            bAccept.setEnabled(true);
        } else {
            tvWalletMnemonic.setText(getActivity().getString(R.string.generate_seed));
            bAccept.setEnabled(false);
        }
    }

    GenerateFragment.Listener activityCallback;

    // Container Activity must implement this interface
    public interface Listener {
        void onGenerate(String name, String password);

        void onAccept(String name, String password);

        File getStorageRoot();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof GenerateFragment.Listener) {
            this.activityCallback = (GenerateFragment.Listener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement Listener");
        }
    }

}
