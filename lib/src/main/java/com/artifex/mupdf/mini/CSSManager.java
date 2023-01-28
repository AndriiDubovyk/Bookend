package com.artifex.mupdf.mini;

public class CSSManager {

    public int fontSize = 10; // range 1-40

    private final float MIN_REAL_FONT_SIZE_EM = 5.0f;
    private final String TABLE_FIX = "td {text-align: center !important;} td p{display: inherit !important; text-align:justify !important;}";

    public CSSManager() {}

    public String getCSS() {
        float realFontSize = MIN_REAL_FONT_SIZE_EM + 0.5f * fontSize;
        StringBuilder builder = new StringBuilder();
        builder
                .append("body { font-size: ")
                .append(realFontSize).append("em !important;}")
                .append(TABLE_FIX);
        return builder.toString();
    }
}
