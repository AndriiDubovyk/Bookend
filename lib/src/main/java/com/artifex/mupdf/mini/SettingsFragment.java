package com.artifex.mupdf.mini;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.lang.reflect.Field;

public class SettingsFragment extends Fragment {

    private static String[] text_align_options = new String[]{"Justify", "Left", "Center", "Right"};
    private static String[] font_face_options = new String[]{"Charis SIL", "Times New Roman", "Helvetica", "Courier"};

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.settings_fragment, null);
        Spinner text_align_dropdown = rootView.findViewById(R.id.text_align_spinner);
        avoidSpinnerDropdownFocus(text_align_dropdown);
        ArrayAdapter<String> text_align_adapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_selected_item, text_align_options);
        text_align_adapter.setDropDownViewResource(R.layout.spinner_item);
        text_align_dropdown.setAdapter(text_align_adapter);

        Spinner font_face_dropdown = rootView.findViewById(R.id.font_face_spinner);
        avoidSpinnerDropdownFocus(font_face_dropdown);
        ArrayAdapter<String> font_face_adapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_selected_item, font_face_options);
        font_face_adapter.setDropDownViewResource(R.layout.spinner_item);
        font_face_dropdown.setAdapter(font_face_adapter);

        return rootView;
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
