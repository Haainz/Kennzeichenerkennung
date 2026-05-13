package de.haainz.kennzeichenerkennung;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

public class SupportDialogFragment extends DialogFragment {

    private SharedPreferences sharedPreferences;
    private TextView adsCountText;
    private static final String PREF_REWARDED_ADS_COUNT = "rewarded_ads_count";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.CustomDialogfullTheme);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_support, container, false);
        sharedPreferences = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);

        adsCountText = view.findViewById(R.id.tv_ads_supported);
        updateAdsCountText();

        ImageButton closeBtn = view.findViewById(R.id.btn_close);
        closeBtn.setOnClickListener(v -> dismiss());

        Button donatePaypalBtn = view.findViewById(R.id.btn_donate_paypal);
        donatePaypalBtn.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/donate/?hosted_button_id=XUTQZBWGBWCLN"));
            startActivity(browserIntent);
        });

        Button watchAdBtn = view.findViewById(R.id.btn_watch_ad);
        watchAdBtn.setOnClickListener(v -> showRewardedAd());

        return view;
    }

    private void updateAdsCountText() {
        int count = sharedPreferences.getInt(PREF_REWARDED_ADS_COUNT, 0);
        if (count > 0) {
            String text = String.format(java.util.Locale.GERMAN, "Du hast mich bereits durch %d freiwillige Werbeanzeigen unterstützt. Danke! ♥️", count);
            adsCountText.setText(text);
            adsCountText.setVisibility(View.VISIBLE);
        } else {
            adsCountText.setVisibility(View.GONE);
        }
    }

    private void incrementAdsCount() {
        int count = sharedPreferences.getInt(PREF_REWARDED_ADS_COUNT, 0);
        sharedPreferences.edit().putInt(PREF_REWARDED_ADS_COUNT, count + 1).apply();
        updateAdsCountText();
    }

    private void showRewardedAd() {
        Activity activity = getActivity();
        if (activity == null) return;

        ConsentRequestParameters params = new ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build();

        ConsentInformation consentInformation = UserMessagingPlatform.getConsentInformation(requireContext());
        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                () -> {
                    if (consentInformation.isConsentFormAvailable() &&
                            consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {
                        UserMessagingPlatform.loadConsentForm(
                                requireContext(),
                                consentForm -> consentForm.show(activity, formError -> loadAndShowRewardedAd(activity)),
                                formError -> {
                                    Log.e("Consent", "Form load error: " + formError.getMessage());
                                    loadAndShowRewardedAd(activity);
                                }
                        );
                    } else {
                        loadAndShowRewardedAd(activity);
                    }
                },
                requestError -> {
                    Log.e("Consent", "Consent update error: " + requestError.getMessage());
                    loadAndShowRewardedAd(activity);
                }
        );
    }

    private void loadAndShowRewardedAd(Activity activity) {
        MobileAds.initialize(requireContext());
        AdRequest adRequest = new AdRequest.Builder().build();
        
        Toast.makeText(requireContext(), "Lade Werbung...", Toast.LENGTH_SHORT).show();

        RewardedAd.load(requireContext(), getString(R.string.admob_rewarded_ad_unit_id_donate), adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                rewardedAd.show(activity, rewardItem -> {
                    incrementAdsCount();
                    Toast.makeText(requireContext(), "Vielen Dank für deine Unterstützung!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull com.google.android.gms.ads.LoadAdError loadAdError) {
                Toast.makeText(requireContext(), "Fehler beim Laden der Werbung. Bitte versuche es später erneut.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}