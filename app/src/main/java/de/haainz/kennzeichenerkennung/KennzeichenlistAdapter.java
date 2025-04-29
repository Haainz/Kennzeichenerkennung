package de.haainz.kennzeichenerkennung;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class KennzeichenlistAdapter extends ArrayAdapter<Kennzeichen> {
    private final Context context;
    private final List<Kennzeichen> kennzeichenList;

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

        textViewKennzeichen.setText(kennzeichen.OertskuerzelGeben());
        textViewDetails.setText(kennzeichen.OrtGeben() + " - " + kennzeichen.StadtKreisGeben());
        textViewDetails2.setText(kennzeichen.BundeslandGeben() + ", " + kennzeichen.LandGeben());

        return convertView;
    }
}