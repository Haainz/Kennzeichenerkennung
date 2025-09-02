package de.haainz.kennzeichenerkennung;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class ContactFragment extends DialogFragment {

    private EditText maileingabe;
    private EditText nameeingabe;
    private EditText texteingabe;
    private CheckBox lobCheckBox;
    private CheckBox fehlerCheckBox;
    private CheckBox verbesserungCheckBox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.CustomDialogfullTheme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact, container, false);

        maileingabe = view.findViewById(R.id.maileingabe);
        nameeingabe = view.findViewById(R.id.nameeingabe);
        texteingabe = view.findViewById(R.id.texteingabe);
        lobCheckBox = view.findViewById(R.id.lob_chk);
        fehlerCheckBox = view.findViewById(R.id.fehler_chk);
        verbesserungCheckBox = view.findViewById(R.id.verbesseung_chk);

        Button sendenButton = view.findViewById(R.id.send_btn);
        sendenButton.setOnClickListener(v -> senden());

        ImageButton xBtn = view.findViewById(R.id.x);
        xBtn.setOnClickListener(v -> dismiss());

        return view;
    }

    private void senden() {
        String email = maileingabe.getText().toString().trim();
        String name = nameeingabe.getText().toString().trim();
        String nachricht = texteingabe.getText().toString().trim();

        // Überprüfen, ob die EditTexts leer sind
        if (email.isEmpty()) {
            Toast.makeText(getActivity(), "Bitte geben Sie Ihre E-Mail-Adresse ein.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isValidEmail(email)) {
            Toast.makeText(getActivity(), "Ungültige E-Mail-Adresse.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (name.isEmpty()) {
            Toast.makeText(getActivity(), "Bitte geben Sie Ihren Namen ein.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (nachricht.isEmpty()) {
            Toast.makeText(getActivity(), "Bitte geben Sie Ihre Nachricht ein.", Toast.LENGTH_SHORT).show();
            return;
        }

        // E-Mail senden
        sendEmail(name, email, nachricht);
    }

    private boolean isValidEmail(String email) {
        return email.contains("@") && email.indexOf('.') > email.indexOf('@') + 1;
    }

    private void sendEmail(String name, String email, String message) {
        final String username = getResources().getString(R.string.mail_address);
        final String password = getResources().getString(R.string.mail_password);

        // Tags aus den Checkboxen sammeln
        List<String> tags = new ArrayList<>();
        if (lobCheckBox.isChecked()) {
            tags.add("Lob");
        }
        if (fehlerCheckBox.isChecked()) {
            tags.add("Fehler");
        }
        if (verbesserungCheckBox.isChecked()) {
            tags.add("Verbesserung");
        }

        String tagsString = String.join(", ", tags); // Tags in einen String umwandeln

        // ExecutorService für Hintergrund-Threads
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587"); // Port für STARTTLS

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            try {
                Message msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(username));
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse("kennzeichenerkennung@gmail.com"));
                msg.setSubject("Kennzeichenapp Kontaktaufnahme");
                msg.setText("E-Mail: " + email + "\n\nName: " + name + "\n\nTags: " + tagsString + "\n\nNachricht: " + message);

                Transport.send(msg);
                // Erfolgreiche E-Mail-Benachrichtigung im UI-Thread
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getActivity(), "Nachricht erfolgreich gesendet!", Toast.LENGTH_SHORT).show();
                    dismiss();
                });
            } catch (MessagingException e) {
                e.printStackTrace();
                // Fehlerbenachrichtigung im UI-Thread
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getActivity(), "Fehler beim Senden der Nachricht: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}