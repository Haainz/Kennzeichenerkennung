package com.example.kennzeichenerkennung;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat darkModeSwitch;
    private SharedPreferences sharedPreferences;
    private Spinner aiSp;
    private ArrayList<String> aiList = new ArrayList<>();
    private ArrayAdapter<String> aiAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);

        darkModeSwitch = findViewById(R.id.dark_mode_switch);
        aiSp = findViewById(R.id.ai_spinner);

        darkModeSwitch.setChecked(sharedPreferences.getBoolean("darkMode", true));

        ImageButton backBtn = findViewById(R.id.back);
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                sharedPreferences.edit().putBoolean("darkMode", true).apply();
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                sharedPreferences.edit().putBoolean("darkMode", false).apply();
            }
        });

        Button iconInfo = findViewById(R.id.button_ueber);
        iconInfo.setOnClickListener(v -> showDialogFragment(new UeberFragment(), "UeberFragment"));

        Button updateInfo = findViewById(R.id.button_update);
        updateInfo.setOnClickListener(v -> checkForUpdates());

        SwitchCompat logSwitch = findViewById(R.id.log_switch);
        int logSwitchStatus = sharedPreferences.getInt("logSwitch", 1);
        logSwitch.setChecked(logSwitchStatus == 1);

        logSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                sharedPreferences.edit().putInt("logSwitch", 1).apply();
            } else {
                sharedPreferences.edit().putInt("logSwitch", 0).apply();
            }
        });

        SwitchCompat offlineSwitch = findViewById(R.id.offline_switch);
        boolean offlineSwitchStatus = sharedPreferences.getBoolean("offlineSwitch", false);
        offlineSwitch.setChecked(offlineSwitchStatus);

        offlineSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("offlineSwitch", isChecked).apply();
        });

        SwitchCompat updateSwitch = findViewById(R.id.update_switch);
        boolean updateSwitchStatus = sharedPreferences.getBoolean("updateSwitch", true);
        updateSwitch.setChecked(updateSwitchStatus);

        updateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("updateSwitch", isChecked).apply();
        });

        loadAIModels();

        aiSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedModel = aiList.get(position);
                sharedPreferences.edit().putString("selectedAIModel", selectedModel).apply();
                checkModelWorking(selectedModel);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nichts zu tun
            }
        });

        TextView apiquestion = findViewById(R.id.questionbtn);
        TextView apitext = findViewById(R.id.apitxt);
        if (!sharedPreferences.getString("apikey", getResources().getString(R.string.api_key)).equals(getResources().getString(R.string.api_key))) {
            apiquestion.setText("Entfernen");
            String apihint = sharedPreferences.getString("apikey", "FEHLER1").substring(sharedPreferences.getString("apikey", "FEHLER2").length() - 6);
            apitext.setText("Du verwendest einen eigenen API-Key: ..." + apihint);
            apiquestion.setOnClickListener(v -> {
                apiquestion.setText("❓");
                apitext.setText("Klicke hier um einen eigenen API-Schlüssel zu hinterlegen");
                apiquestion.setOnClickListener(view1 -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Haainz/Kennzeichenerkennung/blob/master/API-Schl%C3%BCssel.md"));
                    startActivity(browserIntent);
                });
                apitext.setOnClickListener(view1 -> showDialogFragment(new ApikeyFragment(), "ApikeyFragment"));
                String currentApiKey = sharedPreferences.getString("apikey", getResources().getString(R.string.api_key));
                sharedPreferences.edit().putString("apikey", getResources().getString(R.string.api_key)).apply();
                Snackbar snackbar = Snackbar.make(v, "API-Schlüssel erfolgreich entfernt", Snackbar.LENGTH_LONG)
                        .setAction("Rückgängig", v1 -> {
                            sharedPreferences.edit().putString("apikey", currentApiKey).apply();
                            Toast.makeText(this, "API-Schlüssel wiederhergestellt", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(SettingsActivity.this, SettingsActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(intent);
                        });
                snackbar.show();
            });
        } else {
            apiquestion.setText("❓");
            apitext.setText("Klicke hier um einen eigenen API-Schlüssel zu hinterlegen");
            apiquestion.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Haainz/Kennzeichenerkennung/blob/master/API-Schl%C3%BCssel.md"));
                startActivity(browserIntent);
            });
            apitext.setOnClickListener(v -> showDialogFragment(new ApikeyFragment(), "ApikeyFragment"));
        }
    }

    private void loadAIModels() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://raw.githubusercontent.com/Haainz/Kennzeichenerkennung/refs/heads/master/aimodels.json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SettingsActivity", "Error loading AI models", e);
                runOnUiThread(() -> Toast.makeText(SettingsActivity.this, "Fehler beim Laden der AI-Modelle", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String jsonData = response.body().string();
                        JSONObject json = new JSONObject(jsonData);
                        JSONArray models = json.getJSONArray("ai_models");

                        runOnUiThread(() -> {
                            aiList.clear(); // Clear previous entries
                            for (int i = 0; i < models.length(); i++) {
                                try {
                                    JSONObject model = models.getJSONObject(i);
                                    String name = model.getString("name");
                                    aiList.add(name);
                                } catch (JSONException e) {
                                    Log.e("SettingsActivity", "Error parsing model JSON", e);
                                    Toast.makeText(SettingsActivity.this, "Fehler beim Verarbeiten eines Modells", Toast.LENGTH_SHORT).show();
                                }
                            }

                            aiAdapter = new ArrayAdapter<>(SettingsActivity.this, R.layout.item_dropdown, aiList);
                            aiSp.setAdapter(aiAdapter); // Set adapter to spinner

                            String savedModel = sharedPreferences.getString("selectedAIModel", aiList.get(0)); // Default to first item if not found
                            int savedPosition = aiList.indexOf(savedModel);
                            if (savedPosition >= 0) {
                                aiSp.setSelection(savedPosition); // Set the spinner to the saved position
                            }

                            aiAdapter.notifyDataSetChanged(); // Notify adapter of data change
                        });

                    } catch (JSONException e) {
                        Log.e("SettingsActivity", "Error parsing JSON", e);
                        runOnUiThread(() -> Toast.makeText(SettingsActivity.this, "Fehler beim Verarbeiten der JSON-Daten", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e("SettingsActivity", "Response not successful: " + response.code());
                    runOnUiThread(() -> Toast.makeText(SettingsActivity.this, "Fehler beim Abrufen der AI-Modelle: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void checkModelWorking(String modelName) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://raw.githubusercontent.com/Haainz/Kennzeichenerkennung/refs/heads/master/aimodels.json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SettingsActivity", "Error checking model status", e);
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
                                runOnUiThread(() ->
                                        Toast.makeText(SettingsActivity.this, "Achtung: Es können Fehler bei diesem KI-Modell auftreten!", Toast.LENGTH_LONG).show());
                            }
                        }
                    } catch (JSONException e) {
                        Log.e("SettingsActivity", "Error parsing JSON", e);
                    }
                }
            }
        });
    }

    private void showDialogFragment(DialogFragment fragment, String tag) {
        fragment.show(getSupportFragmentManager(), tag);
    }

    private int getCurrentAppVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (Exception e) {
            Log.e("SettingsActivity", "Error getting current app version", e);
            return 1;
        }
    }

    private void checkForUpdates() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.github.com/repos/Haainz/Kennzeichenerkennung/releases/latest")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(SettingsActivity.this, "Fehler bei der Update-Prüfung", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String jsonData = response.body().string();
                        JSONObject json = new JSONObject(jsonData);
                        String versionTag = json.getString("tag_name");
                        int latestVersion = Integer.parseInt(versionTag.replaceAll("[^0-9]", ""));
                        String body = json.getString("body");
                        String downloadUrl = json.getJSONArray("assets")
                                .getJSONObject(0)
                                .getString("browser_download_url");
                        String updateSize = json.getJSONArray("assets")
                                .getJSONObject(0)
                                .getString("size");

                        int currentVersion = getCurrentAppVersion();

                        runOnUiThread(() -> {
                            if (latestVersion > currentVersion) {
                                showUpdateDialog(versionTag, body, downloadUrl, updateSize);
                            } else {
                                Toast.makeText(SettingsActivity.this, "Keine Updates verfügbar", Toast.LENGTH_SHORT).show();
                            }
                        });

                    } catch (JSONException e) {
                        runOnUiThread(() ->
                                Toast.makeText(SettingsActivity.this, "Fehler beim Verarbeiten der Daten", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    private void showUpdateDialog(String version, String body, String downloadUrl, String updateSize) {
        UpdateFragment updateFragment = new UpdateFragment();
        Bundle args = new Bundle();
        args.putString("version", version);
        args.putString("body", body);
        args.putString("downloadUrl", downloadUrl);
        args.putString("updateSize", updateSize);
        updateFragment.setArguments(args);
        updateFragment.show(getSupportFragmentManager(), "UpdateFragment");
    }
}