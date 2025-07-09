package de.haainz.kennzeichenerkennung;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.HashMap;
import java.util.Map;

public class TourPopupDialog extends Dialog {

    private ViewPager viewPager;
    private LinearLayout dotsLayout;
    private Button closeButton;
    private DotIndicator[] dots;
    private TourPage[] pages;
    private TourPagerAdapter tourPagerAdapter;

    public TourPopupDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_tour_popup);

        viewPager = findViewById(R.id.viewPager);
        dotsLayout = findViewById(R.id.dotsLayout);
        closeButton = findViewById(R.id.closeButton);

        // Setup pages data
        pages = new TourPage[]{
                new TourPage(-1, "Willkommen in der Kennzeichenapp!",
                                "\nDurch die Nutzung erklärst du dich mit der Datenschutzrichtlinie und den Nutzungsbedingungen einverstanden. Diese kannst du jederzeit über 'Einstellungen'>'Über die App' einsehen." +
                                "\n\nLerne die App doch gleich etwas genauer kennen und wische zur Seite um eine kleine Tour zu starten.\n" +
                                "Du kannst sie jederzeit über das Fragezeichen oben rechts nochmals aufrufen.\n",
                        "Viel Spaß!🎉"),
                new TourPage(R.raw.vid_home, "Hauptfunktionen",
                        "Gebe das Kürzel ein oder mache ein Bild eines Kennzeichens und erhalte viele Informationen dazu.",
                        "PS: Für noch mehr Infos lass dir doch einfach einen Text von KI erstellen 😉."),
                new TourPage(R.raw.tour1, "Liste",
                        "Durchsuche die Liste oder filtere um z.B. nach Bundesländern zu suchen. Like auch Kennzeichen um sie zu speichern.",
                        "Erstelle auch eigene Kennzeichen für dich persönlich um z.B. Kennzeichen aus anderen Ländern zu speichern"),
                new TourPage(R.raw.tour1, "Kennzeichen des Tages",
                        "Sehe dir jeden Tag das heutige Kennzeichen des Tages und die Infos dazu an.",
                        "PS: Auch als Widget für den Homescreen verfügbar."),
                new TourPage(R.raw.vid_offline,"Offline-Modus",
                        "Die App funktioniert auch komplett offline (KI-Funktionen, Karten, Kontakt nicht verfügbar) und 100% kostenlos!",
                        "Unterstütze mich gerne durch eine kleine Spende!"),
                new TourPage(R.raw.vid_kontakt_spende, "Noch Fragen?",
                        "Kontaktiere mich einfach über die App.\nUnterstütze mich doch gerne durch eine gute Bewertung im Playstore oder mit einer Spende.",
                        "Danke!♥️")
        };

        tourPagerAdapter = new TourPagerAdapter();
        viewPager.setAdapter(tourPagerAdapter);
        addDotsIndicator(0);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // no-op
            }

            @Override
            public void onPageSelected(int position) {
                addDotsIndicator(position);
                // Show close button only on last page
                if (position == pages.length - 1) {
                    closeButton.setVisibility(View.VISIBLE);
                } else {
                    closeButton.setVisibility(View.GONE);
                }
                // Restart video on current page if video present
                View currentView = tourPagerAdapter.getViewAtPosition(position);
                if (currentView != null) {
                    VideoView videoView = currentView.findViewById(R.id.videoView);
                    if (videoView != null && videoView.getVisibility() == View.VISIBLE) {
                        videoView.seekTo(0);
                        videoView.start();
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // no-op
            }
        });

        closeButton.setOnClickListener(v -> dismiss());
    }

    private void addDotsIndicator(int position) {
        dotsLayout.removeAllViews();
        dots = new DotIndicator[pages.length];

        for (int i = 0; i < dots.length; i++) {
            TextView dot = new TextView(getContext());
            dot.setText("•");
            dot.setTextSize(40);
            dot.setTextColor(i == position ? Color.parseColor("#FDBB06") /*Gold*/ : Color.parseColor("#AFAFAF") /*Gray*/);
            dots[i] = new DotIndicator(dot);
            dotsLayout.addView(dot);
        }
    }

    class DotIndicator {
        TextView dotTextView;
        DotIndicator(TextView dot) {
            this.dotTextView = dot;
        }
    }

    class TourPage {
        int tourResId;
        String header;
        String description;
        String bottom;
        TourPage(int tourResId, String header, String description, String bottom) {
            this.tourResId = tourResId;
            this.header = header;
            this.description = description;
            this.bottom = bottom;
        }
    }

    class TourPagerAdapter extends PagerAdapter {

        private final Map<Integer, View> pagesViews = new HashMap<>();

        @Override
        public int getCount() {
            return pages.length;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View view = inflater.inflate(R.layout.item_tour_page, container, false);

            VideoView videoView = view.findViewById(R.id.videoView);
            TextView textView = view.findViewById(R.id.textView);
            TextView textViewHeader = view.findViewById(R.id.textViewheader);
            TextView textViewBottom = view.findViewById(R.id.textViewBottom);

            TourPage page = pages[position];
            if (page.tourResId != -1) {
                String path = "android.resource://" + getContext().getPackageName() + "/" + page.tourResId;
                Log.e(path, path);
                videoView.setVideoURI(Uri.parse(path));
                videoView.setOnPreparedListener(mp -> {
                    mp.setLooping(true);
                    mp.setVolume(0, 0);
                    PlaybackParams params = new PlaybackParams();
                    // Check if specific resource and set speed accordingly
                    if (path.equals("android.resource://de.haainz.kennzeichenerkennung/2131951617")) {
                        params.setSpeed(1.5f);
                    } else {
                        params.setSpeed(2.2f);
                    }
                    mp.setPlaybackParams(params);
                    videoView.start();
                });
                videoView.setVisibility(View.VISIBLE);
            } else {
                videoView.setVisibility(View.GONE);
            }

            textViewHeader.setText(page.header);
            textView.setText(page.description);
            textViewBottom.setText(page.bottom);

            container.addView(view);
            pagesViews.put(position, view);
            return view;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            View view = (View) object;
            VideoView videoView = view.findViewById(R.id.videoView);
            if (videoView.isPlaying()) {
                videoView.stopPlayback();
            }
            container.removeView(view);
            pagesViews.remove(position);
        }

        @Nullable
        public View getViewAtPosition(int position) {
            return pagesViews.get(position);
        }
    }

}

