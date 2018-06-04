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

package com.m2049r.aeonwallet.fragment.send;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.m2049r.aeonwallet.OnBackPressedListener;
import com.m2049r.aeonwallet.R;
import com.m2049r.aeonwallet.data.BarcodeData;
import com.m2049r.aeonwallet.data.PendingTx;
import com.m2049r.aeonwallet.data.TxData;
import com.m2049r.aeonwallet.layout.SpendViewPager;
import com.m2049r.aeonwallet.model.PendingTransaction;
import com.m2049r.aeonwallet.util.Helper;
import com.m2049r.aeonwallet.util.Notice;
import com.m2049r.aeonwallet.util.UserNotes;
import com.m2049r.aeonwallet.widget.DotBar;
import com.m2049r.aeonwallet.widget.Toolbar;

import java.lang.ref.WeakReference;

import timber.log.Timber;

public class SendFragment extends Fragment
        implements SendAddressWizardFragment.Listener,
        SendAmountWizardFragment.Listener,
        SendSettingsWizardFragment.Listener,
        SendConfirmWizardFragment.Listener,
        SendSuccessWizardFragment.Listener,
        OnBackPressedListener {

    private Listener activityCallback;

    public interface Listener {
        SharedPreferences getPrefs();

        long getTotalFunds();

        void onPrepareSend(String tag, TxData data);

        boolean verifyWalletPassword(String password);

        void onSend(UserNotes notes);

        void onDisposeRequest();

        void onFragmentDone();

        void setToolbarButton(int type);

        void setTitle(String title);

        void setSubtitle(String subtitle);
    }

    private EditText etDummy;
    private Drawable arrowPrev;
    private Drawable arrowNext;

    private View llNavBar;
    private DotBar dotBar;
    private Button bPrev;
    private Button bNext;

    private Button bDone;

    private View llXmrToEnabled;
    private View ibXmrToInfoClose;


    static private int MAX_FALLBACK = Integer.MAX_VALUE;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_send, container, false);

        llNavBar = view.findViewById(R.id.llNavBar);
        bDone = (Button) view.findViewById(R.id.bDone);

        dotBar = (DotBar) view.findViewById(R.id.dotBar);
        bPrev = (Button) view.findViewById(R.id.bPrev);
        bNext = (Button) view.findViewById(R.id.bNext);
        arrowPrev = getResources().getDrawable(R.drawable.ic_navigate_prev_white_24dp);
        arrowNext = getResources().getDrawable(R.drawable.ic_navigate_next_white_24dp);

        ViewGroup llNotice = (ViewGroup) view.findViewById(R.id.llNotice);
        Notice.showAll(llNotice,".*_send");

        spendViewPager = (SpendViewPager) view.findViewById(R.id.pager);
        pagerAdapter = new SpendPagerAdapter(getChildFragmentManager());
        spendViewPager.setOffscreenPageLimit(pagerAdapter.getCount()); // load & keep all pages in cache
        spendViewPager.setAdapter(pagerAdapter);

        spendViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            private int fallbackPosition = MAX_FALLBACK;
            private int currentPosition = 0;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int newPosition) {
                Timber.d("onPageSelected=%d/%d", newPosition, fallbackPosition);
                if (fallbackPosition < newPosition) {
                    spendViewPager.setCurrentItem(fallbackPosition);
                } else {
                    pagerAdapter.getFragment(currentPosition).onPauseFragment();
                    pagerAdapter.getFragment(newPosition).onResumeFragment();
                    updatePosition(newPosition);
                    currentPosition = newPosition;
                    fallbackPosition = MAX_FALLBACK;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                    if (!spendViewPager.validateFields(spendViewPager.getCurrentItem())) {
                        fallbackPosition = spendViewPager.getCurrentItem();
                    } else {
                        fallbackPosition = spendViewPager.getCurrentItem() + 1;
                    }
                }
            }
        });

        bPrev.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                spendViewPager.previous();
            }
        });

        bNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                spendViewPager.next();
            }
        });

        bDone.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Timber.d("bDone.onClick");
                activityCallback.onFragmentDone();
            }
        });

        updatePosition(0);

        etDummy = (EditText) view.findViewById(R.id.etDummy);
        etDummy.setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etDummy.requestFocus();
        Helper.hideKeyboard(getActivity());

        return view;
    }

    void updatePosition(int position) {
        dotBar.setActiveDot(position);
        CharSequence nextLabel = pagerAdapter.getPageTitle(position + 1);
        bNext.setText(nextLabel);
        if (nextLabel != null) {
            bNext.setCompoundDrawablesWithIntrinsicBounds(null, null, arrowNext, null);
        } else {
            bNext.setCompoundDrawables(null, null, null, null);
        }
        CharSequence prevLabel = pagerAdapter.getPageTitle(position - 1);
        bPrev.setText(prevLabel);
        if (prevLabel != null) {
            bPrev.setCompoundDrawablesWithIntrinsicBounds(arrowPrev, null, null, null);
        } else {
            bPrev.setCompoundDrawables(null, null, null, null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume");
        activityCallback.setSubtitle(getString(R.string.send_title));
        if (spendViewPager.getCurrentItem() == SpendPagerAdapter.POS_SUCCESS) {
            activityCallback.setToolbarButton(Toolbar.BUTTON_NONE);
        } else {
            activityCallback.setToolbarButton(Toolbar.BUTTON_CANCEL);
        }
    }

    @Override
    public void onAttach(Context context) {
        Timber.d("onAttach %s", context);
        super.onAttach(context);
        if (context instanceof Listener) {
            this.activityCallback = (Listener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement Listener");
        }
    }

    private SpendViewPager spendViewPager;
    private SpendPagerAdapter pagerAdapter;

    @Override
    public boolean onBackPressed() {
        if (isComitted()) return true; // no going back
        if (spendViewPager.getCurrentItem() == 0) {
            return false;
        } else {
            spendViewPager.previous();
            return true;
        }
    }

    enum Mode {
        XMR
    }

    Mode mode = Mode.XMR;

    @Override
    public void setMode(Mode aMode) {
        if (mode != aMode) {
            mode = aMode;
            switch (aMode) {
                case XMR:
                    txData = new TxData();
                    break;
                default:
                    throw new IllegalArgumentException("Mode " + String.valueOf(aMode) + " unknown!");
            }
            getView().post(new Runnable() {
                @Override
                public void run() {
                    pagerAdapter.notifyDataSetChanged();
                }
            });
            Timber.d("New Mode = %s", mode.toString());
        }
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    public class SpendPagerAdapter extends FragmentStatePagerAdapter {
        private static final int POS_ADDRESS = 0;
        private static final int POS_AMOUNT = 1;
        private static final int POS_SETTINGS = 2;
        private static final int POS_CONFIRM = 3;
        private static final int POS_SUCCESS = 4;
        private int numPages = 4;

        SparseArray<WeakReference<SendWizardFragment>> myFragments = new SparseArray<>();

        public SpendPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void addSuccess() {
            numPages++;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return numPages;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Timber.d("instantiateItem %d", position);
            SendWizardFragment fragment = (SendWizardFragment) super.instantiateItem(container, position);
            myFragments.put(position, new WeakReference<>(fragment));
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            Timber.d("destroyItem %d", position);
            myFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public SendWizardFragment getFragment(int position) {
            WeakReference ref = myFragments.get(position);
            if (ref != null)
                return myFragments.get(position).get();
            else
                return null;
        }

        @Override
        public SendWizardFragment getItem(int position) {
            Timber.d("getItem(%d) CREATE", position);
            Timber.d("Mode=%s", mode.toString());
            if (mode == Mode.XMR) {
                switch (position) {
                    case POS_ADDRESS:
                        return SendAddressWizardFragment.newInstance(SendFragment.this);
                    case POS_AMOUNT:
                        return SendAmountWizardFragment.newInstance(SendFragment.this);
                    case POS_SETTINGS:
                        return SendSettingsWizardFragment.newInstance(SendFragment.this);
                    case POS_CONFIRM:
                        return SendConfirmWizardFragment.newInstance(SendFragment.this);
                    case POS_SUCCESS:
                        return SendSuccessWizardFragment.newInstance(SendFragment.this);
                    default:
                        throw new IllegalArgumentException("no such send position(" + position + ")");
                }
            } else {
                throw new IllegalStateException("Unknown mode!");
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Timber.d("getPageTitle(%d)", position);
            if (position >= numPages) return null;
            switch (position) {
                case POS_ADDRESS:
                    return getString(R.string.send_address_title);
                case POS_AMOUNT:
                    return getString(R.string.send_amount_title);
                case POS_SETTINGS:
                    return getString(R.string.send_settings_title);
                case POS_CONFIRM:
                    return getString(R.string.send_confirm_title);
                case POS_SUCCESS:
                    return getString(R.string.send_success_title);
                default:
                    return null;
            }
        }

        @Override
        public int getItemPosition(Object object) {
            Timber.d("getItemPosition %s", String.valueOf(object));
            if ((object instanceof SendAddressWizardFragment) || (object instanceof SendSettingsWizardFragment)) {
                return POSITION_UNCHANGED;
            } else {
                return POSITION_NONE;
            }
        }
    }

    @Override
    public TxData getTxData() {
        return txData;
    }

    private TxData txData = new TxData();

    private BarcodeData barcodeData;

    // Listeners
    @Override
    public void setBarcodeData(BarcodeData data) {
        barcodeData = data;
    }

    @Override
    public BarcodeData popBarcodeData() {
        BarcodeData data = barcodeData;
        barcodeData = null;
        return data;
    }

    boolean isComitted() {
        return committedTx != null;
    }

    PendingTx committedTx;

    @Override
    public PendingTx getCommittedTx() {
        return committedTx;
    }


    @Override
    public void commitTransaction() {
        Timber.d("REALLY SEND");
        disableNavigation(); // committed - disable all navigation
        activityCallback.onSend(txData.getUserNotes());
        committedTx = pendingTx;
    }

    void disableNavigation() {
        spendViewPager.allowSwipe(false);
    }

    void enableNavigation() {
        spendViewPager.allowSwipe(true);
    }

    @Override
    public void enableDone() {
        llNavBar.setVisibility(View.INVISIBLE);
        bDone.setVisibility(View.VISIBLE);
    }

    public Listener getActivityCallback() {
        return activityCallback;
    }


    // callbacks from send service

    public void onTransactionCreated(final String txTag, final PendingTransaction pendingTransaction) {
        final SendConfirm confirm = getSendConfirm();
        if (confirm != null) {
            pendingTx = new PendingTx(pendingTransaction);
            confirm.transactionCreated(txTag, pendingTransaction);
        } else {
            // not in confirm fragment => dispose & move on
            disposeTransaction();
        }
    }

    @Override
    public void disposeTransaction() {
        pendingTx = null;
        activityCallback.onDisposeRequest();
    }

    PendingTx pendingTx;

    public PendingTx getPendingTx() {
        return pendingTx;
    }

    public void onCreateTransactionFailed(String errorText) {
        final SendConfirm confirm = getSendConfirm();
        if (confirm != null) {
            confirm.createTransactionFailed(errorText);
        }
    }

    SendConfirm getSendConfirm() {
        final SendWizardFragment fragment = pagerAdapter.getFragment(SpendPagerAdapter.POS_CONFIRM);
        if (fragment instanceof SendConfirm) {
            return (SendConfirm) fragment;
        } else {
            return null;
        }
    }

    public void onTransactionSent(final String txId) {
        Timber.d("txid=%s", txId);
        pagerAdapter.addSuccess();
        Timber.d("numPages=%d", spendViewPager.getAdapter().getCount());
        activityCallback.setToolbarButton(Toolbar.BUTTON_NONE);
        spendViewPager.setCurrentItem(SpendPagerAdapter.POS_SUCCESS);
    }

    public void onSendTransactionFailed(final String error) {
        Timber.d("error=%s", error);
        committedTx = null;
        Toast.makeText(getContext(), getString(R.string.status_transaction_failed, error), Toast.LENGTH_SHORT).show();
        enableNavigation();
        final SendConfirm fragment = getSendConfirm();
        if (fragment != null) {
            fragment.sendFailed();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.send_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    // xmr.to info box
    private static final String PREF_SHOW_XMRTO_ENABLED = "info_xmrto_enabled_send";

    boolean showXmrtoEnabled = true;

    void loadPrefs() {
        SharedPreferences sharedPref = activityCallback.getPrefs();
        showXmrtoEnabled = sharedPref.getBoolean(PREF_SHOW_XMRTO_ENABLED, true);
    }

    void saveXmrToPrefs() {
        SharedPreferences sharedPref = activityCallback.getPrefs();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(PREF_SHOW_XMRTO_ENABLED, showXmrtoEnabled);
        editor.apply();
    }

}
