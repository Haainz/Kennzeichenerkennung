package de.haainz.kennzeichenerkennung;

import static org.osmdroid.tileprovider.cachemanager.CacheManager.getFileName;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UploadFragment extends DialogFragment {
    private static final int REQUEST_CODE_IMPORT_FILE = 100;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.CustomDialogfullTheme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upload, container, false);

        ImageButton xBtn  = view.findViewById(R.id.x);
        xBtn.setOnClickListener(v -> dismiss());

        // Button-Handler
        Button hochladenButton = view.findViewById(R.id.import_btn);
        hochladenButton.setOnClickListener(v -> importFile());

        return view;
    }

    private void importFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("text/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_IMPORT_FILE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_IMPORT_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                handleCsvImport(uri);
            }
        }
    }

    private void handleCsvImport(Uri uri) {
        // Überprüfe Dateinamen
        String filename = getFileName(uri);
        if (filename == null || !filename.matches("de_(kennzeichenstandard|sonderkennzeichen|kennzeichenauslaufend|kennzeicheneigene)_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}\\.csv")) {
            showErrorDialog("Ungültiger Dateiname. Erwarte z. B.: de_kennzeicheneigene_2025-07-21_14-30.csv");
            return;
        }

        // Datei lesen & Header validieren
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
             Reader reader = new InputStreamReader(in)) {

            CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
            List<String> headers = parser.getHeaderNames();

            if (!isHeaderValid(filename, headers)) {
                showErrorDialog("CSV-Header ist ungültig oder unvollständig.");
                return;
            }

            showConfirmDialog(filename, parser.getRecords());

        } catch (IOException e) {
            showErrorDialog("Fehler beim Lesen der Datei: " + e.getMessage());
        }
    }

    private boolean isHeaderValid(String filename, List<String> headers) {
        List<String> expected;

        if (filename.contains("standard")) {
            expected = Arrays.asList("Nationalitätszeichen", "Unterscheidungszeichen", "StadtOderKreis", "Herleitung",
                    "Bundesland.Name", "Bundesland.Iso3166-2", "Fußnoten", "Bemerkung", "gespeichert");
        } else if (filename.contains("sonder")) {
            expected = Arrays.asList("Nationalitätszeichen", "Unterscheidungszeichen", "Zulassungsbehörde", "Bedeutung", "Typ", "gespeichert");
        } else if (filename.contains("auslaufend")) {
            expected = Arrays.asList("Nationalitätszeichen", "Unterscheidungszeichen", "Abwicklung", "BisherigerVerwaltungsbezirkOderKreis", "gespeichert");
        } else if (filename.contains("eigene")) {
            expected = Arrays.asList("Nationalitätszeichen", "Unterscheidungszeichen", "StadtOderKreis", "Herleitung",
                    "Bundesland.Name", "Bundesland.Iso3166-2", "Fußnoten", "Bemerkung", "gespeichert");
        } else {
            return false;
        }

        return headers.containsAll(expected);
    }

    private void showConfirmDialog(String filename, List<CSVRecord> records) {
        String timestamp = extractTimestampFromFilename(filename);
        String readableTimestamp = formatTimestampToReadable(timestamp);

        String beschreibung;

        if (filename.contains("standard")) {
            beschreibung = "die gespeicherten Standardkennzeichen ersetzen";
        } else if (filename.contains("sonder")) {
            beschreibung = "die gespeicherten Sonderkennzeichen ersetzen";
        } else if (filename.contains("auslaufend")) {
            beschreibung = "die gespeicherten auslaufenden Kennzeichen ersetzen";
        } else if (filename.contains("eigene")) {
            beschreibung = "die alten eigenen Kennzeichen löschen und überschreiben";
        } else {
            beschreibung = "die vorhandenen Daten ersetzen";
        }

        String nachricht = "Möchtest du wirklich " + beschreibung + " mit den Daten vom " + readableTimestamp + "?";

        new AlertDialog.Builder(requireContext())
                .setTitle("Daten überschreiben?")
                .setMessage(nachricht)
                .setPositiveButton("Ja", (dialog, which) -> {
                    showLoading(true);
                    new Thread(() -> {
                        if (filename.contains("eigene")) {
                            replaceEigeneKennzeichen(records);
                        } else {
                            compareAndUpdateStatus(filename, records);
                        }
                        requireActivity().runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(requireContext(), "Import abgeschlossen", Toast.LENGTH_SHORT).show();
                            dismiss();
                        });
                    }).start();
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void showErrorDialog(String message) {
        requireActivity().runOnUiThread(() ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Fehler beim Import")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show()
        );
    }

    private void showLoading(boolean show) {
        // Ein einfaches Beispiel mit ProgressBar
        ProgressBar progressBar = getView().findViewById(R.id.progress_bar);
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void compareAndUpdateStatus(String filename, List<CSVRecord> records) {
        Kennzeichen_KI ki = new Kennzeichen_KI(requireContext());

        List<String> nichtGefunden = new ArrayList<>();

        for (CSVRecord record : records) {
            String kuerzel = record.get("Unterscheidungszeichen");
            String status = record.get("gespeichert");

            Kennzeichen existing = ki.getKennzeichen(kuerzel);
            if (existing != null) {
                if (!status.equals(existing.saved)) {
                    ki.changesavestatus(existing, status);
                }
            } else {
                nichtGefunden.add(kuerzel);
            }
        }

        if (!nichtGefunden.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append("Einige Kennzeichen konnten nicht aktualisiert werden, da sie in der bestehenden Liste nicht vorhanden sind:\n\n");
            for (String k : nichtGefunden) {
                message.append("- ").append(k).append("\n");
            }
            message.append("\nNur die vorhandenen Kennzeichen wurden aktualisiert.");

            requireActivity().runOnUiThread(() -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Teilweiser Import")
                        .setMessage(message.toString())
                        .setPositiveButton("OK", null)
                        .show();
            });
        }
    }

    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    private String extractTimestampFromFilename(String filename) {
        // Extrahiert z. B. "2025-07-21_14-30-15"
        int underscoreIndex = filename.lastIndexOf('_');
        int dotIndex = filename.lastIndexOf('.');
        if (underscoreIndex != -1 && dotIndex != -1 && dotIndex > underscoreIndex) {
            return filename.substring(underscoreIndex - 10, dotIndex); // 10 vor '_' bis '.'
        }
        return "";
    }

    private String formatTimestampToReadable(String timestamp) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault());
            Date date = inputFormat.parse(timestamp);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy, HH:mm 'Uhr'", Locale.getDefault());
            return outputFormat.format(date != null ? date : new Date());
        } catch (Exception e) {
            return timestamp;
        }
    }

    private void replaceEigeneKennzeichen(List<CSVRecord> records) {
        File file = new File(getActivity().getFilesDir(), "kennzeicheneigene.csv");

        try {
            // Datei überschreiben (löschen und neu erstellen mit Header)
            FileOutputStream outputStream = new FileOutputStream(file, false); // false = überschreiben
            String header = "Nationalitätszeichen,Unterscheidungszeichen,StadtOderKreis,Herleitung,Bundesland.Name,Bundesland.Iso3166-2,Fußnoten,Bemerkung,gespeichert\n";
            outputStream.write(header.getBytes());

            // Neue Datensätze hinzufügen
            for (CSVRecord record : records) {
                String nationalitaetszeichen = record.get("Nationalitätszeichen");
                String unterscheidungszeichen = record.get("Unterscheidungszeichen");
                String stadtOderKreis = record.get("StadtOderKreis");
                String herleitung = record.get("Herleitung");
                String bundeslandName = record.get("Bundesland.Name");
                String bundeslandIso31662 = record.get("Bundesland.Iso3166-2");
                String fussnoten = record.get("Fußnoten");
                String bemerkung = record.get("Bemerkung");
                String gespeichert = record.get("gespeichert");

                String csvZeile = nationalitaetszeichen + "," + unterscheidungszeichen + "," + stadtOderKreis + "," +
                        herleitung + "," + bundeslandName + "," + bundeslandIso31662 + "," +
                        fussnoten + "," + bemerkung + "," + gespeichert + "\n";

                outputStream.write(csvZeile.getBytes());
            }

            outputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(getActivity(), "Fehler beim Schreiben der Datei", Toast.LENGTH_SHORT).show()
            );
        }
    }
}
