package com.example.xyzreader.utils;

import android.content.Context;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ru.noties.markwon.Markwon;
import ru.noties.markwon.SpannableConfiguration;

public class Pagination {

    private final boolean mIncludePad;
    private final int mWidth;
    private final int mHeight;
    private final float mSpacingMult;
    private final float mSpacingAdd;
    private final CharSequence mText;
    private final TextPaint mPaint;
    private final List<CharSequence> mPages;

    public Pagination(CharSequence text, TextView view) {
        Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);
        this.mText = text;
        this.mWidth = view.getMeasuredWidth();
        this.mHeight = rect.height() != 0 ? rect.height() : view.getMeasuredHeight();
        this.mPaint = view.getPaint();
        this.mSpacingMult = view.getLineSpacingMultiplier();
        this.mSpacingAdd = view.getLineSpacingExtra();
        this.mIncludePad = view.getIncludeFontPadding();
        this.mPages = new ArrayList<>();
        layout();
    }

    private void layout() {
        final StaticLayout layout = new StaticLayout(mText, mPaint, mWidth,
                Layout.Alignment.ALIGN_NORMAL, mSpacingMult, mSpacingAdd, mIncludePad);
        final int lines = layout.getLineCount();
        final CharSequence text = layout.getText();
        int startOffset = 0;
        int height = mHeight;

        for (int i = 0; i < lines; i++) {

            if (height < layout.getLineBottom(i)) {
                // When the layout height has been exceeded
                addPage(text.subSequence(startOffset, layout.getLineStart(i)));
                startOffset = layout.getLineStart(i);
                height = layout.getLineTop(i) + mHeight;
            }

            if (i == lines - 1) {
                // Put the rest of the text into the last page
                addPage(text.subSequence(startOffset, layout.getLineEnd(i)));
                return;
            }
        }
    }

    private void addPage(CharSequence text) {
        mPages.add(text);
    }

    public int size() {
        return mPages.size();
    }

    public CharSequence get(int index) {
        return (index >= 0 && index < mPages.size()) ? mPages.get(index) : null;
    }
}