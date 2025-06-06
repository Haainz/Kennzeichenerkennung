package de.haainz.kennzeichenerkennung;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.core.content.res.ResourcesCompat;

import java.text.SimpleDateFormat;
import java.util.Date;

public class KennzeichenGenerator {

    private Context context;

    public KennzeichenGenerator(Context context) {
        this.context = context;
    }

    public Bitmap generateImage(Drawable img, Kennzeichen kennzeichen) {
        // Größe des Kennzeichens in dp festlegen
        int height = dpToPx(188);
        int width = dpToPx(746);

        // Bitmap erstellen
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Canvas erstellen
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();

        // Bild zeichnen
        if (img instanceof BitmapDrawable) {
            Bitmap imageBitmap = ((BitmapDrawable) img).getBitmap();
            int imageWidth = imageBitmap.getWidth();
            int imageHeight = imageBitmap.getHeight();

            // Bild auf ein Drittel der Originalgröße skalieren
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(imageBitmap, (imageWidth * 2) / 3, (imageHeight * 2) / 3, true);

            // Berechnungen für die Positionierung des skalierten Bildes
            canvas.drawBitmap(scaledBitmap, dpToPx(-30), dpToPx(-177), paint);
        }

        // Schriftart für das Ortskürzel laden
        Typeface typeface = ResourcesCompat.getFont(context, R.font.schriftkraftfahrzeugkennzeichen);

        // Text zeichnen (Ortskürzel)
        paint.setColor(Color.BLACK);
        paint.setTextSize(dpToPx(140));
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setTypeface(typeface); // Schriftart für das Ortskürzel setzen
        canvas.drawText(kennzeichen.OertskuerzelGeben(), dpToPx(180), dpToPx(145), paint);

        // "Ort" Text rechtsbündig zeichnen (Standard-Schriftart)
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.RIGHT); // Textausrichtung auf rechts setzen
        String ortText = kennzeichen.OrtGeben();
        textPaint.setTextSize(getTextSizeBasedOnLength(ortText));
        float ortXPosition = width - dpToPx(100); // 25 Pixel vom rechten Rand
        canvas.drawText(ortText, ortXPosition, dpToPx(65), textPaint);

        // "Bundesland" Text rechtsbündig zeichnen (Standard-Schriftart)
        String bundeslandText = kennzeichen.BundeslandGeben();
        textPaint.setTextSize(getTextSizeBasedOnLength(bundeslandText));
        canvas.drawText(bundeslandText, ortXPosition, dpToPx(108), textPaint);

        // Aktuelles Datum ermitteln (Standard-Schriftart)
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        String currentDate = dateFormat.format(new Date());
        textPaint.setTextSize(dpToPx(30));
        canvas.drawText(currentDate, ortXPosition, dpToPx(151), textPaint);

        return bitmap;
    }

    private float getTextSizeBasedOnLength(String text) {
        if (text.length() > 10) {
            return text.length()>15 ? text.length()>=20 ? text.length()>25 ? dpToPx(17):dpToPx(21) : dpToPx(25) : dpToPx(30);
        } else {
            return dpToPx(30);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}