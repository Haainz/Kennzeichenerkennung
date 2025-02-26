package com.example.kennzeichenerkennung;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.kennzeichenerkennung.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private SharedPreferences sharedPreferences;
    private static final String TAG = "MainActivity";
    private DrawerLayout drawerLayout;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable networkCheckRunnable;
    private ImageButton iconInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        setNightMode();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        drawerLayout = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawerLayout)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            Log.d(TAG, "Navigated to: " + destination.getLabel());
        });

        setupIconButtons();
        handleIntent(getIntent());
        iconInfo = findViewById(R.id.icon_offline);
        startNetworkCheck();

        // Direkter Aufruf des GitHub Update-Checks
        checkForUpdates();
    }

    // Alle Firebase-bezogenen Methoden wurden entfernt

    private void setNightMode() {
        if (sharedPreferences.getBoolean("darkMode", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void setupIconButtons() {
        Button settingsButton = findViewById(R.id.button_settings);
        settingsButton.setOnClickListener(v -> showDialogFragment(new SettingsFragment(), "SettingsFragment"));
    }

    private void showDialogFragment(DialogFragment fragment, String tag) {
        if (!isFinishing() && !isDestroyed()) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragment.show(fragmentManager, tag);
        } else {
            Log.w(TAG, "Activity is not in a valid state to show the dialog fragment.");
        }
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("openFragment")) {
            String fragmentToOpen = intent.getStringExtra("openFragment");
            Log.d(TAG, "Intent received with openFragment: " + fragmentToOpen);
            navigateToFragment(fragmentToOpen);
        } else {
            Log.d(TAG, "No specific fragment to open, defaulting to HomeFragment");
            navigateToFragment("HomeFragment");
        }
    }

    private void navigateToFragment(String fragmentToOpen) {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        int destinationId = R.id.nav_home;
        if ("GalleryFragment".equals(fragmentToOpen)) {
            destinationId = R.id.nav_gallery;
        } else if ("HomeFragment".equals(fragmentToOpen)) {
            destinationId = R.id.nav_home;
        }
        navController.navigate(destinationId, null, new NavOptions.Builder()
                .setPopUpTo(R.id.nav_home, true)
                .build());
        Log.d(TAG, "Navigating to " + fragmentToOpen);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        boolean navigatedUp = NavigationUI.navigateUp(navController, mAppBarConfiguration);
        Log.d(TAG, "onSupportNavigateUp: " + navigatedUp);
        return navigatedUp || super.onSupportNavigateUp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.appBarMain.toolbar.setVisibility(View.VISIBLE);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected() && !isOfflineMode();
    }

    private void startNetworkCheck() {
        networkCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (isNetworkAvailable() && !isOfflineMode()) {
                    iconInfo.setVisibility(View.GONE);
                } else {
                    iconInfo.setVisibility(View.VISIBLE);
                    iconInfo.setOnClickListener(v -> showDialogFragment(new OfflineFragment(), "OfflineFragment"));
                }
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(networkCheckRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(networkCheckRunnable);
    }

    @Override
    public void onBackPressed() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        if (drawerLayout.isOpen()) {
            drawerLayout.close();
        } else if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != R.id.nav_home) {
            navController.navigate(R.id.nav_home);
        } else {
            super.onBackPressed();
        }
    }

    private int getCurrentAppVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (Exception e) {
            Log.e(TAG, "Error getting current app version", e);
            return 1;
        }
    }

    void startDownload(String downloadUrl, String version) {
        String cleanVersion = version.replaceAll("[^a-zA-Z0-9.-]", "_");
        String fileName = "App-Update-" + cleanVersion + ".apk";

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        request.setTitle("App-Update Kennzeichenerkennung" + version);
        request.setDescription("Downloading update...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        request.setMimeType("application/vnd.android.package-archive");

        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = downloadManager.enqueue(request);

        BroadcastReceiver onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Uri uri = downloadManager.getUriForDownloadedFile(downloadId);
                if (uri != null) {
                    Intent installIntent = new Intent(Intent.ACTION_VIEW);
                    installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
                    installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(installIntent);
                } else {
                    Log.e(TAG, "Download URI is null");
                }
                context.unregisterReceiver(this);
            }
        };
        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED);
    }

    private void checkForUpdates() {
        Log.e("sharedPreferences", String.valueOf(sharedPreferences.getBoolean("updateSwitch", true)) + sharedPreferences.getBoolean("offlineSwitch", false));
        if(sharedPreferences.getBoolean("updateSwitch", true) && !sharedPreferences.getBoolean("offlineSwitch", false)) {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://api.github.com/repos/Haainz/Kennzeichenerkennung/releases/latest")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
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

                            if (latestVersion > getCurrentAppVersion()) {
                                runOnUiThread(() -> showUpdateDialog(versionTag, body, downloadUrl, updateSize));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    private void showUpdateDialog(String version, String body, String downloadUrl, String updateSize) {
        if (!isFinishing() && !isDestroyed()) {
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

    private boolean isOfflineMode() {
        SharedPreferences prefs = getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getBoolean("offlineSwitch", false);
    }
}