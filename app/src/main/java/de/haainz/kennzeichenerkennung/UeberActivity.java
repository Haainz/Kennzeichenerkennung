package de.haainz.kennzeichenerkennung;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
        setContentView(R.layout.activity_ueber);

        View spacer = findViewById(R.id.navigation_bar_spacer);
        if (spacer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(spacer, (v, insets) -> {
                Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                ViewGroup.LayoutParams params = v.getLayoutParams();
                params.height = navInsets.bottom;
                v.setLayoutParams(params);
                return insets;
            });
        }

        View statusbarView = findViewById(R.id.statusbar);
        if (statusbarView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(statusbarView, (v, insets) -> {
                Insets sysInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());

                // Höhe manuell setzen
                ViewGroup.LayoutParams params = v.getLayoutParams();
                params.height = sysInsets.top;
                v.setLayoutParams(params);

                // Hintergrundfarbe setzen
                v.setBackgroundColor(ContextCompat.getColor(this, R.color.blue_700));

                return insets;
            });
        }

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

        Button rechtlichesBtn = findViewById(R.id.button_rechtliches);
        rechtlichesBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, RechtActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right,R.anim.slide_not);
        });
        overridePendingTransition(R.anim.slide_in_right,R.anim.slide_not);

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