package com.example.kennzeichenerkennung;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.fragment.NavHostFragment;

import com.example.kennzeichenerkennung.ui.home.HomeFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SettingsFragment extends DialogFragment {

    private SwitchCompat darkModeSwitch;
    private SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        sharedPreferences = getActivity().getSharedPreferences("settings", getActivity().MODE_PRIVATE);

        darkModeSwitch = view.findViewById(R.id.dark_mode_switch);

        darkModeSwitch.setChecked(sharedPreferences.getBoolean("darkMode", false));

        ImageButton xBtn = view.findViewById(R.id.x);
        xBtn.setOnClickListener(v -> dismiss());

        darkModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    sharedPreferences.edit().putBoolean("darkMode", true).apply();
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    sharedPreferences.edit().putBoolean("darkMode", false).apply();
                }
            }
        });

        Button iconInfo = view.findViewById(R.id.button_ueber);
        iconInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogFragment(new UeberFragment(), "UeberFragment");
            }
        });

        Button updateInfo = view.findViewById(R.id.button_update);
        updateInfo.setOnClickListener(v -> checkForUpdates());

        SwitchCompat logSwitch = view.findViewById(R.id.log_switch);

        int logSwitchStatus = sharedPreferences.getInt("logSwitch", 1);
        logSwitch.setChecked(logSwitchStatus == 1);

        logSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    sharedPreferences.edit().putInt("logSwitch", 1).apply();
                } else {
                    sharedPreferences.edit().putInt("logSwitch", 0).apply();
                }

                // Aktualisieren Sie die Sichtbarkeit der textViewAusgabe2
                NavHostFragment navHostFragment = (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
                if (navHostFragment != null) {
                    Fragment fragment = navHostFragment.getChildFragmentManager().getFragments().get(0);
                    if (fragment instanceof HomeFragment) {
                        HomeFragment homeFragment = (HomeFragment) fragment;
                        homeFragment.updateTextViewAusgabe2();
                    }
                }
            }
        });

        SwitchCompat offlineSwitch = view.findViewById(R.id.offline_switch);

        boolean offlineSwitchStatus = sharedPreferences.getBoolean("offlineSwitch", false);
        offlineSwitch.setChecked(offlineSwitchStatus);

        offlineSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    sharedPreferences.edit().putBoolean("offlineSwitch", true).apply();
                } else {
                    sharedPreferences.edit().putBoolean("offlineSwitch", false).apply();
                }
            }
        });

        SwitchCompat updateSwitch = view.findViewById(R.id.update_switch);

        boolean updateSwitchStatus = sharedPreferences.getBoolean("updateSwitch", true);
        updateSwitch.setChecked(updateSwitchStatus);

        updateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    sharedPreferences.edit().putBoolean("updateSwitch", true).apply();
                } else {
                    sharedPreferences.edit().putBoolean("updateSwitch", false).apply();
                }
            }
        });

        return view;
    }

    private void showDialogFragment(DialogFragment fragment, String tag) {
        FragmentManager fragmentManager = getParentFragmentManager();
        fragment.show(fragmentManager, tag);
    }

    private int getCurrentAppVersion() {
        try {
            return requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0).versionCode;
        } catch (Exception e) {
            Log.e("SettingsFragment", "Error getting current app version", e);
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
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Fehler bei der Update-Prüfung", Toast.LENGTH_SHORT).show());
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

                        int currentVersion = getCurrentAppVersion();

                        requireActivity().runOnUiThread(() -> {
                            if (latestVersion > currentVersion) {
                                showUpdateDialog(versionTag, body, downloadUrl);
                            } else {
                                Toast.makeText(requireContext(), "Keine Updates verfügbar", Toast.LENGTH_SHORT).show();
                            }
                        });

                    } catch (JSONException e) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Fehler beim Verarbeiten der Daten", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    private void showUpdateDialog(String version, String body, String downloadUrl) {
        if (isAdded()) {
            UpdateFragment updateFragment = new UpdateFragment();
            Bundle args = new Bundle();
            args.putString("version", version);
            args.putString("body", body);
            args.putString("downloadUrl", downloadUrl);
            updateFragment.setArguments(args);
            updateFragment.show(getParentFragmentManager(), "UpdateFragment");
        }
    }
}