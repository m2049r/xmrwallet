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
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.Helper;

import java.io.File;

// TODO: somehow show which net we are generating for

public class GenerateFragment extends Fragment {

    EditText etWalletName;
    EditText etWalletPassword;
    Button bGenerate;
    LinearLayout llAccept;
    TextView tvWalletAddress;
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
        tvWalletAddress = (TextView) view.findViewById(R.id.tvWalletAddress);
        tvWalletMnemonic = (TextView) view.findViewById(R.id.tvWalletMnemonic);
        bAccept = (Button) view.findViewById(R.id.bAccept);

        boolean testnet = WalletManager.getInstance().isTestNet();
        tvWalletMnemonic.setTextIsSelectable(testnet);

        etWalletName.requestFocus();
        Helper.showKeyboard(getActivity());
        setGenerateEnabled();
        etWalletName.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                showMnemonic("");
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
                Helper.showKeyboard(getActivity());
            }
        });
        etWalletName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    if (etWalletName.length() > 0) {
                        etWalletPassword.requestFocus();
                    } // otherwise ignore
                    return false;
                }
                return false;
            }
        });

        etWalletPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.showKeyboard(getActivity());
            }
        });
        etWalletPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                showMnemonic("");
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        etWalletPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    Helper.hideKeyboard(getActivity());
                    generateWallet();
                    return false;
                }
                return false;
            }
        });

        bGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.hideKeyboard(getActivity());
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
        llAccept.setVisibility(View.GONE);

        return view;
    }

    private void generateWallet() {
        String name = etWalletName.getText().toString();
        if (name.length() == 0) return;
        String walletPath = Helper.getWalletPath(getActivity(), name);
        if (WalletManager.getInstance().walletExists(walletPath)) {
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

    public void showMnemonic(String mnemonic) {
        setGenerateEnabled();
        if (mnemonic.length() > 0) {
            tvWalletMnemonic.setText(mnemonic);
            bAccept.setEnabled(true);
            llAccept.setVisibility(View.VISIBLE);
        } else {
            if (llAccept.getVisibility() != View.GONE) {
                tvWalletMnemonic.setText(getActivity().getString(R.string.generate_seed));
                bAccept.setEnabled(false);
                llAccept.setVisibility(View.GONE);
            }
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
