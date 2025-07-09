package de.haainz.kennzeichenerkennung;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class RechtActivity extends AppCompatActivity {

    private WebView webView;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recht);

        webView = findViewById(R.id.webview_recht);
        backButton = findViewById(R.id.backbtn);

        // WebView-Einstellungen
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(false); // Kein JavaScript notwendig
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // Lade lokale HTML-Datei aus dem assets-Ordner
        webView.loadUrl("file:///android_asset/rechtliches.html");

        // Zurück-Button
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed(); // Zurück zur vorherigen Activity
            }
        });
    }
}
