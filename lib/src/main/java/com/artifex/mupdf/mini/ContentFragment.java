package com.artifex.mupdf.mini;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.ListFragment;

import java.util.ArrayList;
import java.util.List;

public class ContentFragment extends ListFragment {

    private DocumentActivity actionListener;
    private ContentListAdapter adapter;
    private ArrayList<ContentItem> displayedItems;
    private int selectedItem = -1;

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
        displayedItems = new ArrayList<>();
        adapter = new ContentListAdapter(getActivity(), R.layout.content_item, displayedItems);
        setListAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        setSelection(selectedItem);
    }

    public static ArrayList<Integer> getContentItemIndicesByPage(ArrayList<ContentItem> items, int page) {
        int index = -1;
        for(int i = 0; i<items.size(); i++) {
            ContentItem ci = items.get(i);
            if(page>=ci.page) {
                index = i;
            }
        }
        ArrayList<Integer> indices = new ArrayList<>();
        if(index>=0) {
            indices.add(index);
            ArrayList<ContentItem> childItems = items.get(index).down;
            if(childItems.size()>0)
                indices.addAll(getContentItemIndicesByPage(childItems, page));
        }
        return indices;
    }

    public void processExpansionWithIndices(ArrayList<Integer> indices) {
        selectedItem = -1;
        int shift = 0;
        for(int k = 0; k<indices.size(); k++) {
            int index = shift+indices.get(k);
            shift=index+1;
            if(k<indices.size()-1) expandItem(index);
            selectedItem = index;
        }
    }

    public void updateItems() {
        displayedItems.clear();
        displayedItems.addAll(actionListener.getContentItems());
        removeExpandedMark(displayedItems);
        ArrayList<Integer> indices = getContentItemIndicesByPage(displayedItems, actionListener.currentPage);
        processExpansionWithIndices(indices);
        setListAdapter(getListAdapter());
        getListView().setSelection(selectedItem);
    }

    private void removeExpandedMark(ArrayList<ContentItem> list) {
        if(list!=null) {
            for (ContentItem ci : list) {
                ci.isExpanded = false;
                removeExpandedMark(ci.down);
            }
        }
    }

    public void setActionListener(DocumentActivity da) {
        actionListener = da;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            if(actionListener==null) actionListener = (DocumentActivity) activity;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        ContentItem ci = adapter.getItem(position);
        actionListener.gotoPage(ci.page);
        actionListener.manageFragmentTransaction(DocumentActivity.FragmentsState.NONE);
    }

    private void expandItem(int position) {
        ContentItem di = displayedItems.get(position);
        if(!di.isExpanded) {
            di.isExpanded = true;
            if(position<selectedItem) selectedItem+=di.down.size();
            for(int i = 0; i<di.down.size(); i++) {
                ContentItem innerDi = di.down.get(i);
                int insertPos = position+1+i;
                if(position>=selectedItem) {
                    if(actionListener.currentPage>=innerDi.page) {
                        selectedItem = insertPos;
                    }
                }
                displayedItems.add(insertPos, innerDi);
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
            // calculate new selection mark
            if(selectedItem<position) selectedItem-=di.down.size();
            else if (selectedItem>position && selectedItem<=position+di.down.size())  selectedItem = position;

        }
    }

    private class ContentListAdapter extends ArrayAdapter<ContentItem> {
        private final Context context;



        public ContentListAdapter(@NonNull Context context, int resource, @NonNull List<ContentItem> objects) {
            super(context, resource, objects);
            this.context = context;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View itemView = inflater.inflate(R.layout.content_item, parent, false);
            if(position==selectedItem)
                itemView.setBackground(ContextCompat.getDrawable(context, R.drawable.content_selected_item_background));
            ContentItem item = getItem(position);
            TextView titleTextView = itemView.findViewById(R.id.contentTitle);
            if(item.down.size()>0) titleTextView.setTypeface(titleTextView.getTypeface(), Typeface.BOLD);
            int leftPadding = titleTextView.getPaddingLeft()/2 + titleTextView.getPaddingLeft() * (item.level < 4 ? item.level : 4);
            titleTextView.setPadding(leftPadding, titleTextView.getPaddingTop(), titleTextView.getPaddingRight(), titleTextView.getPaddingBottom());
            titleTextView.setText(item.title);
            TextView pageTextView = itemView.findViewById(R.id.page_number);
            pageTextView.setText(""+(item.page+1));
            View expandButton = itemView.findViewById(R.id.expand_button);
            if(item.down.size()>0) {
                expandButton.setOnClickListener(expandBtnClickListener);
                if(item.isExpanded) expandButton.setRotation(90f);
            } else {
                expandButton.setVisibility(View.INVISIBLE);
            }
            return itemView;
        }

    }

    private View.OnClickListener expandBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int position = getListView().getPositionForView(v);
            if(displayedItems.get(position).isExpanded) {
                collapseItem(position);
            } else {
                expandItem(position);
            }
            adapter.notifyDataSetChanged();
        }
    };
}
