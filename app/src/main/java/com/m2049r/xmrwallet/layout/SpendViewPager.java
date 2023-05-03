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

package com.m2049r.xmrwallet.layout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

import com.m2049r.xmrwallet.fragment.send.SendFragment;

public class SpendViewPager extends ViewPager {

    public interface OnValidateFieldsListener {
        boolean onValidateFields();
    }

    public SpendViewPager(Context context) {
        super(context);
    }

    public SpendViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void next() {
        int pos = getCurrentItem();
        if (validateFields(pos)) {
            setCurrentItem(pos + 1);
        }
    }

    public void previous() {
        setCurrentItem(getCurrentItem() - 1);
    }

    private boolean allowSwipe = true;

    public void allowSwipe(boolean allow) {
        allowSwipe = allow;
    }

    public boolean validateFields(int position) {
        OnValidateFieldsListener c = ((SendFragment.SpendPagerAdapter) getAdapter()).getFragment(position);
        return c.onValidateFields();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (allowSwipe) return super.onInterceptTouchEvent(event);
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (allowSwipe) return super.onTouchEvent(event);
        return false;
    }
}