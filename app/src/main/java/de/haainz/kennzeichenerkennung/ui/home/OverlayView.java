package de.haainz.kennzeichenerkennung.ui.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import org.opencv.core.Point;

public class OverlayView extends View {
    private Paint paintScanner = new Paint();
    private Paint paintGuide = new Paint();
    private Paint paintScrim = new Paint();
    private Paint paintTransparent = new Paint();
    private Paint paintText = new Paint();
    private Point[] points;
    private boolean isScannerActive = false;

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // Style für den aktiven Scanner (Gelb für Fokus)
        paintScanner.setColor(Color.parseColor("#FFFF00"));
        paintScanner.setStrokeWidth(10f);
        paintScanner.setStyle(Paint.Style.STROKE);
        paintScanner.setAntiAlias(true);

        // Style für das Hilfs-Rechteck
        paintGuide.setColor(Color.WHITE);
        paintGuide.setStrokeWidth(6f);
        paintGuide.setStyle(Paint.Style.STROKE);
        paintGuide.setAntiAlias(true);

        // Abdunkelung
        paintScrim.setColor(Color.parseColor("#99000000"));
        paintScrim.setStyle(Paint.Style.FILL);

        // Transparentes Loch
        paintTransparent.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        // Text
        paintText.setColor(Color.WHITE);
        paintText.setTextSize(45f);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setAntiAlias(true);
        paintText.setFakeBoldText(true);
    }

    public void setPoints(Point[] pts) {
        this.points = pts;
        invalidate();
    }

    public void setScannerActive(boolean active) {
        this.isScannerActive = active;
        invalidate();
    }

    public void clear() {
        this.points = null;
        invalidate();
    }

    public RectF getGuideRect() {
        float width = getWidth();
        float height = getHeight();
        float rectW = width * 0.90f;
        float rectH = rectW * 0.25f;
        return new RectF(
                (width - rectW) / 2f,
                (height - rectH) / 2f,
                (width + rectW) / 2f,
                (height + rectH) / 2f
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Erst den Scrim zeichnen
        canvas.drawRect(0, 0, getWidth(), getHeight(), paintScrim);

        RectF guideRect = getGuideRect();

        // Das Loch freistanzen
        canvas.drawRoundRect(guideRect, 20, 20, paintTransparent);

        // Text über dem Rahmen
        canvas.drawText("Kennzeichen im Rahmen platzieren", getWidth() / 2f, guideRect.top - 40, paintText);

        if (isScannerActive) {
            // Wenn Scanner aktiv, zeichne die erkannten Punkte (dynamisch)
            if (points != null && points.length >= 4) {
                for (int i = 0; i < 4; i++) {
                    Point p1 = points[i];
                    Point p2 = points[(i + 1) % 4];
                    canvas.drawLine((float)p1.x, (float)p1.y, (float)p2.x, (float)p2.y, paintScanner);
                }
            } else {
                // Wenn aktiv aber nix erkannt, zeige das Hilferechteck in Gelb
                paintScanner.setAlpha(100);
                canvas.drawRoundRect(guideRect, 20, 20, paintScanner);
                paintScanner.setAlpha(255);
            }
        } else {
            // Weißes Hilferechteck
            canvas.drawRoundRect(guideRect, 20, 20, paintGuide);
        }
    }
}