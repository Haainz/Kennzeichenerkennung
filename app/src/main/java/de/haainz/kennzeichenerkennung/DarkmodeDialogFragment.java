package de.haainz.kennzeichenerkennung;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.DialogFragment;

public class DarkmodeDialogFragment extends DialogFragment {

    private static final String PREFS_NAME = "settings";
    private static final String THEME_KEY = "theme_mode";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.CustomDialogfullTheme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_darkmode_dialog, container, false);

        RadioGroup radioGroup = view.findViewById(R.id.darkmode_radio_group);
        SharedPreferences prefs = requireContext().getSharedPreferences("settings", 0);
        int saved = prefs.getInt("theme_mode", R.id.radio_system);
        radioGroup.check(saved);

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            prefs.edit().putInt("theme_mode", checkedId).apply();
            applyTheme(checkedId);
            requireActivity().recreate(); // <- wichtig: Activity neu laden
            dismiss();
        });

        ImageButton xBtn = view.findViewById(R.id.x);
        xBtn.setOnClickListener(v -> dismiss());

        return view;
    }

    private void applyTheme(int id) {
        if (id == R.id.radio_light) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (id == R.id.radio_dark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
