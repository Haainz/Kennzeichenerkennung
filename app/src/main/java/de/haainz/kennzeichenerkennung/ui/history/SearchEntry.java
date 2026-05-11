package de.haainz.kennzeichenerkennung.ui.history;

public class SearchEntry {
    private long timestamp;
    private String kuerzel;
    private String herleitung;
    private String imageUri;
    private String id;

    public SearchEntry(String kuerzel, String herleitung, String imageUri) {
        this.timestamp = System.currentTimeMillis();
        this.kuerzel = kuerzel;
        this.herleitung = herleitung;
        this.imageUri = imageUri;
        this.id = String.valueOf(this.timestamp);
    }

    // Default constructor for JSON parsing
    public SearchEntry() {}

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getKuerzel() { return kuerzel; }
    public void setKuerzel(String kuerzel) { this.kuerzel = kuerzel; }

    public String getHerleitung() { return herleitung; }
    public void setHerleitung(String herleitung) { this.herleitung = herleitung; }

    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
