package com.artifex.mupdf.mini;


import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class ReaderView extends ViewPager {
    private PagerAdapter pagerAdapter;
    private DocumentActivity actionListener;

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
        //FragmentManager fm =  ((FragmentActivity) context).getSupportFragmentManager();
        setPageTransformer(true, new SlideStackPageTransformer());
        addOnPageChangeListener(new OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {}
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            public void onPageSelected(int position) {
                Log.i("mytag", "onPageSelected: "+position);
                actionListener.updatePageNumberInfo(position);
            }
        });
    }

}
