package com.example.xyzreader.data;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;

import com.example.xyzreader.utils.Pagination;

public class BodyViewModel extends ViewModel{

    private MutableLiveData<Pagination> pages = new MutableLiveData<>();
    private MutableLiveData<Pagination> landPages = new MutableLiveData<>();

    public void setPages(Pagination pager, boolean isLandscape) {
        if (isLandscape) landPages.setValue(pager);
        else pages.setValue(pager);
    }

    public boolean hasData(){
        return pages.getValue() != null && landPages.getValue() != null;
    }

    public CharSequence getPage(int pageNum, boolean isLandscape) {
        if (isLandscape) {
            if (landPages.getValue() == null) return null;
            return landPages.getValue().get(pageNum);
        }

        if (pages.getValue() == null) return null;
        return pages.getValue().get(pageNum);
    }

    public void registerObserver(LifecycleOwner lifecycleOwner, Observer<Pagination> observer,
                                 boolean isLandscape) {
        if (isLandscape) landPages.observe(lifecycleOwner, observer);
        else pages.observe(lifecycleOwner, observer);
    }

    public void unregisterObserver(Observer<Pagination> observer) {
        pages.removeObserver(observer);
        landPages.removeObserver(observer);
    }
}
