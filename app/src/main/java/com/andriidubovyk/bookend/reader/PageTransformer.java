package com.andriidubovyk.bookend.reader;

import android.view.View;
import androidx.viewpager.widget.ViewPager;

/**
 * Represents page swipe animation
 */
public class PageTransformer implements ViewPager.PageTransformer {
    public void transformPage(View view, float position) {
        int pageWidth = view.getWidth();
        if (position < -1) {
            view.setAlpha(0);
        } else if (position <= 0) {
            view.setAlpha(1f);
            view.setTranslationX(0f);
        } else if (position <= 1) {
            view.setAlpha(1f);
            view.setTranslationX(pageWidth * -position);
        } else {
            view.setAlpha(0);
        }
    }
}
