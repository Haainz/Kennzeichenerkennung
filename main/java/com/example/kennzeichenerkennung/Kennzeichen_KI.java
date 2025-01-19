package com.example.kennzeichenerkennung;

import static android.app.PendingIntent.getActivity;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

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
    private ArrayList<Kennzeichen> likedliste;
    private Context context;

    public Kennzeichen_KI(Context context) {
        this.context = context;
        kennzeichenliste = new ArrayList<Kennzeichen>();
        KennzeichenNormalEinlesen();
        KennzeichenSonderEinlesen();
        KennzeichenAuslaufendEinlesen();
        KennzeichenEigeneEinlesen();
        likedliste = new ArrayList<Kennzeichen>();
        KennzeichenLikedEinlesen();
    }

    public ArrayList<Kennzeichen> getKennzeichenListe() {
        return kennzeichenliste;
    }

    public ArrayList<Kennzeichen> getLikedliste() {
        return likedliste;
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

    public boolean LikeÜberprüfen(String kuerzel) {
        KennzeichenLikedEinlesen(); // Sicherstellen, dass die likedliste aktuell ist
        for (Kennzeichen kennzeichen : likedliste) {
            if (kennzeichen.OertskuerzelGeben().equals(kuerzel)) {
                return true;
            }
        }
        return false;
    }

    private void KennzeichenNormalEinlesen() {
        try {
            InputStream inputStream = context.getAssets().open("kennzeichen.csv");
            Reader reader = new InputStreamReader(inputStream);
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
                kennzeichen.setTyp("normal");
                kennzeichenliste.add(kennzeichen);
            }
        } catch (IOException e) {
            Log.e("KennzeichenEinlesen", "Fehler bei der Einlesung der Datei: " + e.getMessage());
        }
    }

    private void KennzeichenSonderEinlesen() {
        try {
            InputStream inputStream = context.getAssets().open("sonderkennzeichen.csv");
            Reader reader = new InputStreamReader(inputStream);
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(reader);

            for (CSVRecord record : records) {
                Kennzeichen kennzeichen = new Kennzeichen();
                kennzeichen.nationalitaetskuerzel = record.get("Nationalitätszeichen");
                kennzeichen.oertskuerzel = record.get("Unterscheidungszeichen");
                kennzeichen.stadtkreis = record.get("Zulassungsbehörde");
                kennzeichen.ort = record.get("Bedeutung");
                kennzeichen.bundesland = record.get("Typ");
                kennzeichen.setTyp("sonder");
                kennzeichenliste.add(kennzeichen);
            }
        } catch (IOException e) {
            Log.e("KennzeichenEinlesen", "Fehler bei der Einlesung der Datei: " + e.getMessage());
        }
    }

    private void KennzeichenAuslaufendEinlesen() {
        try {
            InputStream inputStream = context.getAssets().open("kennzeichenauslaufend.csv");
            Reader reader = new InputStreamReader(inputStream);
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(reader);

            for (CSVRecord record : records) {
                Kennzeichen kennzeichen = new Kennzeichen();
                kennzeichen.nationalitaetskuerzel = record.get("Nationalitätszeichen");
                kennzeichen.oertskuerzel = record.get("Unterscheidungszeichen");
                kennzeichen.stadtkreis = record.get("Abwicklung");
                kennzeichen.ort = record.get("BisherigerVerwaltungsbezirkOderKreis");
                kennzeichen.bemerkungen = "Abwicklung durch die "+record.get("Abwicklung");
                kennzeichen.setTyp("auslaufend");
                kennzeichenliste.add(kennzeichen);
            }
        } catch (IOException e) {
            Log.e("KennzeichenEinlesen", "Fehler bei der Einlesung der Datei: " + e.getMessage());
        }
    }

    private void KennzeichenEigeneEinlesen() {
        try {
            File file = new File(context.getFilesDir(), "kennzeicheneigene.csv");
            if (!file.exists()) {
                // Erstellen Sie die Datei, wenn sie nicht existiert
                file.createNewFile();
            }
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String headerLine = reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",(?! )");
                Kennzeichen kennzeichen = new Kennzeichen();
                if (values.length > 0) {
                    kennzeichen.nationalitaetskuerzel = values[0];
                }
                if (values.length > 1) {
                    kennzeichen.oertskuerzel = values[1];
                }
                if (values.length > 2) {
                    kennzeichen.stadtkreis = values[2];
                }
                if (values.length > 3) {
                    kennzeichen.ort = values[3];
                }
                if (values.length > 4) {
                    kennzeichen.bundesland = values[4];
                }
                if (values.length > 7) {
                    kennzeichen.bemerkungen = values[7];
                }
                kennzeichen.setTyp("eigene");
                kennzeichenliste.add(kennzeichen);
            }
            reader.close();
        } catch (IOException e) {
            Log.e("KennzeichenEinlesen", "Fehler bei der Einlesung der Datei: " + e.getMessage());
        }
    }

    public void KennzeichenLikedEinlesen() {
        try {
            File file = new File(context.getFilesDir(), "kennzeichenliked.csv");
            if (!file.exists()) {
                file.createNewFile();
            }
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(","); // Korrektur hier
                if (values.length > 2) { // Sicherstellen, dass genügend Werte vorhanden sind
                    Kennzeichen kennzeichen = new Kennzeichen();
                    kennzeichen.nationalitaetskuerzel = values[0];
                    kennzeichen.oertskuerzel = values[1];
                    kennzeichen.ort = values[2];
                    kennzeichen.setTyp("liked");
                    likedliste.add(kennzeichen);
                }
            }
            reader.close();
        } catch (IOException e) {
            Log.e("KennzeichenEinlesen", "Fehler bei der Einlesung der Datei: " + e.getMessage());
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
                    FileWriter writer = new FileWriter(file);
                    writer.write("Oertskuerzel,Stadtkreis,Ort,Bundesland\n");
                    writer.write(content.toString());
                    writer.close();
                } else {
                    Log.e("KennzeichenEinlesen", "Die Datei kennzeicheneigene.csv existiert nicht.");
                }
            } catch (IOException e) {

                Log.e("KennzeichenEinlesen", "Fehler bei der Einlesung der Datei: " + e.getMessage());
            }
            kennzeichenliste.remove(kennzeichen);
        }
    }

    public void deletelikedKennzeichen(Kennzeichen kennzeichen) {
        try {
            File file = new File(context.getFilesDir(), "kennzeichenliked.csv");
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                StringBuilder content = new StringBuilder();
                String line;
                boolean found = false; // Flag, um zu überprüfen, ob das Kennzeichen gefunden wurde
                while ((line = reader.readLine()) != null) {
                    String[] values = line.split(","); // Korrektur hier
                    if (values.length > 1) { // Nur Zeilen mit mindestens zwei Elementen bearbeiten
                        if (!values[1].equals(kennzeichen.OertskuerzelGeben())) {
                            content.append(line).append("\n");
                        } else {
                            Log.e("KennzeichenEinlesen", kennzeichen.OertskuerzelGeben());
                            found = true; // Kennzeichen gefunden
                        }
                    }
                }
                reader.close();
                FileWriter writer = new FileWriter(file);
                writer.write(content.toString());
                writer.close();
                Log.e("KennzeichenEinlesen", String.valueOf(found));
                // Wenn das Kennzeichen gefunden wurde, entferne es auch aus der likedliste
                if (found) {
                    likedliste.remove(kennzeichen);

                    // Remove from kennzeichenliste
                    for (int i = 0; i < kennzeichenliste.size(); i++) {
                        if (kennzeichenliste.get(i).OertskuerzelGeben().equals(kennzeichen.OertskuerzelGeben())) {
                            kennzeichenliste.remove(i);
                            break;
                        }
                    }
                }
            } else {
                Log.e("KennzeichenEinlesen", "Die Datei kennzeichenliked.csv existiert nicht.");
            }
        } catch (IOException e) {
            Log.e("KennzeichenEinlesen", "Fehler bei der Einlesung der Datei: " + e.getMessage());
        }
    }
}