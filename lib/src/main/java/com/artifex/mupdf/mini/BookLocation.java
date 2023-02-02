package com.artifex.mupdf.mini;

import android.util.Log;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Location;

public class BookLocation {
    public final int chapter;
    public final float chapterProgress;

    public BookLocation(int chapter, float chapterProgress) {
        this.chapter = chapter;
        this.chapterProgress = chapterProgress;
    }

    public BookLocation(Document doc, Location loc) {
        this.chapter = loc.chapter;
        int currentChapterPages = doc.countPages(chapter);
        this.chapterProgress = getReadProgress(loc.page, currentChapterPages);
    }

    public BookLocation(Document doc, int currentPage) {
        this(doc, doc.locationFromPageNumber(currentPage));
    }

    public int toPage(Document doc) {
        int currentChapterPages = doc.countPages(chapter);
        int page = Math.round(chapterProgress * currentChapterPages);
        if(page>=currentChapterPages)  page = currentChapterPages-1;
        Location loc = new Location(this.chapter, page);
        return doc.pageNumberFromLocation(loc);
    }

    public static float getReadProgress(int current, int count) {
        if(count <= 1) return 1f;
        return ((float) current) / (count - 1);
    }

    @Override
    public String toString() {
        return "BookLocation{" +
                "chapter=" + chapter +
                ", chapterProgress=" + chapterProgress +
                '}';
    }
}
