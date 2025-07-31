package de.haainz.kennzeichenerkennung.ui.day;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.animation.ValueAnimator;
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
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import de.haainz.kennzeichenerkennung.InfosFragment;
import de.haainz.kennzeichenerkennung.Kennzeichen;
import de.haainz.kennzeichenerkennung.KennzeichenGenerator;
import de.haainz.kennzeichenerkennung.Kennzeichen_KI;
import de.haainz.kennzeichenerkennung.MapFragment;
import de.haainz.kennzeichenerkennung.PicInfoDialogFragment;
import de.haainz.kennzeichenerkennung.R;
import de.haainz.kennzeichenerkennung.databinding.FragmentDayBinding;

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

import de.haainz.kennzeichenerkennung.ui.AIManager;

public class DayFragment extends Fragment {

    public FragmentDayBinding binding;
    private Kennzeichen_KI kennzeichenKI;
    private KennzeichenGenerator kennzeichenGenerator;
    private Bitmap imageBitmap;
    private MapView mapView;
    public Kennzeichen currentKennzeichen;
    private Handler loadingHandler;
    private Runnable loadingRunnable;
    private int loadingStep = 0;
    private NestedScrollView scrollView;
    private View mainContent;
    private float originalScale = 1.0f;
    private float minScale = 0.0f;
    private boolean isSnapping = false;
    private int snapDistance = 1300; // Höhe des schrumpfenden Elements (kannst du dynamisch setzen)
    private int snapThreshold = 500;
    private TextView title1, title2;
    private int titleStartMargin = 80; // dp
    private int titleEndMargin = -15;   // dp
    private int titleStartMarginPx, titleEndMarginPx;
    private LinearLayout titleContainer;
    private SwipeRefreshLayout swipeRefreshLayout;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentDayBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Configuration.getInstance().load(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("settings", getActivity().MODE_PRIVATE);

        kennzeichenKI = new Kennzeichen_KI(getActivity());
        kennzeichenGenerator = new KennzeichenGenerator(getContext());

        binding.Bemerkungenwert.setVisibility(VISIBLE);

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
        binding.imagecardoftheday.setImageBitmap(imageBitmap);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        String currentDate = dateFormat.format(new Date());
        binding.titleLine2.setText("Heute, " + currentDate + ":");

        binding.kuerzelwert.setText(currentKennzeichen.OertskuerzelGeben());
        binding.herleitungswert.setText(currentKennzeichen.OrtGeben());
        binding.stadtoderkreiswert.setText(currentKennzeichen.StadtKreisGeben());
        binding.bundeslandwert.setText(currentKennzeichen.BundeslandGeben());
        binding.bundeslandIsoWert.setText(currentKennzeichen.BundeslandIsoGeben());
        binding.landwert.setText(currentKennzeichen.LandGeben());
        if (!currentKennzeichen.BemerkungenGeben().isEmpty()) {
            binding.Bemerkungenwert.setText(currentKennzeichen.BemerkungenGeben());
        } else {
            binding.Bemerkungenwert.setVisibility(GONE);
        }

        binding.thinkBtn.setOnClickListener(v -> {
            stopLoadingAnimation();
            checkNetworkAndGenerateText(currentKennzeichen);
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

        binding.imagecardoftheday.setOnClickListener(v -> {
            int currentY = scrollView.getScrollY();

            ValueAnimator animator = ValueAnimator.ofInt(currentY, 0);
            animator.setDuration(600); // Dauer in Millisekunden – hier: 600ms für langsameres Scrollen
            animator.setInterpolator(new DecelerateInterpolator()); // sanftes Abbremsen
            animator.addUpdateListener(animation -> {
                int animatedValue = (int) animation.getAnimatedValue();
                scrollView.scrollTo(0, animatedValue);
            });
            animator.start();
        });

        binding.kurzCard.setOnClickListener(v -> binding.imagecardoftheday.performClick());

        binding.factscardOftheday.setOnClickListener(v -> {
            InfosFragment infosFragment = new InfosFragment(currentKennzeichen);
            infosFragment.show(getParentFragmentManager(), "InfosFragment");
        });

        Configuration.getInstance().load(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));

        if (isNetworkAvailable()) {
            mapView = binding.map;
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setBuiltInZoomControls(false);
            mapView.setMultiTouchControls(false);
            mapView.setVisibility(VISIBLE);
            binding.maprel.setVisibility(VISIBLE);
            binding.kurzCard.setVisibility(GONE);
            binding.imagecardOftheday.setVisibility(VISIBLE);
            showaiText(currentKennzeichen, "on");
            binding.thinkBtn.setVisibility(VISIBLE);

            if (mapView.getVisibility() == View.VISIBLE) {
                getCoordinates(currentKennzeichen.OrtGeben() + "_" + currentKennzeichen.BundeslandGeben());
            } else {
                Log.e("DayFragment", "mapView is not visible, cannot get coordinates.");
            }
        } else {
            binding.mapCard.setVisibility(GONE);
            binding.map.setVisibility(GONE);
            binding.maprel.setVisibility(GONE);
            binding.kurzCard.setVisibility(VISIBLE);
            binding.kurzCardText.setText(currentKennzeichen.oertskuerzel);
            binding.imagecardOftheday.setVisibility(GONE);
            showaiText(currentKennzeichen, "off");
            binding.thinkBtn.setVisibility(GONE);
        }
        showStandardText(currentKennzeichen, dayOfYear);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        scrollView = view.findViewById(R.id.scroll);
        mainContent = view.findViewById(R.id.maincontent);
        scrollView = view.findViewById(R.id.scroll);
        mainContent = view.findViewById(R.id.maincontent);
        title1 = view.findViewById(R.id.title_line1);
        title2 = view.findViewById(R.id.title_line2);
        titleContainer = view.findViewById(R.id.titleContainer);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.popBackStack(); // Entfernt das Fragment
            navController.navigate(R.id.nav_gallery); // Navigiert neu zum selben Fragment
        });

