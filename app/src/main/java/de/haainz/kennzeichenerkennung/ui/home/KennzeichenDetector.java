package de.haainz.kennzeichenerkennung.ui.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KennzeichenDetector {
    private final Context context;
    private static final String TAG = "KennzeichenDetector";

    public KennzeichenDetector(Context ctx) {
        this.context = ctx;
    }

    public Point[] detectLicensePlate(Bitmap bitmap) {
        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);

        try {
            // Konvertierung & Skalierung für Speed
            double scale = Math.max(1.0, src.width() / 600.0);
            Mat work = new Mat();
            Imgproc.resize(src, work, new Size(src.width() / scale, src.height() / scale));

            // Vorverarbeitung: Grau -> Blur -> Canny (Kanten finden)
            Mat gray = new Mat();
            Imgproc.cvtColor(work, gray, Imgproc.COLOR_RGBA2GRAY);
            Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);

            Mat edges = new Mat();
            Imgproc.Canny(gray, edges, 75, 200);

            // Kanten verstärken
            Imgproc.dilate(edges, edges, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));

            // Konturen suchen
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

            Point[] bestQuad = null;
            double maxArea = 0;

            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area < 1000) continue;

                MatOfPoint2f c2f = new MatOfPoint2f(contour.toArray());
                double peri = Imgproc.arcLength(c2f, true);
                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true);

                if (approx.total() == 4) {
                    Point[] pts = approx.toArray();
                    Point[] ordered = orderPoints(pts);

                    double w = distance(ordered[0], ordered[1]);
                    double h = distance(ordered[1], ordered[2]);
                    double ar = w / h;

                    // Kennzeichen-Verhältnis (ca. 3:1 bis 5:1)
                    if (ar > 2.2 && ar < 6.5) {
                        if (area > maxArea) {
                            maxArea = area;
                            bestQuad = new Point[4];
                            for(int i=0; i<4; i++) {
                                bestQuad[i] = new Point(ordered[i].x * scale, ordered[i].y * scale);
                            }
                        }
                    }
                }
            }

            // Cleanup
            src.release(); work.release(); gray.release(); edges.release(); hierarchy.release();

            return bestQuad; // Gibt jetzt den exakten Rahmen zurück (für den Live-View)

        } catch (Exception e) {
            Log.e(TAG, "Fehler: " + e.getMessage());
            return null;
        }
    }

    public Bitmap cropAndWarp(Bitmap bitmap, Point[] quad) {
        if (quad == null) return null;
        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);
        try {
            double w = Math.max(distance(quad[0], quad[1]), distance(quad[3], quad[2]));
            double h = Math.max(distance(quad[0], quad[3]), distance(quad[1], quad[2]));

            Mat dest = new Mat((int)h, (int)w, CvType.CV_8UC4);
            MatOfPoint2f srcQuad = new MatOfPoint2f(quad);
            MatOfPoint2f dstQuad = new MatOfPoint2f(
                    new Point(0, 0), new Point(w, 0), new Point(w, h), new Point(0, h)
            );

            Mat trans = Imgproc.getPerspectiveTransform(srcQuad, dstQuad);
            Imgproc.warpPerspective(src, dest, trans, new Size(w, h));

            Bitmap res = Bitmap.createBitmap((int)w, (int)h, Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(dest, res);
            src.release(); dest.release(); trans.release();
            return res;
        } catch (Exception e) { return null; }
    }

    public Point[] expandQuad(Point[] pts, double f) {
        if (pts == null) return null;
        Point c = new Point(0,0);
        for(Point p : pts) { c.x += p.x; c.y += p.y; }
        c.x /= 4; c.y /= 4;
        Point[] res = new Point[4];
        for(int i=0; i<4; i++) res[i] = new Point(c.x + (pts[i].x - c.x) * f, c.y + (pts[i].y - c.y) * f);
        return res;
    }

    private double distance(Point a, Point b) { return Math.hypot(a.x - b.x, a.y - b.y); }

    private Point[] orderPoints(Point[] pts) {
        if (pts == null || pts.length != 4) return pts;
        Point[] ordered = new Point[4];

        // Sortiere nach Y-Koordinate (oben vs unten)
        ArrayList<Point> list = new ArrayList<>();
        for (Point p : pts) list.add(p);
        Collections.sort(list, (p1, p2) -> Double.compare(p1.y, p2.y));

        // Die zwei oberen Punkte nach X sortieren (links vs rechts)
        List<Point> top = new ArrayList<>(list.subList(0, 2));
        Collections.sort(top, (p1, p2) -> Double.compare(p1.x, p2.x));

        // Die zwei unteren Punkte nach X sortieren
        List<Point> bot = new ArrayList<>(list.subList(2, 4));
        Collections.sort(bot, (p1, p2) -> Double.compare(p1.x, p2.x));

        ordered[0] = top.get(0); // Top-Left
        ordered[1] = top.get(1); // Top-Right
        ordered[2] = bot.get(1); // Bottom-Right
        ordered[3] = bot.get(0); // Bottom-Left

        return ordered;
    }
}