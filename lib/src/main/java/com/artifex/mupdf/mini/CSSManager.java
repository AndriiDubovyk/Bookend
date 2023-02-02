package com.artifex.mupdf.mini;

import java.util.HashMap;
import java.util.Map;

public class CSSManager {

    public String font = "Merriweather";
    public int fontSize = 10; // range 1-40
    public String textAlign = "justify";

    private final static float MIN_REAL_FONT_SIZE_EM = 5.0f;
    private final static String TABLE_FIX = "td {text-align: center !important;} td p{display: inherit !important;}";

    public CSSManager() {}

    public String getCSS() {
        float realFontSize = MIN_REAL_FONT_SIZE_EM + 0.5f * fontSize;
        String res = "body{margin: 0 !important; padding: 0 !important; font-size: "+realFontSize+"em !important; font-family: "+FONTS.get(font) + " !important;}" +
                "p{text-align: "+textAlign+" !important;} " +
                TABLE_FIX;
        return res;
    }

    private static final HashMap<String, String> FONTS  = new HashMap<String, String>() {{
        put("Merriweather", "'Merriweather', serif");
        put("Roboto", "'Roboto', sans-serif");
    }};
}
