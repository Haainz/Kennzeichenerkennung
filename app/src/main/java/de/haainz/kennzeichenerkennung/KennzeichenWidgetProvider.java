package de.haainz.kennzeichenerkennung;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Calendar;

public class KennzeichenWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "KennzeichenWidgetProvider"; // Definiere ein Tag für das Logging

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        // Bild des Kennzeichens des Tages setzen
        Bitmap kennzeichenImage = getKennzeichenImage(context);
        if (kennzeichenImage != null) {
            views.setImageViewBitmap(R.id.widget_image, kennzeichenImage);
        }

        // Intent für das Öffnen des DayFragment
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("openFragment", "DayFragment");
        Log.d(TAG, "Sending intent to open DayFragment");
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_image, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private Bitmap getKennzeichenImage(Context context) {
        // Erzeuge das Bild des Kennzeichens des Tages
        Kennzeichen_KI kennzeichenKI = new Kennzeichen_KI(context);
        KennzeichenGenerator kennzeichenGenerator = new KennzeichenGenerator(context);

        Kennzeichen kennzeichen = getKennzeichen(kennzeichenKI);

        // Erzeuge das Bild
        Drawable img = context.getDrawable(R.drawable.img3);
        Bitmap originalBitmap = kennzeichenGenerator.generateImage(img, kennzeichen);

        // Skaliere das Bild auf die Größe des Widgets
        int widgetWidth = context.getResources().getDimensionPixelSize(R.dimen.widget_width) * 2;
        int widgetHeight = context.getResources().getDimensionPixelSize(R.dimen.widget_height) * 2;
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, widgetWidth, widgetHeight, true);

        // Berechne die Zuschneideparameter
        int cutTop = scaledBitmap.getHeight() / 9 + scaledBitmap.getHeight() / 200;
        int cutBottom = scaledBitmap.getHeight() / 10 + scaledBitmap.getHeight() / 150;
        int cutLeft = scaledBitmap.getWidth() / 9;
        int cutRight = scaledBitmap.getWidth() / 10;

        // Berechne die neuen Dimensionen nach dem Zuschneiden
        int newWidth = scaledBitmap.getWidth() - cutLeft - cutRight;
        int newHeight = scaledBitmap.getHeight() - cutTop - cutBottom;

        // Zuschneiden des Bildes
        Bitmap croppedBitmap = Bitmap.createBitmap(scaledBitmap, cutLeft, cutTop, newWidth, newHeight);

        return croppedBitmap;
    }

    private Kennzeichen getKennzeichen(Kennzeichen_KI kennzeichenKI) {
        ArrayList<Kennzeichen> kennzeichenListe = kennzeichenKI.getKennzeichenListe();

        // Filtere die Liste nach normalen Kennzeichen
        ArrayList<Kennzeichen> filteredList = new ArrayList<>();
        for (Kennzeichen kennzeichen : kennzeichenListe) {
            if (kennzeichen.isNormal()) {
                filteredList.add(kennzeichen);
            }
        }

        // Berechne das Kennzeichen basierend auf dem Datum
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1; // Monat ist 0-basiert, also +1
        int year = calendar.get(Calendar.YEAR);

        // Hash-Funktion anwenden, um eine eindeutige Zahl zu erzeugen
        int index = (day * 31 + month * 12 + year) % filteredList.size();
        return filteredList.get(index);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, KennzeichenWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }
}