package com.artifex.mupdf.mini;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ContentFragment extends ListFragment {

    private DocumentActivity actionListener;
    private ContentListAdapter adapter;
    private ArrayList<ContentItem> contentItems;
    private ArrayList<DisplayedItem> displayedItems;

    public static class ContentItem  {
        public String title;
        public String uri;
        public int page;
        public int level;
        public ArrayList<ContentItem> down;
        public ContentItem(String title, String uri, int page, int level, ArrayList<ContentItem> down) {
            this.title = title;
            this.uri = uri;
            this.page = page;
            this.level = level;
            this.down = down;
        }
        public String toString() {
            String res = "";
            if(down!=null) {
                for(ContentItem ci : down) {
                    res += ci.toString() + ", ";
                }
            }
            return title + "{ "+res +" }";
        }
    }

    public static class DisplayedItem {
        public String title;
        public String uri;
        public int page;
        public int level;
        public ContentItem ci;
        public boolean isExpanded = false;

        public DisplayedItem(String title, String uri, int page, int level) {
            this.title = ci.title;
            this.uri = ci.uri;
            this.page = ci.page;
            this.level = ci.level;
        }

        public DisplayedItem(ContentItem ci) {
            this.title = ci.title;
            this.uri = ci.uri;
            this.page = ci.page;
            this.level = ci.level;
            this.ci = ci;
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

        contentItems = actionListener.getContentItems();

        displayedItems = new ArrayList<>();
        for(ContentItem ci: contentItems) {
            displayedItems.add(new DisplayedItem(ci));
        }
        adapter = new ContentListAdapter(getActivity(), android.R.layout.simple_list_item_1, displayedItems);
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
        DisplayedItem di = displayedItems.get(position);
        if(di.isExpanded) collapseItem(position);
        else expandItem(position);
        adapter.notifyDataSetChanged();
    }

    private void expandItem(int position) {
        DisplayedItem di = displayedItems.get(position);
        ContentItem ci = di.ci;
        if(!di.isExpanded) {
            di.isExpanded = true;
            for(int i = 0; i<ci.down.size(); i++) {
                displayedItems.add(position+1+i, new DisplayedItem(ci.down.get(i)));
            }
        }
    }

    private void collapseItem(int position) {
        DisplayedItem di = displayedItems.get(position);
        ContentItem ci = di.ci;
        if(di.isExpanded) {
            di.isExpanded = false;
            for(int i = 0; i<ci.down.size(); i++) {
                DisplayedItem innerDi = displayedItems.get(position+1);
                collapseItem(position+1);
                displayedItems.remove(innerDi);
            }
        }
    }

    public class ContentListAdapter extends ArrayAdapter<DisplayedItem> {
        private final Context context;
        private final ArrayList<DisplayedItem> items;



        public ContentListAdapter(@NonNull Context context, int resource, @NonNull List<DisplayedItem> objects) {
            super(context, resource, objects);
            this.items = new ArrayList<>(objects);
            this.context = context;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View itemView = inflater.inflate(R.layout.content_item, parent, false);
            DisplayedItem item = getItem(position);
            int leftPadding = itemView.getPaddingLeft() * (item.level < 4 ? item.level : 4);
            itemView.setPadding(leftPadding, itemView.getPaddingTop(), itemView.getPaddingRight(), itemView.getPaddingBottom());
            TextView titleTextView = itemView.findViewById(R.id.contentTitle);
            titleTextView.setText(item.title);
            TextView pageTextView = itemView.findViewById(R.id.page_number);
            pageTextView.setText(""+(item.page+1));
            return itemView;
        }


    }
}
