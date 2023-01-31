package com.artifex.mupdf.mini;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import com.artifex.mupdf.fitz.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PageAdapter extends FragmentStatePagerAdapter {
    private final DocumentActivity actionListener;
    private PageFragment mCurrentPageFragment;
    private HashMap<Integer, Fragment> cache = new HashMap<Integer, Fragment>(3);

    public PageAdapter(FragmentManager fm, DocumentActivity da) {
        super(fm);
        actionListener = da;
    }

    public PageFragment getCurrentPageFragment() {
        return mCurrentPageFragment;
    }

    public void updateCachedPages() {
        for(Fragment fg : cache.values()) {
            ((PageFragment)fg).updatePage();
        }
    }

    public void updateCachedPagesZoom(float newZoom) {
        for(Fragment fg : cache.values()) {
            ((PageFragment)fg).setZoom(newZoom);
        }
    }

    public void updateCachedPagesScroll(int scrollX, int scrollY) {
        for(Fragment fg : cache.values()) {
            ((PageFragment)fg).setPageScroll(scrollX, scrollY);
        }
    }

    public void setLeftPageScroll(int scrollX, int scrollY, int currentPage) {
        Log.i("mytag", "set left "+scrollX+" "+scrollY);
        int leftPage = currentPage-1;
        if(cache.containsKey(leftPage)) {
            ((PageFragment)cache.get(leftPage)).setPageScroll(scrollX, scrollY);
        }
    }

    public void setRightPageScroll(int scrollX, int scrollY, int currentPage) {
        int rightPage = currentPage+1;
        if(cache.containsKey(rightPage)) {
            ((PageFragment)cache.get(rightPage)).setPageScroll(scrollX, scrollY);
        }
    }

    @Override
    public Fragment getItem(int position) {
        Bundle arguments = new Bundle();
        arguments.putInt(PageFragment.PAGE_NUMBER, position);

        PageFragment fragment = new PageFragment(actionListener);
        fragment.setArguments(arguments);
        cache.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        super.destroyItem(container, position, object);
        cache.remove(position);
    }

    @Override
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        if (mCurrentPageFragment != object) {
            mCurrentPageFragment = ((PageFragment) object);
        }
        super.setPrimaryItem(container, position, object);
    }



    @Override
    public int getCount() {
        return actionListener.pageCount;
    }
}
