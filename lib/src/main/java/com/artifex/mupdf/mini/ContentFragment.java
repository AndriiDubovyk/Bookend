package com.artifex.mupdf.mini;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
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
    private ArrayList<ContentItem> displayedItems;

    public static class ContentItem  {
        public String title;
        public String uri;
        public int page;
        public int level;
        public ArrayList<ContentItem> down;
        public boolean isExpanded = false;

        public ContentItem(String title, String uri, int page, int level, ArrayList<ContentItem> down) {
            this.title = title;
            this.uri = uri;
            this.page = page;
            this.level = level;
            this.down = down;
        }

        public ContentItem clone() {
            return new ContentItem(title, uri, page, level, down);
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
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.content_fragment, null);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        displayedItems = new ArrayList<>(actionListener.getContentItems());
        adapter = new ContentListAdapter(getActivity(), android.R.layout.simple_list_item_1, displayedItems);
        setListAdapter(adapter);
    }

    public void updateItems() {
        displayedItems.clear();
        displayedItems.addAll(actionListener.getContentItems());
        removeExpandedMark(displayedItems);
        adapter.notifyDataSetChanged();
    }

    private void removeExpandedMark(ArrayList<ContentItem> list) {
        if(list!=null) {
            for (ContentItem ci : list) {
                ci.isExpanded = false;
                removeExpandedMark(ci.down);
            }
        }
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
        ContentItem ci = (ContentItem) (adapter.getItem(position));
        actionListener.gotoPage(ci.page);
        actionListener.manageFragmentTransaction(DocumentActivity.FragmentsState.NONE);
    }

    private void expandItem(int position) {
        ContentItem di = displayedItems.get(position);
        if(!di.isExpanded) {
            di.isExpanded = true;
            for(int i = 0; i<di.down.size(); i++) {
                displayedItems.add(position+1+i, di.down.get(i));
            }
        }
    }

    private void collapseItem(int position) {
        ContentItem di = displayedItems.get(position);
        if(di.isExpanded) {
            di.isExpanded = false;
            for(int i = 0; i<di.down.size(); i++) {
                ContentItem innerDi = displayedItems.get(position+1);
                collapseItem(position+1);
                displayedItems.remove(innerDi);
            }
        }
    }

    private class ContentListAdapter extends ArrayAdapter<ContentItem> {
        private final Context context;
        private final ArrayList<ContentItem> items;



        public ContentListAdapter(@NonNull Context context, int resource, @NonNull List<ContentItem> objects) {
            super(context, resource, objects);
            this.items = new ArrayList<>(objects);
            this.context = context;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View itemView = inflater.inflate(R.layout.content_item, parent, false);
            ContentItem item = getItem(position);
            TextView titleTextView = itemView.findViewById(R.id.contentTitle);
            int leftPadding = titleTextView.getPaddingLeft()/2 + titleTextView.getPaddingLeft() * (item.level < 4 ? item.level : 4);
            titleTextView.setPadding(leftPadding, titleTextView.getPaddingTop(), titleTextView.getPaddingRight(), titleTextView.getPaddingBottom());
            titleTextView.setText(item.title);
            TextView pageTextView = itemView.findViewById(R.id.page_number);
            pageTextView.setText(""+(item.page+1));
            View expandButton = itemView.findViewById(R.id.expand_button);
            if(item.down.size()>0) expandButton.setOnClickListener(expandBtnClickListener);
            else expandButton.setVisibility(View.INVISIBLE);
            return itemView;
        }

    }

    private View.OnClickListener expandBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int position = getListView().getPositionForView(v);
            ImageView expandIcon = v.findViewById(R.id.expand_icon);
            if(displayedItems.get(position).isExpanded) {
                collapseItem(position);
            } else {
                expandItem(position);
            }
            adapter.notifyDataSetChanged();
        }
    };
}
