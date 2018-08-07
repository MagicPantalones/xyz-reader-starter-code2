package com.example.xyzreader.utils;

import android.annotation.SuppressLint;
import android.os.Build;

import io.reactivex.disposables.Disposable;

public class DataUtils {

    public static final int SDK_V = Build.VERSION.SDK_INT;

    public static void dispose(Disposable... disposables) {
        for (Disposable disposable : disposables) {
            if (disposable != null && !disposable.isDisposed()) disposable.dispose();
        }
    }

    @SuppressLint("DefaultLocale")
    public static String convertToFraction(float aspectRatio){
        String textNum = String.valueOf(aspectRatio);
        int numDecimals = textNum.length() - 1 - textNum.indexOf('.');
        int den = 1;

        for (int i = 0; i < numDecimals; i++) {
            aspectRatio *= 10;
            den *= 10;
        }
        int num = Math.round(aspectRatio);
        int commonDenom = commonFactor(num, den);
        return String.format("%d:%d", num/commonDenom, den/commonDenom);
    }

    private static int commonFactor(int num, int den) {
        if (den == 0) return num;
        return commonFactor(den, num % den);
    }

    private DataUtils() {}
}
