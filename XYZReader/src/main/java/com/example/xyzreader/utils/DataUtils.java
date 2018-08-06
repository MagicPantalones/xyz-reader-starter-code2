package com.example.xyzreader.utils;

import android.content.Context;
import android.os.Build;
import android.util.TypedValue;

import io.reactivex.disposables.Disposable;

public class DataUtils {

    public static final int SDK_V = Build.VERSION.SDK_INT;

    public static void dispose(Disposable... disposables) {
        for (Disposable disposable : disposables) {
            if (disposable != null && !disposable.isDisposed()) disposable.dispose();
        }
    }

    public static int dpToPx(Context context, int dip) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip,
                context.getResources().getDisplayMetrics());
    }
}
