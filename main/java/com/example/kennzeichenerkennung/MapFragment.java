package com.example.kennzeichenerkennung;

import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Objects;

public class MapFragment extends DialogFragment {
    private MapView mapView;
    private Kennzeichen kennzeichen;

    public MapFragment(Kennzeichen kennzeichen) {
        this.kennzeichen = kennzeichen;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        mapView = view.findViewById(R.id.map);
        Configuration.getInstance().load(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls (true);
        mapView.setVisibility(View.VISIBLE);

        setMarkerOnMap(kennzeichen.OrtGeben());

        ImageButton xBtn = view.findViewById(R.id.x);
        xBtn.setOnClickListener(v -> dismiss());

        return view;
    }

    private void setMarkerOnMap(String location) {
        new MapFragment.GetCoordinatesTask(this).execute(location);
    }

    private static class GetCoordinatesTask extends AsyncTask<String, Void, GeoPoint> {
        private final WeakReference<MapFragment> fragmentReference;
        String label;

        GetCoordinatesTask(MapFragment fragment) {
            fragmentReference = new WeakReference<>(fragment);
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
                if (jsonArray. length() > 0) {
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    double lat = jsonObject.getDouble("lat");
                    double lon = jsonObject.getDouble("lon");
                    return new GeoPoint(lat, lon);
                }
            } catch (Exception e) {
                Log.e("Error", "Failed to get coordinates", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(GeoPoint geoPoint) {
            MapFragment fragment = fragmentReference.get();
            if (fragment != null && geoPoint != null) {
                fragment.mapView.getController().setZoom(7.25);
                fragment.mapView.getController().setCenter(new GeoPoint(51.163409, 10.447718));
                Marker marker = new Marker(fragment.mapView);
                marker.setPosition(geoPoint);
                marker.setTitle(formatLabel(label));
                fragment.mapView.getOverlays().add(marker);
                fragment.mapView.invalidate();
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
}