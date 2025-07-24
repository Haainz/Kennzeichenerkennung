package de.haainz.kennzeichenerkennung.ui.day;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import androidx.preference.PreferenceManager;

import de.haainz.kennzeichenerkennung.Kennzeichen;
import de.haainz.kennzeichenerkennung.KennzeichenGenerator;
import de.haainz.kennzeichenerkennung.Kennzeichen_KI;
import de.haainz.kennzeichenerkennung.MapFragment;
import de.haainz.kennzeichenerkennung.PicInfoDialogFragment;
import de.haainz.kennzeichenerkennung.R;
import de.haainz.kennzeichenerkennung.databinding.FragmentDayBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DayFragment extends Fragment {

    private FragmentDayBinding binding;
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

        binding = FragmentDayBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Configuration.getInstance().load(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("settings", getActivity().MODE_PRIVATE);

        kennzeichenKI = new Kennzeichen_KI(getActivity());
        kennzeichenGenerator = new KennzeichenGenerator(getContext());

        binding.fussnotenwert.setVisibility(VISIBLE);
        binding.fussnotentitel.setVisibility(VISIBLE);
        binding.Bemerkungenwert.setVisibility(VISIBLE);
        binding.bemerkungentitel.setVisibility(VISIBLE);

        Calendar calendar = Calendar.getInstance();
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        currentKennzeichen = getKennzeichen();

        if (currentKennzeichen.isSaved()) {
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
        String[] fussnoten = {
                "Stadt- und Landkreis führen das gleiche Unterscheidungszeichen. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungs nummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung für deren Behörden oder zusätzliche Verwaltungsstellen erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle.",
                "Stadt- und Landkreis führen das gleiche Unterscheidungszeichen. Die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle stellt durch geeignete verwaltungsinterne Maßnahmen sicher, dass eine Doppelvergabe desselben Kennzeichens ausgeschlossen ist.",
                "amtlicher Hinweis: Das Unterscheidungszeichen wird durch mehrere Verwaltungsbezirke verwaltet. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung, die in den jeweiligen Verwaltungsbezirken durch die dort zuständigen Behörden oder zusätzliche Verwaltungsstellen " +
                        "ausgegeben werden, erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle.",
                "amtlicher Hinweis: Das Unterscheidungszeichen wird durch mehrere Verwaltungsbezirke verwaltet. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung, die in den jeweiligen Verwaltungsbezirken durch die dort zuständigen Behörden oder zusätzliche Verwaltungsstellen " +
                        "ausgegeben werden, erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle in Sachsen-Anhalt im Einvernehmen mit der obersten Landesbehörde oder der nach Landesrecht zuständigen Stelle in Baden-Württemberg.",
                "amtlicher Hinweis: Das Unterscheidungszeichen wird durch mehrere Verwaltungsbezirke verwaltet. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung, die in den jeweiligen Verwaltungsbezirken durch die dort zuständigen Behörden oder zusätzliche Verwaltungsstellen " +
                        "ausgegeben werden, erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle in Baden-Württemberg im Einvernehmen mit der obersten Landesbehörde oder der nach Landesrecht zuständigen Stelle in Sachsen-Anhalt.",
                "amtlicher Hinweis: Die Stadt und die Landespolizei Sachsen führen das gleiche Unterscheidungszeichen. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung für deren Behörden oder zusätzlichen Verwaltungsstellen erfolgt durch die zuständige oberste Landesbehörde " +
                        "oder die nach Landesrecht zuständige Stelle.",
                "---",
                "amtlicher Hinweis: Das Unterscheidungszeichen wird durch mehrere Verwaltungsbezirke verwaltet. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung, die in den jeweiligen Verwaltungsbezirken durch die dort zuständigen Behörden oder zusätzliche Verwaltungsstellen " +
                        "ausgegeben werden, erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle in Baden-Württemberg im Einvernehmen mit der obersten Landesbehörde oder der nach Landesrecht zuständigen Stelle in Sachsen-Anhalt.\n\nweiterer amtlicher Hinweis: Die Stadt und die Landespolizei Sachsen " +
                        "führen das gleiche Unterscheidungszeichen. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung für deren Behörden oder zusätzlichen Verwaltungsstellen erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle.",
        };
        if (!currentKennzeichen.FussnoteGeben().isEmpty()) {
            binding.fussnotenwert.setText(fussnoten[fussnoteNummer]);
        } else {
            binding.fussnotenwert.setVisibility(GONE);
            binding.fussnotentitel.setVisibility(GONE);
        }
        if (!currentKennzeichen.BemerkungenGeben().isEmpty()) {
            binding.Bemerkungenwert.setText(currentKennzeichen.BemerkungenGeben());
        } else {
            binding.Bemerkungenwert.setVisibility(GONE);
            binding.bemerkungentitel.setVisibility(GONE);
        }

        binding.thinkBtn.setOnClickListener(v -> {
            stopLoadingAnimation();
            checkNetworkAndGenerateText(currentKennzeichen, dayOfYear);
        });

        binding.likeBtn.setOnClickListener(v -> {
            if (!currentKennzeichen.isSaved()) {
                binding.likedBtn.setVisibility(VISIBLE);
                kennzeichenKI.changesavestatus(currentKennzeichen, "ja");
            } else {
                Toast.makeText(getActivity(), "Kennzeichen bereits geliked", Toast.LENGTH_SHORT).show();
            }
        });

        binding.likedBtn.setOnClickListener(v -> {
            binding.likedBtn.setVisibility(GONE);
            kennzeichenKI.changesavestatus(currentKennzeichen, "nein");
        });

        binding.saveBtn.setOnClickListener(v -> {
            try {
                Bitmap croppedBitmap = cropImage(imageBitmap); // Zuschneiden des Bildes
                String filename = "bild_" + System.currentTimeMillis() + ".jpg";
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Kennzeichenerkennung");
                if (!file.exists()) {
                    file.mkdirs();
                }
                File outputFile = new File(file, filename);
                FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                fileOutputStream.close();
                MediaScannerConnection.scanFile(getContext(), new String[]{outputFile.getAbsolutePath()}, null, null);
                Toast.makeText(getContext(), "Bild gespeichert", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Fehler beim Speichern des Bildes", Toast.LENGTH_SHORT).show();
            }
        });

        binding.shareBtn.setOnClickListener(v -> {
            try {
                Bitmap croppedBitmap = cropImage(imageBitmap); // Zuschneiden des Bildes
                String filename = "bild_" + System.currentTimeMillis() + ".jpg";
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Kennzeichenerkennung");
                if (!file.exists()) {
                    file.mkdirs();
                }
                File outputFile = new File(file, filename);
                FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                fileOutputStream.close();

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("image/jpeg");
                Uri contentUri = FileProvider.getUriForFile(getContext(), "de.haainz.kennzeichenerkennung.fileprovider", outputFile);
                intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Teilen"));
            } catch (Exception e) {
                Toast.makeText(getContext(), "Fehler beim Teilen des Bildes", Toast.LENGTH_SHORT).show();
            }
        });

        binding.picinfoBtn.setOnClickListener(v -> {
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
        });

        binding.maprel.setOnClickListener(view -> {
            MapFragment mapFragment = new MapFragment(currentKennzeichen);
            mapFragment.show(getParentFragmentManager(), "MapFragment");
        });

        Configuration.getInstance().load(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));

        if (isNetworkAvailable()) {
            mapView = binding.map;
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setBuiltInZoomControls(false);
            mapView.setMultiTouchControls(false);
            mapView.setVisibility(VISIBLE);
            binding.maprel.setVisibility(VISIBLE);

            if (mapView.getVisibility() == View.VISIBLE) {
                getCoordinates(currentKennzeichen.OrtGeben() + "_" + currentKennzeichen.BundeslandGeben());
            } else {
                Log.e("DayFragment", "mapView is not visible, cannot get coordinates.");
            }
        } else {
            binding.mapCard.setVisibility(GONE);
            binding.map.setVisibility(GONE);
            binding.maprel.setVisibility(GONE);
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
        private final WeakReference<DayFragment> fragmentReference;
        private final Kennzeichen kennzeichen;
        String label;

        GetCoordinatesTask(DayFragment fragment, Kennzeichen kennzeichen) {
            fragmentReference = new WeakReference<>(fragment);
            this.kennzeichen = kennzeichen;
        }

        @Override
        protected GeoPoint doInBackground(String... params) {
            String location = params[0];
            Log.e("location", location);
            label = location;
            try {
                String url = "https://nominatim.openstreetmap.org/search?q=" + URLEncoder.encode(location, "UTF-8") + "&format=json&addressdetails=1";
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection ();
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
            DayFragment fragment = fragmentReference.get();
            try {
            if (fragment != null && fragment.mapView != null) { // Überprüfe, ob mapView nicht null ist
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
                }
            } else {
                Log.e("DayFragment", "mapView is null, cannot add marker.");
            }
            } catch (Exception e) {
                fragment.mapView.setVisibility(GONE);
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

    private Kennzeichen getKennzeichen() {
        ArrayList<Kennzeichen> kennzeichenListe = kennzeichenKI.getKennzeichenListe();

        ArrayList<Kennzeichen> filteredList = new ArrayList<>();
        for (Kennzeichen kennzeichen : kennzeichenListe) {
            if (kennzeichen.isNormalDE()) {
                filteredList.add(kennzeichen);
            }
        }

        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);

        int index = (day * 31 + month * 12 + year) % filteredList.size();
        Log.e("day", day + " " + month + " " + year + " " + filteredList.size() + " " + index);
        return filteredList.get(index);
    }

    private Bitmap generateImage(Kennzeichen kennzeichen) {
        Drawable img = getActivity().getDrawable(R.drawable.img3);
        return kennzeichenGenerator.generateImage(img, kennzeichen);
    }

    private void checkNetworkAndGenerateText(Kennzeichen kennzeichen, int dayOfYear) {
        if (isNetworkAvailable() && !isOfflineMode()) {
            generateAIText(kennzeichen, dayOfYear);
            binding.textOftheday.setText("Analysiere Informationen...");
        } else {
            showStandardText(kennzeichen, dayOfYear);
        }
    }

    private void generateAIText(Kennzeichen kennzeichen, int dayOfYear) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        String aiModel = sharedPreferences.getString("selectedAIModel", "Deepseek V3");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final WeakReference<DayFragment> fragmentRef = new WeakReference<>(this);
        checkModelWorking(aiModel);
        executor.execute(() -> {
            try {
                if (isAdded()) {
                    getActivity().runOnUiThread(this::startLoadingAnimation);
                }
                String prompt = "Erstelle mir einen sehr kurzen informativen Text (maximal 75 Wörter) über " + kennzeichen.OrtGeben() +
                        " in " + kennzeichen.StadtKreisGeben() + " im Bundesland " + kennzeichen.BundeslandGeben() +
                        ". Gib auch wichtige Fakten wie Einwohnerzahl, geografische Besonderheiten und " +
                        "historische Hintergründe an. Sei präzise, antworte auf deutsch, halte dich an nachweisbare Fakten und gebe mir nur den Text.";

                getaiModel(aiModel, modelId -> {
                    if (!modelId.equals("Fehler")) {
                        // Proceed with AI text generation using the modelId
                        JSONObject jsonBody = new JSONObject();
                        JSONArray messages = new JSONArray();
                        JSONObject message = new JSONObject();
                        message.put("content", prompt);
                        message.put("role", "user");
                        messages.put(message);

                        try {
                            jsonBody.put("model", modelId);
                            jsonBody.put("messages", messages);

                            RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json"));

                            Request request = new Request.Builder()
                                    .url("https://openrouter.ai/api/v1/chat/completions")
                                    .post(body)
                                    .addHeader("Authorization", "Bearer " + sharedPreferences.getString("apikey", getResources().getString(R.string.api_key)))
                                    .addHeader("Content-Type", "application/json")
                                    .build();

                            OkHttpClient client = new OkHttpClient();
                            Response response = client.newCall(request).execute();
                            String responseData = response.body().string();
                            Log.d("API_RESPONSE", responseData);
                            openResponse(responseData);

                            if (response.isSuccessful()) {
                                if (isAdded() && getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        DayFragment fragment = fragmentRef.get();
                                        if (fragment != null && fragment.isAdded() && fragment.binding != null) {
                                            try {
                                                JSONObject jsonResponse = new JSONObject(responseData);
                                                String aiText = jsonResponse.getJSONArray("choices")
                                                        .getJSONObject(0)
                                                        .getJSONObject("message")
                                                        .getString("content");

                                                stopLoadingAnimation();
                                                fragment.binding.textOftheday.setText(
                                                        fragment.formatAIText(aiText, kennzeichen, dayOfYear)
                                                );
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                stopLoadingAnimation();
                                                fragment.showStandardText(kennzeichen, dayOfYear);
                                            }
                                        }
                                    });
                                }
                            } else {
                                stopLoadingAnimation();
                                if (isAdded() && getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        DayFragment fragment = fragmentRef.get();
                                        if (fragment != null && fragment.isAdded() && fragment.binding != null) {
                                            fragment.showStandardText(kennzeichen, dayOfYear);
                                        }
                                    });
                                }
                            }
                        } catch (Exception e) {
                            stopLoadingAnimation();
                            if (isAdded() && getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    DayFragment fragment = fragmentRef.get();
                                    if (fragment != null && fragment.isAdded() && fragment.binding != null) {
                                        fragment.showStandardText(kennzeichen, dayOfYear);
                                    }
                                });
                            }
                        }
                    } else {
                        stopLoadingAnimation();
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                DayFragment fragment = fragmentRef.get();
                                if (fragment != null && fragment.isAdded() && fragment.binding != null) {
                                    fragment.showStandardText(kennzeichen, dayOfYear);
                                }
                            });
                        }
                    }
                });
            } catch (Exception e) {
                stopLoadingAnimation();
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        DayFragment fragment = fragmentRef.get();
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
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        String aiModel = sharedPreferences.getString("selectedAIModel", "Gemini Pro 2.0");

        return "KI-Infotext für " + currentDate + " (" + dayOfYear + ". Tag):\n\n" +
                kennzeichen.OertskuerzelGeben() + " - " + kennzeichen.OrtGeben() + "\n\n" +
                aiText.trim() + "\n\n(Quelle: KI-generiert von " + aiModel + ", keine Gewähr)";
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
        if (binding != null) {
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
        } else {
            Log.e("DayFragment", "Binding is null, cannot start loading animation.");
        }
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

    private Bitmap cropImage(Bitmap originalBitmap) {
        int height = originalBitmap.getHeight();
        int width = originalBitmap.getWidth();

        int cutTop = height / 9 + height / 200;
        int cutBottom = height / 10 + height / 150;
        int cutLeft = width / 8;
        int cutRight = width / 9;

        int newWidth = width - cutLeft - cutRight;
        int newHeight = height - cutTop - cutBottom;

        return Bitmap.createBitmap(originalBitmap, cutLeft, cutTop, newWidth, newHeight);
    }

    private boolean isOfflineMode() {
        SharedPreferences prefs = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getBoolean("offlineSwitch", false);
    }

    private void getaiModel(String aiModel, OnModelIdReceivedListener listener) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://raw.githubusercontent.com/Haainz/Kennzeichenerkennung/refs/heads/master/aimodels.json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("DayFragment", "Error loading AI models", e);
                try {
                    listener.onModelIdReceived("Fehler"); // Notify listener of error
                } catch (JSONException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String jsonData = response.body().string();
                        JSONObject json = new JSONObject(jsonData);
                        JSONArray models = json.getJSONArray("ai_models");

                        for (int i = 0; i < models.length(); i++) {
                            JSONObject model = models.getJSONObject(i);
                            String name = model.getString("name");
                            String id = model.getString("id");

                            if (name.equals(aiModel)) {
                                listener.onModelIdReceived(id); // Notify listener with the found ID
                                return;
                            }
                        }
                        listener.onModelIdReceived("Fehler"); // Notify listener if model not found
                    } catch (JSONException e) {
                        Log.e("DayFragment", "Error parsing JSON", e);
                        try {
                            listener.onModelIdReceived("Fehler"); // Notify listener of JSON parsing error
                        } catch (JSONException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                } else {
                    Log.e("DayFragment", "Response not successful: " + response.code());
                    try {
                        listener.onModelIdReceived("Fehler"); // Notify listener of response error
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    // Define an interface for the callback
    public interface OnModelIdReceivedListener {
        void onModelIdReceived(String modelId) throws JSONException;
    }

    private void checkModelWorking(String modelName) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://raw.githubusercontent.com/Haainz/Kennzeichenerkennung/refs/heads/master/aimodels.json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("DayFragment", "Error checking model status", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String jsonData = response.body().string();
                        JSONObject json = new JSONObject(jsonData);
                        JSONArray models = json.getJSONArray("ai_models");

                        for (int i = 0; i < models.length(); i++) {
                            JSONObject model = models.getJSONObject(i);
                            String name = model.getString("name");
                            boolean working = model.getBoolean("working");

                            if (name.equals(modelName) && !working) {
                                requireActivity().runOnUiThread(() ->
                                        Toast.makeText(requireContext(), "Achtung: Derzeit liegen bei diesem KI-Modell Fehler vor!", Toast.LENGTH_LONG).show());
                            }
                        }
                    } catch (JSONException e) {
                        Log.e("DayFragment", "Error parsing JSON", e);
                    }
                }
            }
        });
    }

    private void openResponse(String response) {
        String cleanedResponse = response.trim();

        binding.thinkBtn.setOnLongClickListener(v -> {
            requireActivity().runOnUiThread(() ->
                    new AlertDialog.Builder(requireContext())
                            .setTitle("API-Antwort")
                            .setMessage(cleanedResponse)
                            .setPositiveButton("Kopieren", (dialog, which) -> {
                                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("API Response", cleanedResponse);
                                clipboard.setPrimaryClip(clip);
                                Toast.makeText(requireContext(), "Antwort kopiert", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Abbrechen", null)
                            .show()
            );
            return true;
        });
    }
}