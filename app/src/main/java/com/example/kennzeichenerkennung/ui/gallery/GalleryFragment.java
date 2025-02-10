package com.example.kennzeichenerkennung.ui.gallery;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.example.kennzeichenerkennung.Kennzeichen;
import com.example.kennzeichenerkennung.KennzeichenGenerator;
import com.example.kennzeichenerkennung.Kennzeichen_KI;
import com.example.kennzeichenerkennung.MapFragment;
import com.example.kennzeichenerkennung.PicInfoDialogFragment;
import com.example.kennzeichenerkennung.R;
import com.example.kennzeichenerkennung.databinding.FragmentGalleryBinding;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GalleryFragment extends Fragment {

    private FragmentGalleryBinding binding;
    private Kennzeichen_KI kennzeichenKI;
    private KennzeichenGenerator kennzeichenGenerator;
    private Bitmap imageBitmap;
    private MapView mapView;
    private Kennzeichen currentKennzeichen;
    private Handler loadingHandler;
    private Runnable loadingRunnable;
    private int loadingStep = 0;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        GalleryViewModel galleryViewModel =
                new ViewModelProvider(this).get(GalleryViewModel.class);

        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        kennzeichenKI = new Kennzeichen_KI(getActivity());
        kennzeichenGenerator = new KennzeichenGenerator(getContext());

        // Kennzeichenkürzel basierend auf dem Datum berechnen
        Calendar calendar = Calendar.getInstance();
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        currentKennzeichen = getKennzeichen(dayOfYear);

        kennzeichenKI.KennzeichenLikedEinlesen();
        if (kennzeichenKI.LikeÜberprüfen(currentKennzeichen.OertskuerzelGeben())) {
            binding.likedBtn.setVisibility(VISIBLE);
            binding.countText.setVisibility(VISIBLE);
        } else {
            binding.likedBtn.setVisibility(GONE);
            binding.countText.setVisibility(GONE);
        }

        imageBitmap = generateImage(currentKennzeichen);
        binding.imageoftheday.setImageBitmap(imageBitmap);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        String currentDate = dateFormat.format(new Date());
        binding.title.setText("Kennzeichen des Tages\nHeute, " + currentDate + ":");

        checkNetworkAndGenerateText(currentKennzeichen, dayOfYear);

        binding.kuerzelwert.setText(currentKennzeichen.OertskuerzelGeben());
        binding.herleitungswert.setText(currentKennzeichen.OrtGeben());
        binding.stadtoderkreiswert.setText(currentKennzeichen.StadtKreisGeben());
        binding.bundeslandwert.setText(currentKennzeichen.BundeslandGeben());
        binding.bundeslandIsoWert.setText(currentKennzeichen.BundeslandIsoGeben());
        binding.landwert.setText(currentKennzeichen.LandGeben());
        int fussnoteNummer = 7;
        if (!Objects.equals(currentKennzeichen.FussnoteGeben(), "")) {
            fussnoteNummer = Integer.parseInt(currentKennzeichen.FussnoteGeben());
        }
        fussnoteNummer = fussnoteNummer - 1;
        String[] fussnoten = {
                "Stadt- und Landkreis führen das gleiche Unterscheidungszeichen. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung für deren Behörden oder zusätzliche Verwaltungsstellen erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle. \n",
                "Stadt- und Landkreis führen das gleiche Unterscheidungszeichen. Die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle stellt durch geeignete verwaltungsinterne Maßnahmen sicher, dass eine Doppelvergabe desselben Kennzeichens ausgeschlossen ist.\n",
                "amtlicher Hinweis: Das Unterscheidungszeichen wird durch mehrere Verwaltungsbezirke verwaltet. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung, die in den jeweiligen Verwaltungsbezirken durch die dort zuständigen Behörden oder zusätzliche Verwaltungsstellen ausgegeben werden, erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle.\n",
                "amtlicher Hinweis: Das Unterscheidungszeichen wird durch mehrere Verwaltungsbezirke verwaltet. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung, die in den jeweiligen Verwaltungsbezirken durch die dort zuständigen Behörden oder zusätzliche Verwaltungsstellen ausgegeben werden, erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle in Sachsen-Anhalt im Einvernehmen mit der obersten Landesbehörde oder der nach Landesrecht zuständigen Stelle in Baden-Württemberg.\n",
                "amtlicher Hinweis: Das Unterscheidungszeichen wird durch mehrere Verwaltungsbezirke verwaltet. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung, die in den jeweiligen Verwaltungsbezirken durch die dort zuständigen Behörden oder zusätzliche Verwaltungsstellen ausgegeben werden, erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle in Baden-Württemberg im Einvernehmen mit der obersten Landesbehörde oder der nach Landesrecht zuständigen Stelle in Sachsen-Anhalt.\n",
                "amtlicher Hinweis: Die Stadt und die Landespolizei Sachsen führen das gleiche Unterscheidungszeichen. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung für deren Behörden oder zusätzlichen Verwaltungsstellen erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle.\n",
                "---\n",
                "amtlicher Hinweis: Das Unterscheidungszeichen wird durch mehrere Verwaltungsbezirke verwaltet. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung, die in den jeweiligen Verwaltungsbezirken durch die dort zuständigen Behörden oder zusätzliche Verwaltungsstellen ausgegeben werden, erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle in Baden-Württemberg im Einvernehmen mit der obersten Landesbehörde oder der nach Landesrecht zuständigen Stelle in Sachsen-Anhalt.\n\nweiterer amtlicher Hinweis: Die Stadt und die Landespolizei Sachsen führen das gleiche Unterscheidungszeichen. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung für deren Behörden oder zusätzlichen Verwaltungsstellen erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle.\n",
        };
        binding.fussnotenwert.setText(fussnoten[fussnoteNummer]);
        binding.Bemerkungenwert.setText(currentKennzeichen.BemerkungenGeben());

        binding.thinkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkNetworkAndGenerateText(currentKennzeichen, dayOfYear);
            }
        });

        binding.likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!kennzeichenKI.LikeÜberprüfen(currentKennzeichen.OertskuerzelGeben())) {
                    String csvZeile = currentKennzeichen.LandGeben() + "," + currentKennzeichen.OertskuerzelGeben() + "," + currentKennzeichen.StadtKreisGeben() + "," + currentKennzeichen.OrtGeben() + "," + currentKennzeichen.BundeslandGeben() + "," + currentKennzeichen.BundeslandIsoGeben() + "," + currentKennzeichen.FussnoteGeben() + "," + currentKennzeichen.BemerkungenGeben();
                    try {
                        File file = new File(getActivity().getFilesDir(), "kennzeichenliked.csv");
                        FileOutputStream fileOutputStream = new FileOutputStream(file, true);
                        fileOutputStream.write((csvZeile + "\n").getBytes());
                        fileOutputStream.close();
                        binding.likedBtn.setVisibility(VISIBLE);
                        binding.countText.setVisibility(VISIBLE);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), "Es ist ein Fehler aufgetreten", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getActivity(), "Kennzeichen bereits geliked", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.likedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.likedBtn.setVisibility(GONE);
                binding.countText.setVisibility(GONE);
                kennzeichenKI.deletelikedKennzeichen(currentKennzeichen);
            }
        });

        binding.saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String filename = "bild_" + System.currentTimeMillis() + ".jpg";
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Kennzeichenerkennung");
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    File outputFile = new File(file, filename);
                    FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    fileOutputStream.close();
                    MediaScannerConnection.scanFile(getContext(), new String[]{outputFile.getAbsolutePath()}, null, null);
                    Toast.makeText(getContext(), "Bild gespeichert", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Fehler beim Speichern des Bildes", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Bild speichern
                    String filename = "bild_" + System.currentTimeMillis() + ".jpg";
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Kennzeichenerkennung");
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    File outputFile = new File(file, filename);
                    FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    fileOutputStream.close();

                    // Bild teilen
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("image/jpeg");
                    Uri contentUri = FileProvider.getUriForFile(getContext(), "com.example.kennzeichenerkennung.fileprovider", outputFile);
                    intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent, "Teilen"));
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Fehler beim Teilen des Bildes", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.picinfoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String info = "Auflösung: " + imageBitmap.getWidth() + "x" + imageBitmap.getHeight() + " dpi" + "\n";
                    info += "Farbtiefe: " + imageBitmap.getConfig() + "\n";
                    info += "Größe: " + imageBitmap.getByteCount() + " Bytes";
                    String finalInfo = info;
                    DialogFragment dialogFragment = new PicInfoDialogFragment(finalInfo);
                    dialogFragment.show(getParentFragmentManager(), "PicInfoDialog");
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Fehler beim Laden des Bildes", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.maprel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // MapFragment öffnen
                MapFragment mapFragment = new MapFragment(currentKennzeichen);
                mapFragment.show(getParentFragmentManager(), "MapFragment");
            }
        });

        Configuration.getInstance().load(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));

        if (isNetworkAvailable()) {
            mapView = binding.map;
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setBuiltInZoomControls(false);
            mapView.setMultiTouchControls(false);
            mapView.setVisibility(VISIBLE);
            binding.maprel.setVisibility(VISIBLE);

            getCoordinates(currentKennzeichen.OrtGeben());
        } else {
            binding.map.setVisibility(GONE);
            binding.maprel.setVisibility(GONE);
            //Toast.makeText(getContext(), "Keine Internetverbindung. Die Karte wird nicht angezeigt.", Toast.LENGTH_SHORT).show();
        }
        return root;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected() && !isOfflineMode();
    }

    private void getCoordinates(String locationName) {
        new GetCoordinatesTask(this, currentKennzeichen).execute(locationName);
    }

    private static class GetCoordinatesTask extends AsyncTask<String, Void, GeoPoint> {
        private final WeakReference<GalleryFragment> fragmentReference;
        private final Kennzeichen kennzeichen;
        String label;

        GetCoordinatesTask(GalleryFragment fragment, Kennzeichen kennzeichen) {
            fragmentReference = new WeakReference<>(fragment);
            this.kennzeichen = kennzeichen;
        }

        @Override
        protected GeoPoint doInBackground(String... params) {
            String location = params[0];
            if (Objects.equals(location, "WeißenbUrG")) {
                location = "Weißenburg-Gunzenhausen";
            }
            Log.e("Achtung", location);
            label = location;
            try {
                String url = "https://nominatim.openstreetmap.org/search?q=" + URLEncoder.encode(location, "UTF-8") + "&format=json&addressdetails=1";
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
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
            GalleryFragment fragment = fragmentReference.get();
            if (fragment != null) {
                if (geoPoint != null) {
                    fragment.mapView.getController().setZoom(6.5);
                    fragment.mapView.getController().setCenter(new GeoPoint(51.163409, 10.447718));
                    Marker marker = new Marker(fragment.mapView);
                    marker.setPosition(geoPoint);
                    String formattedLabel = formatLabel(label);

                    String bundesland = kennzeichen.BundeslandGeben();

                    String title = formattedLabel + "<br>" + bundesland;
                    marker.setTitle(String.valueOf(Html.fromHtml(title, Html.FROM_HTML_MODE_LEGACY)));

                    fragment.mapView.getOverlays().add(marker);
                    fragment.mapView.invalidate();
                } else {
                    //Toast.makeText(fragment.getContext(), "Koordinaten konnten nicht gefunden werden", Toast.LENGTH_SHORT).show();
                }
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

    private Kennzeichen getKennzeichen(int dayOfYear) {
        ArrayList<Kennzeichen> kennzeichenListe = kennzeichenKI.getKennzeichenListe();

        // Nur normale und sonder Kennzeichen berücksichtigen
        ArrayList<Kennzeichen> filteredList = new ArrayList<>();
        for (Kennzeichen kennzeichen : kennzeichenListe) {
            if (kennzeichen.isNormal()) {
                filteredList.add(kennzeichen);
            } /*else if (kennzeichen.isSonder()) {
                filteredList.add(kennzeichen);
            }*/
        }

        // Kennzeichen basierend auf dem Datum berechnen
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1; // Monat ist 0-basiert, also +1
        int year = calendar.get(Calendar.YEAR);

        // Hash-Funktion anwenden, um eine eindeutige Zahl zu erzeugen
        int index = (day * 31 + month * 12 + year) % filteredList.size();
        return filteredList.get(index);
    }

    private Bitmap generateImage(Kennzeichen kennzeichen) {
        String kuerzel = kennzeichen.OertskuerzelGeben();
        Drawable img = getActivity().getDrawable(R.drawable.img);
        return kennzeichenGenerator.generateImage(img, kennzeichen);
    }

    private void checkNetworkAndGenerateText(Kennzeichen kennzeichen, int dayOfYear) {
        if (isNetworkAvailable() && !isOfflineMode()) {
            generateAIText(kennzeichen, dayOfYear);
            binding.textOftheday.setText("Analysiere Informationen...");
            binding.thinkBtn.setVisibility(VISIBLE);
        } else {
            showStandardText(kennzeichen, dayOfYear);
            binding.thinkBtn.setVisibility(GONE);
        }
    }

    private void generateAIText(Kennzeichen kennzeichen, int dayOfYear) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final WeakReference<GalleryFragment> fragmentRef = new WeakReference<>(this);
        executor.execute(() -> {
            try {
                if (isAdded()) {
                    getActivity().runOnUiThread(this::startLoadingAnimation);
                }
                String prompt = "Erstelle einen sehr kurzen informativen Text (maximal 100 Wörter) über " + kennzeichen.OrtGeben() +
                        " in " + kennzeichen.StadtKreisGeben() + ", " + kennzeichen.BundeslandGeben() +
                        ". Gib wichtige Fakten wie Einwohnerzahl, geografische Besonderheiten und " +
                        "historische Hintergründe. Sei präzise, antworte auf deutsch und halte dich an nachweisbare Fakten.";

                JSONObject jsonBody = new JSONObject();
                JSONArray messages = new JSONArray();
                JSONObject message = new JSONObject();
                message.put("content", prompt);
                message.put("role", "user");
                messages.put(message);

                jsonBody.put("messages", messages);
                jsonBody.put("model", "deepseek-ai/DeepSeek-V3");
                jsonBody.put("max_tokens", 1024);

                RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json"));

                Request request = new Request.Builder()
                        .url("https://api.blackbox.ai/api/chat")
                        .post(body)
                        .build();

                OkHttpClient client = new OkHttpClient();
                Response response = client.newCall(request).execute();
                String responseData = response.body().string();
                Log.d("API_RESPONSE", responseData);

                if (response.isSuccessful()) {
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            GalleryFragment fragment = fragmentRef.get();
                            if (fragment != null && fragment.isAdded() && fragment.binding != null) {
                                final String aiText = responseData;
                                stopLoadingAnimation();
                                fragment.binding.textOftheday.setText(
                                        fragment.formatAIText(aiText, kennzeichen, dayOfYear)
                                );
                            }
                        });
                    }
                }
            } catch (Exception e) {
                stopLoadingAnimation();
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        GalleryFragment fragment = fragmentRef.get();
                        if (fragment != null && fragment.isAdded() && fragment.binding != null) {
                            fragment.showStandardText(kennzeichen, dayOfYear);
                        }
                    });
                }
            }
        });
    }

    private String formatAIText(String aiText, Kennzeichen kennzeichen, int dayOfYear) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        String currentDate = dateFormat.format(new Date());

        return "KI-Infotext für " + currentDate + " (" + dayOfYear + ". Tag):\n\n" +
                kennzeichen.OertskuerzelGeben() + " - " + kennzeichen.OrtGeben() + "\n\n" +
                aiText.trim() + "\n\n(Quelle: KI-generiert, keine Gewähr)";
    }

    private void showStandardText(Kennzeichen kennzeichen, int dayOfYear) {
        String bemerk = "";
        if (!kennzeichen.BemerkungenGeben().equals("---") && !kennzeichen.BemerkungenGeben().isEmpty()) {
            bemerk = "Folgende Bemerkung ist noch hinterlegt: " + kennzeichen.BemerkungenGeben();
        }

        String standardText = "Heute, am " + new SimpleDateFormat("dd.MM.yyyy").format(new Date()) +
                ", dem " + dayOfYear + ". Tag diesen Jahres ist das Kennzeichen-Kürzel des Tages " +
                kennzeichen.OertskuerzelGeben() + ". " + kennzeichen.OertskuerzelGeben() +
                " leitet sich von " + kennzeichen.OrtGeben() + " ab und gehört zur Stadt bzw. zum Kreis " +
                kennzeichen.StadtKreisGeben() + ".\n" + kennzeichen.StadtKreisGeben() +
                " liegt in Deutschland im Bundesland " + kennzeichen.BundeslandGeben() + ".\n" + bemerk;

        binding.textOftheday.setText(standardText);
    }

    private void startLoadingAnimation() {
        binding.textOftheday.setText("Analysiere Informationen");

        loadingHandler = new Handler();
        loadingRunnable = new Runnable() {
            @Override
            public void run() {
                switch (loadingStep % 4) {
                    case 0:
                        binding.textOftheday.setText("Analysiere Informationen.");
                        break;
                    case 1:
                        binding.textOftheday.setText("Analysiere Informationen..");
                        break;
                    case 2:
                        binding.textOftheday.setText("Analysiere Informationen...");
                        break;
                    case 3:
                        binding.textOftheday.setText("Analysiere Informationen");
                        break;
                }
                loadingStep++;
                loadingHandler.postDelayed(this, 550);
            }
        };
        loadingHandler.postDelayed(loadingRunnable, 550);
    }

    private void stopLoadingAnimation() {
        if (loadingHandler != null && loadingRunnable != null) {
            loadingHandler.removeCallbacks(loadingRunnable);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopLoadingAnimation();
        binding = null;
    }

    private boolean isOfflineMode() {
        SharedPreferences prefs = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getBoolean("offlineSwitch", false);
    }
}