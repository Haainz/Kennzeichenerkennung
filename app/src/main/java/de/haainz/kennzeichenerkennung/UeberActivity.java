package de.haainz.kennzeichenerkennung;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UeberActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ueber); // Setze das Layout

        TextView linkText = findViewById(R.id.gittext);
        linkText.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Haainz/Kennzeichenerkennung/releases/"));
            startActivity(browserIntent);
        });

        ImageButton xBtn = findViewById(R.id.backbtn);
        xBtn.setOnClickListener(v -> onBackPressed());

        TextView versionText = findViewById(R.id.versiontext);
        versionText.setText("\n App-Version:\nV" + getCurrentAppVersion() + " ⓘ\n");
        versionText.setOnClickListener(v -> UpdateInfo());

        Button spendeBtn = findViewById(R.id.button_spende);
        spendeBtn.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/donate/?hosted_button_id=XUTQZBWGBWCLN"));
            startActivity(browserIntent);
        });

        Button kontaktBtn = findViewById(R.id.button_kontakt);
        kontaktBtn.setOnClickListener(v -> showDialogFragment(new ContactFragment(), "ContactFragment"));
    }

    private String getCurrentAppVersion() {
        try {
            return this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
        } catch (Exception e) {
            Log.e("UeberActivity", "Error getting current app version", e);
            return "Fehler. Bitte informiere den Entwickler.\nFehler: " + e;
        }
    }

    private void UpdateInfo() {
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
                        String body = json.getString("body");
                        String downloadCount = json.getJSONArray("assets")
                                .getJSONObject(0)
                                .getString("download_count");

                        String info = "Version " + versionTag + "\n\n" + body + "\n\n" + "Github-Downloads: " + downloadCount;
                        showDialogFragment(new PicInfoDialogFragment(info), "PicInfoDialog");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void showDialogFragment(DialogFragment fragment, String tag) {
        fragment.show(getSupportFragmentManager(), tag);
    }
}