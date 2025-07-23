package de.haainz.kennzeichenerkennung.ui.home;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
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
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import de.haainz.kennzeichenerkennung.Kennzeichen;
import de.haainz.kennzeichenerkennung.Kennzeichen_KI;
import de.haainz.kennzeichenerkennung.MapFragment;
import de.haainz.kennzeichenerkennung.PicInfoDialogFragment;
import de.haainz.kennzeichenerkennung.R;
import de.haainz.kennzeichenerkennung.TourPopupDialog;
import de.haainz.kennzeichenerkennung.databinding.FragmentHomeBinding;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
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
    private MapView mapView;
    private RelativeLayout mapRel;
    private CardView mapCardView;
    private Handler loadingHandler;
    private Runnable loadingRunnable;
    private int loadingStep = 0;
    private int aistatus = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null)
                            selectedImageUri = data.getData();
                        Glide.with(getContext()).load(selectedImageUri).apply(RequestOptions.circleCropTransform()).into(searchPic);
                    }
                }
        );
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        searchPic = binding.imageView2;
        textViewAusgabe = binding.textViewAusgabe;
        kuerzelEingabe = binding.kuerzeleingabe;
        kuerzelEingabe.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
        textViewAusgabe2 = binding.textViewAusgabe2;
        kennzeichenKI = new Kennzeichen_KI(getContext());

        Configuration.getInstance().load(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));

        ImageButton deleteText = binding.x;
        ImageView deleteBtn = binding.deleteBtn;
        ImageView saveBtn = binding.saveBtn;
        ImageView shareBtn = binding.shareBtn;
        ImageView picinfoBtn = binding.picinfoBtn;
        TextView tourbtn = binding.tourbtn;
        deleteText.setVisibility(View.GONE);
        deleteBtn.setVisibility(View.GONE);
        saveBtn.setVisibility(View.GONE);
        shareBtn.setVisibility(View.GONE);
        picinfoBtn.setVisibility(View.GONE);

        updateTextViewAusgabe2();

        if (!kuerzelEingabe.getText().toString().isEmpty()) {
            deleteText.setVisibility(View.VISIBLE);
        }

        TextView infobtn = binding.textHome;
        infobtn.setOnClickListener(v -> {
            TourPopupDialog dialog = new TourPopupDialog(getContext());
            dialog.show();
        });
        tourbtn.setOnClickListener(v -> {
            TourPopupDialog dialog = new TourPopupDialog(getContext());
            dialog.show();
        });

        kuerzelEingabe.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // "X"-Button ein-/ausblenden
                if (s.length() > 0) {
                    binding.x.setVisibility(View.VISIBLE);
                } else {
                    binding.x.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        deleteText.setOnClickListener(v -> {
            kuerzelEingabe.setText("");
            deleteText.setVisibility(View.GONE);
        });

        deleteBtn.setOnClickListener(v -> {
            selectedImageUri = null;
            Glide.with(getContext()).load(R.drawable.camera_pic).apply(RequestOptions.circleCropTransform()).into(searchPic);
            kuerzelEingabe.setText("");
            deleteText.setVisibility(View.GONE);
            deleteBtn.setVisibility(View.GONE);
            saveBtn.setVisibility(View.GONE);
            shareBtn.setVisibility(View.GONE);
            picinfoBtn.setVisibility(View.GONE);
        });

        saveBtn.setOnClickListener(v -> {
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
        });

        picinfoBtn.setOnClickListener(v -> {
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
        });

        shareBtn.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                try {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("image/jpeg");

                    Uri contentUri = FileProvider.getUriForFile(getContext(), "de.haainz.kennzeichenerkennung.fileprovider", new File(selectedImageUri.getPath()));
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
        });

        searchPic.setOnClickListener(v -> {
            ImagePicker.with(HomeFragment.this).cropSquare().compress(512).maxResultSize(512, 512)
                    .createIntent(intent -> {
                        imagePickLauncher.launch(intent);
                        deleteBtn.setVisibility(View.VISIBLE);
                        saveBtn.setVisibility(View.VISIBLE);
                        shareBtn.setVisibility(View.VISIBLE);
                        picinfoBtn.setVisibility(View.VISIBLE);
                        return null;
                    });
        });

        textViewAusgabe.setOnClickListener(v -> {
            if (ausgabe != null) {
                String kuerzelAusgabe = kuerzelEingabe.getText().toString();
                Log.e("?", kuerzelAusgabe);
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
        });

        kuerzelEingabe.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                buttongenerate.performClick(); // Simuliere den Button-Klick
                return true; // Signalisiere, dass das Event verarbeitet wurde
            }
            return false; // Andernfalls nicht verarbeitet
        });

        buttongenerate = binding.buttongenerate;
        buttongenerate.setOnClickListener(v -> {
            binding.fussnotenwert.setVisibility(VISIBLE);
            binding.fussnotentitel.setVisibility(VISIBLE);
            binding.bemerkungenwert.setVisibility(VISIBLE);
            binding.bemerkungentitel.setVisibility(VISIBLE);
            aistatus=0;
            hideKeyboard(v);
            if (!kuerzelEingabe.getText().toString().isEmpty()) {
                deleteText.setVisibility(View.VISIBLE);
            }
            if (String.valueOf(kuerzelEingabe.getText()).isEmpty()) {
                recognizeTextInImage();
            } else {
                recognizeCity(String.valueOf(kuerzelEingabe.getText()));
            }
            Log.d("Kennzeichen", "Ausgabe: " + ausgabe);
            Kennzeichen kennzeichen = kennzeichenKI.getKennzeichen(String.valueOf(kuerzelEingabe.getText()));

            Log.d("Kennzeichen", "Eingegebenes Kürzel: " + kuerzelEingabe.getText());
            Log.d("Kennzeichen", "Ausgabe: " + ausgabe);
            Log.d("Kennzeichen", "Gefundenes Kennzeichen: " + (kennzeichen != null ? kennzeichen.OertskuerzelGeben() : "null"));

            if (kennzeichen != null) {
                binding.sliderview.setVisibility(View.VISIBLE);
                binding.kuerzelwert.setText(kennzeichen.OertskuerzelGeben());
                binding.herleitungswert.setText(kennzeichen.OrtGeben());
                binding.stadtoderkreiswert.setText(kennzeichen.StadtKreisGeben());
                if(!Objects.equals(kennzeichen.BundeslandGeben(), "---")) {
                    binding.bundeslandwert.setText(kennzeichen.BundeslandGeben());
                } else {
                    binding.bundeslandwert.setVisibility(GONE);
                    binding.bundeslandtitel.setVisibility(GONE);
                }
                if(!Objects.equals(kennzeichen.BundeslandIsoGeben(), "---")) {
                    binding.bundeslandIsoWert.setText(kennzeichen.BundeslandIsoGeben());
                } else {
                    binding.bundeslandIsoWert.setVisibility(GONE);
                    binding.bundeslandIsoTitel.setVisibility(GONE);
                }
                binding.landwert.setText(kennzeichen.LandGeben());
                if (kennzeichen.isSaved()) {
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
                if (kennzeichen.FussnoteGeben().isEmpty() || Objects.equals(kennzeichen.FussnoteGeben(), "6")) {
                    binding.fussnotenwert.setVisibility(GONE);
                    binding.fussnotentitel.setVisibility(GONE);
                } else {
                    binding.fussnotenwert.setVisibility(VISIBLE);
                    binding.fussnotentitel.setVisibility(VISIBLE);
                    binding.fussnotenwert.setText(fussnoten[fussnoteNummer]);
                }
                if (kennzeichen.BemerkungenGeben().isEmpty() || Objects.equals(kennzeichen.BemerkungenGeben(), "---")) {
                    binding.bemerkungenwert.setVisibility(GONE);
                    binding.bemerkungentitel.setVisibility(GONE);
                } else {
                    binding.bemerkungenwert.setVisibility(VISIBLE);
                    binding.bemerkungentitel.setVisibility(VISIBLE);
                    binding.bemerkungenwert.setText(kennzeichen.BemerkungenGeben());
                }
                checkNetworkAndGenerateText(kennzeichen);

                binding.maprel.setOnClickListener(view -> {
                    MapFragment mapFragment = new MapFragment(kennzeichen);
                    mapFragment.show(getParentFragmentManager(), "MapFragment");
                });

                binding.infotextwert.setOnClickListener(view2 -> {
                    if (!binding.infotextwert.getText().toString().startsWith("Analysiere Informationen")) {
                        checkNetworkAndGenerateText(kennzeichen);
                    }
                });

                if (kennzeichen.isSonderDE()) {
                    binding.bundeslandIsoWert.setVisibility(GONE);
                    binding.bundeslandIsoTitel.setVisibility(GONE);
                    binding.stadtoderkreistitel.setText("Typ:  ");
                    binding.herleitungstitel.setText("Bedeutung:  ");
                    binding.bundeslandtitel.setText("Zulassungsbehörde:  ");
                } else if (kennzeichen.isAuslaufendDE()) {
                    binding.bundeslandwert.setVisibility(GONE);
                    binding.bundeslandtitel.setVisibility(GONE);
                    binding.bundeslandIsoWert.setVisibility(GONE);
                    binding.bundeslandIsoTitel.setVisibility(GONE);
                    binding.stadtoderkreistitel.setText("Bisheriger Ver-\nwaltungsbezirk/   \n-kreis:  ");
                    binding.stadtoderkreistitel.setTextSize(11);
                    binding.herleitungstitel.setText("Abwicklung:  ");
                } else {
                    binding.bundeslandIsoWert.setVisibility(VISIBLE);
                    binding.bundeslandIsoTitel.setVisibility(VISIBLE);
                    binding.stadtoderkreistitel.setText("Stadt/Kreis:  ");
                    binding.stadtoderkreistitel.setTextSize(17);
                    binding.herleitungstitel.setText("Herleitung:  ");
                    binding.bundeslandtitel.setText("Bundesland:  ");
                }

                if (isNetworkAvailable()) {
                    mapView = binding.map;
                    mapRel = binding.maprel;
                    mapCardView = binding.mapcardview;
                    mapView.setTileSource(TileSourceFactory.MAPNIK);
                    mapView.setBuiltInZoomControls(true);
                    mapView.setMultiTouchControls(true);
                    mapCardView.setVisibility(View.VISIBLE);

                    if(!kennzeichen.isSonderDE()) {
                        getCoordinates(kennzeichen.OrtGeben() + "_" + kennzeichen.BundeslandGeben());
                    } else {
                        binding.mapcardview.setVisibility(GONE);
                    }
                } else {
                    binding.mapcardview.setVisibility(View.GONE);
                }
            } else {
                binding.sliderview.setVisibility(View.GONE);
                Toast.makeText(getActivity(), "Kein Kennzeichen gefunden", Toast.LENGTH_SHORT).show();
            }
        });

        binding.likeBtn.setOnClickListener(v -> {
            Kennzeichen kennzeichen = kennzeichenKI.getKennzeichen(String.valueOf(kuerzelEingabe.getText()));
            if (!kennzeichen.isSaved()) {
                binding.likedBtn.setVisibility(VISIBLE);
                kennzeichenKI.changesavestatus(kennzeichen, "ja");
            } else {
                Toast.makeText(getActivity(), "Kennzeichen bereits geliked", Toast.LENGTH_SHORT).show();
            }
        });

        binding.likedBtn.setOnClickListener(v -> {
            Kennzeichen kennzeichen = kennzeichenKI.getKennzeichen(String.valueOf(kuerzelEingabe.getText()));
            binding.likedBtn.setVisibility(GONE);
            kennzeichenKI.changesavestatus(kennzeichen, "nein");
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
    public void onResume() {
        super.onResume();
        kuerzelEingabe.setText("");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopLoadingAnimation();
        kuerzelEingabe.setText("");
        binding.x.setVisibility(View.GONE);
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
            textViewAusgabe.setText("Bitte wähle ein Bild aus oder gebe ein Kürzel ein!");
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
            ausgabe = kennzeichenKI.OrtZuKennzeichenAusgeben(kuerzelAusgabe) + kennzeichenKI.BundeslandZuKennzeichenAusgeben(kuerzelAusgabe);
            if (ausgabe.equals("Dieses Kennzeichen kenne ich leider nicht 😒!")) {
                String modifiedText = text.toString().replace("M", "W").replace("H", "W");
                String kuerzel1 = modifiedText.replaceAll("[^A-ZÄÜÖ]", " ").trim();
                String[] kuerzelArray1 = kuerzel1.split("\\s+");
                String kuerzelAusgabe1 = kuerzelArray1.length > 0 ? kuerzelArray1[0] : "";
                kuerzelEingabe.setText(kuerzelAusgabe1);
                ausgabe = kennzeichenKI.OrtZuKennzeichenAusgeben(kuerzelAusgabe1) + kennzeichenKI.BundeslandZuKennzeichenAusgeben(kuerzelAusgabe1);
            }
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
        return activeNetworkInfo != null && activeNetworkInfo.isConnected() && !isOfflineMode();
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
            Log.e("Achtung", location);
            try {
                String url = "https://nominatim.openstreetmap.org/search?q=" + URLEncoder.encode(location + "_", "UTF-8") + "&format=json&addressdetails=1";
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
                    fragment.mapView.getOverlays().clear();
                    fragment.mapView.getController().setZoom(6.25);
                    fragment.mapView.getController().setCenter(new GeoPoint(51.163409, 10.447718));
                    Marker marker = new Marker(fragment.mapView);
                    marker.setPosition(geoPoint);
                    marker.setTitle("Gesuchtes Kennzeichen");
                    fragment.mapView.getOverlays().add(marker);
                    fragment.mapView.invalidate();
                } else {
                    fragment.mapCardView.setVisibility(View.GONE);
                }
            }
        }
    }

    private void checkNetworkAndGenerateText(Kennzeichen kennzeichen) {
        if (isNetworkAvailable() && !isOfflineMode()) {
            binding.infotexttitel.setVisibility(View.VISIBLE);
            binding.infotextwert.setVisibility(View.VISIBLE);
            Log.e("aistatus", String.valueOf(aistatus));
            if (aistatus==0) {
                aistatus = 1;
                binding.infotextwert.setText("Klicke um einen Informationstext von KI generieren zu lassen");
            } else {
                generateAIText(kennzeichen);
            }
        } else {
            binding.infotexttitel.setVisibility(View.GONE);
            binding.infotextwert.setVisibility(View.GONE);
        }
    }

    private void generateAIText(Kennzeichen kennzeichen) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        String aiModel = sharedPreferences.getString("selectedAIModel", "Gemini Pro 2.0");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final WeakReference<HomeFragment> fragmentRef = new WeakReference<>(this);
        executor.execute(() -> {
            try {
                if (isAdded()) {
                    getActivity().runOnUiThread(this::startLoadingAnimation);
                }
                String prompt = "Erstelle mir einen sehr kurzen informativen Text (maximal 75 Wörter) über " + kennzeichen.OrtGeben() +
                        " in " + kennzeichen.StadtKreisGeben() + " im Bundesland " + kennzeichen.BundeslandGeben() +
                        ". Gib auch wichtige Fakten wie Einwohnerzahl, geografische Besonderheiten und " +
                        "historische Hintergründe an. Sei präzise, antworte auf deutsch und halte dich an nachweisbare Fakten.";

                getaiModel(aiModel, modelId -> {
                    if (!modelId.equals("Fehler")) {
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

                            if (response.isSuccessful()) {
                                if (isAdded() && getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        HomeFragment fragment = fragmentRef.get();
                                        if (fragment != null && fragment.isAdded() && fragment.binding != null) {
                                            try {
                                                JSONObject jsonResponse = new JSONObject(responseData);
                                                String aiText = jsonResponse.getJSONArray("choices")
                                                        .getJSONObject(0)
                                                        .getJSONObject("message")
                                                        .getString("content");

                                                stopLoadingAnimation();
                                                fragment.binding.infotextwert.setText(
                                                        fragment.formatAIText(aiText)
                                                );
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                stopLoadingAnimation();
                                                fragment.showErrorState();
                                                Log.e("ai1", String.valueOf(e));
                                            }
                                        }
                                    });
                                }
                            }
                        } catch (Exception e) {
                            if (isAdded() && getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    HomeFragment fragment = fragmentRef.get();
                                    if (fragment != null && fragment.isAdded() && fragment.binding != null) {
                                        fragment.stopLoadingAnimation();
                                        fragment.showErrorState();
                                        Log.e("ai2", String.valueOf(e));
                                    }
                                });
                            }
                        }
                    } else {
                        stopLoadingAnimation();
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                HomeFragment fragment = fragmentRef.get();
                                if (fragment != null && fragment.isAdded() && fragment.binding != null) {
                                    fragment.showErrorState();
                                    Log.e("ai3", "Model == Fehler");
                                }
                            });
                        }
                    }
                });
            } catch (Exception e) {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        HomeFragment fragment = fragmentRef.get();
                        if (fragment != null && fragment.isAdded() && fragment.binding != null) {
                            fragment.stopLoadingAnimation();
                            fragment.showErrorState();
                            Log.e("ai4", String.valueOf(e));
                        }
                    });
                }
            }
        });
    }

    private String formatAIText(String aiText) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        String aiModel = sharedPreferences.getString("selectedAIModel", "Gemini Pro 2.0");
        return aiText.trim() + "\n(Quelle: KI-generiert von " + aiModel + ", keine Gewähr)";
    }

    private void startLoadingAnimation() {
        if (binding != null) {
            binding.infotexttitel.setVisibility(View.VISIBLE);
            binding.infotextwert.setVisibility(View.VISIBLE);
            binding.infotextwert.setText("Analysiere Informationen");

            loadingHandler = new Handler();
            loadingRunnable = new Runnable() {
                private final WeakReference<HomeFragment> fragmentRef = new WeakReference<>(HomeFragment.this);

                @Override
                public void run() {
                    HomeFragment fragment = fragmentRef.get();
                    if (fragment == null || fragment.binding == null) return;

                    switch (loadingStep % 4) {
                        case 0:
                            fragment.binding.infotextwert.setText("Analysiere Informationen.");
                            break;
                        case 1:
                            fragment.binding.infotextwert.setText("Analysiere Informationen..");
                            break;
                        case 2:
                            fragment.binding.infotextwert.setText("Analysiere Informationen...");
                            break;
                        case 3:
                            fragment.binding.infotextwert.setText("Analysiere Informationen");
                            break;
                    }
                    loadingStep++;
                    if (fragment.loadingHandler != null) {
                        fragment.loadingHandler.postDelayed(this, 550);
                    }
                }
            };
            loadingHandler.postDelayed(loadingRunnable, 550);
        }
    }

    private void stopLoadingAnimation() {
        if (loadingHandler != null && loadingRunnable != null) {
            loadingHandler.removeCallbacks(loadingRunnable);
        }
        loadingStep = 0;
    }

    private void showErrorState() {
        if (binding != null) {
            stopLoadingAnimation();
            binding.infotextwert.setText("Informationen aktuell nicht verfügbar");
            binding.infotexttitel.setVisibility(View.VISIBLE);
            binding.infotextwert.setVisibility(View.VISIBLE);
        }
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
                Log.e("HomeFragment", "Error loading AI models", e);
                try {
                    listener.onModelIdReceived("Fehler");
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
                                listener.onModelIdReceived(id);
                                return;
                            }
                        }
                        listener.onModelIdReceived("Fehler");
                    } catch (JSONException e) {
                        Log.e("HomeFragment", "Error parsing JSON", e);
                        try {
                            listener.onModelIdReceived("Fehler");
                        } catch (JSONException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                } else {
                    Log.e("HomeFragment", "Response not successful: " + response.code());
                    try {
                        listener.onModelIdReceived("Fehler");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    public interface OnModelIdReceivedListener {
        void onModelIdReceived(String modelId) throws JSONException;
    }
}