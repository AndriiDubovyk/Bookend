package com.artifex.mupdf.mini;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;

public class PageFragment extends Fragment {

    public static final String PAGE_NUMBER = "page_number";
    private ViewGroup root;
    private PageView pageView;
    private int pageNumber;
    private final DocumentActivity actionListener;

    public PageFragment(DocumentActivity da) {
        actionListener = da;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.page_fragment, container, false);
        root = rootView;
        pageView = root.findViewById(R.id.page_view_fragment);
        Bundle arguments = getArguments();
        if (arguments != null) {
            int pageNumber = arguments.getInt(PAGE_NUMBER);
            setPage(pageNumber);
        }
        return rootView;
    }



    public void setPage(int pageNumber) {
        this.pageNumber = pageNumber;
        pageView.setActionListener(actionListener);
        pageView.setPage(pageNumber);
    }

    public void updatePage() {
        pageView.setPage(pageNumber);
    }

    public void setZoom(float newZoom) {
        pageView.setPageZoom(newZoom);
    }

    public void setPageScroll(int scrollX, int scrollY) {
        pageView.setPageScroll(scrollX, scrollY);
    }
}