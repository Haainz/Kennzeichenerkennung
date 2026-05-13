package de.haainz.kennzeichenerkennung.ui;

import static android.content.Context.MODE_PRIVATE;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import de.haainz.kennzeichenerkennung.MainActivity;
import de.haainz.kennzeichenerkennung.R;
import de.haainz.kennzeichenerkennung.RechtActivity;
import de.haainz.kennzeichenerkennung.ui.home.HomeFragment;

public class FirstStartDialogFragment extends DialogFragment {

    private SharedPreferences sharedPreferences;
    private SwitchCompat adSwitch;
    private boolean isHandled = false;
    private static final String PREF_FIRST_START_SHOWN = "first_start_dialog_shown";
    private static final String PREF_FIRST_TOUR_SHOWN = "first_nav_tour_shown";
    private static final String PREF_TOUR_LIST_SHOWN = "tour_list_shown";
    private static final String PREF_TOUR_DAY_SHOWN = "tour_day_shown";

    public static boolean shouldShow(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", MODE_PRIVATE);
        return !prefs.getBoolean(PREF_FIRST_START_SHOWN, false);
    }

    public static void markShown(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_FIRST_START_SHOWN, true).apply();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getActivity().getSharedPreferences("settings", MODE_PRIVATE);
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.CustomDialogfullTheme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_first_start_dialog, container, false);

        Button closeBtn = view.findViewById(R.id.button_close);
        Button tipsBtn = view.findViewById(R.id.button_show_tips);
        ImageButton xBtn = view.findViewById(R.id.x);
        TextView legalLink = view.findViewById(R.id.text_view_link);
        adSwitch = view.findViewById(R.id.ad_switch);

        boolean currentAdStatus = sharedPreferences.getBoolean("adSwitch", false);
        adSwitch.setChecked(currentAdStatus);

        adSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("adSwitch", isChecked).apply();
        });

        closeBtn.setOnClickListener(v -> handleDismiss(false));
        xBtn.setOnClickListener(v -> handleDismiss(false));
        tipsBtn.setOnClickListener(v -> handleDismiss(true));

        legalLink.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), RechtActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void handleDismiss(boolean showTips) {
        if (isHandled) return;
        handleDismissLogic(showTips);
        dismiss();
    }

    private void handleDismissLogic(boolean showTips) {
        if (isHandled) return;
        isHandled = true;

        if (showTips) {
            sharedPreferences.edit().putBoolean(PREF_FIRST_TOUR_SHOWN, false).apply();
            sharedPreferences.edit().putBoolean(PREF_TOUR_LIST_SHOWN, false).apply();
            sharedPreferences.edit().putBoolean(PREF_TOUR_DAY_SHOWN, false).apply();

            FragmentActivity activity = getActivity();
            if (activity != null) {
                try {
                    HomeFragment homeFragment = (HomeFragment)
                            activity.getSupportFragmentManager()
                                    .findFragmentById(R.id.nav_host_fragment_content_main)
                                    .getChildFragmentManager()
                                    .getPrimaryNavigationFragment();

                    if (homeFragment != null && homeFragment.isAdded()) {
                        homeFragment.performTourClick();
                    }
                } catch (Exception e) {
                    Log.e("FirstStartDialog", "Error starting tour", e);
                }
            }
        } else {
            sharedPreferences.edit().putBoolean(PREF_FIRST_TOUR_SHOWN, true).apply();
            sharedPreferences.edit().putBoolean(PREF_TOUR_LIST_SHOWN, true).apply();
            sharedPreferences.edit().putBoolean(PREF_TOUR_DAY_SHOWN, true).apply();
        }

        markShown(requireContext());
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        if (!isHandled) {
            handleDismissLogic(false);
        }
        super.onDismiss(dialog);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).maybeShowNativeAd();
        }
    }
}
