package com.example.xyzreader.data;

import android.os.Build;

import io.reactivex.disposables.Disposable;

public class DataUtils {

    public static final int SDK_V = Build.VERSION.SDK_INT;

    public static void dispose(Disposable... disposables) {
        for (Disposable disposable : disposables) {
            if (disposable != null && disposable.isDisposed()) disposable.dispose();
        }
    }

}
