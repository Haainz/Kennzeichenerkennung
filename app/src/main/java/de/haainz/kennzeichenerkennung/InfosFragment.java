package de.haainz.kennzeichenerkennung;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import de.haainz.kennzeichenerkennung.ui.AIManager;
import de.haainz.kennzeichenerkennung.ui.list.ListFragment;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Objects;

public class InfosFragment extends DialogFragment {

    private Kennzeichen kennzeichen;
    private Kennzeichen_KI kennzeichenKI;
    private MapView mapView;
    private CardView mapCardView;

    public InfosFragment(Kennzeichen kennzeichen) {
        this.kennzeichen = kennzeichen;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.CustomDialogTheme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_infos, container, false);
        kennzeichenKI = new Kennzeichen_KI(getActivity());

        ImageView likedBtn = view.findViewById(R.id.liked_btn);
        ImageView likeBtn = view.findViewById(R.id.like_btn);

        if (kennzeichen.isSaved()) {
            likedBtn.setVisibility(VISIBLE);
        } else {
            likedBtn.setVisibility(GONE);
        }

        ImageView sharebtn = view.findViewById(R.id.share_btn);
        ImageView editbtn = view.findViewById(R.id.edit_btn);
        ImageView deletebtn = view.findViewById(R.id.delete_btn);

        if (kennzeichen.isEigene()) {
            editbtn.setVisibility(VISIBLE);
            deletebtn.setVisibility(VISIBLE);
        } else {
            editbtn.setVisibility(GONE);
            deletebtn.setVisibility(GONE);
        }

        TextView kuerzelWert = view.findViewById(R.id.kuerzelwert);
        TextView kuerzelTitel = view.findViewById(R.id.kuerzeltitel);
        kuerzelWert.setText(kennzeichen.OertskuerzelGeben());

        TextView herleitungWert = view.findViewById(R.id.herleitungswert);
        TextView herleitungTitel = view.findViewById(R.id.herleitungstitel);
        herleitungWert.setText(kennzeichen.OrtGeben());

        TextView stadtOderKreisWert = view.findViewById(R.id.stadtoderkreiswert);
        TextView stadtOderKreisTitel = view.findViewById(R.id.stadtoderkreistitel);
        stadtOderKreisWert.setText(kennzeichen.StadtKreisGeben());

        TextView bundeslandWert = view.findViewById(R.id.bundeslandwert);
        TextView bundeslandTitel = view.findViewById(R.id.bundeslandtitel);
        bundeslandWert.setText(kennzeichen.BundeslandGeben());

        TextView bundeslandIsoWert = view.findViewById(R.id.bundesland_iso_wert);
        TextView bundeslandIsoTitel = view.findViewById(R.id.bundesland_iso_titel);
        bundeslandIsoWert.setText(kennzeichen.BundeslandIsoGeben());

        TextView landWert = view.findViewById(R.id.landwert);
        TextView landTitel = view.findViewById(R.id.landtitel);
        landWert.setText(kennzeichen.LandGeben());

        TextView fussnotenWert = view.findViewById(R.id.fussnotenwert);
        TextView fussnotenTitel = view.findViewById(R.id.fussnotentitel);

        String fussnoteString = kennzeichen.FussnoteGeben();
        int fussnoteNummer = 6;

