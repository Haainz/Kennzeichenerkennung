package com.example.kennzeichenerkennung.ui.home;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.kennzeichenerkennung.InfosFragment;
import com.example.kennzeichenerkennung.Kennzeichen;
import com.example.kennzeichenerkennung.Kennzeichen_KI;
import com.example.kennzeichenerkennung.MapFragment;
import com.example.kennzeichenerkennung.PicInfoDialogFragment;
import com.example.kennzeichenerkennung.R;
import com.example.kennzeichenerkennung.databinding.FragmentHomeBinding;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Objects;

import android.view.inputmethod.InputMethodManager;
import android.content.Context;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private Kennzeichen_KI kennzeichenKI;
    private ActivityResultLauncher<Intent> imagePickLauncher;
    private Uri selectedImageUri;
    private ImageView searchPic;
    private Button buttongenerate;
    private TextView textViewAusgabe;
    private EditText kuerzelEingabe;
    private TextView textViewAusgabe2;
    private String ausgabe;
    private SwitchCompat logSwitch;
    private MapView mapView;
    private RelativeLayout mapRel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            selectedImageUri = data.getData();
                            Glide.with(getContext()).load(selectedImageUri).apply(RequestOptions.circleCropTransform()).into(searchPic);
                        }
                    }
                }
        );
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        searchPic = binding.imageView2;
        textViewAusgabe = binding.textViewAusgabe;
        kuerzelEingabe = binding.kuerzeleingabe;
        textViewAusgabe2 = binding.textViewAusgabe2;
        kennzeichenKI = new Kennzeichen_KI(getContext());

        Configuration.getInstance().load(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));

        ImageButton deleteText = binding.x;
        ImageView deleteBtn = binding.deleteBtn;
        ImageView saveBtn = binding.saveBtn;
        ImageView shareBtn = binding.shareBtn;
        ImageView picinfoBtn = binding.picinfoBtn;
        deleteText.setVisibility(View.GONE);
        deleteBtn.setVisibility(View.GONE);
        saveBtn.setVisibility(View.GONE);
        shareBtn.setVisibility(View.GONE);
        picinfoBtn.setVisibility(View.GONE);

        if (!kuerzelEingabe.getText().toString().isEmpty()) {
            deleteText.setVisibility(View.VISIBLE);
        }
        if (kuerzelEingabe.getHint()=="") {
            deleteText.setVisibility(View.GONE);
        }

        deleteText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                kuerzelEingabe.setText("");
                kuerzelEingabe.setHint("");
                deleteText.setVisibility(View.GONE);
            }
        });

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedImageUri = null;
                Glide.with(getContext()).load(R.drawable.camera_pic).apply(RequestOptions.circleCropTransform()).into(searchPic);
                deleteBtn.setVisibility(View.GONE);
                saveBtn.setVisibility(View.GONE);
                shareBtn.setVisibility(View.GONE);
                picinfoBtn.setVisibility(View.GONE);
            }
        });
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    InputStream inputStream = getContext().getContentResolver().openInputStream(selectedImageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    String filename = "bild_" + System.currentTimeMillis() + ".jpg";
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Kennzeichenerkennung");
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    File outputFile = new File(file, filename);
                    FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    fileOutputStream.close();
                    MediaScannerConnection.scanFile(getContext(), new String[]{outputFile.getAbsolutePath()}, null, null);
                    Toast.makeText(getContext(), "Bild gespeichert", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Fehler beim Speichern des Bildes", Toast.LENGTH_SHORT).show();
                }
            }
        });

        picinfoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedImageUri != null) {
                    try {
                        InputStream inputStream = getContext().getContentResolver().openInputStream(selectedImageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        if (bitmap != null) {
                            String info = "Auflösung: " + bitmap.getWidth() + "x" + bitmap.getHeight() + " dpi" + "\n";
                            info += "Farbtiefe: " + bitmap.getConfig() + "\n";
                            info += "Größe: " + bitmap.getByteCount() + " Bytes";
                            String finalInfo = info;
                            DialogFragment dialogFragment = new PicInfoDialogFragment(finalInfo);
                            dialogFragment.show(getParentFragmentManager(), "PicInfoDialog");
                        } else {
                            Toast.makeText(getContext(), "Fehler beim Laden des Bildes", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Fehler beim Laden des Bildes", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Bitte wähle ein Bild aus, um Informationen anzuzeigen", Toast.LENGTH_SHORT).show();
                }
            }
        });

        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedImageUri != null) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("image/jpeg");

                        Uri contentUri = FileProvider.getUriForFile(getContext(), "com.example.kennzeichenerkennung.fileprovider", new File(selectedImageUri.getPath()));
                        intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                        intent.putExtra(Intent.EXTRA_TEXT, "Kürzel: " + ausgabe.split(", ")[0].trim() + "\nOrt: " + ausgabe.split(", ")[1].trim());
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(intent, "Teilen"));
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Fehler beim Teilen des Bildes\nBitte suche erst nach Stadt", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Bitte wähle ein Bild aus, um es zu teilen", Toast.LENGTH_SHORT).show();
                }
            }
        });

        searchPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImagePicker.with(HomeFragment.this).cropSquare().compress(512).maxResultSize(512, 512)
                        .createIntent(intent -> {
                            imagePickLauncher.launch(intent);
                            deleteBtn.setVisibility(View.VISIBLE);
                            saveBtn.setVisibility(View.VISIBLE);
                            shareBtn.setVisibility(View.VISIBLE);
                            picinfoBtn.setVisibility(View.VISIBLE);
                            return null;
                        });
            }
        });

        textViewAusgabe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ausgabe == null) {
                    Toast.makeText(getContext(), " Kein Kennzeichen gefunden", Toast.LENGTH_SHORT).show();
                } else {
                    String kuerzelAusgabe = ausgabe.split(", ")[0].trim();
                    Kennzeichen kennzeichen = kennzeichenKI.getKennzeichen(kuerzelAusgabe);
                    if (kennzeichen != null) {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_TEXT, "Kürzel: " + kennzeichen.OertskuerzelGeben() + "\nOrt: " + kennzeichen.OrtGeben() + "\nStadt bzw. Kreis: " + kennzeichen.StadtKreisGeben() + "\nBundesland: " + kennzeichen.BundeslandGeben());
                        startActivity(Intent.createChooser(intent, "Teilen"));
                    } else {
                        Toast.makeText(getContext(), "Kein Kennzeichen gefunden", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        buttongenerate = binding.buttongenerate;
        buttongenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard(v);
                if (!kuerzelEingabe.getText().toString().isEmpty()) {
                    deleteText.setVisibility(View.VISIBLE);
                }
                if (kuerzelEingabe.getHint()!="") {
                    deleteText.setVisibility(View.VISIBLE);
                }
                if (String.valueOf(kuerzelEingabe.getText()).isEmpty()) {
                    if (String.valueOf(kuerzelEingabe.getHint()).isEmpty()) {
                        recognizeTextInImage();
                    } else {
                        recognizeCity(String.valueOf(kuerzelEingabe.getHint()));
                        recognizeTextInImage();
                    }
                } else {
                    recognizeCity(String.valueOf(kuerzelEingabe.getText()));
                }
                Log.d("Kennzeichen", "Ausgabe: " + ausgabe);
                //String kuerzelAusgabe = ausgabe.split(", ")[0].trim();
                Kennzeichen kennzeichen = kennzeichenKI.getKennzeichen(String.valueOf(kuerzelEingabe.getText()));

                Log.d("Kennzeichen", "Eingegebenes Kürzel: " + kuerzelEingabe.getText());
                Log.d("Kennzeichen", "Ausgabe: " + ausgabe);
                //Log.d("Kennzeichen", "Kürzel Ausgabe: " + kuerzelAusgabe);
                Log.d("Kennzeichen", "Gefundenes Kennzeichen: " + (kennzeichen != null ? kennzeichen.OertskuerzelGeben() : "null"));

                if (kennzeichen != null) {
                    binding.sliderview.setVisibility(View.VISIBLE);
                    binding.kuerzelwert.setText(kennzeichen.OertskuerzelGeben());
                    binding.herleitungswert.setText(kennzeichen.OrtGeben());
                    binding.stadtoderkreiswert.setText(kennzeichen.StadtKreisGeben());
                    binding.bundeslandwert.setText(kennzeichen.BundeslandGeben());
                    binding.bundeslandIsoWert.setText(kennzeichen.BundeslandIsoGeben());
                    binding.landwert.setText(kennzeichen.LandGeben());
                    if (kennzeichenKI.LikeÜberprüfen(kennzeichen.OertskuerzelGeben())) {
                        binding.likedBtn.setVisibility(VISIBLE);
                        Log.d("Kennzeichen", "1");
                    } else {
                        binding.likedBtn.setVisibility(GONE);
                        Log.d("Kennzeichen", "2");
                    }
                    int fussnoteNummer = 6;
                    if (!Objects.equals(kennzeichen.FussnoteGeben(), "")) {
                        fussnoteNummer = Integer.parseInt(kennzeichen.FussnoteGeben());
                    }
                    String[] fussnoten = {
                            "Stadt- und Landkreis führen das gleiche Unterscheidungszeichen. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung für deren Behörden oder zusätzliche Verwaltungsstellen erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle.\n",
                            "Stadt- und Landkreis führen das gleiche Unterscheidungszeichen. Die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle stellt durch geeignete verwaltungsinterne Maßnahmen sicher, dass eine Doppelvergabe desselben Kennzeichens ausgeschlossen ist.\n",
                            "amtlicher Hinweis: Das Unterscheidungszeichen wird durch mehrere Verwaltungsbezirke verwaltet. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung, die in den jeweiligen Verwaltungsbezirken durch die dort zuständigen Behörden oder zusätzliche Verwaltungsstellen ausgegeben werden, erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle.\n",
                            "amtlicher Hinweis: Das Unterscheidungszeichen wird durch mehrere Verwaltungsbezirke verwaltet. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung, die in den jeweiligen Verwaltungsbezirken durch die dort zuständigen Behörden oder zusätzliche Verwaltungsstellen ausgegeben werden, erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle in Sachsen-Anhalt im Einvernehmen mit der obersten Landesbehörde oder der nach Landesrecht zuständigen Stelle in Baden-Württemberg.\n",
                            "amtlicher Hinweis: Das Unterscheidungszeichen wird durch mehrere Verwaltungsbezirke verwaltet. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung, die in den jeweiligen Verwaltungsbezirken durch die dort zuständigen Behörden oder zusätzliche Verwaltungsstellen ausgegeben werden, erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle in Baden-Württemberg im Einver in Baden-Württemberg im Einvernehmen mit der obersten Landesbehörde oder der nach Landes recht zuständigen Stelle in Sachsen-Anhalt.\n",
                            "amtlicher Hinweis: Die Stadt und die Landespolizei Sachsen führen das gleiche Unterscheidungszeichen. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung für deren Behörden oder zusätzlichen Verwaltungsstellen erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle.\n",
                            "---\n",
                            "amtlicher Hinweis: Das Unterscheidungszeichen wird durch mehrere Verwaltungsbezirke verwaltet. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung, die in den jeweiligen Verwaltungsbezirken durch die dort zuständigen Behörden oder zusätzliche Verwaltungsstellen ausgegeben werden, erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle in Baden-Württemberg im Einvernehmen mit der obersten Landesbehörde oder der nach Landesrecht zuständigen Stelle in Sachsen-Anhalt.\n\nweiterer amtlicher Hinweis: Die Stadt und die Landespolizei Sachsen führen das gleiche Unterscheidungszeichen. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung für deren Behörden oder zusätzlichen Verwaltungsstellen erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle.\n",
                    };
                    binding.fussnotenwert.setText(fussnoten[fussnoteNummer]);
                    binding.Bemerkungenwert.setText(kennzeichen.BemerkungenGeben());

                    binding.maprel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // MapFragment öffnen
                            MapFragment mapFragment = new MapFragment(kennzeichen);
                            mapFragment.show(getParentFragmentManager(), "MapFragment");
                        }
                    });

                    if (isNetworkAvailable()) {
                        mapView = binding.map;
                        mapRel = binding.maprel;
                        mapView.setTileSource(TileSourceFactory.MAPNIK);
                        mapView.setBuiltInZoomControls(true);
                        mapView.setMultiTouchControls(true);
                        mapView.setVisibility(View.VISIBLE);
                        mapRel.setVisibility(View.VISIBLE);

                        getCoordinates(kennzeichen.OrtGeben());
                    } else {
                        binding.map.setVisibility(View.GONE);
                        binding.maprel.setVisibility(View.GONE);
                        //Toast.makeText(getContext(), "Keine Internetverbindung. Die Karte wird nicht angezeigt.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    binding.sliderview.setVisibility(View.GONE);
                    Toast.makeText(getActivity(), "Kein Kennzeichen gefunden", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                kennzeichenKI.KennzeichenLikedEinlesen();
                Kennzeichen kennzeichen = kennzeichenKI.getKennzeichen(String.valueOf(kuerzelEingabe.getText()));
                if (!kennzeichenKI.LikeÜberprüfen(kennzeichen.OertskuerzelGeben())) {
                    String csvZeile = kennzeichen.LandGeben() + "," + kennzeichen.OertskuerzelGeben() + "," + kennzeichen.StadtKreisGeben() + "," + kennzeichen.OrtGeben() + "," + kennzeichen.BundeslandGeben() + "," + kennzeichen.BundeslandIsoGeben() + "," + kennzeichen.FussnoteGeben() + "," + kennzeichen.BemerkungenGeben();
                    Log.d("Kennzeichen", "11");
                    try {
                        File file = new File(getActivity().getFilesDir(), "kennzeichenliked.csv");
                        FileOutputStream fileOutputStream = new FileOutputStream(file, true);
                        fileOutputStream.write((csvZeile + "\n").getBytes());
                        fileOutputStream.close();
                        binding.likedBtn.setVisibility(VISIBLE);
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
                Kennzeichen kennzeichen = kennzeichenKI.getKennzeichen(String.valueOf(kuerzelEingabe.getText()));
                binding.likedBtn.setVisibility(GONE);
                kennzeichenKI.deletelikedKennzeichen(kennzeichen);
                kennzeichenKI.KennzeichenLikedEinlesen();
            }
        });

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("settings", getActivity().MODE_PRIVATE);
        int logSwitchStatus = sharedPreferences.getInt("logSwitch", 1);
        if (logSwitchStatus == 1) {
            textViewAusgabe2.setVisibility(View.VISIBLE);
        } else {
            textViewAusgabe2.setVisibility(View.GONE);
        }
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void recognizeTextInImage() {
        if (selectedImageUri == null) {
            textViewAusgabe.setText("Bitte wähle ein Bild aus!");
            return;
        }

        TextRecognizer textRecognizer = new TextRecognizer.Builder(getContext()).build();
        if (!textRecognizer.isOperational()) {
            Log.e("Error", "Detector dependencies are not yet available");
            return;
        }

        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(selectedImageUri);
            Frame frame = new Frame.Builder().setBitmap(BitmapFactory.decodeStream(inputStream)).build();
            SparseArray<TextBlock> textBlocks = textRecognizer.detect(frame);

            StringBuilder text = new StringBuilder();
            for (int i = 0; i < textBlocks.size(); i++) {
                TextBlock textBlock = textBlocks.valueAt(i);
                text.append(textBlock.getValue());
                text.append("\n");
            }
            String kennzeichen = text.toString();
            textViewAusgabe2.setText(kennzeichen);
            String kuerzel = kennzeichen.replaceAll("[^A-ZÄÜÖ]", " ").trim();
            String[] kuerzelArray = kuerzel.split("\\s+");
            String kuerzelAusgabe = kuerzelArray.length > 0 ? kuerzelArray[0] : "";
            kuerzelEingabe.setText(kuerzelAusgabe);
            kuerzelEingabe.setHint(kuerzelAusgabe);
            ausgabe = kennzeichenKI.OrtZuKennzeichenAusgeben(kuerzelAusgabe) + kennzeichenKI.BundeslandZuKennzeichenAusgeben(kuerzelAusgabe);
            textViewAusgabe.setText(ausgabe);

        } catch (FileNotFoundException e) {
            Log.e("Error", "Datei nicht gefunden", e);
            kuerzelEingabe.setText(e.toString());
        }
    }

    private void recognizeCity(String kennzeichen) {
        textViewAusgabe2.setText(kennzeichen);
        String kuerzel = kennzeichen.replaceAll("[^A-ZÄÜÖ]", " ").trim();
        String[] kuerzelArray = kuerzel.split("\\s+");
        String kuerzelAusgabe = kuerzelArray.length > 0 ? kuerzelArray[0] : "";
        Log.d("Kennzeichen1", "Ausgabe:1" + kuerzelAusgabe);
        kuerzelEingabe.setText(kuerzelAusgabe);
        kuerzelEingabe.setHint("");
        ausgabe = kennzeichenKI.OrtZuKennzeichenAusgeben(kuerzelAusgabe) + kennzeichenKI.BundeslandZuKennzeichenAusgeben(kuerzelAusgabe);
        textViewAusgabe.setText(ausgabe);
    }

    public void updateTextViewAusgabe2() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("settings", getActivity().MODE_PRIVATE);
        int logSwitchStatus = sharedPreferences.getInt("logSwitch", 1);
        if (logSwitchStatus == 1) {
            textViewAusgabe2.setVisibility(View.VISIBLE);
        } else {
            textViewAusgabe2.setVisibility(View.GONE);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void getCoordinates(String locationName) {
        new GetCoordinatesTask(this).execute(locationName);
    }

    private static class GetCoordinatesTask extends AsyncTask<String, Void, GeoPoint> {
        private final WeakReference<HomeFragment> fragmentReference;

        GetCoordinatesTask(HomeFragment fragment) {
            fragmentReference = new WeakReference<>(fragment);
        }

        @Override
        protected GeoPoint doInBackground(String... params) {
            String location = params[0];
            if (Objects.equals(location, "WeißenbUrG")) {
                location = "Weißenburg-Gunzenhausen";
            }
            Log.e("Achtung", location);
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
            HomeFragment fragment = fragmentReference.get();
            if (fragment != null) {
                if (geoPoint != null) {
                    fragment.mapView.getController().setZoom(6.25);
                    fragment.mapView.getController().setCenter(new GeoPoint(51.163409, 10.447718));
                    Marker marker = new Marker(fragment.mapView);
                    marker.setPosition(geoPoint);
                    marker.setTitle("Gesuchtes Kennzeichen");
                    fragment.mapView.getOverlays().add(marker);
                    fragment.mapView.invalidate();
                } else {
                    fragment.mapView.setVisibility(View.GONE);
                    fragment.mapRel.setVisibility(View.GONE);
                    Toast.makeText(fragment.getContext(), "Koordinaten konnten nicht gefunden werden", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}