        // Umrechnung von dp in px
        float density = getResources().getDisplayMetrics().density;
        titleStartMarginPx = (int) (titleStartMargin * density);
        titleEndMarginPx = (int) (titleEndMargin * density);

        scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            private int lastScrollY = 0;
            private final long SCROLL_IDLE_DELAY = 150; // ms bis wir Scroll-Stillstand annehmen

            private final Handler idleHandler = new Handler();
            private Runnable idleRunnable = null;

            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                lastScrollY = scrollY;

                // ⬇️ Dynamisches Schrumpfen von maincontent
                float factor = 1 - (scrollY / (float) snapDistance);
                if (factor < minScale) factor = minScale;
                if (factor > originalScale) factor = originalScale;

                mainContent.setScaleX(factor);
                mainContent.setScaleY(factor);
                mainContent.setPivotX(mainContent.getWidth() / 2f);
                mainContent.setPivotY(mainContent.getHeight());

                // ⬇️ Dynamisches Margin für Title
                float marginFactor = Math.max(0f, Math.min(1f, scrollY / (float) snapDistance));
                int newMarginTop = (int) (titleStartMarginPx - (titleStartMarginPx - titleEndMarginPx) * marginFactor);

                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) titleContainer.getLayoutParams();
                params.topMargin = newMarginTop;
                titleContainer.post(() -> titleContainer.setLayoutParams(params));

                // Fade out titleLine1
                float fadeFactor = 1f - marginFactor; // 1 → 0
                title1.setAlpha(fadeFactor);

                // 🧲 Snap nur wenn Scrollen stoppt
                if (idleRunnable != null) idleHandler.removeCallbacks(idleRunnable);
                idleRunnable = () -> {
                    if (isSnapping) return;
                    if (lastScrollY < snapThreshold) {
                        smoothScrollTo(0);
                    } else if (lastScrollY < snapDistance) {
                        smoothScrollTo(snapDistance);
                    }
                };
                idleHandler.postDelayed(idleRunnable, SCROLL_IDLE_DELAY);
            }
        });
    }

    private void smoothScrollTo(int targetY) {
        isSnapping = true;
        scrollView.post(() -> {
            scrollView.smoothScrollTo(0, targetY);
            scrollView.postDelayed(() -> isSnapping = false, 300);
        });
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
                    fragment.mapView.getController().setZoom(6.25);
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

    private void checkNetworkAndGenerateText(Kennzeichen kennzeichen) {
        AIManager aiManager = new AIManager(requireContext(), this, null);
        if (isNetworkAvailable() && !isOfflineMode()) {
            aiManager.generateAIText(kennzeichen, null);
            binding.aitextOftheday.setText("Analysiere Informationen...");
        } else {
            showaiText(kennzeichen, "off");
        }
    }

    private String formatAIText(String aiText) {
        return aiText.trim() + "\n\n(KI-generierter Inhalt, keine Gewähr)";
    }

    private void showStandardText(Kennzeichen kennzeichen, int dayOfYear) {
        String standardText = "Heute, am " + new SimpleDateFormat("dd.MM.yyyy").format(new Date()) +
                ", dem " + dayOfYear + ". Tag diesen Jahres ist das Kennzeichen-Kürzel des Tages " +
                kennzeichen.OertskuerzelGeben() + ". " + kennzeichen.OertskuerzelGeben() +
                " leitet sich von " + kennzeichen.OrtGeben() + " ab und gehört zur Stadt bzw. zum Kreis " +
                kennzeichen.StadtKreisGeben() + ".\n" + kennzeichen.StadtKreisGeben() +
                " liegt in Deutschland im Bundesland " + kennzeichen.BundeslandGeben() + ".";

        binding.textOftheday.setText(standardText);
    }

    public void showaiText(Kennzeichen kennzeichen, String onlinestatus) {
        if(!Objects.equals(kennzeichen.aiTextGeben(), "")) {
            binding.aitextOftheday.setText(formatAIText(kennzeichen.aiTextGeben()));
        } else {
            String standardText = "Es wurde noch kein KI-Text zu diesem Kennzeichen erstellt. Klicke auf die Denkblase um einen zu generieren.";
            if(Objects.equals(onlinestatus, "off")) {
                standardText = "Es wurde noch kein KI-Text zu diesem Kennzeichen erstellt.";
            }
            binding.aitextOftheday.setText(standardText);
        }
    }

    public void startLoadingAnimation() {
        if (binding != null) {
            binding.aitextOftheday.setText("Analysiere Informationen");

            loadingHandler = new Handler();
            loadingRunnable = new Runnable() {
                @Override
                public void run() {
                    switch (loadingStep % 4) {
                        case 0:
                            binding.aitextOftheday.setText("Analysiere Informationen.");
                            break;
                        case 1:
                            binding.aitextOftheday.setText("Analysiere Informationen..");
                            break;
                        case 2:
                            binding.aitextOftheday.setText("Analysiere Informationen...");
                            break;
                        case 3:
                            binding.aitextOftheday.setText("Analysiere Informationen");
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

    public void stopLoadingAnimation() {
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
}