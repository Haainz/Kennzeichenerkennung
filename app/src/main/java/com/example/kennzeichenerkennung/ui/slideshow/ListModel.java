package com.example.kennzeichenerkennung.ui.slideshow;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ListModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public ListModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is list fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}