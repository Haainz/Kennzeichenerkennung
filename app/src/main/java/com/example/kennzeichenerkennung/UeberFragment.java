package com.example.kennzeichenerkennung;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UeberFragment extends DialogFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ueber, container, false);

        TextView linkText = view.findViewById(R.id.gittext);
        linkText.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Haainz/Kennzeichenerkennung/releases/"));
            startActivity(browserIntent);
        });

        ImageButton xBtn = view.findViewById(R.id.x);
        xBtn.setOnClickListener(v -> dismiss());

        TextView versionText = view.findViewById(R.id.versiontext);
        versionText.setText("\n App-Version:\nV"+getCurrentAppVersion()+" ⓘ\n");
        versionText.setOnClickListener(v -> UpdateInfo());

        Button spendeBtn = view.findViewById(R.id.button_spende);
        spendeBtn.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/donate/?hosted_button_id=XUTQZBWGBWCLN"));
            startActivity(browserIntent);
        });

        Button kontaktBtn = view.findViewById(R.id.button_kontakt);
        kontaktBtn.setOnClickListener(v -> {
            ContactFragment contactFragment = new ContactFragment();
            contactFragment.show(getParentFragmentManager(), "ContactFragment");
        });
        return view;
    }

    private String getCurrentAppVersion() {
        try {
            return requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (Exception e) {
            Log.e("UeberFragment", "Error getting current app version", e);
            return "Fehler. Bitte informiere den Entwickler.\nFehler: "+e;
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

                            String info = "Version "+versionTag+"\n\n"+body+"\n\n"+"Downloads: "+downloadCount;
                            DialogFragment dialogFragment = new PicInfoDialogFragment(info);
                            dialogFragment.show(getParentFragmentManager(), "PicInfoDialog");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
    }
}