package com.artifex.mupdf.mini;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.lang.reflect.Field;

public class SettingsFragment extends Fragment {

    private static final String[] TEXT_ALIGN_OPT = new String[]{"Justify", "Left", "Center", "Right"};
    private static final String[] FONT_FACE_OPT = new String[]{"Charis SIL", "Times New Roman", "Helvetica", "Courier"};
    private static final int MARGIN_MAX = 30;
    private static final int MAX_FONT = 79;

    private Spinner text_align_dropdown;

    private DocumentActivity actionListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.settings_fragment, null);
        initAlignSettings(rootView);
        initFontSettings(rootView);
        initMarginSettings(rootView);
        return rootView;
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

    public void initFontSettings(View rootView) {
        Spinner font_face_dropdown = rootView.findViewById(R.id.font_face_spinner);
        avoidSpinnerDropdownFocus(font_face_dropdown);
        ArrayAdapter<String> font_face_adapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_selected_item, FONT_FACE_OPT);
        font_face_adapter.setDropDownViewResource(R.layout.spinner_item);
        font_face_dropdown.setAdapter(font_face_adapter);

        TextView fontSizeText = rootView.findViewById(R.id.font_size_text);
        SeekBar fonSizeSeekBar = rootView.findViewById(R.id.font_size_seekbar);
        fonSizeSeekBar.setMax(MAX_FONT);
        fonSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public int newProgress = -1;
            public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {newProgress = progress;fontSizeText.setText(""+progress);}
            public void onStartTrackingTouch(SeekBar seekbar) {}
            public void onStopTrackingTouch(SeekBar seekbar) {/* set newProgress as value and refresh */}
        });
        fontSizeText.setText(""+fonSizeSeekBar.getProgress());
        ImageButton minusFontSizeBtn = rootView.findViewById(R.id.minus_font_size_btn);
        minusFontSizeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(fonSizeSeekBar.getProgress()>0)
                    fonSizeSeekBar.setProgress(fonSizeSeekBar.getProgress()-1);
            }
        });
        ImageButton plusFontSizeBtn = rootView.findViewById(R.id.plus_font_size_btn);
        plusFontSizeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(fonSizeSeekBar.getProgress()<MARGIN_MAX)
                    fonSizeSeekBar.setProgress(fonSizeSeekBar.getProgress()+1);
            }
        });
    }

    public void initAlignSettings(View rootView) {
        text_align_dropdown = rootView.findViewById(R.id.text_align_spinner);
        avoidSpinnerDropdownFocus(text_align_dropdown);
        ArrayAdapter<String> text_align_adapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_selected_item, TEXT_ALIGN_OPT);
        text_align_adapter.setDropDownViewResource(R.layout.spinner_item);
        text_align_dropdown.setAdapter(text_align_adapter);
        text_align_dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String newValue = TEXT_ALIGN_OPT[position];
                if(actionListener.cssManager.textAlign!=newValue)
                    actionListener.setTextAlignment(TEXT_ALIGN_OPT[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    public void initMarginSettings(View rootView) {
        initMarginSettingsItem(rootView,
                R.id.top_margin_text,
                R.id.top_margin_seekbar,
                R.id.minus_top_margin_btn,
                R.id.plus_top_margin_btn);
        initMarginSettingsItem(rootView,
                R.id.bot_margin_text,
                R.id.bot_margin_seekbar,
                R.id.minus_bot_margin_btn,
                R.id.plus_bot_margin_btn);
        initMarginSettingsItem(rootView,
                R.id.left_margin_text,
                R.id.left_margin_seekbar,
                R.id.minus_left_margin_btn,
                R.id.plus_left_margin_btn);
        initMarginSettingsItem(rootView,
                R.id.right_margin_text,
                R.id.right_margin_seekbar,
                R.id.minus_right_margin_btn,
                R.id.plus_right_margin_btn);

    }

    public void updateValues() {
        for(int i = 0; i<TEXT_ALIGN_OPT.length; i++) {
            if(TEXT_ALIGN_OPT[i].equals(actionListener.cssManager.textAlign)) {
                text_align_dropdown.setSelection(i);
                break;
            }
        }
    }

    public void initMarginSettingsItem(View rootView, int marginTextId, int marginSeekbarId, int minusBtnId, int plusBtnId) {
        TextView marginText = rootView.findViewById(marginTextId);
        SeekBar marginSeekBar = rootView.findViewById(marginSeekbarId);
        marginSeekBar.setMax(MARGIN_MAX);
        marginSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public int newProgress = -1;
            public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {newProgress = progress;marginText.setText(""+progress);}
            public void onStartTrackingTouch(SeekBar seekbar) {}
            public void onStopTrackingTouch(SeekBar seekbar) {/* set newProgress as value and refresh */}
        });
        marginText.setText(""+marginSeekBar.getProgress());
        ImageButton minusMarginBtn = rootView.findViewById(minusBtnId);
        minusMarginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(marginSeekBar.getProgress()>0)
                    marginSeekBar.setProgress(marginSeekBar.getProgress()-1);
            }
        });
        ImageButton plusMarginBtn = rootView.findViewById(plusBtnId);
        plusMarginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(marginSeekBar.getProgress()<MARGIN_MAX)
                    marginSeekBar.setProgress(marginSeekBar.getProgress()+1);
            }
        });
    }

    public static void avoidSpinnerDropdownFocus(Spinner spinner) {
        try {
            Field listPopupField = Spinner.class.getDeclaredField("mPopup");
            listPopupField.setAccessible(true);
            Object listPopup = listPopupField.get(spinner);
            if (listPopup instanceof ListPopupWindow) {
                Field popupField = ListPopupWindow.class.getDeclaredField("mPopup");
                popupField.setAccessible(true);
                Object popup = popupField.get((ListPopupWindow) listPopup);
                if (popup instanceof PopupWindow) {
                    ((PopupWindow) popup).setFocusable(false);
                }
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
