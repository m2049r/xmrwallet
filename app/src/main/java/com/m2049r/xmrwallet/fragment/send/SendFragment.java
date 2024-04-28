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

package com.m2049r.xmrwallet.fragment.send;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.transition.MaterialContainerTransform;
import com.m2049r.xmrwallet.OnUriScannedListener;
import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.WalletActivity;
import com.m2049r.xmrwallet.data.BarcodeData;
import com.m2049r.xmrwallet.data.PendingTx;
import com.m2049r.xmrwallet.data.TxData;
import com.m2049r.xmrwallet.data.TxDataBtc;
import com.m2049r.xmrwallet.data.UserNotes;
import com.m2049r.xmrwallet.layout.SpendViewPager;
import com.m2049r.xmrwallet.model.PendingTransaction;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.Notice;
import com.m2049r.xmrwallet.util.ThemeHelper;
import com.m2049r.xmrwallet.widget.DotBar;
import com.m2049r.xmrwallet.widget.Toolbar;

import java.lang.ref.WeakReference;

import timber.log.Timber;

public class SendFragment extends Fragment
        implements SendAddressWizardFragment.Listener,
        SendAmountWizardFragment.Listener,
        SendConfirmWizardFragment.Listener,
        SendSuccessWizardFragment.Listener,
        OnUriScannedListener {

    final static public int MIXIN = 0;

    private Listener activityCallback;

    public interface Listener {
        SharedPreferences getPrefs();

        long getTotalFunds();

        boolean isStreetMode();

        void onPrepareSend(String tag, TxData data);

        String getWalletName();

        void onSend(UserNotes notes);

        void onDisposeRequest();

        void onFragmentDone();

        void setToolbarButton(int type);

        void setTitle(String title);

        void setSubtitle(String subtitle);

        void setOnUriScannedListener(OnUriScannedListener onUriScannedListener);
    }

    private View llNavBar;
    private DotBar dotBar;
    private Button bPrev;
    private Button bNext;

    private Button bDone;

    static private final int MAX_FALLBACK = Integer.MAX_VALUE;

    public static SendFragment newInstance(String uri) {
        SendFragment f = new SendFragment();
        Bundle args = new Bundle();
        args.putString(WalletActivity.REQUEST_URI, uri);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_send, container, false);

        llNavBar = view.findViewById(R.id.llNavBar);
        bDone = view.findViewById(R.id.bDone);

        dotBar = view.findViewById(R.id.dotBar);
        bPrev = view.findViewById(R.id.bPrev);
        bNext = view.findViewById(R.id.bNext);

        ViewGroup llNotice = view.findViewById(R.id.llNotice);
        Notice.showAll(llNotice, ".*_send");

        spendViewPager = view.findViewById(R.id.pager);
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

        bPrev.setOnClickListener(v -> spendViewPager.previous());

        bNext.setOnClickListener(v -> spendViewPager.next());

        bDone.setOnClickListener(v -> {
            Timber.d("bDone.onClick");
            activityCallback.onFragmentDone();
        });

        updatePosition(0);

        final EditText etDummy = view.findViewById(R.id.etDummy);
        etDummy.setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etDummy.requestFocus();
        Helper.hideKeyboard(getActivity());

        Bundle args = getArguments();
        if (args != null) {
            String uri = args.getString(WalletActivity.REQUEST_URI);
            Timber.d("URI: %s", uri);
            if (uri != null) {
                barcodeData = BarcodeData.fromString(uri);
                Timber.d("barcodeData: %s", barcodeData != null ? barcodeData.toString() : "null");
            }
        }

        return view;
    }

    void updatePosition(int position) {
        dotBar.setActiveDot(position);
        CharSequence nextLabel = pagerAdapter.getPageTitle(position + 1);
        bNext.setText(nextLabel);
        if (nextLabel != null) {
            bNext.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_navigate_next, 0);
        } else {
            bNext.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
        CharSequence prevLabel = pagerAdapter.getPageTitle(position - 1);
        bPrev.setText(prevLabel);
        if (prevLabel != null) {
            bPrev.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_navigate_prev, 0, 0, 0);
        } else {
            bPrev.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
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
    public void onAttach(@NonNull Context context) {
        Timber.d("onAttach %s", context);
        super.onAttach(context);
        if (context instanceof Listener) {
            activityCallback = (Listener) context;
            activityCallback.setOnUriScannedListener(this);
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement Listener");
        }
    }

    @Override
    public void onDetach() {
        activityCallback.setOnUriScannedListener(null);
        super.onDetach();
    }

    private SpendViewPager spendViewPager;
    private SpendPagerAdapter pagerAdapter;

    OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (isComitted()) return; // no going back
            if (spendViewPager.getCurrentItem() == 0) {
                setEnabled(false);
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            } else {
                spendViewPager.previous();
            }
        }
    };

    @Override
    public boolean onUriScanned(BarcodeData barcodeData) {
        if (spendViewPager.getCurrentItem() == SpendPagerAdapter.POS_ADDRESS) {
            final SendWizardFragment fragment = pagerAdapter.getFragment(SpendPagerAdapter.POS_ADDRESS);
            if (fragment instanceof SendAddressWizardFragment) {
                ((SendAddressWizardFragment) fragment).processScannedData(barcodeData);
                return true;
            }
        }
        return false;
    }

    enum Mode {
        XMR, BTC
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
                case BTC:
                    txData = new TxDataBtc();
                    break;
                default:
                    throw new IllegalArgumentException("Mode " + String.valueOf(aMode) + " unknown!");
            }
            getView().post(() -> pagerAdapter.notifyDataSetChanged());
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
        private static final int POS_CONFIRM = 2;
        private static final int POS_SUCCESS = 3;
        private int numPages = 3;

        SparseArray<WeakReference<SendWizardFragment>> myFragments = new SparseArray<>();

        public SpendPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        public void addSuccess() {
            numPages++;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return numPages;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            Timber.d("instantiateItem %d", position);
            SendWizardFragment fragment = (SendWizardFragment) super.instantiateItem(container, position);
            myFragments.put(position, new WeakReference<>(fragment));
            return fragment;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            Timber.d("destroyItem %d", position);
            myFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public SendWizardFragment getFragment(int position) {
            WeakReference<SendWizardFragment> ref = myFragments.get(position);
            if (ref != null)
                return myFragments.get(position).get();
            else
                return null;
        }

        @NonNull
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
                    case POS_CONFIRM:
                        return SendConfirmWizardFragment.newInstance(SendFragment.this);
                    case POS_SUCCESS:
                        return SendSuccessWizardFragment.newInstance(SendFragment.this);
                    default:
                        throw new IllegalArgumentException("no such send position(" + position + ")");
                }
            } else if (mode == Mode.BTC) {
                switch (position) {
                    case POS_ADDRESS:
                        return SendAddressWizardFragment.newInstance(SendFragment.this);
                    case POS_AMOUNT:
                        return SendBtcAmountWizardFragment.newInstance(SendFragment.this);
                    case POS_CONFIRM:
                        return SendBtcConfirmWizardFragment.newInstance(SendFragment.this);
                    case POS_SUCCESS:
                        return SendBtcSuccessWizardFragment.newInstance(SendFragment.this);
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
                case POS_CONFIRM:
                    return getString(R.string.send_confirm_title);
                case POS_SUCCESS:
                    return getString(R.string.send_success_title);
                default:
                    return null;
            }
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            Timber.d("getItemPosition %s", String.valueOf(object));
            if (object instanceof SendAddressWizardFragment) {
                // keep these pages
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
    public BarcodeData getBarcodeData() {
        return barcodeData;
    }

    @Override
    public BarcodeData popBarcodeData() {
        Timber.d("POPPED");
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
        final SendConfirm confirm = getSendConfirm();
        if (confirm != null) {
            confirm.sendFailed(getString(R.string.status_transaction_failed, error));
        }
        enableNavigation();
    }

    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        final MaterialContainerTransform transform = new MaterialContainerTransform();
        transform.setDrawingViewId(R.id.fragment_container);
        transform.setDuration(getResources().getInteger(R.integer.tx_item_transition_duration));
        transform.setAllContainerColors(ThemeHelper.getThemedColor(requireContext(), android.R.attr.colorBackground));
        setSharedElementEnterTransition(transform);
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.send_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
}
