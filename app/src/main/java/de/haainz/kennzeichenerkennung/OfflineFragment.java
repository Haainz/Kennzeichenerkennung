package de.haainz.kennzeichenerkennung;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

public class OfflineFragment extends DialogFragment {

    private SharedPreferences sharedPreferences;

    public static OfflineFragment newInstance(String param1, String param2) {
        OfflineFragment fragment = new OfflineFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.CustomDialogfullTheme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_offline, container, false);

        sharedPreferences = getActivity().getSharedPreferences("settings", MODE_PRIVATE);

        ImageButton xBtn = view.findViewById(R.id.x);
        xBtn.setOnClickListener(v -> dismiss());

        Button wlanBtn = view.findViewById(R.id.wlan_btn);
        wlanBtn.setText("Internet-Settings");
        wlanBtn.setOnClickListener(v -> {
            WifiManager wifiManager = (WifiManager) requireContext().getSystemService(Context.WIFI_SERVICE);
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
                // Leite den Benutzer zu den WLAN-Einstellungen weiter
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), "WLAN ist bereits aktiviert", Toast.LENGTH_SHORT).show();
            }
        });

        TextView offlinetext = view.findViewById(R.id.offlinetext);
        if (sharedPreferences.getBoolean("offlineSwitch", false)) {
            wlanBtn.setText("App-Einstellungen");
            wlanBtn.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), SettingsActivity.class);
                startActivity(intent);
            });
            offlinetext.setText("Oh, es sieht so aus\nals hast du den Offlinemodus aktiviert. Deaktiviere ihn um Karten angezeigt zu bekommen, Updates zu empfangen und KI-generierte Inhalte angezeigt zu bekommen.");
        } else {
            offlinetext.setText("Oh, es sieht so aus\nals wärst du offline. Bitte aktiviere WLAN um Karten angezeigt zu bekommen, Updates zu empfangen und KI-generierte Inhalte angezeigt zu bekommen.");
        }

        return view;
    }
}