package com.m2049r.xmrwallet.widget;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;
import com.m2049r.xmrwallet.R;
import com.nulabinc.zxcvbn.Zxcvbn;

public class PasswordEntryView extends TextInputLayout implements TextWatcher {
    final private Zxcvbn zxcvbn = new Zxcvbn();

    public PasswordEntryView(@NonNull Context context) {
        super(context, null);
    }

    public PasswordEntryView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, com.google.android.material.R.attr.textInputStyle);
    }

    public PasswordEntryView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void addView(@NonNull View child, int index, @NonNull final ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        final EditText et = getEditText();
        if (et != null)
            et.addTextChangedListener(this);
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String password = s.toString();
        int icon = 0;
        if (!password.isEmpty()) {
            final double strength = Math.min(zxcvbn.measure(password).getGuessesLog10(), 15) / 3 * 20; // 0-100%
            if (strength < 21)
                icon = R.drawable.ic_smiley_sad_filled;
            else if (strength < 40)
                icon = R.drawable.ic_smiley_meh_filled;
            else if (strength < 60)
                icon = R.drawable.ic_smiley_neutral_filled;
            else if (strength < 80)
                icon = R.drawable.ic_smiley_happy_filled;
            else if (strength < 99)
                icon = R.drawable.ic_smiley_ecstatic_filled;
            else
                icon = R.drawable.ic_smiley_gunther_filled;
        }
        setErrorIconDrawable(icon);
        if (icon != 0)
            setError(" ");
        else setError(null);
    }
}