        if (!Objects.equals(fussnoteString, "")) {
            try {
                fussnoteNummer = Integer.parseInt(fussnoteString);
                String[] fussnoten = {
                        "Stadt- und Landkreis führen das gleiche Unterscheidungszeichen. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung für deren Behörden oder zusätzliche Verwaltungsstellen erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle.\n",
                        "Stadt- und Landkreis führen das gleiche Unterscheidungszeichen. Die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle stellt durch geeignete verwaltungsinterne Maßnahmen sicher, dass eine Doppelvergabe desselben Kennzeichens ausgeschlossen ist.\n",
                        "amtlicher Hinweis: Das Unterscheidungszeichen wird durch mehrere Verwaltungsbezirke verwaltet. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung, die in den jeweiligen Verwaltungsbezirken durch die dort zuständigen Behörden oder zusätzliche Verwaltungsstellen ausgegeben werden, erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle.\n",
                        "amtlicher Hinweis: Das Unterscheidungszeichen wird durch mehrere Verwaltungsbezirke verwaltet. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung, die in den jeweiligen Verwaltungsbezirken durch die dort zuständigen Behörden oder zusätzliche Verwaltungsstellen ausgegeben werden, erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle in Sachsen-Anhalt im Einvernehmen mit der obersten Landesbehörde oder der nach Landesrecht zuständigen Stelle in Baden-Württemberg.\n",
                        "amtlicher Hinweis: Das Unterscheidungszeichen wird durch mehrere Verwaltungsbezirke verwaltet. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung, die in den jeweiligen Verwaltungsbezirken durch die dort zuständigen Behörden oder zusätzliche Verwaltungsstellen ausgegeben werden, erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle in Baden-Württemberg im Einvernehmen mit der obersten Landesbehörde oder der nach Landesrecht zuständigen Stelle in Sachsen-Anhalt.\n",
                        "amtlicher Hinweis: Die Stadt und die Landespolizei Sachsen führen das gleiche Unterscheidungszeichen. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung für deren Behörden oder zusätzlichen Verwaltungsstellen erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle.\n",
                        "---\n",
                        "amtlicher Hinweis: Das Unterscheidungszeichen wird durch mehrere Verwaltungsbezirke verwaltet. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung, die in den jeweiligen Verwaltungsbezirken durch die dort zuständigen Behörden oder zusätzliche Verwaltungsstellen ausgegeben werden, erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle in Baden-Württemberg im Einvernehmen mit der obersten Landesbehörde oder der nach Landesrecht zuständigen Stelle in Sachsen-Anhalt.\n\nweiterer amtlicher Hinweis: Die Stadt und die Landespolizei Sachsen führen das gleiche Unterscheidungszeichen. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung für deren Behörden oder zusätzlichen Verwaltungsstellen erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle.\n",
                };
                fussnotenWert.setText(fussnoten[fussnoteNummer]);
            } catch (NumberFormatException e) {
                fussnotenWert.setText(fussnoteString + "\n");
            }
        } else {
            fussnotenWert.setText("---\n");
        }
        fussnotenWert.setOnClickListener(v -> showTextPopup("Fußnoten", fussnotenWert.getText().toString()));

        TextView aiTextWert = view.findViewById(R.id.aitextwert);
        TextView aiTextTitel = view.findViewById(R.id.aitexttitel);
        if (kennzeichen.aiTextGeben()=="") {
            aiTextWert.setText("---");
        } else {
            aiTextWert.setText(kennzeichen.aiTextGeben());
        }
        aiTextWert.setOnClickListener(v -> showAITextPopup(kennzeichen));

        TextView bemerkungenWert = view.findViewById(R.id.Bemerkungenwert);
        TextView bemerkungenTitel = view.findViewById(R.id.bemerkungentitel);
        if (kennzeichen.BemerkungenGeben().isEmpty()) {
            bemerkungenWert.setText("---");
        } else {
            bemerkungenWert.setText(kennzeichen.BemerkungenGeben());
        }

        if (kennzeichen.isSonderDE()) {
            bemerkungenWert.setVisibility(GONE);
            bemerkungenTitel.setVisibility(GONE);
            fussnotenWert.setVisibility(GONE);
            fussnotenTitel.setVisibility(GONE);
            bundeslandIsoWert.setVisibility(GONE);
            bundeslandIsoTitel.setVisibility(GONE);
            stadtOderKreisTitel.setText("Typ:  ");
            herleitungTitel.setText("Bedeutung:  ");
            bundeslandTitel.setText("Zulassungsbehörde:  ");
        } else if (kennzeichen.isAuslaufendDE()) {
            bemerkungenWert.setVisibility(GONE);
            bemerkungenTitel.setVisibility(GONE);
            fussnotenWert.setVisibility(GONE);
            fussnotenTitel.setVisibility(GONE);
            bundeslandWert.setVisibility(GONE);
            bundeslandTitel.setVisibility(GONE);
            bundeslandIsoWert.setVisibility(GONE);
            bundeslandIsoTitel.setVisibility(GONE);
            stadtOderKreisTitel.setText("Bisheriger Ver-\nwaltungsbezirk/   \n-kreis:  ");
            stadtOderKreisTitel.setTextSize(11);
            herleitungTitel.setText("Abwicklung:  ");
        }

