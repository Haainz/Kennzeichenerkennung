package de.haainz.kennzeichenerkennung;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

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

import de.haainz.kennzeichenerkennung.databinding.ActivityMainBinding;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private SharedPreferences sharedPreferences;
    private static final String TAG = "MainActivity";
    private DrawerLayout drawerLayout;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable networkCheckRunnable;
    private ImageButton iconInfo;
    private ConsentInformation consentInformation;
    private ConsentForm consentForm;
    private static final String PREF_FIRST_TOUR_SHOWN = "first_nav_tour_shown";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        setNightMode();
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        drawerLayout = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        ConsentRequestParameters params = new ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build();

        consentInformation = UserMessagingPlatform.getConsentInformation(this);

        // ⬇️ Consent-Status anfragen
        consentInformation.requestConsentInfoUpdate(
                this,
                params,
                () -> {
                    if (consentInformation.isConsentFormAvailable()) {
                        loadAndShowConsentForm();
                    }
                },
                formError -> Log.e("Consent", "Consent error: " + formError.getMessage())
        );

        MobileAds.initialize(this, initializationStatus -> {
        });

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

        setupSettingsButtons();

        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                SharedPreferences sP = getSharedPreferences("settings", MODE_PRIVATE);
                boolean tourShown = sP.getBoolean(PREF_FIRST_TOUR_SHOWN, false);
                Log.e("tourShown", String.valueOf(tourShown));
                if (!tourShown) {
                    sP.edit().putBoolean(PREF_FIRST_TOUR_SHOWN, true).apply();
                    startNavigationTour();
                }
            }
        });

        handleIntent(getIntent());
        iconInfo = findViewById(R.id.icon_offline);
        startNetworkCheck();
        maybeShowNativeAd();

        if (de.haainz.kennzeichenerkennung.ui.FirstStartDialogFragment.shouldShow(this)) {
            de.haainz.kennzeichenerkennung.ui.FirstStartDialogFragment.markShown(this);
            new de.haainz.kennzeichenerkennung.ui.FirstStartDialogFragment().show(getSupportFragmentManager(), "FirstStartDialog");
        }
    }

    private void setNightMode() {
        int themeId = sharedPreferences.getInt("theme_mode", R.id.radio_system);
        if (themeId == R.id.radio_light) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (themeId == R.id.radio_dark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    private void setupSettingsButtons() {
        ImageButton donateButton = findViewById(R.id.button_donate);
        donateButton.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/donate/?hosted_button_id=XUTQZBWGBWCLN"));
            startActivity(browserIntent);
        });
        ImageButton uploadButton = findViewById(R.id.button_download);
        uploadButton.setOnClickListener(v -> {
            ImportFragment importFragment = new ImportFragment();
            importFragment.show(getSupportFragmentManager(), "ImportFragment");
        });
        ImageButton downloadButton = findViewById(R.id.button_upload);
        downloadButton.setOnClickListener(v -> {
            ExportFragment exportFragment = new ExportFragment();
            exportFragment.show(getSupportFragmentManager(), "ExportFragment");
        });
        ImageButton settingsButton = findViewById(R.id.button_settings);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_not);
        });
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
        if ("DayFragment".equals(fragmentToOpen)) {
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

    private boolean isOfflineMode() {
        SharedPreferences prefs = getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getBoolean("offlineSwitch", false);
    }

    private void loadAndShowConsentForm() {
        UserMessagingPlatform.loadConsentForm(
                this,
                form -> {
                    consentForm = form;

                    if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {
                        consentForm.show(
                                this,
                                dismissError -> {
                                    // Benutzer hat Formular geschlossen – Consent ggf. erneut prüfen
                                    Log.d("Consent", "Form closed. Status: " + consentInformation.getConsentStatus());
                                });
                    }
                },
                formError -> Log.e("Consent", "Form load error: " + formError.getMessage())
        );
    }

    private void maybeShowNativeAd() {
        boolean showAds = sharedPreferences.getBoolean("adSwitch", false); // ad_switch Status aus Settings
        if (!showAds) return;

        NativeAdView adView = findViewById(R.id.native_ad_view);
        AdLoader adLoader = new AdLoader.Builder(this, this.getString(R.string.admob_native_ad_unit_id)) // Test-ID
                .forNativeAd(nativeAd -> {
                    // Ad erfolgreich geladen → Layout befüllen
                    populateNativeAdView(nativeAd, adView);
                    adView.setVisibility(View.VISIBLE);
                })
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        TextView headlineView = adView.findViewById(R.id.ad_headline);
        MediaView mediaView = adView.findViewById(R.id.ad_image); // Das ist jetzt ein MediaView!

        headlineView.setText(nativeAd.getHeadline());
        adView.setHeadlineView(headlineView);

        // ✅ Das ist korrekt für MediaView:
        adView.setMediaView(mediaView);

        adView.setNativeAd(nativeAd);
    }

    private void startNavigationTour() {
        ImageButton donateBtn = findViewById(R.id.button_donate);
        LinearLayout updownloadBtn = findViewById(R.id.button_layout);
        ImageButton settingsBtn = findViewById(R.id.button_settings);

        new TapTargetSequence(this)
                .targets(
                        TapTarget.forView(donateBtn, "Spenden", "Unterstütze mich gerne mit einer kleinen Spende via PayPal")
                                .outerCircleColor(R.color.yellow)
                                .transparentTarget(true)
                                .targetCircleColor(android.R.color.white)
                                .titleTextColor(android.R.color.black)
                                .descriptionTextColor(android.R.color.black)
                                .targetRadius(35)
                                .cancelable(false),

                        TapTarget.forView(updownloadBtn, "Export & Import", "Exportiere und importiere Kennzeichen und ihre Infos, um sie z.B. auf ein anderes Gerät zu übertragen.")
                                .outerCircleColor(R.color.red)
                                .transparentTarget(true)
                                .targetCircleColor(android.R.color.white)
                                .titleTextColor(android.R.color.black)
                                .descriptionTextColor(android.R.color.black)
                                .targetRadius(56)
                                .cancelable(false),

                        TapTarget.forView(settingsBtn, "Einstellungen", "Passe die App nach deinen Wünschen an.")
                                .outerCircleColor(R.color.yellow)
                                .transparentTarget(true)
                                .targetCircleColor(android.R.color.white)
                                .titleTextColor(android.R.color.black)
                                .descriptionTextColor(android.R.color.black)
                                .targetRadius(35)
                                .cancelable(false)
                )
                .listener(new TapTargetSequence.Listener() {
                    @Override
                    public void onSequenceFinish() {
                    }

                    @Override
                    public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                    }

                    @Override
                    public void onSequenceCanceled(TapTarget lastTarget) {
                    }
                })
                .start();
    }
}