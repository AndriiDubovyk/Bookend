package com.artifex.mupdf.mini;


import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.lang.reflect.Field;

public class ReaderView extends ViewPager {
    private PagerAdapter pagerAdapter;
    private DocumentActivity actionListener;
    private float zoom = 1f;
    private boolean enabled = true;

    public ReaderView(@NonNull Context context) {
        super(context);
        setup(context);
    }

    public ReaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    @Override
    public void setAdapter(@Nullable PagerAdapter adapter) {
        super.setAdapter(adapter);
        pagerAdapter = adapter;
    }

    public void setZoom(float newZoom) {
        zoom = newZoom;
        ((PageAdapter)getAdapter()).updateCachedPagesZoom(newZoom);
    }

    public void setZoomWithoutUpdate(float newZoom) {
        zoom = newZoom;
    }

    public float getZoom() {
        return zoom;
    }

    /**
     * Temporary solution
     * By some cause serCurrentItem(p) or serCurrentItem(p, true) work perfectly.
     * But serCurrentItem(p, false) work only if we reattach adapter before
     */
    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        if(!smoothScroll) this.setAdapter(pagerAdapter);
        super.setCurrentItem(item, smoothScroll);
    }




    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (actionListener != null)
            actionListener.onPageViewSizeChanged(w, h);
    }

    public PageFragment getCurrentPageFragment() {
        return ((PageAdapter)getAdapter()).getCurrentPageFragment();
    }

    public void updateCachedPages() {
        ((PageAdapter)getAdapter()).updateCachedPages();
    }

    public void setActionListener(DocumentActivity da) {
        actionListener = da;
    }


    private void setup(Context context) {
        setBackgroundColor(PageView.BACKGROUND_COLOR);
        setPageTransformer(true, new com.artifex.mupdf.mini.PageTransformer());
        addOnPageChangeListener(new OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {}
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            public void onPageSelected(int position) {
                actionListener.updatePageNumberInfo(position);
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.enabled) {
            return super.onTouchEvent(event);
        }

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.enabled) {
            return super.onInterceptTouchEvent(event);
        }

        return false;
    }

    public void setPagingEnabled(boolean enabled) {
        this.enabled = enabled;
    }



}
