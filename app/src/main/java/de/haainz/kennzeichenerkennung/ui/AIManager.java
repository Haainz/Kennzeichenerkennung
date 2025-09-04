package de.haainz.kennzeichenerkennung.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.haainz.kennzeichenerkennung.Kennzeichen;
import de.haainz.kennzeichenerkennung.Kennzeichen_KI;
import de.haainz.kennzeichenerkennung.R;
import de.haainz.kennzeichenerkennung.ui.day.DayFragment;
import de.haainz.kennzeichenerkennung.ui.home.HomeFragment;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.OnUserEarnedRewardListener;

public class AIManager {

    private final Context context;
    private final Kennzeichen_KI kennzeichenKI;
    private final WeakReference<DayFragment> fragmentRefDay;
    private final WeakReference<HomeFragment> fragmentRefHome;

    public AIManager(Context context, DayFragment dayFragment, HomeFragment homeFragment) {
        this.context = context;
        this.kennzeichenKI = new Kennzeichen_KI(context);
        this.fragmentRefDay = new WeakReference<>(dayFragment);
        this.fragmentRefHome = new WeakReference<>(homeFragment);
    }

    public void generateAIText(Kennzeichen kennzeichen, AICallback callback) {
        showWordCountDialog(kennzeichen, callback);
    }

    private void showWordCountDialog(Kennzeichen kennzeichen, AICallback callback) {
        Activity activity = (Activity) context;
        activity.runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final AlertDialog dialog = builder.create();

            dialog.setView(activity.getLayoutInflater().inflate(R.layout.dialog_wordcount_picker, null));
            dialog.setCancelable(false);
            dialog.show();

            SeekBar slider = dialog.findViewById(R.id.wordcount_slider);
            TextView countDisplay = dialog.findViewById(R.id.wordcount_display);
            Button generateBtn = dialog.findViewById(R.id.generate_button);
            Button cancelBtn = dialog.findViewById(R.id.cancel_button);

            if (slider == null || countDisplay == null || generateBtn == null || cancelBtn == null)
                return;

            // Diese 5 erlaubten Werte
            int[] allowedValues = {25, 50, 75, 100, 125};
            final int[] selectedIndex = {2}; // Standard: 75 Wörter

            countDisplay.setText(allowedValues[selectedIndex[0]] + " Wörter");
            slider.setProgress(selectedIndex[0]);

            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    selectedIndex[0] = progress;
                    countDisplay.setText(allowedValues[progress] + " Wörter");
                }

                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            cancelBtn.setOnClickListener(v -> dialog.dismiss());

