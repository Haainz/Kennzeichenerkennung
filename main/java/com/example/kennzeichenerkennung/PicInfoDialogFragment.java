package com.example.kennzeichenerkennung;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class PicInfoDialogFragment extends DialogFragment {
    private String info;

    public PicInfoDialogFragment(String info) {
        this.info = info;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pic_info_dialog, container, false);
        TextView textView = view.findViewById(R.id.textView);
        textView.setText(info);
        return view;
    }
}