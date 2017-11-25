package com.m2049r.xmrwallet.widget;


import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.m2049r.xmrwallet.R;

public class NumberPadView extends LinearLayout
        implements View.OnClickListener, View.OnLongClickListener {

    @Override
    public void onClick(final View view) {
        if (listener == null) {
            throw new IllegalArgumentException("NumberPadListener has to be set, use setListener() to set it.");
        }
        switch (view.getId()) {
            case R.id.numberPadPoint:
                listener.onPointPressed();
                break;
            case R.id.numberPadBackSpace:
                listener.onBackSpacePressed();
                break;
            default:
                if (view.getTag() != null) {
                    listener.onDigitPressed(Integer.parseInt(view.getTag().toString()));
                }
        }
    }

    @Override
    public boolean onLongClick(final View view) {
        if (view.getId() == R.id.numberPadBackSpace) {
            listener.onClearAll();
            return true;
        }
        return false;
    }

    public void setListener(final NumberPadListener listener) {
        this.listener = listener;
    }

    public interface NumberPadListener {
        void onDigitPressed(final int digit);

        void onBackSpacePressed();

        void onPointPressed();

        void onClearAll();
    }

    private NumberPadListener listener;

    public NumberPadView(final Context context) {
        this(context, null);
    }

    public NumberPadView(final Context context,
                         @Nullable final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NumberPadView(final Context context, @Nullable final AttributeSet attrs,
                         final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final View view = View.inflate(context, R.layout.view_number_pad, this);
        setOrientation(VERTICAL);
        view.findViewById(R.id.numberPad0).setOnClickListener(this);
        view.findViewById(R.id.numberPad1).setOnClickListener(this);
        view.findViewById(R.id.numberPad2).setOnClickListener(this);
        view.findViewById(R.id.numberPad3).setOnClickListener(this);
        view.findViewById(R.id.numberPad4).setOnClickListener(this);
        view.findViewById(R.id.numberPad5).setOnClickListener(this);
        view.findViewById(R.id.numberPad6).setOnClickListener(this);
        view.findViewById(R.id.numberPad7).setOnClickListener(this);
        view.findViewById(R.id.numberPad8).setOnClickListener(this);
        view.findViewById(R.id.numberPad9).setOnClickListener(this);
        view.findViewById(R.id.numberPadPoint).setOnClickListener(this);
        view.findViewById(R.id.numberPadBackSpace).setOnClickListener(this);
        view.findViewById(R.id.numberPadBackSpace).setOnLongClickListener(this);
    }
}
