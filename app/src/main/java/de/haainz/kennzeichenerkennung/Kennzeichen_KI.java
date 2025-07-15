package de.haainz.kennzeichenerkennung;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Ein Programm, das Fragen nach Ortskürzeln mit dem passenden Ort beantwortet.
 * Daten der KfZ-Kennzeichen
 * CC-BY Berlin Open Data https://berlinonline.github.io/berlin-legacy-datasets/data/kfz-kennz-d.csv
 * bzw. https://www.govdata.de/web/guest/suchen/-/details/kfz-kennzeichen-deutschland3785e
 *
 * @author (Stefan Seegerer, Peter Brichzin)
 * @version (18.5.23)
 */
public class Kennzeichen_KI {
    private ArrayList<Kennzeichen> kennzeichenliste;
    private Context context;

    public Kennzeichen_KI(Context context) {
        this.context = context;
        kennzeichenliste = new ArrayList<>();

        // 📁 Sicherstellen, dass die CSV-Dateien im internen Speicher existieren
        copyAssetToFileDirIfNeeded("kennzeichen.csv");
        copyAssetToFileDirIfNeeded("kennzeichenauslaufend.csv");
        copyAssetToFileDirIfNeeded("sonderkennzeichen.csv");

        KennzeichenNormalEinlesen();
        KennzeichenSonderEinlesen();
        KennzeichenAuslaufendEinlesen();
        KennzeichenEigeneEinlesen();
    }

    public ArrayList<Kennzeichen> getKennzeichenListe() {
        return kennzeichenliste;
    }

    public String OrtZuKennzeichenAusgeben(String kennzeichen)
    {
        String ort = "Dieses Kennzeichen kenne ich leider nicht 😒!";
        for (Kennzeichen value : kennzeichenliste) {
            if (value.OertskuerzelGeben() != null && value.OertskuerzelGeben().equals(kennzeichen)) {
                ort = value.OrtGeben();
                break;
            }
        }
        return ort;
    }

    public String BundeslandZuKennzeichenAusgeben(String kennzeichen)
    {
        String bundesland = "";
        for (Kennzeichen value : kennzeichenliste) {
            if (value.OertskuerzelGeben() != null && value.OertskuerzelGeben().equals(kennzeichen)) {
                bundesland = ", "+value.BundeslandGeben();
                break;
            }
        }
        return bundesland;
    }

    public Kennzeichen getKennzeichen(String kuerzel) {
        for (Kennzeichen kennzeichen : kennzeichenliste) {
            if (kennzeichen.OrtGeben() != null && kennzeichen.OertskuerzelGeben().equals(kuerzel)) {
                return kennzeichen;
            }
        }
        return null;
    }

    private void KennzeichenNormalEinlesen() {
        try {
            File file = new File(context.getFilesDir(), "kennzeichen.csv");
            Reader reader = new FileReader(file);
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(reader);

            for (CSVRecord record : records) {
                Kennzeichen kennzeichen = new Kennzeichen();
                kennzeichen.nationalitaetskuerzel = record.get("Nationalitätszeichen");
                kennzeichen.oertskuerzel = record.get("Unterscheidungszeichen");
                kennzeichen.stadtkreis = record.get("StadtOderKreis");
                kennzeichen.ort = record.get("Herleitung");
                kennzeichen.bundesland = record.get("Bundesland.Name");
                kennzeichen.bundeslandiso = record.get("Bundesland.Iso3166-2");
                kennzeichen.fussnote = record.get("Fußnoten");
                kennzeichen.bemerkungen = record.get("Bemerkung");
                kennzeichen.saved = record.get("gespeichert");
                kennzeichen.setTyp("normal");
                kennzeichenliste.add(kennzeichen);
            }
            reader.close();
        } catch (IOException e) {
            Log.e("KennzeichenEinlesen", "Fehler bei der Einlesung der Datei: " + e.getMessage());
        }
    }


    private void KennzeichenSonderEinlesen() {
        try {
            File file = new File(context.getFilesDir(), "sonderkennzeichen.csv");
            Reader reader = new FileReader(file);
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(reader);

            for (CSVRecord record : records) {
                Kennzeichen kennzeichen = new Kennzeichen();
                kennzeichen.nationalitaetskuerzel = record.get("Nationalitätszeichen");
                kennzeichen.oertskuerzel = record.get("Unterscheidungszeichen");
                kennzeichen.stadtkreis = record.get("Zulassungsbehörde");
                kennzeichen.ort = record.get("Bedeutung");
                kennzeichen.bundesland = record.get("Typ");
                kennzeichen.saved = record.get("gespeichert");
                kennzeichen.setTyp("sonder");
                kennzeichenliste.add(kennzeichen);
            }
            reader.close();
        } catch (IOException e) {
            Log.e("KennzeichenEinlesen", "Fehler bei der Einlesung der Datei: " + e.getMessage());
        }
    }

    private void KennzeichenAuslaufendEinlesen() {
        try {
            File file = new File(context.getFilesDir(), "kennzeichenauslaufend.csv");
            Reader reader = new FileReader(file);
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(reader);

            for (CSVRecord record : records) {
                Kennzeichen kennzeichen = new Kennzeichen();
                kennzeichen.nationalitaetskuerzel = record.get("Nationalitätszeichen");
                kennzeichen.oertskuerzel = record.get("Unterscheidungszeichen");
                kennzeichen.stadtkreis = record.get("Abwicklung");
                kennzeichen.ort = record.get("BisherigerVerwaltungsbezirkOderKreis");
                kennzeichen.saved = record.get("gespeichert");
                kennzeichen.setTyp("auslaufend");
                kennzeichenliste.add(kennzeichen);
            }
            reader.close();
        } catch (IOException e) {
            Log.e("KennzeichenEinlesen", "Fehler bei der Einlesung der Datei: " + e.getMessage());
        }
    }

