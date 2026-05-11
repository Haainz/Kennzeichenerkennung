package de.haainz.kennzeichenerkennung.ui.home;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.pm.PackageManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import androidx.exifinterface.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
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
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.os.BundleCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import de.haainz.kennzeichenerkennung.ExportFragment;
import de.haainz.kennzeichenerkennung.Kennzeichen;
import de.haainz.kennzeichenerkennung.Kennzeichen_KI;
import de.haainz.kennzeichenerkennung.MapFragment;
import de.haainz.kennzeichenerkennung.PicInfoDialogFragment;
import de.haainz.kennzeichenerkennung.R;
import de.haainz.kennzeichenerkennung.databinding.FragmentHomeBinding;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.yalantis.ucrop.UCrop;

import org.json.JSONArray;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.haainz.kennzeichenerkennung.ui.AIManager;
import de.haainz.kennzeichenerkennung.ui.history.SearchEntry;
import de.haainz.kennzeichenerkennung.ui.history.SearchHistoryManager;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private Kennzeichen_KI kennzeichenKI;
    private Uri selectedImageUri;
    private Uri pendingImageUri = null;
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
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ConstraintLayout mapconstlayout;
    private TextView tourBtn;
    private SearchHistoryManager historyManager;
    private String lastSavedSearchKey = "";
    private Uri cameraImageUri;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Intent> cropImageLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private static final String TAG_IMG = "HomeFragmentImg";

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // Wir müssen den SupportFragmentManager der Activity nutzen,
        // da der Scanner dort manuell hinzugefügt wird.
        requireActivity().getSupportFragmentManager().setFragmentResultListener("kennzeichen_scan_result", this, (requestKey, bundle) -> {
            Log.d(TAG_IMG, "Ergebnis von Activity FM erhalten");
            handleIncomingScanResult(bundle);
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Uri selectedUri = null;

                        if (data != null && data.getData() != null) {
                            selectedUri = data.getData();
                        } else if (cameraImageUri != null) {
                            selectedUri = cameraImageUri;
                        }

                        if (selectedUri != null) {
                            Uri destinationUri = Uri.fromFile(new File(requireContext().getCacheDir(),
                                    "cropped_" + System.currentTimeMillis() + ".jpg"));

                            UCrop.Options options = new UCrop.Options();
                            options.setCompressionQuality(90);
                            options.setFreeStyleCropEnabled(true);
                            options.setHideBottomControls(false);
                            options.setToolbarTitle("Bild zuschneiden");

                            UCrop.of(selectedUri, destinationUri)
                                    .withOptions(options)
                                    .withAspectRatio(1, 1)
                                    .withMaxResultSize(2048, 2048)
                                    .getIntent(requireContext());

                            cropImageLauncher.launch(UCrop.of(selectedUri, destinationUri)
                                    .withOptions(options)
                                    .withAspectRatio(1, 1)
                                    .withMaxResultSize(2048, 2048)
                                    .getIntent(requireContext()));
                        }
                    }
                }
        );

        cropImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri resultUri = UCrop.getOutput(result.getData());
                        if (resultUri != null) {
                            selectedImageUri = resultUri;
                            if (binding == null) {
                                pendingImageUri = resultUri;
                            } else {
                                requireActivity().runOnUiThread(() -> applySelectedImage(resultUri));
                            }
                        }
                    } else if (result.getResultCode() == UCrop.RESULT_ERROR && result.getData() != null) {
                        final Throwable cropError = UCrop.getError(result.getData());
                        if (cropError != null) {
                            Log.e("UCrop", "Crop error: ", cropError);
                        }
                        Toast.makeText(getContext(), "Fehler beim Zuschneiden", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        Toast.makeText(getContext(), "Kamera-Berechtigung wird benötigt, um Kennzeichen zu scannen.", Toast.LENGTH_SHORT).show();
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
        kuerzelEingabe = binding.kuerzeleingabe2;
        kuerzelEingabe.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
        textViewAusgabe2 = binding.textViewAusgabe2;
        kennzeichenKI = new Kennzeichen_KI(getContext());
        historyManager = new SearchHistoryManager(requireContext());

        Configuration.getInstance().load(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));

        ImageButton deleteText = binding.x;
        ImageView deleteBtn = binding.deleteBtn;
        ImageView saveBtn = binding.saveBtn;
        ImageView shareBtn = binding.shareBtn;
        ImageView picinfoBtn = binding.picinfoBtn;
        tourBtn = binding.tourbtn;
        mapconstlayout = binding.mapconstlayout;
        deleteText.setVisibility(View.GONE);
        deleteBtn.setVisibility(View.GONE);
        saveBtn.setVisibility(View.GONE);
        shareBtn.setVisibility(View.GONE);
        picinfoBtn.setVisibility(View.GONE);

        if (!kuerzelEingabe.getText().toString().isEmpty()) {
            deleteText.setVisibility(View.VISIBLE);
        }

        TextView infobtn = binding.textHome;
        infobtn.setOnClickListener(v -> {
            tourBtn.performClick();
        });
        tourBtn.setOnClickListener(v -> {
            ImageButton offlineButton = requireActivity().findViewById(R.id.icon_offline);
            kuerzelEingabe.setText("WOB");
            buttongenerate.performClick();
            // Warte sicherheitshalber, bis das Layout geladen ist
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);

                toolbar.post(() -> {
                    CharSequence origDesc = toolbar.getNavigationContentDescription();
                    boolean hadDesc = !TextUtils.isEmpty(origDesc);
                    CharSequence navDesc = hadDesc ? origDesc : "navigationIcon";
                    toolbar.setNavigationContentDescription(navDesc);

                    ArrayList<View> potential = new ArrayList<>();
                    toolbar.findViewsWithText(potential, navDesc, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
                    View menuButton = potential.size() > 0 ? potential.get(0) : null;

                    if (!hadDesc) {
                        toolbar.setNavigationContentDescription(null);
                    }

                    // Prüfe, ob alle Views existieren
                    if (searchPic != null && kuerzelEingabe != null && binding.likeBtn != null && menuButton != null && offlineButton != null) {

                        TapTarget target1 = TapTarget.forView(searchPic, "Bild auswählen", "Hier kannst du ein Bild deines Kennzeichens auswählen.")
                                .outerCircleColor(R.color.yellow)
                                .targetCircleColor(android.R.color.white)
                                .titleTextColor(android.R.color.black)
                                .descriptionTextColor(android.R.color.black)
                                .transparentTarget(true)
                                .targetRadius(100)
                                .cancelable(true);

                        TapTarget target2 = TapTarget.forView(binding.imagekennzeichen, "Kürzel eingeben", "Du kannst ein Ortskürzel auch direkt eintippen.")
                                .outerCircleColor(R.color.red)
                                .targetCircleColor(android.R.color.white)
                                .titleTextColor(android.R.color.black)
                                .descriptionTextColor(android.R.color.black)
                                .transparentTarget(true)
                                .targetRadius(100)
                                .cancelable(true);

                        TapTarget target3 = TapTarget.forView(binding.likeBtn, "Kennzeichen liken", "Klicke hier, um dieses Kennzeichen zu speichern.")
                                .outerCircleColor(R.color.blue_200)
                                .targetCircleColor(android.R.color.white)
                                .titleTextColor(android.R.color.black)
                                .descriptionTextColor(android.R.color.black)
                                .transparentTarget(true)
                                .targetRadius(50)
                                .cancelable(true);

                        TapTarget target4 = TapTarget.forView(offlineButton, "Offline-Modus", "Dieser Button zeigt an, dass du im Offlinemodus bist. Klicke auf ihn, um zu erfahren warum du im Offlinemodus bist.")
                                .outerCircleColor(R.color.yellow)
                                .targetCircleColor(android.R.color.white)
                                .titleTextColor(android.R.color.black)
                                .descriptionTextColor(android.R.color.black)
                                .transparentTarget(true)
                                .targetRadius(50)
                                .cancelable(true);

                        TapTarget target5 = TapTarget.forView(menuButton, "Menü öffnen", "Hier findest du weitere Seiten, Einstellungen und Funktionen.")
                                .outerCircleColor(R.color.red)
                                .targetCircleColor(android.R.color.white)
                                .titleTextColor(android.R.color.black)
                                .descriptionTextColor(android.R.color.black)
                                .transparentTarget(true)
                                .targetRadius(50)
                                .cancelable(true);

                        new TapTargetSequence(requireActivity())
                                .targets(target1, target2, target3, target4, target5)
                                .continueOnCancel(true)
                                .listener(new TapTargetSequence.Listener() {
                                    @Override
                                    public void onSequenceFinish() {}

                                    @Override
                                    public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                                        if (isNetworkAvailable()&&!isOfflineMode()) {
                                            if (lastTarget == target3) {
                                                offlineButton.setVisibility(VISIBLE);
                                            } else {
                                                offlineButton.setVisibility(GONE);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onSequenceCanceled(TapTarget lastTarget) {}
                                })
                                .start();

                    } else {
                        Toast.makeText(getContext(), "Ein oder mehrere Tour-Elemente sind nicht verfügbar", Toast.LENGTH_SHORT).show();
                    }
                });
            }, 250); // Delay, um sicherzugehen, dass alle Views sichtbar sind
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

        searchPic.setOnClickListener(v -> showImageSourceDialog());

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
            String kuerzel = String.valueOf(kuerzelEingabe.getText());
            if (kuerzel.isEmpty()) {
                recognizeTextInImage();
            } else {
                performAnalysis(kuerzel);
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
        int logSwitchStatus = sharedPreferences.getInt("logSwitch", 0);
        if (logSwitchStatus == 1) {
            textViewAusgabe2.setVisibility(View.VISIBLE);
        } else {
            textViewAusgabe2.setVisibility(View.GONE);
        }

        if (pendingImageUri != null) {
            applySelectedImage(pendingImageUri);
            pendingImageUri = null;
        }

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check for history arguments
        if (getArguments() != null) {
            String histKuerzel = getArguments().getString("history_kuerzel");
            String histImageUri = getArguments().getString("history_image_uri");
            if (histKuerzel != null) {
                kuerzelEingabe.setText(histKuerzel);
                if (histImageUri != null && !histImageUri.isEmpty()) {
                    if (histImageUri.startsWith("/")) {
                        selectedImageUri = Uri.fromFile(new File(histImageUri));
                    } else {
                        selectedImageUri = Uri.parse(histImageUri);
                    }
                    applySelectedImageNoOCR(selectedImageUri);
                } else {
                    selectedImageUri = null;
                    Glide.with(this).load(R.drawable.camera_pic).apply(RequestOptions.circleCropTransform()).into(searchPic);
                    binding.deleteBtn.setVisibility(GONE);
                    binding.saveBtn.setVisibility(GONE);
                    binding.shareBtn.setVisibility(GONE);
                    binding.picinfoBtn.setVisibility(GONE);
                }
                performAnalysis(histKuerzel);
            }
            setArguments(null);
        }

        // Falls vorher URI gependet wurde -> jetzt anwenden
        if (pendingImageUri != null) {
            Uri uri = pendingImageUri;
            pendingImageUri = null;
            handleIncomingUriOnMainThread(uri);
        }
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
            String resultText = text.toString();
            textViewAusgabe2.setText(resultText);

            String kuerzel = resultText.replaceAll("[^A-ZÄÜÖ]", " ").trim();
            String[] kuerzelArray = kuerzel.split("\\s+");
            String kuerzelAusgabe = kuerzelArray.length > 0 ? kuerzelArray[0] : "";
            
            // Erst prüfen ob das Kürzel existiert, sonst Fallback-M/W-Tausch
            if (kennzeichenKI.getKennzeichen(kuerzelAusgabe) == null) {
                String modifiedText = resultText.replace("M", "W").replace("H", "W");
                String kuerzel1 = modifiedText.replaceAll("[^A-ZÄÜÖ]", " ").trim();
                String[] kuerzelArray1 = kuerzel1.split("\\s+");
                String kuerzelAusgabe1 = kuerzelArray1.length > 0 ? kuerzelArray1[0] : "";
                if (kennzeichenKI.getKennzeichen(kuerzelAusgabe1) != null) {
                    kuerzelAusgabe = kuerzelAusgabe1;
                }
            }
            
            kuerzelEingabe.setText(kuerzelAusgabe);
            performAnalysis(kuerzelAusgabe);

        } catch (FileNotFoundException e) {
            Log.e("Error", "Datei nicht gefunden", e);
            kuerzelEingabe.setText(e.toString());
        }
    }

    private void performAnalysis(String kuerzel) {
        binding.fussnotenwert.setVisibility(VISIBLE);
        binding.fussnotentitel.setVisibility(VISIBLE);
        aistatus = 0;
        hideKeyboard(binding.getRoot());
        updateTextViewAusgabe2();

        if (kuerzel != null && !kuerzel.isEmpty()) {
            binding.x.setVisibility(View.VISIBLE);
        }

        recognizeCity(kuerzel);
        Kennzeichen kennzeichen = kennzeichenKI.getKennzeichen(kuerzel);

        Log.d("Kennzeichen", "Analyse für Kürzel: " + kuerzel);
        Log.d("Kennzeichen", "Gefundenes Kennzeichen: " + (kennzeichen != null ? kennzeichen.OertskuerzelGeben() : "null"));

        if (kennzeichen != null) {
            binding.sliderview.setVisibility(View.VISIBLE);
            binding.kuerzelwert.setText(kennzeichen.OertskuerzelGeben());
            binding.herleitungswert.setText(kennzeichen.OrtGeben());
            binding.stadtoderkreiswert.setText(kennzeichen.StadtKreisGeben());
            if (!Objects.equals(kennzeichen.BundeslandGeben(), "---")) {
                binding.bundeslandwert.setText(kennzeichen.BundeslandGeben());
                binding.bundeslandwert.setVisibility(VISIBLE);
                binding.bundeslandtitel.setVisibility(VISIBLE);
            } else {
                binding.bundeslandwert.setVisibility(GONE);
                binding.bundeslandtitel.setVisibility(GONE);
            }
            if (!Objects.equals(kennzeichen.BundeslandIsoGeben(), "---")) {
                binding.bundeslandIsoWert.setText(kennzeichen.BundeslandIsoGeben());
                binding.bundeslandIsoWert.setVisibility(VISIBLE);
                binding.bundeslandIsoTitel.setVisibility(VISIBLE);
            } else {
                binding.bundeslandIsoWert.setVisibility(GONE);
                binding.bundeslandIsoTitel.setVisibility(GONE);
            }
            binding.landwert.setText(kennzeichen.LandGeben());
            if (kennzeichen.isSaved()) {
                binding.likedBtn.setVisibility(VISIBLE);
            } else {
                binding.likedBtn.setVisibility(GONE);
            }

            int fussnoteNummer = 6;
            if (!Objects.equals(kennzeichen.FussnoteGeben(), "")) {
                try {
                    fussnoteNummer = Integer.parseInt(kennzeichen.FussnoteGeben());
                } catch (NumberFormatException ignored) {}
            }
            String[] fussnoten = {
                    "Stadt- und Landkreis führen das gleiche Unterscheidungszeichen. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungs nummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung für deren Behörden oder zusätzliche Verwaltungsstellen erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle.",
                    "Stadt- und Landkreis führen das gleiche Unterscheidungszeichen. Die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle stellt durch geeignete verwaltungsinterne Maßnahmen sicher, dass eine Doppelvergabe desselben Kennzeichens ausgeschlossen ist.",
                    "amtlicher Hinweis: Das Unterscheidungszeichen wird durch mehrere Verwaltungsbezirke verwaltet. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung, die in den jeweiligen Verwaltungsbezirken durch die dort zuständigen Behörden oder zusätzliche Verwaltungsstellen ausgegeben werden, erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle.",
                    "amtlicher Hinweis: Das Unterscheidungszeichen wird durch mehrere Verwaltungsbezirke verwaltet. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung, die in den jeweiligen Verwaltungsbezirken durch die dort zuständigen Behörden oder zusätzliche Verwaltungsstellen ausgegeben werden, erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle in Sachsen-Anhalt im Einvernehmen mit der obersten Landesbehörde oder der nach Landesrecht zuständigen Stelle in Baden-Württemberg.",
                    "amtlicher Hinweis: Das Unterscheidungszeichen wird durch mehrere Verwaltungsbezirke verwaltet. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung, die in den jeweiligen Verwaltungsbezirken durch die dort zuständigen Behörden oder zusätzliche Verwaltungsstellen ausgegeben werden, erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle in Baden-Württemberg im Einvernehmen mit der obersten Landesbehörde oder der nach Landesrecht zuständigen Stelle in Sachsen-Anhalt.",
                    "amtlicher Hinweis: Die Stadt und die Landespolizei Sachsen führen das gleiche Unterscheidungszeichen. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung für deren Behörden oder zusätzlichen Verwaltungsstellen erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle.",
                    "---",
                    "amtlicher Hinweis: Das Unterscheidungszeichen wird durch mehrere Verwaltungsbezirke verwaltet. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung, die in den jeweiligen Verwaltungsbezirken durch die dort zuständigen Behörden oder zusätzliche Verwaltungsstellen ausgegeben werden, erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle in Baden-Württemberg im Einvernehmen mit der obersten Landesbehörde oder der nach Landesrecht zuständigen Stelle in Sachsen-Anhalt.\n\n- weiterer amtlicher Hinweis: Die Stadt und die Landespolizei Sachsen führen das gleiche Unterscheidungszeichen. Die Festlegung der Gruppen oder Nummerngruppen der Erkennungsnummer nach Anlage 2 der Fahrzeug-Zulassungsverordnung für deren Behörden oder zusätzlichen Verwaltungsstellen erfolgt durch die zuständige oberste Landesbehörde oder die nach Landesrecht zuständige Stelle.",
            };

            String fussnoteText = "";
            if (fussnoteNummer >= 0 && fussnoteNummer < fussnoten.length) {
                fussnoteText = fussnoten[fussnoteNummer];
            } else {
                fussnoteText = String.valueOf(fussnoteNummer);
            }

            binding.fussnotenwert.setText(fussnoteText);

            if (kennzeichen.FussnoteGeben().isEmpty() || Objects.equals(kennzeichen.FussnoteGeben(), "6")) {
                if (kennzeichen.BemerkungenGeben().isEmpty() || Objects.equals(kennzeichen.BemerkungenGeben(), "---")) {
                    binding.fussnotencard.setVisibility(GONE);
                } else {
                    binding.fussnotencard.setVisibility(VISIBLE);
                    binding.fussnotenwert.setText("- " + kennzeichen.BemerkungenGeben());
                }
            } else if (kennzeichen.BemerkungenGeben().isEmpty() || Objects.equals(kennzeichen.BemerkungenGeben(), "---")) {
                binding.fussnotencard.setVisibility(VISIBLE);
                binding.fussnotenwert.setText("- " + fussnoteText);
            } else {
                binding.fussnotencard.setVisibility(VISIBLE);
                binding.fussnotenwert.setText("- " + kennzeichen.BemerkungenGeben() + "\n\n- " + fussnoteText);
            }

            // Save to history if not same as last one
            String imageUriString = selectedImageUri != null ? selectedImageUri.toString() : null;
            String currentSearchKey = kennzeichen.OertskuerzelGeben() + (imageUriString != null ? imageUriString : "");
            if (!currentSearchKey.equals(lastSavedSearchKey)) {
                historyManager.addEntry(new SearchEntry(kennzeichen.OertskuerzelGeben(), kennzeichen.OrtGeben(), imageUriString));
                lastSavedSearchKey = currentSearchKey;
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
                binding.stadtoderkreistitel.setText("Bisheriger Verwaltungsbezirk/-kreis:  ");
                binding.stadtoderkreistitel.setTextSize(13.5F);
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
                mapView.setMultiTouchControls(true);
                mapCardView.setVisibility(View.VISIBLE);
                binding.kurzCard.setVisibility(GONE);
                showaiText(kennzeichen, "on");

                if (!kennzeichen.isSonderDE()) {
                    setMarkerOnMap(kennzeichen.OrtGeben() + "_" + kennzeichen.BundeslandGeben());
                } else {
                    binding.mapconstlayout.setVisibility(GONE);
                }
            } else {
                if (kennzeichen.isSonderDE()) {
                    binding.mapconstlayout.setVisibility(GONE);
                } else {
                    binding.mapconstlayout.setVisibility(VISIBLE);
                    binding.mapcardview.setVisibility(GONE);
                }
                binding.kurzCard.setVisibility(VISIBLE);
                binding.kurzCardText.setText(kennzeichen.OertskuerzelGeben());
                showaiText(kennzeichen, "off");
            }
        } else {
            binding.sliderview.setVisibility(View.GONE);
            textViewAusgabe.setText("Dieses Kennzeichen kenne ich leider nicht 😒!");
            Toast.makeText(getActivity(), "Kein Kennzeichen gefunden", Toast.LENGTH_SHORT).show();
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
        int logSwitchStatus = sharedPreferences.getInt("logSwitch", 0);
        if (logSwitchStatus == 0) {
            textViewAusgabe2.setVisibility(View.GONE);
            Log.e("visible", "1");
        } else {
            textViewAusgabe2.setVisibility(View.VISIBLE);
            Log.e("visible", "0");
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && !isOfflineMode();
    }

    private void setMarkerOnMap(String locationName) {
        executor.execute(() -> {
            GeoPoint geoPoint = getCoordinates(locationName);
            mainHandler.post(() -> {
                if (geoPoint != null) {
                    mapconstlayout.setVisibility(VISIBLE);
                    mapView.getOverlays().clear();
                    mapView.getController().setZoom(6.25);
                    mapView.getController().setCenter(new GeoPoint(51.163409, 10.447718));
                    Marker marker = new Marker(mapView);
                    marker.setPosition(geoPoint);
                    marker.setTitle("Gesuchtes Kennzeichen");
                    mapView.getOverlays().add(marker);
                    mapView.invalidate();
                } else {
                    mapconstlayout.setVisibility(GONE);
                }
            });
        });
    }

    private GeoPoint getCoordinates(String location) {
        Log.e("Achtung", location);
        try {
            String url = "https://nominatim.openstreetmap.org/search?q=" + URLEncoder.encode(location + "_", "UTF-8") + "&format=json&addressdetails=1";
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "de.haainz.kennzeichenerkennung/1.0 (mailto:kennzeichenerkennung@gmail.com)");
            connection.setRequestProperty("Accept-Language", "de");
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return null;
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

    private String formatAIText(String aiText) {
        return aiText.trim() + "\n\n(KI-generierter Inhalt, keine Gewähr)";
    }

    public void showaiText(Kennzeichen kennzeichen, String onlinestatus) {
        if(!Objects.equals(kennzeichen.aiTextGeben(), "")) {
            binding.infotextwert.setText(formatAIText(kennzeichen.aiTextGeben()));
        } else {
            String standardText = "Es wurde noch kein KI-Text zu diesem Kennzeichen erstellt. Klicke hier um einen zu generieren.";
            if(Objects.equals(onlinestatus, "off")) {
                standardText = "Es wurde noch kein KI-Text zu diesem Kennzeichen erstellt.";
            }
            binding.infotextwert.setText(standardText);
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
            showaiText(kennzeichen, "off");
        }
    }

    private void generateAIText(Kennzeichen kennzeichen) {
        AIManager aiManager = new AIManager(requireContext(), null, this);

        startLoadingAnimation();

        aiManager.generateAIText(kennzeichen, new AIManager.AICallback() {
            @Override
            public void onResult(String aiText) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    stopLoadingAnimation();
                    binding.infotextwert.setText(aiText);
                });
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    stopLoadingAnimation();
                    showErrorState();
                    Log.e("AI_ERROR", errorMessage);
                });
            }
        });
    }

    private void startLoadingAnimation() {
        if (binding != null) {
            binding.infotexttitel.setVisibility(View.VISIBLE);
            binding.infotextwert.setVisibility(View.VISIBLE);
            binding.infotextwert.setText("Analysiere Informationen");

            loadingHandler = new Handler(Looper.getMainLooper());
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

    public void performTourClick() {
        if (tourBtn != null) {
            tourBtn.performClick();
        }
    }

    private void showImageSourceDialog() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        NavHostFragment.findNavController(this).navigate(R.id.nav_scanner);
    }

    private void openGallery() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK);
        pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        pickImageLauncher.launch(pickIntent);
    }

    // verarbeite Bundle-Ergebnis (verschiedene Keys möglich)
    private void handleIncomingScanResult(Bundle bundle) {
        if (bundle == null) {
            Log.w(TAG_IMG, "handleIncomingScanResult: bundle == null");
            return;
        }

        Uri imageUri;
        if (bundle.containsKey("image_uri")) {
            imageUri = BundleCompat.getParcelable(bundle, "image_uri", Uri.class);
        } else if (bundle.containsKey("scanned_plate_uri")) {
            String s = bundle.getString("scanned_plate_uri");
            if (s != null) imageUri = Uri.parse(s);
            else {
                imageUri = null;
            }
        } else if (bundle.containsKey("imageUri")) {
            imageUri = BundleCompat.getParcelable(bundle, "imageUri", Uri.class);
        } else {
            imageUri = null;
        }

        if (imageUri == null) {
            Log.w(TAG_IMG, "handleIncomingScanResult: no uri found in bundle");
            return;
        }

        Log.d(TAG_IMG, "handleIncomingScanResult: got imageUri=" + imageUri);

        // Wenn Fragment noch nicht bereit ist, cache die URI
        if (!isAdded() || getActivity() == null || binding == null) {
            Log.d(TAG_IMG, "handleIncomingScanResult: fragment not ready, caching pendingImageUri");
            pendingImageUri = imageUri;
            return;
        }

        // Auf Main thread anwenden
        requireActivity().runOnUiThread(() -> applySelectedImage(imageUri));
    }

    // sorgt dafür, dass UI-Update sicher ausgeführt wird (oder cached)
    private void handleIncomingUriOnMainThread(Uri uri) {
        if (uri == null) return;

        if (binding == null) {
            Log.d(TAG_IMG, "handleIncomingUriOnMainThread: binding null - caching pendingImageUri=" + uri);
            pendingImageUri = uri;
            return;
        }

        if (Looper.myLooper() != Looper.getMainLooper()) {
            requireActivity().runOnUiThread(() -> applySelectedImage(uri));
        } else {
            applySelectedImage(uri);
        }
    }

    private void applySelectedImageNoOCR(Uri imageUri) {
        if (imageUri == null || binding == null) return;
        Glide.with(this)
                .load(imageUri)
                .apply(RequestOptions.fitCenterTransform())
                .into(binding.imageView2);
        binding.deleteBtn.setVisibility(View.VISIBLE);
        binding.saveBtn.setVisibility(View.VISIBLE);
        binding.shareBtn.setVisibility(View.VISIBLE);
        binding.picinfoBtn.setVisibility(View.VISIBLE);
        binding.x.setVisibility(View.VISIBLE);
    }

    // setzt das Bild in imageView2 (Glide) + sichtbar machen Buttons + startet OCR
    private void applySelectedImage(Uri imageUri) {
        if (imageUri == null) {
            Log.w(TAG_IMG, "applySelectedImage: imageUri == null");
            return;
        }

        // binding prüfen
        if (binding == null) {
            Log.d(TAG_IMG, "applySelectedImage: binding null -> caching pendingImageUri=" + imageUri);
            pendingImageUri = imageUri;
            return;
        }

        // Sicherstellen Main-Thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            requireActivity().runOnUiThread(() -> applySelectedImage(imageUri));
            return;
        }

        try {
            Log.d(TAG_IMG, "applySelectedImage: loading uri=" + imageUri);

            binding.imageView2.setAdjustViewBounds(true);
            binding.imageView2.setScaleType(ImageView.ScaleType.FIT_CENTER);

            // Versuche Glide
            Glide.with(requireActivity())
                    .asBitmap()
                    .load(imageUri)
                    .apply(RequestOptions.fitCenterTransform())
                    .into(new com.bumptech.glide.request.target.ImageViewTarget<Bitmap>(binding.imageView2) {
                        @Override
                        protected void setResource(@Nullable Bitmap resource) {
                            if (resource != null) {
                                binding.imageView2.setImageBitmap(resource);
                                // Buttons sichtbar
                                binding.deleteBtn.setVisibility(View.VISIBLE);
                                binding.saveBtn.setVisibility(View.VISIBLE);
                                binding.shareBtn.setVisibility(View.VISIBLE);
                                binding.picinfoBtn.setVisibility(View.VISIBLE);
                                binding.x.setVisibility(View.VISIBLE);

                                selectedImageUri = imageUri;
                                // OCR starten (nutzt selectedImageUri)
                                recognizeTextInImage();
                            } else {
                                // Glide lieferte null -> fallback
                                Log.w(TAG_IMG, "Glide returned null bitmap, falling back to manual load");
                                runFallbackLoad(imageUri);
                            }
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            Log.e(TAG_IMG, "Glide failed to load image, falling back to manual load");
                            runFallbackLoad(imageUri);
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG_IMG, "applySelectedImage error -> fallback", e);
            runFallbackLoad(imageUri);
        }
    }

    private void runFallbackLoad(Uri imageUri) {
        // Manuelles Laden mit Sampling (falls Glide scheitert)
        try {
            Bitmap bmp = loadBitmapFromUriWithSampling(requireContext(), imageUri, 1200, 1200);
            if (bmp != null) {
                binding.imageView2.setImageBitmap(bmp);
                binding.imageView2.setAdjustViewBounds(true);
                binding.imageView2.setScaleType(ImageView.ScaleType.FIT_CENTER);

                binding.deleteBtn.setVisibility(View.VISIBLE);
                binding.saveBtn.setVisibility(View.VISIBLE);
                binding.shareBtn.setVisibility(View.VISIBLE);
                binding.picinfoBtn.setVisibility(View.VISIBLE);
                binding.x.setVisibility(View.VISIBLE);

                selectedImageUri = imageUri;
                recognizeTextInImage();
            } else {
                Log.e(TAG_IMG, "runFallbackLoad: loaded bitmap == null");
                Toast.makeText(getContext(), "Bild konnte nicht geladen werden", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG_IMG, "runFallbackLoad: exception", e);
            Toast.makeText(getContext(), "Fehler beim Laden des Bildes", Toast.LENGTH_SHORT).show();
        }
    }

    private static Bitmap loadBitmapFromUriWithSampling(Context ctx, Uri uri, int reqWidth, int reqHeight) {
        if (ctx == null || uri == null) return null;

        try {
            // 1) nur Maße auslesen
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream is = ctx.getContentResolver().openInputStream(uri)) {
                if (is == null) return null;
                BitmapFactory.decodeStream(is, null, options);
            }

            // 2) inSampleSize berechnen
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // 3) Bitmap tatsächlich laden
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inMutable = true; // erlaubt evtl. spätere Bearbeitung
            Bitmap bitmap;
            try (InputStream is2 = ctx.getContentResolver().openInputStream(uri)) {
                if (is2 == null) return null;
                bitmap = BitmapFactory.decodeStream(is2, null, options);
            }

            if (bitmap == null) return null;

            // 4) EXIF-Rotation berücksichtigen (falls vorhanden)
            try (InputStream exifStream = ctx.getContentResolver().openInputStream(uri)) {
                if (exifStream != null) {
                    ExifInterface exif = new ExifInterface(exifStream);
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

                    Matrix matrix = new Matrix();
                    boolean needTransform = false;

                    switch (orientation) {
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            matrix.postRotate(90);
                            needTransform = true;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            matrix.postRotate(180);
                            needTransform = true;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            matrix.postRotate(270);
                            needTransform = true;
                            break;
                        case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                            matrix.postScale(-1, 1);
                            needTransform = true;
                            break;
                        case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                            matrix.postScale(1, -1);
                            needTransform = true;
                            break;
                        case ExifInterface.ORIENTATION_TRANSPOSE:
                            matrix.postRotate(90);
                            matrix.postScale(-1, 1);
                            needTransform = true;
                            break;
                        case ExifInterface.ORIENTATION_TRANSVERSE:
                            matrix.postRotate(270);
                            matrix.postScale(-1, 1);
                            needTransform = true;
                            break;
                        default:
                            // ORIENTATION_UNDEFINED or normal -> nichts zu tun
                    }

                    if (needTransform) {
                        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        if (rotated != bitmap) {
                            bitmap.recycle();
                            bitmap = rotated;
                        }
                    }
                }
            } catch (Exception e) {
                // EXIF lesen darf nicht fehlschlagen — falls doch, benutzen wir das unveränderte Bitmap
                e.printStackTrace();
            }

            return bitmap;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            // wähle größtes inSampleSize (Potenz von 2), das noch >= requirments ist
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }

            // zusätzlicher Feinabgleich: wenn noch zu groß, erhöhe weiter
            while ((height / inSampleSize) > reqHeight * 2 || (width / inSampleSize) > reqWidth * 2) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}