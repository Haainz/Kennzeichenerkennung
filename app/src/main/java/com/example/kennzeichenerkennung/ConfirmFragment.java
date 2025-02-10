package com.example.kennzeichenerkennung;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import com.example.kennzeichenerkennung.ui.slideshow.ListFragment;

public class ConfirmFragment extends DialogFragment {

    private Kennzeichen_KI kennzeichenKI;
    private Kennzeichen kennzeichen;
    private ListFragment.OnConfirmListener onConfirmListener;

    public void setOnConfirmListener(ListFragment.OnConfirmListener onConfirmListener) {
        this.onConfirmListener = onConfirmListener;
    }

    public void setKennzeichenKI(Kennzeichen_KI kennzeichenKI) {
        this.kennzeichenKI = kennzeichenKI;
    }

    public void setKennzeichen(Kennzeichen kennzeichen) {
        this.kennzeichen = kennzeichen;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_confirm, container, false);

        Button deleteBtn = view.findViewById(R.id.delete_btn);
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Hier löschen wir das Kennzeichen
                if (kennzeichenKI != null) {
                    kennzeichenKI.deleteKennzeichen(kennzeichen);
                    Toast.makeText(getActivity(), "Kennzeichen gelöscht\nBitte aktualisiere die Liste", Toast.LENGTH_SHORT).show();
                }
                if (onConfirmListener != null) {
                    onConfirmListener.updateList();
                }
                dismiss();
            }
        });

        Button stopBtn = view.findViewById(R.id.stop_btn);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Hier schließen wir das ConfirmFragment
                dismiss();
            }
        });

        ImageButton xBtn = view.findViewById(R.id.x);
        xBtn.setOnClickListener(v -> dismiss());

        return view;
    }

    public void updateList() {
        // Hier sollten Sie die updateList()-Methode nicht direkt aufrufen
        // Stattdessen sollten Sie einen Listener verwenden, um das ListFragment zu benachrichtigen
        if (onConfirmListener != null) {
            onConfirmListener.updateList();
        }
    }
}