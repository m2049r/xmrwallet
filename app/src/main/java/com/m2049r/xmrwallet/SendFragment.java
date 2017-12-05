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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
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

import com.m2049r.xmrwallet.data.BarcodeData;
import com.m2049r.xmrwallet.data.PendingTx;
import com.m2049r.xmrwallet.data.TxData;
import com.m2049r.xmrwallet.layout.SpendViewPager;
import com.m2049r.xmrwallet.model.PendingTransaction;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.widget.DotBar;
import com.m2049r.xmrwallet.widget.Toolbar;

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
        long getTotalFunds();

        void onPrepareSend(TxData data);

        boolean verifyWalletPassword(String password);

        void onSend(String notes);

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

    static private int MAX_FALLBACK = Integer.MAX_VALUE;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_send_new, container, false);

        llNavBar = view.findViewById(R.id.llNavBar);
        bDone = (Button) view.findViewById(R.id.bDone);

        dotBar = (DotBar) view.findViewById(R.id.dotBar);
        bPrev = (Button) view.findViewById(R.id.bPrev);
        bNext = (Button) view.findViewById(R.id.bNext);
        arrowPrev = getResources().getDrawable(R.drawable.ic_navigate_prev_white_24dp);
        arrowNext = getResources().getDrawable(R.drawable.ic_navigate_next_white_24dp);

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

    public class SpendPagerAdapter extends FragmentPagerAdapter {
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
            SendWizardFragment fragment = (SendWizardFragment) super.instantiateItem(container, position);
            myFragments.put(position, new WeakReference<>(fragment));
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
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
    }

    @Override
    public TxData getTxData() {
        return new TxData(sendAddress, sendPaymentId, sendAmount, sendMixin, sendPriority);
    }

    @Override
    public String getNotes() {
        return sendNotes;
    }

    private String sendAddress;
    private String sendPaymentId;
    private long sendAmount;
    private PendingTransaction.Priority sendPriority;
    private int sendMixin;
    private String sendNotes;
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

    @Override
    public void setAddress(final String address) {
        sendAddress = address;
    }

    @Override
    public void setPaymentId(final String paymentId) {
        sendPaymentId = paymentId;
    }

    @Override
    public void setAmount(final long amount) {
        sendAmount = amount;
    }

    @Override
    public void setPriority(final PendingTransaction.Priority priority) {
        sendPriority = priority;
    }

    @Override
    public void setMixin(final int mixin) {
        sendMixin = mixin;
    }

    @Override
    public void setNotes(final String notes) {
        sendNotes = notes;
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
        Timber.d("REALLY SEND A %s", getNotes());
        disableNavigation(); // committed - disable all navigation
        activityCallback.onSend(getNotes());
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

    public void onTransactionCreated(PendingTransaction pendingTransaction) {
        //public void onTransactionCreated(TestTransaction pendingTransaction) {
        final SendConfirmWizardFragment confirmFragment = getConfirmFragment();
        if (confirmFragment != null) {
            pendingTx = new PendingTx(pendingTransaction);
            confirmFragment.transactionCreated(pendingTransaction);
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
        final SendConfirmWizardFragment confirmFragment = getConfirmFragment();
        if (confirmFragment != null) {
            confirmFragment.hideProgress();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setCancelable(true).
                    setTitle(getString(R.string.send_create_tx_error_title)).
                    setMessage(errorText).
                    create().
                    show();
        }
    }

    SendConfirmWizardFragment getConfirmFragment() {
        final SendWizardFragment fragment = pagerAdapter.getFragment(SpendPagerAdapter.POS_CONFIRM);
        if (fragment instanceof SendConfirmWizardFragment) {
            return (SendConfirmWizardFragment) fragment;
        } else {
            return null;
        }
    }

    public void onTransactionSent(final String txId) {
        Timber.d("txid=%s", txId);
        pagerAdapter.addSuccess();
        Timber.d("numPages=%d", spendViewPager.getAdapter().getCount());
        spendViewPager.setCurrentItem(SpendPagerAdapter.POS_SUCCESS);
        activityCallback.setToolbarButton(Toolbar.BUTTON_NONE);
    }

    public void onSendTransactionFailed(final String error) {
        Timber.d("error=%s", error);
        committedTx = null;
        Toast.makeText(getContext(), getString(R.string.status_transaction_failed, error), Toast.LENGTH_SHORT).show();
        enableNavigation();
        final SendConfirmWizardFragment fragment = getConfirmFragment();
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
}
