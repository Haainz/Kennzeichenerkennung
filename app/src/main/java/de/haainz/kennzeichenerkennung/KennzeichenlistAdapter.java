package de.haainz.kennzeichenerkennung;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class KennzeichenlistAdapter extends ArrayAdapter<Kennzeichen> {
    private final Context context;
    private final List<Kennzeichen> kennzeichenList;
    private List<Kennzeichen> selectedItems = new ArrayList<>();

    public KennzeichenlistAdapter(Context context, List<Kennzeichen> kennzeichenList) {
        super(context, R.layout.list_item_kennzeichen, kennzeichenList);
        this.context = context;
        this.kennzeichenList = kennzeichenList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Verwenden Sie das vorhandene View oder erstellen Sie ein neues
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item_kennzeichen, parent, false);
        }

        // Holen Sie sich die Kennzeichen-Instanz
        Kennzeichen kennzeichen = kennzeichenList.get(position);

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
}