package de.haainz.kennzeichenerkennung;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

import de.haainz.kennzeichenerkennung.ui.ModernFastScroller;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class KennzeichenlistAdapter extends ArrayAdapter<Object> implements ModernFastScroller.SectionIndexer {
    private static final int TYPE_KENNZEICHEN = 0;
    private static final int TYPE_AD = 1;

    private final Context context;
    private List<Kennzeichen> selectedItems = new ArrayList<>();
    private boolean onlyOneTypeSelected = false;

    public KennzeichenlistAdapter(Context context, List<Object> items) {
        super(context, 0, items);
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position) instanceof NativeAd) {
            return TYPE_AD;
        }
        return TYPE_KENNZEICHEN;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int viewType = getItemViewType(position);

        if (viewType == TYPE_AD) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_native_ad, parent, false);
            }
            populateNativeAdView((NativeAd) getItem(position), (NativeAdView) convertView);
            return convertView;
        }

        // Kennzeichen View
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item_kennzeichen, parent, false);
        }

        // Holen Sie sich die Kennzeichen-Instanz
        Kennzeichen kennzeichen = (Kennzeichen) getItem(position);

        // Setzen Sie die TextViews mit den Daten
        TextView textViewKennzeichen = convertView.findViewById(R.id.textViewKennzeichen);
        TextView textViewDetails = convertView.findViewById(R.id.textViewDetails);
        TextView textViewDetails2 = convertView.findViewById(R.id.textViewDetails2);
        ImageView imgnation = convertView.findViewById(R.id.D);
        ImageView savedView = convertView.findViewById(R.id.savedview);
        FrameLayout redDotContainer = convertView.findViewById(R.id.redDotContainer);
        TextView redDotText = convertView.findViewById(R.id.redDotText);
        LinearLayout element = convertView.findViewById(R.id.element);

        textViewKennzeichen.setText(kennzeichen.OertskuerzelGeben());
        textViewDetails.setText(kennzeichen.OrtGeben() + " - " + kennzeichen.StadtKreisGeben());
        textViewDetails2.setText(kennzeichen.BundeslandGeben() + ", " + kennzeichen.LandGeben());

        if(Objects.equals(kennzeichen.OertskuerzelGeben(), "Y")) {
            imgnation.setImageResource(R.drawable.img4_1);
            formatKuerzel(textViewKennzeichen, 24);
        } else if (Objects.equals(kennzeichen.OertskuerzelGeben(), "X")) {
            imgnation.setImageResource(R.drawable.img4_2);
            formatKuerzel(textViewKennzeichen, 11);
        } else {
            imgnation.setImageResource(R.drawable.img4);
            formatKuerzel(textViewKennzeichen, 22);
        }

        if(Objects.equals(kennzeichen.saved, "nein")) {
            savedView.setVisibility(GONE);
        } else {
            savedView.setVisibility(VISIBLE);
        }

        if (selectedItems.contains(kennzeichen)) {
            redDotContainer.setVisibility(View.VISIBLE);
            element.setBackgroundColor(Color.parseColor("#40FDBB06"));
            redDotText.setText(String.valueOf(selectedItems.indexOf(kennzeichen) + 1));
        } else {
            redDotContainer.setVisibility(View.GONE);
            element.setBackgroundColor(Color.parseColor("#00FDBB06"));
        }

        return convertView;
    }

    private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setMediaView(adView.findViewById(R.id.ad_media));

        if (adView.getHeadlineView() != null) {
            ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        }

        if (adView.getBodyView() != null) {
            if (nativeAd.getBody() == null) {
                adView.getBodyView().setVisibility(View.INVISIBLE);
            } else {
                adView.getBodyView().setVisibility(View.VISIBLE);
                ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
            }
        }

        if (adView.getCallToActionView() != null) {
            if (nativeAd.getCallToAction() == null) {
                adView.getCallToActionView().setVisibility(View.INVISIBLE);
            } else {
                adView.getCallToActionView().setVisibility(View.VISIBLE);
                ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
            }
        }

        // NativeAd am Ende setzen, nachdem alle Views registriert wurden
        adView.setNativeAd(nativeAd);
    }

    private void formatKuerzel(TextView textViewKennzeichen, int length) {
        final float scale = context.getResources().getDisplayMetrics().density;
        int marginInPx = (int) (length * scale + 0.5f);
        android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) textViewKennzeichen.getLayoutParams();
        params.setMarginStart(marginInPx);
        textViewKennzeichen.setLayoutParams(params);
    }

    public void setSelectedItems(List<Kennzeichen> selected) {
        this.selectedItems = selected;
        notifyDataSetChanged();
    }

    public void setOnlyOneTypeSelected(boolean onlyOneTypeSelected) {
        this.onlyOneTypeSelected = onlyOneTypeSelected;
    }

    @Override
    public String getSectionText(int position) {
        if (position >= 0 && position < getCount()) {
            Object item = getItem(position);
            if (item instanceof Kennzeichen) {
                Kennzeichen k = (Kennzeichen) item;
                if (onlyOneTypeSelected || k.isNormalDE()) {
                    String kuerzel = k.OertskuerzelGeben();
                    return kuerzel.isEmpty() ? "" : kuerzel.substring(0, 1).toUpperCase();
                } else if (k.isSonderDE()) {
                    return "Sonder";
                } else if (k.isAuslaufendDE()) {
                    return "Auslaufend";
                } else if (k.isEigene()) {
                    return "Eigene";
                } else {
                    String kuerzel = k.OertskuerzelGeben();
                    return kuerzel.isEmpty() ? "" : kuerzel.substring(0, 1).toUpperCase();
                }
            } else {
                return "Werbung";
            }
        }
        return "";
    }
}