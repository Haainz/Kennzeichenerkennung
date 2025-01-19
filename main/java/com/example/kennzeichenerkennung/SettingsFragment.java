package com.example.kennzeichenerkennung;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.fragment.NavHostFragment;

import com.example.kennzeichenerkennung.ui.home.HomeFragment;

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

        SwitchCompat logSwitch = view.findViewById(R.id.log_switch);

        int logSwitchStatus = sharedPreferences.getInt("logSwitch", 1);
        logSwitch.setChecked(logSwitchStatus == 1);

        logSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Speichern Sie den Status des Switches
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
        return view;
    }
    private void showDialogFragment(DialogFragment fragment, String tag) {
        FragmentManager fragmentManager = getParentFragmentManager();
        fragment.show(fragmentManager, tag);
    }
}