        likeBtn.setOnClickListener(v -> {
            if (!kennzeichen.isSaved()) {
                likedBtn.setVisibility(VISIBLE);
                kennzeichenKI.changesavestatus(kennzeichen, "ja");
            } else {
                Toast.makeText(getActivity(), "Kennzeichen bereits geliked", Toast.LENGTH_SHORT).show();
            }
        });
        likedBtn.setOnClickListener(v -> {
            likedBtn.setVisibility(GONE);
            kennzeichenKI.changesavestatus(kennzeichen, "nein");
            Toast.makeText(getActivity(), "Kennzeichen entfernt.", Toast.LENGTH_SHORT).show();

            // Hier sollten Sie die updateList() Methode aufrufen, um die Anzeige zu aktualisieren
            if (getTargetFragment() instanceof ListFragment) {
                ((ListFragment) getTargetFragment()).updateList();
            }
        });

        sharebtn.setOnClickListener(v -> shareKennzeichen(kennzeichen));

        editbtn.setOnClickListener(v -> editKennzeichen(kennzeichen));

        deletebtn.setOnClickListener(v -> confirmDelete(kennzeichen));

        if (isNetworkAvailable() && !kennzeichen.isSonderDE()) {
            mapView = view.findViewById(R.id.map);
            mapCardView = view.findViewById(R.id.cardviewmap);
            Configuration.getInstance().load(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setBuiltInZoomControls(true);
            mapView.setMultiTouchControls (true);
            mapCardView.setVisibility(View.VISIBLE);

            setMarkerOnMap(kennzeichen.OrtGeben());
        } else {
            view.findViewById(R.id.cardviewmap).setVisibility(GONE);
            //Toast.makeText(getContext(), "Keine Internetverbindung. Die Karte wird nicht angezeigt.", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected() && !isOfflineMode();
    }

    private void shareKennzeichen(Kennzeichen kennzeichen) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "Kürzel: " + kennzeichen.OertskuerzelGeben() + "\nOrt: " + kennzeichen.OrtGeben() + "\nStadt bzw. Kreis: " + kennzeichen.StadtKreisGeben() + "\nBundesland: " + kennzeichen.BundeslandGeben());
        startActivity(Intent.createChooser(intent, "Teilen"));
    }

    private void editKennzeichen(Kennzeichen kennzeichen) {
        AddCityFragment addCityFragment = new AddCityFragment(kennzeichen);
        addCityFragment.show(getParentFragmentManager(), "AddCityFragment");
    }

    private void confirmDelete(Kennzeichen kennzeichen) {
        ConfirmFragment confirmFragment = new ConfirmFragment();
        confirmFragment.setKennzeichen(kennzeichen);
        confirmFragment.setKennzeichenKI(kennzeichenKI);
        confirmFragment.show(getParentFragmentManager(), "ConfirmFragment");
    }

    private void setMarkerOnMap(String location) {
        new GetCoordinatesTask(this).execute(location);
    }

    private static class GetCoordinatesTask extends AsyncTask<String, Void, GeoPoint> {
        private final WeakReference<InfosFragment> fragmentReference;

        GetCoordinatesTask(InfosFragment fragment) {
            fragmentReference = new WeakReference<>(fragment);
        }

        @Override
        protected GeoPoint doInBackground(String ...params) {
            String location = params[0];
            if (Objects.equals(location, "WeißenbUrG")) {
                location = "Weißenburg-Gunzenhausen";
            } else if (Objects.equals(location, "HOhensTein")) {
                location = "Hohenstein, Zwickau";
            }
            Log.e("Achtung", location);
            try {
                String url = "https://nominatim.openstreetmap.org/search?q=" + URLEncoder.encode(location, "UTF-8") + "&format=json&addressdetails=1";
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "de.haainz.kennzeichenerkennung/1.0 (mailto:kennzeichenerkennung@gmail.com)");
                connection.setRequestProperty("Accept-Language", "de");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new Exception("HTTP error code: " + responseCode);
                }

                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder jsonResponse = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonResponse.append(line);
                }
                reader.close();

                JSONArray jsonArray = new JSONArray(jsonResponse.toString());
                if (jsonArray.length() > 0) {
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    double latitude = jsonObject.getDouble("lat");
                    double longitude = jsonObject.getDouble("lon");
                    return new GeoPoint(latitude, longitude);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(GeoPoint geoPoint) {
            InfosFragment fragment = fragmentReference.get();
            try {
            if (fragment != null) {
                if (geoPoint != null) {
                    fragment.mapCardView.setVisibility(VISIBLE);
                    fragment.mapView.getController().setZoom(6.25);
                    fragment.mapView.getController().setCenter(new GeoPoint(51.163409, 10.447718));
                    Marker marker = new Marker(fragment.mapView);
                    marker.setPosition(geoPoint);
                    marker.setTitle(formatLabel(fragment.kennzeichen.OrtGeben()));
                    fragment.mapView.getOverlays().add(marker);
                    fragment.mapView.invalidate();
                } else {
                    fragment.mapCardView.setVisibility(GONE);
                    //Toast.makeText(fragment.getContext(), "Koordinaten konnten nicht gefunden werden", Toast.LENGTH_SHORT).show();
                }
            }
            } catch (Exception e) {
                fragment.mapCardView.setVisibility(GONE);
                e.printStackTrace();
            }
        }
    }
    public static String formatLabel(String label) {
        if (label == null || label.isEmpty()) {
            return label;
        }
        String formattedLabel = label.substring(0, 1).toUpperCase() + label.substring(1).toLowerCase();
        if (!formattedLabel.substring(1).matches("[a-z]*")) {
            new IllegalArgumentException("Der String soll nur Kleinbuchstaben enthalten (außer dem ersten).");
        }
        return formattedLabel;
    }

    private boolean isOfflineMode() {
        SharedPreferences prefs = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getBoolean("offlineSwitch", false);
    }

    private void showTextPopup(String title, String fullText) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle(title);
        builder.setMessage(fullText);
        builder.setPositiveButton("Schließen", null);
        builder.show();
    }

    public void showAITextPopup(Kennzeichen kennzeichen) {
        Activity activity = (Activity) getContext();
        activity.runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_ai_text_popup, null);
            builder.setView(dialogView);

            AlertDialog dialog = builder.create();
            dialog.setCancelable(false);
            dialog.show();

            TextView aiTextView = dialogView.findViewById(R.id.ai_text_view);
            EditText aiEditText = dialogView.findViewById(R.id.ai_edit_text);
            Button btnGenerate = dialogView.findViewById(R.id.button_generate);
            Button btnEdit = dialogView.findViewById(R.id.button_edit);
            ImageButton btnClose = dialogView.findViewById(R.id.button_close);

            // Starte mit aktuellem KI-Text
            String existingText = kennzeichen.aiTextGeben();
            if (!existingText.isEmpty()) {
                aiTextView.setText(existingText);
            } else {
                aiTextView.setText("Es wurde noch kein Text generiert.");
            }

            if (isNetworkAvailable() && !isOfflineMode()) {
                btnGenerate.setVisibility(VISIBLE);
            } else {
                btnGenerate.setVisibility(GONE);
            }

            AIManager aiManager = new AIManager(requireContext(), null, null);

            btnGenerate.setOnClickListener(v -> {
                aiManager.generateAIText(kennzeichen, new AIManager.AICallback() {
                    @Override
                    public void onResult(String aiText) {
                        activity.runOnUiThread(() -> {
                            aiTextView.setText(aiText);
                            aiEditText.setVisibility(View.GONE);
                            aiTextView.setVisibility(View.VISIBLE);
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(getContext(), "Fehler: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            });

            btnEdit.setOnClickListener(v -> {
                if (aiEditText.getVisibility() == View.GONE) {
                    aiEditText.setText(aiTextView.getText().toString());
                    aiTextView.setVisibility(View.GONE);
                    aiEditText.setVisibility(View.VISIBLE);
                    btnEdit.setText("Speichern");
                } else {
                    String editedText = aiEditText.getText().toString();
                    aiTextView.setText(editedText);
                    aiTextView.setVisibility(View.VISIBLE);
                    aiEditText.setVisibility(View.GONE);
                    kennzeichenKI.setaiText(kennzeichen, editedText); // Speichern
                    btnEdit.setText("Bearbeiten");
                }
            });

            btnClose.setOnClickListener(v -> dialog.dismiss());
        });
    }
}