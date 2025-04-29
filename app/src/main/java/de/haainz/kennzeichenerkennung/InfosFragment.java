package de.haainz.kennzeichenerkennung;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

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

import de.haainz.kennzeichenerkennung.ui.list.ListFragment;

public class InfosFragment extends Fragment {

    private Kennzeichen kennzeichen;
    private Kennzeichen_KI kennzeichenKI;
    private MapView mapView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            kennzeichen = (Kennzeichen) getArguments().getSerializable("selectedKennzeichen");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_infos, container, false);
        kennzeichenKI = new Kennzeichen_KI(getActivity());

        getActivity().setTitle("Kennze1chen " + kennzeichen.OertskuerzelGeben());

        ImageView likedBtn = view.findViewById(R.id.liked_btn);
        ImageView likeBtn = view.findViewById(R.id.like_btn);

        kennzeichenKI.KennzeichenLikedEinlesen();
        if (kennzeichenKI.LikeÜberprüfen(kennzeichen.OertskuerzelGeben())) {
            likedBtn.setVisibility(View.VISIBLE);
        } else {
            likedBtn.setVisibility(View.GONE);
        }

        // Setze die TextViews mit den Werten des Kennzeichens
        setTextViews(view);

        likeBtn.setOnClickListener(v -> handleLikeButtonClick(likedBtn));
        likedBtn.setOnClickListener(v -> handleUnlikeButtonClick(likedBtn));

        if (isNetworkAvailable() && !kennzeichen.isSonder()) {
            setupMapView(view);
        } else {
            view.findViewById(R.id.map).setVisibility(View.GONE);
        }

        return view;
    }

    private void setTextViews(View view) {
        TextView kuerzelWert = view.findViewById(R.id.kuerzelwert);
        kuerzelWert.setText(kennzeichen.OertskuerzelGeben());

        TextView herleitungWert = view.findViewById(R.id.herleitungswert);
        herleitungWert.setText(kennzeichen.OrtGeben());

        TextView stadtOderKreisWert = view.findViewById(R.id.stadtoderkreiswert);
        stadtOderKreisWert.setText(kennzeichen.StadtKreisGeben());

        TextView bundeslandWert = view.findViewById(R.id.bundeslandwert);
        bundeslandWert.setText(kennzeichen.BundeslandGeben());

        TextView bundeslandIsoWert = view.findViewById(R.id.bundesland_iso_wert);
        bundeslandIsoWert.setText(kennzeichen.BundeslandIsoGeben());

        TextView landWert = view.findViewById(R.id.landwert);
        landWert.setText(kennzeichen.LandGeben());

        // Weitere TextViews hier setzen...
    }

    private void handleLikeButtonClick(ImageView likedBtn) {
        if (!kennzeichenKI.LikeÜberprüfen(kennzeichen.OertskuerzelGeben())) {
            String csvZeile = kennzeichen.LandGeben() + "," + kennzeichen.OertskuerzelGeben() + "," + kennzeichen.StadtKreisGeben() + "," + kennzeichen.OrtGeben() + "," + kennzeichen.BundeslandGeben() + "," + kennzeichen.BundeslandIsoGeben() + "," + kennzeichen.FussnoteGeben() + "," + kennzeichen.BemerkungenGeben();
            try {
                File file = new File(getActivity().getFilesDir(), "kennzeichenliked.csv ");
                FileOutputStream fileOutputStream = new FileOutputStream(file, true);
                fileOutputStream.write((csvZeile + "\n").getBytes());
                fileOutputStream.close();
                likedBtn.setVisibility(View.VISIBLE);
                Toast.makeText(getActivity(), "Kennzeichen gespeichert.\nBitte aktualisiere die Liste", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), "Es ist ein Fehler aufgetreten", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getActivity(), "Kennzeichen bereits geliked", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleUnlikeButtonClick(ImageView likedBtn) {
        likedBtn.setVisibility(View.GONE);
        kennzeichenKI.deletelikedKennzeichen(kennzeichen);
        Toast.makeText(getActivity(), "Kennzeichen entfernt.\nBitte aktualisiere die Liste", Toast.LENGTH_SHORT).show();
        // Hier sollten Sie die updateList() Methode aufrufen, um die Anzeige zu aktualisieren
        if (getParentFragment() instanceof ListFragment) {
            ((ListFragment) getParentFragment()).updateList();
        }
    }

    private void setupMapView(View view) {
        mapView = view.findViewById(R.id.map);
        Configuration.getInstance().load(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.setVisibility(View.VISIBLE);
        setMarkerOnMap(kennzeichen.OrtGeben());
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected() && !isOfflineMode();
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
        protected GeoPoint doInBackground(String... params) {
            String location = params[0];
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
            InfosFragment fragment = fragmentReference.get();
            if (fragment != null) {
                if (geoPoint != null) {
                    fragment.mapView.getController().setZoom(6.25);
                    fragment.mapView.getController().setCenter(geoPoint);
                    Marker marker = new Marker(fragment.mapView);
                    marker.setPosition(geoPoint);
                    marker.setTitle(formatLabel(fragment.kennzeichen.OrtGeben()));
                    fragment.mapView.getOverlays().add(marker);
                    fragment.mapView.invalidate();
                } else {
                    fragment.mapView.setVisibility(View.GONE);
                }
            }
        }
    }

    public static String formatLabel(String label) {
        if (label == null || label.isEmpty()) {
            return label;
        }
        String formattedLabel = label.substring(0, 1).toUpperCase() + label.substring(1).toLowerCase();
        return formattedLabel;
    }

    private boolean isOfflineMode() {
        SharedPreferences prefs = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getBoolean("offlineSwitch", false);
    }
}