    private void KennzeichenEigeneEinlesen() {
        try {
            File file = new File(context.getFilesDir(), "kennzeicheneigene.csv");
            if (!file.exists()) {
                file.createNewFile();
                FileWriter headerWriter = new FileWriter(file);
                headerWriter.write("Nationalitätszeichen,Unterscheidungszeichen,StadtOderKreis,Herleitung,Bundesland.Name,Bundesland.Iso3166-2,Fußnoten,Bemerkung,gespeichert\n");
                headerWriter.close();
            }
            Reader reader = new FileReader(file);
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(reader);
            for (CSVRecord record : records) {
                Kennzeichen kennzeichen = new Kennzeichen();
                kennzeichen.nationalitaetskuerzel = record.get("Nationalitätszeichen");
                kennzeichen.oertskuerzel = record.get("Unterscheidungszeichen");
                kennzeichen.stadtkreis = record.get("StadtOderKreis");
                kennzeichen.ort = record.get("Herleitung");
                kennzeichen.bundesland = record.get("Bundesland.Name");
                kennzeichen.bundeslandiso = record.get("Bundesland.Iso3166-2");
                kennzeichen.fussnote = record.get("Fußnoten");
                kennzeichen.bemerkungen = record.get("Bemerkung");
                kennzeichen.saved = record.get("gespeichert");
                kennzeichen.setTyp("eigene");
                kennzeichenliste.add(kennzeichen);
            }
            reader.close();
        } catch (IOException e) {
            Log.e("KennzeichenEinlesen", "Fehler bei der Einlesung der Datei: " + e.getMessage());
        }
    }

    private void copyAssetToFileDirIfNeeded(String filename) {
        File outFile = new File(context.getFilesDir(), filename);
        if (!outFile.exists()) {
            try (InputStream in = context.getAssets().open(filename);
                 FileWriter writer = new FileWriter(outFile)) {

                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line + "\n");
                }
                writer.flush();
                Log.i("KennzeichenEinlesen", "Datei kopiert: " + filename);
            } catch (IOException e) {
                Log.e("KennzeichenEinlesen", "Fehler beim Kopieren von " + filename + ": " + e.getMessage());
            }
        }
    }

    public void deleteKennzeichen(Kennzeichen kennzeichen) {
        if (kennzeichen.getTyp().equals("eigene")) {
            try {
                File file = new File(context.getFilesDir(), "kennzeicheneigene.csv");
                if (file.exists()) {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    StringBuilder content = new StringBuilder();
                    String line;
                    reader.readLine(); // Überspringen der Spaltennamen
                    while ((line = reader.readLine()) != null) {
                        String[] values = line.split(",(?! )");
                        if (!values[1].equals(kennzeichen.OertskuerzelGeben())) {
                            content.append(line).append("\n");
                        }
                    }
                    reader.close();
                } else {
                    Log.e("KennzeichenEinlesen", "Die Datei kennzeicheneigene.csv existiert nicht.");
                }
            } catch (IOException e) {

                Log.e("KennzeichenEinlesen", "Fehler bei der Einlesung der Datei: " + e.getMessage());
            }
            kennzeichenliste.remove(kennzeichen);
        }
    }

    public void changesavestatus(Kennzeichen kennzeichen, String savestatus) {
        String filename = kennzeichen.isNormal() ? "kennzeichen.csv" :
                kennzeichen.isAuslaufend() ? "kennzeichenauslaufend.csv" :
                        kennzeichen.isSonder() ? "sonderkennzeichen.csv" :
                                kennzeichen.isEigene() ? "kennzeicheneigene.csv" : null;

        if (filename == null) {
            Log.e("KennzeichenEinlesen", "Unbekannter Typ für Kennzeichen: " + kennzeichen.getTyp());
            return;
        }

        File file = new File(context.getFilesDir(), filename);
        if (!file.exists()) {
            Log.e("KennzeichenEinlesen", "Datei existiert nicht: " + filename);
            return;
        }

        try (
                Reader reader = new FileReader(file);
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
        ) {
            List<CSVRecord> records = csvParser.getRecords();
            List<String> headers = csvParser.getHeaderNames();

            int gespeichertIndex = headers.indexOf("gespeichert");
            int oertskuerzelIndex = headers.indexOf("Unterscheidungszeichen");

            if (gespeichertIndex == -1 || oertskuerzelIndex == -1) {
                Log.e("KennzeichenEinlesen", "Wichtige Spalte fehlt in: " + filename);
                return;
            }

            List<List<String>> updatedRows = new ArrayList<>();
            boolean found = false;

            for (CSVRecord record : records) {
                List<String> row = new ArrayList<>();
                for (String header : headers) {
                    row.add(record.get(header));
                }

                if (record.get("Unterscheidungszeichen").equals(kennzeichen.OertskuerzelGeben())) {
                    row.set(gespeichertIndex, savestatus);
                    found = true;
                    Log.e("KennzeichenEinlesen", "Gespeichert-Wert geändert für: " + record.get("Unterscheidungszeichen"));
                }

                updatedRows.add(row);
            }

            // Schreibe aktualisierte CSV zurück
            try (
                    FileWriter writer = new FileWriter(file);
                    CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers.toArray(new String[0])))
            ) {
                for (List<String> row : updatedRows) {
                    csvPrinter.printRecord(row);
                }
            }

            kennzeichen.saved = savestatus;

            if (!found) {
                Log.w("KennzeichenEinlesen", "Kennzeichen nicht gefunden in: " + filename);
            }

        } catch (IOException e) {
            Log.e("KennzeichenEinlesen", "Fehler beim Bearbeiten der Datei: " + e.getMessage());
        }
    }
}