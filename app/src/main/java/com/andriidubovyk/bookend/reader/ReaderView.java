package com.andriidubovyk.bookend.reader;


import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class ReaderView extends ViewPager {
    private PagerAdapter pagerAdapter;
    private DocumentActivity actionListener;
    private float initialXValue;

    private SwipeDirection direction;
    private float zoom = 1f;
    protected int currentPageScrollX = 0;
    protected int currentPageScrollY = 0;


    enum SwipeDirection {
        ALL, LEFT, RIGHT, NONE
    }

    public ReaderView(@NonNull Context context) {
        super(context);
        setup();
    }

    public ReaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
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

    public void setLeftPageScroll(int scrollX, int scrollY) {
        ((PageAdapter)pagerAdapter).setLeftPageScroll(scrollX, scrollY, getCurrentItem());
    }

    public void setRightPageScroll(int scrollX, int scrollY) {
        ((PageAdapter)pagerAdapter).setRightPageScroll(scrollX, scrollY,  getCurrentItem());
    }

    public PageFragment getCurrentPageFragment() {
        return ((PageAdapter)pagerAdapter).getCurrentPageFragment();
    }

    public void updateCachedPages() {
        ((PageAdapter)getAdapter()).updateCachedPages();
    }

    public void setActionListener(DocumentActivity da) {
        actionListener = da;
    }


    private int prevPage = -1;
    private void setup() {
        this.direction = SwipeDirection.ALL;
        setBackgroundColor(PageView.BACKGROUND_COLOR);
        setPageTransformer(true, new com.andriidubovyk.bookend.reader.PageTransformer());
        ReaderView readerView = this;
        addOnPageChangeListener(new OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {}
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                actionListener.pageSeparator.setVisibility(View.VISIBLE);
                actionListener.pageSeparator.setX(readerView.getWidth()-positionOffsetPixels); // move shadow line
            }

            public void onPageSelected(int position) {
                actionListener.updatePageNumberInfo(position);
                if(zoom>1) {
                    if(position == prevPage + 1) setAllowedSwipeDirection(SwipeDirection.LEFT);
                    else if (position == prevPage-1) setAllowedSwipeDirection(SwipeDirection.RIGHT);
                }
                prevPage = position;
            }
        });
    }

    public void setCurrentPageScroll(int scrollX, int scrollY) {
        this.currentPageScrollX = scrollX;
        this.currentPageScrollY = scrollY;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.isSwipeAllowed(event)) {
            return super.onTouchEvent(event);
        }

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.isSwipeAllowed(event)) {
            return super.onInterceptTouchEvent(event);
        }

        return false;
    }

    private boolean isSwipeAllowed(MotionEvent event) {
        if(this.direction == SwipeDirection.ALL) return true;

        if(direction == SwipeDirection.NONE )//disable any swipe
            return false;

        if(event.getAction()==MotionEvent.ACTION_DOWN) {
            initialXValue = event.getX();
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float diffX = event.getX() - initialXValue;
            // swipe from right to left detected
            if (diffX > 0 && direction == SwipeDirection.RIGHT) {
                // swipe from left to right detected
                return false;
            } else return !(diffX < 0) || direction != SwipeDirection.LEFT;
        }

        return true;
    }

    public void setAllowedSwipeDirection(SwipeDirection direction) {
        this.direction = direction;
    }


}
