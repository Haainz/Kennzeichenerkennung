package de.haainz.kennzeichenerkennung.ui.home;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;
import androidx.exifinterface.media.ExifInterface;

import org.opencv.core.Point;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ImageUtil {

    public static Bitmap imageProxyToBitmap(ImageProxy image) {
        if (image == null) return null;
        try {
            byte[] nv21 = YUV_420_888toNV21(image);
            if (nv21 == null) return null;
            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            boolean ok = yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 90, out);
            if (!ok) return null;
            byte[] jpegBytes = out.toByteArray();
            out.close();
            Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);

            int rotation = image.getImageInfo().getRotationDegrees();
            if (rotation != 0 && bmp != null) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                Bitmap rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                bmp.recycle();
                return rotated;
            }
            return bmp;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static byte[] YUV_420_888toNV21(ImageProxy image) {
        try {
            ImageProxy.PlaneProxy[] planes = image.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            final int width = image.getWidth();
            final int height = image.getHeight();
            final int ySize = yBuffer.remaining();
            final int uSize = uBuffer.remaining();
            final int vSize = vBuffer.remaining();
            byte[] nv21 = new byte[ySize + uSize + vSize];

            // Y
            yBuffer.get(nv21, 0, ySize);

            int chromaHeight = (height + 1) / 2;
            int chromaWidth = (width + 1) / 2;

            int offset = ySize;
            int uRowStride = planes[1].getRowStride();
            int vRowStride = planes[2].getRowStride();
            int uPixelStride = planes[1].getPixelStride();
            int vPixelStride = planes[2].getPixelStride();

            byte[] uRow = new byte[uRowStride];
            byte[] vRow = new byte[vRowStride];

            uBuffer.rewind();
            vBuffer.rewind();

            for (int row = 0; row < chromaHeight; row++) {
                if (uRowStride > 0 && uBuffer.remaining() >= Math.min(uRow.length, uBuffer.remaining())) {
                    uBuffer.get(uRow, 0, Math.min(uRow.length, uBuffer.remaining()));
                } else {
                    // fallback: fill zeros
                    for (int i = 0; i < uRow.length; i++) uRow[i] = 0;
                }
                if (vRowStride > 0 && vBuffer.remaining() >= Math.min(vRow.length, vBuffer.remaining())) {
                    vBuffer.get(vRow, 0, Math.min(vRow.length, vBuffer.remaining()));
                } else {
                    for (int i = 0; i < vRow.length; i++) vRow[i] = 0;
                }
                for (int col = 0; col < chromaWidth; col++) {
                    int uIndex = col * uPixelStride;
                    int vIndex = col * vPixelStride;
                    nv21[offset++] = (vIndex < vRow.length) ? vRow[vIndex] : 0; // V
                    nv21[offset++] = (uIndex < uRow.length) ? uRow[uIndex] : 0; // U
                }
            }
            return nv21;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap loadBitmapFromFile(File f) {
        try {
            InputStream is = new FileInputStream(f);
            Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
            is.close();

            // EXIF-orientation fix
            try {
                ExifInterface exif = new ExifInterface(f.getAbsolutePath());
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                int rotate = 0;
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90: rotate = 90; break;
                    case ExifInterface.ORIENTATION_ROTATE_180: rotate = 180; break;
                    case ExifInterface.ORIENTATION_ROTATE_270: rotate = 270; break;
                    default: rotate = 0; break;
                }
                if (rotate != 0 && bmp != null) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotate);
                    Bitmap rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                    bmp.recycle();
                    return rotated;
                }
            } catch (Exception ex) {
                // ignore EXIF errors
                ex.printStackTrace();
            }
            return bmp;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // center-crop mapping bitmap -> preview
    public static Point[] mapBitmapPointsToView(PreviewView preview, Bitmap bitmap, org.opencv.core.Point[] bitmapPoints) {
        int viewW = Math.max(1, preview.getWidth());
        int viewH = Math.max(1, preview.getHeight());
        int bmpW = Math.max(1, bitmap.getWidth());
        int bmpH = Math.max(1, bitmap.getHeight());

        float scale = Math.max((float) viewW / (float) bmpW, (float) viewH / (float) bmpH);
        float scaledW = bmpW * scale;
        float scaledH = bmpH * scale;
        float dx = (viewW - scaledW) / 2f;
        float dy = (viewH - scaledH) / 2f;

        Point[] out = new Point[bitmapPoints.length];
        for (int i = 0; i < bitmapPoints.length; i++) {
            float x = (float) bitmapPoints[i].x * scale + dx;
            float y = (float) bitmapPoints[i].y * scale + dy;
            out[i] = new Point(Math.round(x), Math.round(y));
        }
        return out;
    }

    // inverse mapping view -> bitmap
    public static org.opencv.core.Point[] mapViewPointsToBitmap(PreviewView preview, Bitmap bitmap, Point[] viewPoints) {
        int viewW = Math.max(1, preview.getWidth());
        int viewH = Math.max(1, preview.getHeight());
        int bmpW = Math.max(1, bitmap.getWidth());
        int bmpH = Math.max(1, bitmap.getHeight());

        float scale = Math.max((float) viewW / (float) bmpW, (float) viewH / (float) bmpH);
        float scaledW = bmpW * scale;
        float scaledH = bmpH * scale;
        float dx = (viewW - scaledW) / 2f;
        float dy = (viewH - scaledH) / 2f;

        org.opencv.core.Point[] out = new org.opencv.core.Point[viewPoints.length];
        for (int i = 0; i < viewPoints.length; i++) {
            float bx = (float) (viewPoints[i].x - dx) / scale;
            float by = (float) (viewPoints[i].y - dy) / scale;
            out[i] = new org.opencv.core.Point(Math.round(bx), Math.round(by));
        }
        return out;
    }
}