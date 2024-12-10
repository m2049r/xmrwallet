package com.m2049r.xmrwallet.util;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;

import lombok.Getter;

public class Flasher {
    public interface Light {
        int getDrawableId();
    }

    final private static int ON_TIME = 80; //ms
    final private static int DURATION = 100 + ON_TIME; //ms
    final private static int OFF_TIME = 600; //ms
    @Getter
    final private Drawable drawable;
    private long t;
    private ValueAnimator animator;

    private final int colorOff, colorOn;
    private int colorCurrent;

    public Flasher(Context ctx, Light light) {
        colorOff = ThemeHelper.getThemedColor(ctx, android.R.attr.colorControlNormal);
        colorOn = ThemeHelper.getThemedColor(ctx, android.R.attr.colorControlActivated);
        drawable = getDrawable(ctx, light.getDrawableId());
        drawable.setTint(colorOff);
    }

    public void flash(View view) {
        if (view == null) return;
        if (animator != null) animator.cancel();

        final long now = System.currentTimeMillis();
        t = now;

        animator = ValueAnimator.ofArgb(colorOff, colorOn); // always blink nomatter what
        animator.addUpdateListener(valueAnimator -> {
            colorCurrent = (Integer) valueAnimator.getAnimatedValue();
            drawable.setTint(colorCurrent);
        });
        animator.setDuration(ON_TIME);
        animator.start();
        view.postDelayed(() -> {
            if (t == now) { // only turn it off if we turned it on last
                animator = ValueAnimator.ofArgb(colorCurrent, colorOff);
                animator.addUpdateListener(valueAnimator -> {
                    colorCurrent = (Integer) valueAnimator.getAnimatedValue();
                    drawable.setTint(colorCurrent);
                });
                animator.setDuration(Math.abs((long) (1. * OFF_TIME * ((colorCurrent - colorOff) / (colorOn - colorOff)))));
                animator.start();
            }
        }, DURATION);
    }

    private Drawable getDrawable(Context ctx, int drawableId) {
        return ResourcesCompat.getDrawable(ctx.getResources(), drawableId, null);
    }
}
