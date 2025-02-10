package com.example.kennzeichenerkennung;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

public class OfflineFragment extends DialogFragment {

    public static OfflineFragment newInstance(String param1, String param2) {
        OfflineFragment fragment = new OfflineFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_offline, container, false);

        ImageButton xBtn = view.findViewById(R.id.x);
        xBtn.setOnClickListener(v -> dismiss());

        Button wlanBtn = view.findViewById(R.id.wlan_btn);
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

        return view;
    }
}