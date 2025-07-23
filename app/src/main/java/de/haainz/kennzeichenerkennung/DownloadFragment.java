package de.haainz.kennzeichenerkennung;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DownloadFragment extends DialogFragment {

    private CheckBox deChk, standardChk, sonderChk, auslaufendChk, eigeneChk;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.CustomDialogfullTheme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_download, container, false);

        // Checkboxen initialisieren
        deChk = view.findViewById(R.id.de_chk);
        standardChk = view.findViewById(R.id.standard_chk);
        sonderChk = view.findViewById(R.id.sonder_chk);
        auslaufendChk = view.findViewById(R.id.auslaufend_chk);
        eigeneChk = view.findViewById(R.id.eigene_chk);

        ImageButton xBtn  = view.findViewById(R.id.x);
        xBtn.setOnClickListener(v -> dismiss());

        // Hauptcheckbox steuert Unterboxen
        deChk.setOnCheckedChangeListener((buttonView, isChecked) -> {
            standardChk.setChecked(isChecked);
            sonderChk.setChecked(isChecked);
            auslaufendChk.setChecked(isChecked);
        });

        CompoundButton.OnCheckedChangeListener unterboxListener = (buttonView, isChecked) -> {
            if (!isChecked) {
                // Bei Deaktivierung einer Unterbox: Hauptcheckbox deaktivieren
                deChk.setOnCheckedChangeListener(null);
                deChk.setChecked(false);
                deChk.setOnCheckedChangeListener(this::onMainCheckboxChanged);
            } else {
                // Prüfen ob alle Unterboxen aktiv sind -> Hauptcheckbox aktivieren
                if (standardChk.isChecked() && sonderChk.isChecked() && auslaufendChk.isChecked()) {
                    deChk.setOnCheckedChangeListener(null);
                    deChk.setChecked(true);
                    deChk.setOnCheckedChangeListener(this::onMainCheckboxChanged);
                }
            }
        };

        standardChk.setOnCheckedChangeListener(unterboxListener);
        sonderChk.setOnCheckedChangeListener(unterboxListener);
        auslaufendChk.setOnCheckedChangeListener(unterboxListener);
        deChk.setOnCheckedChangeListener(this::onMainCheckboxChanged);

        // Button-Handler
        Button speichernButton = view.findViewById(R.id.export_btn);
        speichernButton.setOnClickListener(v -> export());

        return view;
    }

    private void export() {
        File filesDir = requireContext().getFilesDir();
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        ArrayList<String> exportedFiles = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        if (standardChk.isChecked()) {
            String name = "de_kennzeichenstandard_" + timestamp + ".csv";
            exportFile("de_kennzeichen.csv", filesDir, downloadDir, name);
            exportedFiles.add(name);
        }
        if (sonderChk.isChecked()) {
            String name = "de_sonderkennzeichen_" + timestamp + ".csv";
            exportFile("de_sonderkennzeichen.csv", filesDir, downloadDir, name);
            exportedFiles.add(name);
        }
        if (auslaufendChk.isChecked()) {
            String name = "de_kennzeichenauslaufend_" + timestamp + ".csv";
            exportFile("de_kennzeichenauslaufend.csv", filesDir, downloadDir, name);
            exportedFiles.add(name);
        }
        if (eigeneChk.isChecked()) {
            String name = "de_kennzeicheneigene_" + timestamp + ".csv";
            exportFile("kennzeicheneigene.csv", filesDir, downloadDir, name);
            exportedFiles.add(name);
        }

        if (exportedFiles.isEmpty()) {
            Snackbar.make(requireView(), "Keine Dateien ausgewählt!", Snackbar.LENGTH_SHORT).show();
        } else {
            String messageText;
            String firstFileName = exportedFiles.get(0);
            File fileToOpen = new File(downloadDir, firstFileName);

            if (exportedFiles.size() == 1) {
                messageText = firstFileName;
            } else {
                messageText = exportedFiles.size() + " Dateien exportiert";
            }

            LinearLayout notification = requireView().findViewById(R.id.export_notification);
            TextView message = requireView().findViewById(R.id.export_message);
            Button openBtn = requireView().findViewById(R.id.open_file_btn);
            Button folderBtn = requireView().findViewById(R.id.open_folder_btn);

            message.setText(messageText);
            notification.setVisibility(View.VISIBLE);

            openBtn.setOnClickListener(v -> openFile(fileToOpen));
            folderBtn.setOnClickListener(v -> openFolder(fileToOpen));
        }
    }

    private void exportFile(String filename, File inputDir, File outputDir, String exportName) {
        File sourceFile = new File(inputDir, filename);
        File destFile = new File(outputDir, exportName);

        try (FileInputStream inStream = new FileInputStream(sourceFile);
             FileOutputStream outStream = new FileOutputStream(destFile)) {

            byte[] buffer = new byte[1024];
            int length;

            while ((length = inStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, length);
            }

        } catch (IOException e) {
            Log.e("Export", "Fehler beim Exportieren von " + filename + ": " + e.getMessage());
        }
    }

    private void openFile(File file) {
        try {
            Uri fileUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    file
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "text/csv");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, "Datei öffnen mit"));
        } catch (Exception e) {
            Log.e("FileOpen", "Fehler beim Öffnen der Datei: " + e.getMessage());
            Snackbar.make(requireView(), "Fehler beim Öffnen!", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void openFolder(File file) {
        try {
            Uri fileUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    file
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, "Speicherort anzeigen"));
        } catch (Exception e) {
            Log.e("OpenFolder", "Fehler beim Anzeigen des Speicherorts: " + e.getMessage());
            Snackbar.make(requireView(), "Fehler beim Anzeigen des Speicherorts!", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void onMainCheckboxChanged(CompoundButton buttonView, boolean isChecked) {
        standardChk.setChecked(isChecked);
        sonderChk.setChecked(isChecked);
        auslaufendChk.setChecked(isChecked);
    }
}
