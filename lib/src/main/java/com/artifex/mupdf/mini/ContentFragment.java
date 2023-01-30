package com.artifex.mupdf.mini;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ContentFragment extends ListFragment {

    private DocumentActivity actionListener;
    private ListAdapter adapter;

    public static class Item implements Serializable {
        public String title;
        public String uri;
        public int page;
        public Item(String title, String uri, int page) {
            this.title = title;
            this.uri = uri;
            this.page = page;
        }
        public String toString() {
            return title;
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.content_fragment, null);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ArrayList<Item> items = new ArrayList<>();
        Bundle bundle = getArguments();
        int currentPage = bundle.getInt("POSITION");
        ArrayList<Item> outline = (ArrayList<Item>)bundle.getSerializable("OUTLINE");
        int found = -1;
        for (int i = 0; i < outline.size(); ++i) {
            Item item = outline.get(i);
            if (found < 0 && item.page >= currentPage)
                found = i;
            items.add(item);
        }
        adapter = new ContentListAdapter(getActivity(), android.R.layout.simple_list_item_1, items);
        setListAdapter(adapter);

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            actionListener = (DocumentActivity) activity;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Item item = (Item) (adapter.getItem(position));
        actionListener.gotoPage(item.page);
        actionListener.closeContentFragment();
    }

    public class ContentListAdapter extends ArrayAdapter<Item> {
        private final Context context;
        private final ArrayList<Item> items;

        public ContentListAdapter(@NonNull Context context, int resource, @NonNull List<Item> objects) {
            super(context, resource, objects);
            this.items = new ArrayList<>(objects);
            this.context = context;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View itemView = inflater.inflate(R.layout.content_item, parent, false);
            Item item = items.get(position);
            TextView titleTextView = itemView.findViewById(R.id.contentTitle);
            titleTextView.setText(item.title);
            TextView pageTextView = itemView.findViewById(R.id.page_number);
            pageTextView.setText(""+item.page);
            return itemView;
        }


    }
}
