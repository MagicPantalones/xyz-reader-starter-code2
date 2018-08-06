package com.example.xyzreader.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.widget.TextView;

import com.example.xyzreader.data.ItemsContract;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ru.noties.markwon.Markwon;

public class MarkdownCreator {

    private static final String TAG = MarkdownCreator.class.getSimpleName();

    private static final String[] PROJECTION_SINGLE = {
            ItemsContract.Items.BODY
    };

    private final int id;
    private final Context context;
    private final MarkdownListener listener;
    private final TextView textView;

    private Disposable renderDisposable;


    public interface MarkdownListener {
        void onMarkdownRendered(Pagination pages);
    }

    public MarkdownCreator(Context context, int id, TextView textView, MarkdownListener listener) {
        this.context = context;
        this.id = id;
        this.textView = textView;
        this.listener = listener;
        init();
    }

    private void init() {
        DataUtils.dispose(renderDisposable);
        renderDisposable = Observable.just(fetchTextAndParse())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(listener::onMarkdownRendered);
    }

    private Pagination fetchTextAndParse() {

        Uri uri = ItemsContract.Items.buildItemUri(id);

        Cursor cursor = context.getContentResolver().query(
                uri,
                PROJECTION_SINGLE,
                null,
                null,
                null);

        if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
            final String markdown = cursor.getString(cursor.getColumnIndex(ItemsContract.Items.BODY));
            cursor.close();
            CharSequence retMarkdown = Markwon.markdown(context, markdown);
            return new Pagination(retMarkdown, textView);
        }
        return null;
    }

    public void cancel() {
        DataUtils.dispose(renderDisposable);
    }

}
