package com.example.kennzeichenerkennung;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

public class UpdateFragment extends DialogFragment {

    private String updateSize;
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
            updateSize = getArguments().getString("updateSize");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_update, container, false);

        TextView textView3 = view.findViewById(R.id.text_view3);
        CheckBox checkBox = view.findViewById(R.id.clearcheck);
        textView3.setText(String.format("Version %s\n\n%s", version, body));

        TextView sizeText = view.findViewById(R.id.sizetext);
        if (updateSize != null) {
            try {
                long sizeInBytes = Long.parseLong(updateSize);
                double sizeInMegabytes = sizeInBytes / (1024.0 * 1024.0); // Umrechnung in Megabyte
                sizeText.setText(String.format("Updategröße: %.2f MB", sizeInMegabytes));
            } catch (NumberFormatException e) {
                Log.e("UpdateFragment", "Invalid update size format", e);
                sizeText.setText("Updategröße: ungültig");
            }
        } else {
            sizeText.setText("Updategröße: nicht angegeben");
        }

        Button downloadBtn = view.findViewById(R.id.download_btn);
        downloadBtn.setOnClickListener(v -> {
            if (checkBox.isChecked()) {
                if (downloadUrl != null && version != null) {
                    ((MainActivity) requireActivity()).deleteOldDownloads();
                    //((MainActivity) requireActivity()).startDownload(downloadUrl, version);
                    dismiss();
                }
            } else {
                if (downloadUrl != null && version != null) {
                    ((MainActivity) requireActivity()).startDownload(downloadUrl, version);
                    dismiss();
                }
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