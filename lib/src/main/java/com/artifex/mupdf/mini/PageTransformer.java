package com.artifex.mupdf.mini;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.View;

import androidx.viewpager.widget.ViewPager;

/**
 * Represents page swipe animation
 */
public class PageTransformer implements ViewPager.PageTransformer {
    private static final float MIN_SCALE = 1f;
    private static final float MIN_ALPHA = 0.2f;

    public void transformPage(View view, float position) {
        int pageWidth = view.getWidth();

        if (position < -1) {
            view.setAlpha(0);
        } else if (position <= 0) {
            view.setAlpha(1f);
            view.setTranslationX(0f);
            view.setScaleX(1f);
            view.setScaleY(1f);
        } else if (position <= 1) {
            float alpha = MIN_ALPHA + (1-position)*(1-MIN_ALPHA);
            view.setAlpha(alpha);
            view.setTranslationX(pageWidth * -position);

            float scaleFactor = MIN_SCALE
                    + (1 - MIN_SCALE) * (1 - Math.abs(position));
            view.setScaleX(scaleFactor);
            view.setScaleY(scaleFactor);
        } else {
            view.setAlpha(0);
        }
    }
}
