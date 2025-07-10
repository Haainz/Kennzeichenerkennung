package de.haainz.kennzeichenerkennung;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

public class ApikeyFragment extends DialogFragment {

    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.CustomDialogfullTheme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_apikey, container, false);

        sharedPreferences = getActivity().getSharedPreferences("settings", getActivity().MODE_PRIVATE);

        ImageButton xBtn = view.findViewById(R.id.x);
        xBtn.setOnClickListener(v -> dismiss());

        EditText apikey = view.findViewById(R.id.apieingabe);

        TextView moretxt = view.findViewById(R.id.moretext);
        moretxt.setOnClickListener(view1 -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Haainz/Kennzeichenerkennung/blob/master/API-Schl%C3%BCssel.md"));
            startActivity(browserIntent);
        });

        Button saveBtn = view.findViewById(R.id.save_btn);
        saveBtn.setOnClickListener(v -> {
            sharedPreferences.getString("apikey", getResources().getString(R.string.api_key));
            sharedPreferences.edit().putString("apikey", String.valueOf(apikey.getText())).apply();
            Toast.makeText(getContext(), "API-Schlüssel erfolgreich hinterlegt", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getContext(), SettingsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            dismiss();
        });

        return view;
    }
}