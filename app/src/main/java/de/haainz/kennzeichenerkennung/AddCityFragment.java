package de.haainz.kennzeichenerkennung;

import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class AddCityFragment extends DialogFragment {

    private EditText nationalitaetseingabe;
    private EditText kuerzeleingabe;
    private EditText herleitungeingabe;
    private EditText orteingabe;
    private EditText bundeslandeingabe;
    private EditText anmerkungeneingabe;
    private EditText fussnoteneingabe;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.CustomDialogfullTheme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_city, container, false);

        kuerzeleingabe = view.findViewById(R.id.kuerzeleingabe);
        kuerzeleingabe.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
        herleitungeingabe = view.findViewById(R.id.herleitungeingabe);
        orteingabe = view.findViewById(R.id.orteingabe);
        bundeslandeingabe = view.findViewById(R.id.bundeslandeingabe);
        anmerkungeneingabe = view.findViewById(R.id.anmerkungeneingabe);
        fussnoteneingabe = view.findViewById(R.id.fussnoteneingabe);
        nationalitaetseingabe = view.findViewById(R.id.nationalitaetseingabe);

        Button speichernButton = view.findViewById(R.id.save_btn);
        speichernButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speichern();
            }
        });

        ImageButton xBtn = view.findViewById(R.id.x);
        xBtn.setOnClickListener(v -> dismiss());

        return view;
    }

    private void speichern() {
        String nationalitaetszeichen = nationalitaetseingabe.getText().toString();
        String unterscheidungszeichen = kuerzeleingabe.getText().toString();
        String stadtOderKreis = orteingabe.getText().toString();
        String herleitung = herleitungeingabe.getText().toString();
        String bundeslandName = bundeslandeingabe.getText().toString();
        String bundeslandIso31662 = getIso31662(bundeslandName); // Hier müsste die ISO-3166-2-Code für das Bundesland eingetragen werden
        String fussnoten = fussnoteneingabe.getText().toString();
        String bemerkung = anmerkungeneingabe.getText().toString();

        if (unterscheidungszeichen.isEmpty()) {
            Toast.makeText(getActivity(), "Bitte geben Sie ein Unterscheidungszeichen ein.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (herleitung.isEmpty()) {
            Toast.makeText(getActivity(), "Bitte geben Sie eine Herleitung ein.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (stadtOderKreis.isEmpty()) {
            Toast.makeText(getActivity(), "Bitte geben Sie einen Kreis bzw. eine Stadt ein.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (bundeslandName.isEmpty()) {
            Toast.makeText(getActivity(), "Bitte geben Sie das Bundesland ein.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (nationalitaetszeichen.isEmpty()) {
            Toast.makeText(getActivity(), "Bitte geben Sie das Nationalitätskürzel ein.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (istKuerzelBereitsVorhanden(unterscheidungszeichen)) {
            Toast.makeText(getActivity(), "Das Unterscheidungszeichen ist bereits vorhanden.", Toast.LENGTH_SHORT).show();
            return;
        }

        String csvZeile = nationalitaetszeichen + "," + unterscheidungszeichen + "," + stadtOderKreis + "," + herleitung + "," + bundeslandName + "," + bundeslandIso31662 + "," + fussnoten + "," + bemerkung + ",," + "nein";

        try {
            File file = new File(getActivity().getFilesDir(), "kennzeicheneigene.csv");
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            fileOutputStream.write((csvZeile + "\n").getBytes());
            Toast.makeText(getActivity(), "Kennzeichen erfolgreich erstellt\nBitte aktualisiere die Liste", Toast.LENGTH_SHORT).show();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Es ist ein Fehler aufgetreten", Toast.LENGTH_SHORT).show();
        }
        dismiss();
    }

    private String getIso31662(String bundeslandName) {
        switch (bundeslandName.trim()) {
            case "Bayern":
                return "DE-BY";
            case "Baden-Württemberg":
                return "DE-BW";
            case "Sachsen-Anhalt":
                return "DE-ST";
            case "Sachsen":
                return "DE-SN";
            case "Niedersachsen":
                return "DE-NI";
            case "Brandenburg":
                return "DE-BB";
            case "Thüringen":
                return "DE-TH";
            case "Hessen":
                return "DE-HE";
            case "Rheinland-Pfalz":
                return "DE-RP";
            case "Saarland":
                return "DE-SL";
            case "Nordrhein-Westfalen":
                return "DE-NW";
            case "Mecklenburg-Vorpommern":
                return "DE-MV";
            case "Schleswig-Holstein":
                return "DE-SH";
            case "Hamburg":
                return "DE-HH";
            case "Berlin":
                return "DE-BE";
            case "Bremen":
                return "DE-HB";
            default:
                return "---";
        }
    }

    private boolean istKuerzelBereitsVorhanden(String kuerzel) {
        File file = new File(getActivity().getFilesDir(), "kennzeicheneigene.csv");
        if (!file.exists()) {
            return false;
        }

        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length > 1 && values[1].equals(kuerzel)) {
                    reader.close();
                    return true;
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }
}