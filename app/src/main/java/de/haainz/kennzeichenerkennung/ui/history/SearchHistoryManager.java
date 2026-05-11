package de.haainz.kennzeichenerkennung.ui.history;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class SearchHistoryManager {
    private static final String FILE_NAME = "search_history.json";
    private final Context context;

    public SearchHistoryManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void addEntry(SearchEntry entry) {
        if (entry.getImageUri() != null && !entry.getImageUri().isEmpty()) {
            String permanentPath = saveImagePermanently(entry.getImageUri(), entry.getId());
            entry.setImageUri(permanentPath);
        }
        List<SearchEntry> history = getHistory();
        history.add(0, entry); // Add to beginning
        saveHistory(history);
    }

    public List<SearchEntry> getHistory() {
        List<SearchEntry> history = new ArrayList<>();
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) return history;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONArray jsonArray = new JSONArray(sb.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                SearchEntry entry = new SearchEntry();
                entry.setTimestamp(obj.getLong("timestamp"));
                entry.setKuerzel(obj.getString("kuerzel"));
                entry.setHerleitung(obj.getString("herleitung"));
                entry.setImageUri(obj.optString("imageUri", null));
                entry.setId(obj.getString("id"));
                history.add(entry);
            }
        } catch (IOException | JSONException e) {
            Log.e("SearchHistoryManager", "Error reading history", e);
        }
        return history;
    }

    public void deleteEntry(String id) {
        List<SearchEntry> history = getHistory();
        for (SearchEntry entry : history) {
            if (entry.getId().equals(id)) {
                if (entry.getImageUri() != null && entry.getImageUri().startsWith("/")) {
                    File file = new File(entry.getImageUri());
                    if (file.exists()) {
                        boolean deleted = file.delete();
                        if (!deleted) Log.w("SearchHistoryManager", "Could not delete image: " + file.getPath());
                    }
                }
                break;
            }
        }
        history.removeIf(entry -> entry.getId().equals(id));
        saveHistory(history);
    }

    private String saveImagePermanently(String uriString, String id) {
        try {
            Uri uri = Uri.parse(uriString);
            File dir = new File(context.getFilesDir(), "history_images");
            if (!dir.exists()) {
                if (!dir.mkdirs()) Log.e("SearchHistoryManager", "Could not create history images directory");
            }

            File outFile = new File(dir, "img_" + id + ".jpg");
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is == null) return uriString;
                try (OutputStream os = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = is.read(buffer)) > 0) {
                        os.write(buffer, 0, length);
                    }
                }
            }
            return outFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e("SearchHistoryManager", "Error saving image permanently", e);
            return uriString; // Fallback to original
        }
    }

    private void saveHistory(List<SearchEntry> history) {
        JSONArray jsonArray = new JSONArray();
        for (SearchEntry entry : history) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("timestamp", entry.getTimestamp());
                obj.put("kuerzel", entry.getKuerzel());
                obj.put("herleitung", entry.getHerleitung());
                obj.put("imageUri", entry.getImageUri());
                obj.put("id", entry.getId());
                jsonArray.put(obj);
            } catch (JSONException e) {
                Log.e("SearchHistoryManager", "Error saving entry", e);
            }
        }

        File file = new File(context.getFilesDir(), FILE_NAME);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(jsonArray.toString());
        } catch (IOException e) {
            Log.e("SearchHistoryManager", "Error writing history file", e);
        }
    }
}