            generateBtn.setOnClickListener(v -> {
                dialog.dismiss();
                int selectedWordCount = allowedValues[selectedIndex[0]];

                SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
                boolean skipAdDialog = prefs.getBoolean("skip_ad_confirmation", false);

                if (skipAdDialog) {
                    showRewardedAd(() -> generateAITextInternal(kennzeichen, callback, selectedWordCount));
                } else {
                    showAdConfirmationDialog(() -> generateAITextInternal(kennzeichen, callback, selectedWordCount));
                }
            });
        });
    }

    private void showAdConfirmationDialog(Runnable onAdAccepted) {
        Activity activity = (Activity) context;
        activity.runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final AlertDialog dialog = builder.create();

            dialog.setView(activity.getLayoutInflater().inflate(R.layout.dialog_ad_confirmation, null));
            dialog.setCancelable(false);
            dialog.show();

            Button watchAdBtn = dialog.findViewById(R.id.watch_ad_button);
            Button cancelBtn = dialog.findViewById(R.id.cancel_ad_button);
            CheckBox dontShowAgainCheckBox = dialog.findViewById(R.id.dont_show_again_checkbox);

            if (watchAdBtn == null || cancelBtn == null || dontShowAgainCheckBox == null)
                return;

            cancelBtn.setOnClickListener(v -> dialog.dismiss());

            watchAdBtn.setOnClickListener(v -> {
                if (dontShowAgainCheckBox.isChecked()) {
                    SharedPreferences.Editor editor = context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit();
                    editor.putBoolean("skip_ad_confirmation", true);
                    editor.apply();
                }

                dialog.dismiss();
                showRewardedAd(onAdAccepted);
            });
        });
    }

    private void showRewardedAd(Runnable onRewardEarned) {
        Activity activity = (Activity) context;

        RequestConfiguration configuration = new RequestConfiguration.Builder().build();
        MobileAds.setRequestConfiguration(configuration);


        MobileAds.initialize(context);

        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(context, context.getString(R.string.admob_rewarded_ad_unit_id), adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                rewardedAd.show(activity, rewardItem -> {
                    if (onRewardEarned != null) {
                        onRewardEarned.run();
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull com.google.android.gms.ads.LoadAdError loadAdError) {
                Toast.makeText(context, "Fehler beim Laden der Werbung", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateAITextInternal(Kennzeichen kennzeichen, AICallback callback, int wordLimit) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String aiModel = sharedPreferences.getString("selectedAIModel", "DeepSeek V3");

        checkModelWorking(aiModel, modelWorking -> {
            if (!modelWorking) {
                if (callback != null) {
                    callback.onError("Das KI-Modell funktioniert derzeit nicht.");
                } else {
                    //Toast.makeText(context, "Das KI-Modell funktioniert derzeit nicht.", Toast.LENGTH_SHORT).show();
                    Log.e("ai", "Das KI-Modell funktioniert derzeit nicht.");
                }
                return;
            }

            getaiModel(aiModel, modelId -> {
                if (modelId.equals("Fehler")) {
                    if (callback != null) {
                        callback.onError("Fehler beim Laden des AI-Modells");
                    } else {
                        DayFragment fragment = fragmentRefDay.get();
                        handleError(fragment, null);
                    }
                    return;
                }

                String prompt = "Erstelle mir einen sehr kurzen informativen Text mit maximal " + wordLimit + " Wörtern über " +
                        kennzeichen.OrtGeben() + " in " + kennzeichen.StadtKreisGeben() +
                        " im Bundesland " + kennzeichen.BundeslandGeben() +
                        ". Gib auch wichtige Fakten wie Einwohnerzahl, geografische Besonderheiten und " +
                        "historische Hintergründe an. Sei präzise, antworte auf deutsch und halte dich an nachweisbare Fakten.";

                try {
                    JSONObject jsonBody = new JSONObject();
                    JSONArray messages = new JSONArray();
                    JSONObject message = new JSONObject();
                    message.put("role", "user");
                    message.put("content", prompt);
                    messages.put(message);
                    jsonBody.put("model", modelId);
                    jsonBody.put("messages", messages);

                    RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json"));
                    Request request = new Request.Builder()
                            .url("https://openrouter.ai/api/v1/chat/completions")
                            .post(body)
                            .addHeader("Authorization", "Bearer " + sharedPreferences.getString("apikey", context.getString(R.string.api_key)))
                            .addHeader("Content-Type", "application/json")
                            .build();

                    OkHttpClient client = new OkHttpClient();

                    // Wenn DayFragment verwendet wird: Ladeanimation starten
                    DayFragment fragment = fragmentRefDay.get();
                    HomeFragment homefragment = fragmentRefHome.get();
                    if (callback == null && fragment != null && fragment.isAdded()) {
                        fragment.getActivity().runOnUiThread(fragment::startLoadingAnimation);
                    }

                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            if (callback != null) {
                                callback.onError("Netzwerkfehler: " + e.getMessage());
                            } else {
                                handleError(fragment, e);
                            }
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            String responseData = response.body().string();
                            Log.d("API_RESPONSE", responseData);

                            if (callback == null && fragment != null && fragment.isAdded()) {
                                setupLongClickListener(fragment.getActivity(), fragment.binding.thinkBtn, responseData);
                            } else if (homefragment != null && homefragment.isAdded()) {
                                View infoTitle = homefragment.requireView().findViewById(R.id.infotexttitel);
                                setupLongClickListener(homefragment.getActivity(), infoTitle, responseData);
                            }

                            if (response.isSuccessful()) {
                                try {
                                    JSONObject jsonResponse = new JSONObject(responseData);
                                    String aiText = jsonResponse.getJSONArray("choices")
                                            .getJSONObject(0)
                                            .getJSONObject("message")
                                            .getString("content");

                                    kennzeichenKI.setaiText(kennzeichen, aiText);

                                    if (callback != null) {
                                        callback.onResult(formatAIText(aiText));
                                    } else if (fragment != null && fragment.isAdded()) {
                                        fragment.getActivity().runOnUiThread(() -> {
                                            fragment.stopLoadingAnimation();
                                            fragment.binding.aitextOftheday.setText(formatAIText(aiText));
                                        });
                                    }
                                } catch (JSONException e) {
                                    if (callback != null) {
                                        callback.onError("Fehler beim Parsen der Antwort");
                                    } else {
                                        handleError(fragment, e);
                                    }
                                }
                            } else {
                                if (callback != null) {
                                    callback.onError("Serverfehler: " + response.code());
                                } else {
                                    handleErrorResponse(fragment, response);
                                }
                            }
                        }
                    });

                } catch (JSONException e) {
                    if (callback != null) {
                        callback.onError("Fehler beim Erstellen der Anfrage");
                    } else {
                        handleError(fragmentRefDay.get(), e);
                    }
                }
            });
        });
    }

    private void checkModelWorking(String modelName, ModelStatusCallback callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://raw.githubusercontent.com/Haainz/Kennzeichenerkennung/refs/heads/master/aimodels.json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("AIManager", "Error checking model status", e);
                callback.onModelStatusReceived(false); // Notify that the model is not working
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String jsonData = response.body().string();
                        JSONObject json = new JSONObject(jsonData);
                        JSONArray models = json.getJSONArray("ai_models");

                        for (int i = 0; i < models.length(); i++) {
                            JSONObject model = models.getJSONObject(i);
                            String name = model.getString("name");
                            boolean working = model.getBoolean("working");

                            if (name.equals(modelName)) {
                                callback.onModelStatusReceived(working); // Notify the model status
                                return;
                            }
                        }
                        callback.onModelStatusReceived(false); // Notify that the model is not found
                    } catch (JSONException e) {
                        Log.e("AIManager", "Error parsing JSON", e);
                        callback.onModelStatusReceived(false); // Notify that the model is not working
                    }
                } else {
                    callback.onModelStatusReceived(false); // Notify that the model is not working
                }
            }
        });
    }

    public interface ModelStatusCallback {
        void onModelStatusReceived(boolean isWorking);
    }

    private void handleError(DayFragment fragment, Exception e) {
        if (fragment != null && fragment.isAdded()) {
            fragment.getActivity().runOnUiThread(() -> {
                fragment.stopLoadingAnimation();
                fragment.showaiText(fragment.currentKennzeichen, "on");
            });
        }
    }

    private void handleErrorResponse(DayFragment fragment, Response response) {
        if (fragment != null && fragment.isAdded()) {
            fragment.stopLoadingAnimation();
            fragment.getActivity().runOnUiThread(() -> {
                fragment.binding.aitextOftheday.setText("Bei diesem KI-Modell ist ein Fehler aufgetreten. Versuche es mit einem anderen erneut.");
            });
        }
    }

    private void getaiModel(String aiModel, OnModelIdReceivedListener listener) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://raw.githubusercontent.com/Haainz/Kennzeichenerkennung/refs/heads/master/aimodels.json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("AIManager", "Error loading AI models", e);
                try {
                    listener.onModelIdReceived("Fehler");
                } catch (JSONException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String jsonData = response.body().string();
                        JSONObject json = new JSONObject(jsonData);
                        JSONArray models = json.getJSONArray("ai_models");

                        for (int i = 0; i < models.length(); i++) {
                            JSONObject model = models.getJSONObject(i);
                            String name = model.getString("name");
                            String id = model.getString("id");

                            if (name.equals(aiModel)) {
                                listener.onModelIdReceived(id);
                                return;
                            }
                        }
                        listener.onModelIdReceived("Fehler");
                    } catch (JSONException e) {
                        Log.e("AIManager", "Error parsing JSON", e);
                        try {
                            listener.onModelIdReceived("Fehler");
                        } catch (JSONException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                } else {
                    Log.e("AIManager", "Response not successful: " + response.code());
                    try {
                        listener.onModelIdReceived("Fehler");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    private String formatAIText(String aiText) {
        return aiText.trim() + "\n\n(KI-generierter Inhalt, keine Gewähr)";
    }

    private void openResponse(String response) {
        String cleanedResponse = response.trim();
                    new AlertDialog.Builder(context)
                            .setTitle("API-Antwort")
                            .setMessage(cleanedResponse)
                            .setPositiveButton("Kopieren", (dialog, which) -> {
                                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("API Response", cleanedResponse);
                                clipboard.setPrimaryClip(clip);
                                Toast.makeText(context, "Antwort kopiert", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Abbrechen", null)
                            .show();
    }

    public interface OnModelIdReceivedListener {
        void onModelIdReceived(String modelId) throws JSONException;
    }

    public interface AICallback {
        void onResult(String aiText);
        void onError(String errorMessage);
    }

    private void setupLongClickListener(Activity activity, View view, String responseData) {
        activity.runOnUiThread(() -> {
            view.setOnLongClickListener(v -> {
                openResponse(responseData);
                return true;
            });
        });
    }
}
