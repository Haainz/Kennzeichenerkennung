package com.example.kennzeichenerkennung;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

public class UpdateFragment extends DialogFragment {

    private String downloadUrl;
    private String version;
    private String body;

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            version = getArguments().getString("version");
            body = getArguments().getString("body");
            downloadUrl = getArguments().getString("downloadUrl");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_update, container, false);

        TextView textView3 = view.findViewById(R.id.text_view3);
        textView3.setText(String.format("Version %s\n\n%s", version, body));

        Button downloadBtn = view.findViewById(R.id.download_btn);
        downloadBtn.setOnClickListener(v -> {
            if (downloadUrl != null && version != null) {
                ((MainActivity) requireActivity()).startDownload(downloadUrl, version);
                dismiss();
            }
        });

        Button stopBtn = view.findViewById(R.id.stop_btn);
        stopBtn.setOnClickListener(v -> dismiss());

        ImageButton xBtn = view.findViewById(R.id.x);
        xBtn.setOnClickListener(v -> dismiss());

        TextView linkText = view.findViewById(R.id.text_view2);
        linkText.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Haainz/Kennzeichenerkennung/releases/latest/"));
            startActivity(browserIntent);
        });

        return view;
    }
}