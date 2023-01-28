package com.artifex.mupdf.mini;

import java.util.HashMap;
import java.util.Map;

public class CSSManager {

    public String font = "Merriweather";
    public int fontSize = 10; // range 1-40
    public String textAlign = "left";

    private final float MIN_REAL_FONT_SIZE_EM = 5.0f;
    private final String TABLE_FIX = "td {text-align: center !important;} td p{display: inherit !important;}";

    public CSSManager() {}

    public String getCSS() {
        float realFontSize = MIN_REAL_FONT_SIZE_EM + 0.5f * fontSize;
        StringBuilder builder = new StringBuilder();
        builder
                .append("body {font-size: ")
                .append(realFontSize).append("em !important; font-family: ")
                .append(FONTS.get(font)).append(" !important;} p:not(.center){text-align: ")
                .append(textAlign).append(" !important;}")
                .append(TABLE_FIX);
        return builder.toString();
    }

    private static final HashMap<String, String> FONTS  = new HashMap<String, String>() {{
        put("Merriweather", "'Merriweather', serif");
        put("Roboto", "'Roboto', sans-serif");
    }};
}
