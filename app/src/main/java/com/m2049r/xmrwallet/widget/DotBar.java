/*
 * Copyright (c) 2017 m2049r et al.
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

// based on https://github.com/marcokstephen/StepProgressBar

package com.m2049r.xmrwallet.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.m2049r.xmrwallet.R;

import timber.log.Timber;

public class DotBar extends View {

    final private int inactiveColor;
    final private int activeColor;

    final private float dotSize;
    private float dotSpacing;

    final private int numDots;
    private int activeDot;

    final private Paint paint;

    public DotBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.DotBar, 0, 0);
        try {
            inactiveColor = ta.getInt(R.styleable.DotBar_inactiveColor, 0);
            activeColor = ta.getInt(R.styleable.DotBar_activeColor, 0);
            dotSize = ta.getDimensionPixelSize(R.styleable.DotBar_dotSize, 8);
            numDots = ta.getInt(R.styleable.DotBar_numberDots, 5);
            activeDot = ta.getInt(R.styleable.DotBar_activeDot, 0);
        } finally {
            ta.recycle();
        }

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = (int) ((numDots * dotSize) + getPaddingLeft() + getPaddingRight());
        int desiredHeight = (int) (dotSize + getPaddingBottom() + getPaddingTop());

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            width = Math.min(desiredWidth, widthSize);
        } else {
            //Be whatever you want
            width = desiredWidth;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY) {
            //Must be this size
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            height = Math.min(desiredHeight, heightSize);
        } else {
            //Be whatever you want
            height = desiredHeight;
        }

        dotSpacing = (int) (((1.0 * width - (getPaddingLeft() + getPaddingRight())) / numDots - dotSize) / (numDots - 1));

        Timber.d("dotSpacing=%f", dotSpacing);
        //MUST CALL THIS
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Centering the dots in the middle of the canvas
        float singleDotSize = dotSpacing + dotSize;
        float combinedDotSize = singleDotSize * numDots - dotSpacing;
        int startingX = (int) ((canvas.getWidth() - combinedDotSize) / 2);
        int startingY = (int) ((canvas.getHeight() - dotSize) / 2);

        for (int i = 0; i < numDots; i++) {
            int x = (int) (startingX + i * singleDotSize);
            if (i == activeDot) {
                paint.setColor(activeColor);
            } else {
                paint.setColor(inactiveColor);
            }
            canvas.drawCircle(x + dotSize / 2, startingY + dotSize / 2, dotSize / 2, paint);
        }
    }

    public void next() {
        if (activeDot < numDots - 2) {
            activeDot++;
            invalidate();
        } // else no next - stay stuck at end
    }

    public void previous() {
        if (activeDot >= 0) {
            activeDot--;
            invalidate();
        } // else no previous - stay stuck at beginning
    }

    public void setActiveDot(int i) {
        if ((i >= 0) && (i < numDots)) {
            activeDot = i;
            invalidate();
        }
    }

    public int getActiveDot() {
        return activeDot;
    }

    public int getNumDots() {
        return numDots;
    }
}