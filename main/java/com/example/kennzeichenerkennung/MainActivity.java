package com.example.kennzeichenerkennung;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.github.dhaval2404.imagepicker.ImagePicker;

import com.example.kennzeichenerkennung.databinding.ActivityMainBinding;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import android.util.Log; // Importiere das Log-Paket
import android.widget.Toast;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private SharedPreferences sharedPreferences;
    private static final String TAG = "MainActivity";
    private DrawerLayout drawerLayout;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable networkCheckRunnable;
    private ImageButton iconInfo;
    private DatabaseReference updateRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        setNightMode();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseApp.initializeApp(this);
        updateRef = FirebaseDatabase.getInstance().getReference("app_updates");

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

        // Listener für Update-Daten
        updateRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Hole Update-Informationen aus der Firebase-Datenbank
                if (dataSnapshot.exists()) {
                    int latestVersion = dataSnapshot.child("version").getValue(Integer.class);
                    String downloadUrl = dataSnapshot.child("downloadUrl").getValue(String.class);

                    // Vergleiche die Versionsnummern und zeige das UpdateFragment an
                    if (latestVersion > getCurrentAppVersion()) {
                        showUpdateDialog(downloadUrl);
                    } else {
                        Log.d(TAG, "Keine Updates verfügbar.");
                    }
                } else {
                    Log.d(TAG, "Keine Update-Daten in Firebase gefunden.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error fetching update data", databaseError.toException());
            }
        });
    }

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
        if (!isFinishing() && !isDestroyed()) { // Überprüfen, ob die Aktivität noch aktiv ist
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragment.show(fragmentManager, tag);
        } else {
            Log.w(TAG, "Activity is not in a valid state to show the dialog fragment.");
        }
    }

    private void showUpdateDialog(String downloadUrl) {
        if (!isFinishing() && !isDestroyed()) { // Überprüfen, ob die Aktivität noch aktiv ist
            UpdateFragment updateFragment = new UpdateFragment();
            updateFragment.setDownloadUrl(downloadUrl);
            updateFragment.show(getSupportFragmentManager(), "UpdateFragment");
        } else {
            Log.w(TAG, "Activity is not in a valid state to show the dialog.");
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
        int destinationId = R.id.nav_home; // Default destination
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
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void startNetworkCheck() {
        networkCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (isNetworkAvailable()) {
                    iconInfo.setVisibility(View.GONE);
                } else {
                    iconInfo.setVisibility(View.VISIBLE);
                    iconInfo.setOnClickListener(v -> showDialogFragment(new OfflineFragment(), "OfflineFragment"));
                }
                handler.postDelayed(this, 5000); // Check alle 5 Sekunden
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

    void startDownload(String downloadUrl) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        request.setTitle("App-Update");
        request.setDescription("Downloading update...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "app_update.apk");
        request.setMimeType("application/vnd.android.package-archive");

        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = downloadManager.enqueue(request);

        // Listener für den Download-Fortschritt
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
}