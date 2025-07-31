package de.haainz.kennzeichenerkennung;

import android.content.Context;
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
        copyAssetToFileDirIfNeeded("de_kennzeichen.csv");
        copyAssetToFileDirIfNeeded("de_kennzeichenauslaufend.csv");
        copyAssetToFileDirIfNeeded("de_sonderkennzeichen.csv");

        KennzeichenDENormalEinlesen();
        KennzeichenDESonderEinlesen();
        KennzeichenDEAuslaufendEinlesen();
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

    private void KennzeichenDENormalEinlesen() {
        try {
            File file = new File(context.getFilesDir(), "de_kennzeichen.csv");
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
                kennzeichen.aitext = record.get("KI-Text");
                kennzeichen.saved = record.get("gespeichert");
                kennzeichen.setTyp("normal_de");
                kennzeichenliste.add(kennzeichen);
            }
            reader.close();
        } catch (IOException e) {
            Log.e("KennzeichenEinlesen", "Fehler bei der Einlesung der Datei: " + e.getMessage());
        }
    }

    private void KennzeichenDESonderEinlesen() {
        try {
            File file = new File(context.getFilesDir(), "de_sonderkennzeichen.csv");
            Reader reader = new FileReader(file);
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(reader);

            for (CSVRecord record : records) {
                Kennzeichen kennzeichen = new Kennzeichen();
                kennzeichen.nationalitaetskuerzel = record.get("Nationalitätszeichen");
                kennzeichen.oertskuerzel = record.get("Unterscheidungszeichen");
                kennzeichen.stadtkreis = record.get("Zulassungsbehörde");
                kennzeichen.ort = record.get("Bedeutung");
                kennzeichen.bundesland = record.get("Typ");
                kennzeichen.aitext = record.get("KI-Text");
                kennzeichen.saved = record.get("gespeichert");
                kennzeichen.setTyp("sonder_de");
                kennzeichenliste.add(kennzeichen);
            }
            reader.close();
        } catch (IOException e) {
            Log.e("KennzeichenEinlesen", "Fehler bei der Einlesung der Datei: " + e.getMessage());
        }
    }

    private void KennzeichenDEAuslaufendEinlesen() {
        try {
            File file = new File(context.getFilesDir(), "de_kennzeichenauslaufend.csv");
            Reader reader = new FileReader(file);
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(reader);

            for (CSVRecord record : records) {
                Kennzeichen kennzeichen = new Kennzeichen();
                kennzeichen.nationalitaetskuerzel = record.get("Nationalitätszeichen");
                kennzeichen.oertskuerzel = record.get("Unterscheidungszeichen");
                kennzeichen.stadtkreis = record.get("Abwicklung");
                kennzeichen.ort = record.get("BisherigerVerwaltungsbezirkOderKreis");
                kennzeichen.aitext = record.get("KI-Text");
                kennzeichen.saved = record.get("gespeichert");
                kennzeichen.setTyp("auslaufend_de");
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
                headerWriter.write("Nationalitätszeichen,Unterscheidungszeichen,StadtOderKreis,Herleitung,Bundesland.Name,Bundesland.Iso3166-2,Fußnoten,Bemerkung,KI-Text,gespeichert\n");
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
                kennzeichen.aitext = record.get("KI-Text");
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
        if (!kennzeichen.isEigene()) {
            Log.e("delete", "Nur eigene Kennzeichen dürfen gelöscht werden.");
            return;
        }

        File file = new File(context.getFilesDir(), "kennzeicheneigene.csv");
        if (!file.exists()) {
            Log.e("delete", "Datei kennzeicheneigene.csv existiert nicht.");
            return;
        }

        try {
            // CSV einlesen
            Reader reader = new FileReader(file);
            CSVParser csvParser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .parse(reader);

            List<CSVRecord> records = csvParser.getRecords();
            List<String> headers = csvParser.getHeaderNames();

            // Neue Liste ohne das zu löschende Kennzeichen aufbauen
            List<List<String>> updatedRows = new ArrayList<>();
            String target = kennzeichen.OertskuerzelGeben();
            boolean found = false;

            for (CSVRecord record : records) {
                if (!record.get("Unterscheidungszeichen").equals(target)) {
                    List<String> row = new ArrayList<>();
                    for (String header : headers) {
                        row.add(record.get(header));
                    }
                    updatedRows.add(row);
                } else {
                    found = true;
                    Log.i("delete", "Kennzeichen zum Löschen gefunden: " + target);
                }
            }

            reader.close();

            if (!found) {
                Log.w("delete", "Kennzeichen nicht in Datei gefunden: " + target);
            }

            // Neue Datei schreiben
            FileWriter writer = new FileWriter(file);
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers.toArray(new String[0])));

            for (List<String> row : updatedRows) {
                csvPrinter.printRecord(row);
            }

            csvPrinter.flush();
            csvPrinter.close();

            // Aus In-Memory-Liste löschen
            kennzeichenliste.remove(kennzeichen);

            Log.i("delete", "Kennzeichen erfolgreich gelöscht: " + target);

        } catch (IOException e) {
            Log.e("delete", "Fehler beim Bearbeiten der Datei: " + e.getMessage());
        }
    }

    public void setaiText(Kennzeichen kennzeichen, String aitext) {
        String filename = kennzeichen.isNormalDE() ? "de_kennzeichen.csv" :
                kennzeichen.isAuslaufendDE() ? "de_kennzeichenauslaufend.csv" :
                        kennzeichen.isSonderDE() ? "de_sonderkennzeichen.csv" :
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

            int aitextIndex = headers.indexOf("KI-Text");
            int oertskuerzelIndex = headers.indexOf("Unterscheidungszeichen");

            if (aitextIndex == -1 || oertskuerzelIndex == -1) {
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

                if (record.get("Unterscheidungszeichen").equals(kennzeichen.OertskuerzelGeben()) && record.get(3).equals(kennzeichen.OrtGeben())) {
                    row.set(aitextIndex, aitext);
                    found = true;
                    Log.e("KennzeichenEinlesen", "KI-Text-Wert geändert für: " + record.get("Unterscheidungszeichen") + ", " + record.get(3));
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

            kennzeichen.aitext = aitext;

            if (!found) {
                Log.w("KennzeichenEinlesen", "Kennzeichen nicht gefunden in: " + filename);
            }

        } catch (IOException e) {
            Log.e("KennzeichenEinlesen", "Fehler beim Bearbeiten der Datei: " + e.getMessage());
        }
    }

    public void changesavestatus(Kennzeichen kennzeichen, String savestatus) {
        String filename = kennzeichen.isNormalDE() ? "de_kennzeichen.csv" :
                kennzeichen.isAuslaufendDE() ? "de_kennzeichenauslaufend.csv" :
                        kennzeichen.isSonderDE() ? "de_sonderkennzeichen.csv" :
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