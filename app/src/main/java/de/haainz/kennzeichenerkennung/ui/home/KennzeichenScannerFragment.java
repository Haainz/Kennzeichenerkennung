package de.haainz.kennzeichenerkennung.ui.home;

import android.provider.MediaStore;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.appcompat.app.AppCompatActivity;

import com.google.common.util.concurrent.ListenableFuture;
import com.yalantis.ucrop.UCrop;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Point;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import android.os.Handler;

import de.haainz.kennzeichenerkennung.R;

public class KennzeichenScannerFragment extends Fragment {

    private PreviewView previewView;
    private OverlayView overlayView;
    private ImageButton shutterBtn;
    private ImageButton galleryBtn;
    private ImageButton backButton;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private final Executor analysisExecutor = Executors.newSingleThreadExecutor();

    private KennzeichenDetector detector;
    private volatile Point[] latestQuad;
    private static final String KS_TAG = "KSFragResult";

    private Camera camera;
    private ScaleGestureDetector scaleGestureDetector;

    private Button zoom1x, zoom3x, zoom5x;
    private ImageButton scanToggleButton;
    private boolean isScannerEnabled = false; // Jetzt standardmäßig AUS
    
    private TextView notificationText;
    private final Handler notificationHandler = new Handler(Looper.getMainLooper());
    private Runnable notificationRunnable;

    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Intent> cropImageLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!OpenCVLoader.initLocal()) {
            Log.e(KS_TAG, "OpenCV initialization failed");
        }
        detector = new KennzeichenDetector(getContext());

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedUri = result.getData().getData();
                        if (selectedUri != null) {
                            startCrop(selectedUri);
                        }
                    }
                }
        );

        cropImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        final Uri resultUri = UCrop.getOutput(result.getData());
                        if (resultUri != null) {
                            sendResultAndClose(resultUri);
                        }
                    } else if (result.getResultCode() == UCrop.RESULT_ERROR && result.getData() != null) {
                        final Throwable cropError = UCrop.getError(result.getData());
                        Log.e(KS_TAG, "Crop error", cropError);
                    }
                }
        );
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_kennzeichen_scanner, container, false);

        previewView = root.findViewById(R.id.previewView);
        overlayView = root.findViewById(R.id.overlayView);
        shutterBtn = root.findViewById(R.id.captureButton);
        galleryBtn = root.findViewById(R.id.galleryButton);
        backButton = root.findViewById(R.id.backButton);
        scanToggleButton = root.findViewById(R.id.scanToggleButton);
        notificationText = root.findViewById(R.id.notificationText);

        zoom1x = root.findViewById(R.id.zoom1x);
        zoom3x = root.findViewById(R.id.zoom3x);
        zoom5x = root.findViewById(R.id.zoom5x);

        zoom1x.setOnClickListener(v -> setZoom(1.0f));
        zoom3x.setOnClickListener(v -> setZoom(3.0f));
        zoom5x.setOnClickListener(v -> setZoom(5.0f));

        // Initialer Button-Zustand (Scanner aus)
        scanToggleButton.setImageResource(R.drawable.baseline_camera_enhance_24);
        scanToggleButton.setColorFilter(Color.WHITE);
        overlayView.setScannerActive(false);

        setupZoom();
        startCamera();

        galleryBtn.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK);
            pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            pickImageLauncher.launch(pickIntent);
        });

        backButton.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        scanToggleButton.setOnClickListener(v -> {
            isScannerEnabled = !isScannerEnabled;
            if (isScannerEnabled) {
                // Scanner AKTIV
                scanToggleButton.setColorFilter(Color.parseColor("#FFFF00")); // Gelb wenn aktiv
                overlayView.setScannerActive(true); // Umschalten auf dynamischen Rahmen
                showNotification("Auto-Scanner aktiv");
            } else {
                // Scanner INAKTIV (Normales Foto)
                scanToggleButton.setColorFilter(Color.WHITE);
                overlayView.setScannerActive(false); // Umschalten auf festes Sucher-Rechteck
                overlayView.clear(); // Alten grünen Rahmen löschen
                showNotification("Scanner aus");
            }
        });

        shutterBtn.setOnClickListener(v -> {
            if (isScannerEnabled && latestQuad != null) {
                captureAndCropUsingQuad(latestQuad);
            } else {
                captureAndDetect();
            }
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().hide();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().show();
            }
        }
        notificationHandler.removeCallbacks(notificationRunnable);
    }

    private void setupZoom() {
        scaleGestureDetector = new ScaleGestureDetector(requireContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (camera != null) {
                    CameraControl cameraControl = camera.getCameraControl();
                    float currentZoomRatio = camera.getCameraInfo().getZoomState().getValue().getZoomRatio();
                    float delta = detector.getScaleFactor();
                    float newRatio = currentZoomRatio * delta;
                    cameraControl.setZoomRatio(newRatio);
                    showNotification(String.format(Locale.GERMANY, "Zoom: %.1fx", newRatio));
                }
                return true;
            }
        });

        previewView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, getMainExecutor());
    }

    private Executor getMainExecutor() {
        return ContextCompat.getMainExecutor(requireContext());
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        ResolutionSelector resolutionSelector = new ResolutionSelector.Builder()
                .setResolutionStrategy(new ResolutionStrategy(new Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                .build();

        Preview preview = new Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build();

        imageCapture = new ImageCapture.Builder()
                .setResolutionSelector(resolutionSelector)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        analysis.setAnalyzer(analysisExecutor, imageProxy -> {
            try {
                Bitmap bitmap = ImageUtil.imageProxyToBitmap(imageProxy);
                if (bitmap != null) {
                    Point[] quad = detector.detectLicensePlate(bitmap);
                    if (quad != null) {
                        Point[] mapped = ImageUtil.mapBitmapPointsToView(previewView, bitmap, quad);

                        if (isWithinScanRange(mapped)) {
                            latestQuad = mapped;
                            Activity act = getActivity();
                            if (act != null && isAdded()) {
                                act.runOnUiThread(() -> overlayView.setPoints(mapped));
                            }
                        } else {
                            latestQuad = null;
                            Activity act = getActivity();
                            if (act != null && isAdded()) {
                                act.runOnUiThread(() -> overlayView.clear());
                            }
                        }
                    } else {
                        latestQuad = null;
                        Activity act = getActivity();
                        if (act != null && isAdded()) {
                            act.runOnUiThread(() -> overlayView.clear());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                imageProxy.close();
            }
        });

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, analysis);

        camera.getCameraInfo().getZoomState().observe(getViewLifecycleOwner(), zoomState -> {
            updateZoomButtons(zoomState.getZoomRatio());
        });

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
    }

    private void captureAndDetect() {
        if (imageCapture == null) return;

        File tmpFile = createImageFile();
        ImageCapture.OutputFileOptions opts = new ImageCapture.OutputFileOptions.Builder(tmpFile).build();
        imageCapture.takePicture(opts, Executors.newSingleThreadExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        getMainExecutor().execute(() -> {
                            try {
                                Bitmap bitmap = ImageUtil.loadBitmapFromFile(tmpFile);
                                if (bitmap != null) {
                                    if (isScannerEnabled) {
                                        // NORMALER SCANNER-ABLAUF
                                        Point[] quad = detector.detectLicensePlate(bitmap);
                                        if (quad != null) {
                                            performCropAndReturn(bitmap, quad, 2.5);
                                        } else {
                                            showNotification("Kein Kennzeichen erkannt.\nZoome etwas näher hin.");
                                        }
                                    } else {
                                        // FOTO-MODUS: Ausschnitt aus dem Hilfsrechteck
                                        android.graphics.RectF guideRect = overlayView.getGuideRect();
                                        Point[] quadInView = new Point[] {
                                                new Point(guideRect.left, guideRect.top),
                                                new Point(guideRect.right, guideRect.top),
                                                new Point(guideRect.right, guideRect.bottom),
                                                new Point(guideRect.left, guideRect.bottom)
                                        };
                                        Point[] quadBitmap = ImageUtil.mapViewPointsToBitmap(previewView, bitmap, quadInView);
                                        performCropAndReturn(bitmap, quadBitmap, 2.4); // Auf 2.4 erhöht, um sicherzustellen, dass das Bild nicht zu klein wirkt
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(KS_TAG, "Processing failed", e);
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        getMainExecutor().execute(() ->
                                showNotification("Fehler bei Aufnahme"));
                    }
                });
    }

    private void captureAndCropUsingQuad(Point[] quadInViewCoords) {
        if (imageCapture == null) return;

        File tmpFile = createImageFile();
        ImageCapture.OutputFileOptions opts = new ImageCapture.OutputFileOptions.Builder(tmpFile).build();
        imageCapture.takePicture(opts, Executors.newSingleThreadExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        getMainExecutor().execute(() -> {
                            try {
                                Bitmap bitmap = ImageUtil.loadBitmapFromFile(tmpFile);
                                if (bitmap != null) {
                                    // WICHTIG: Die View-Koordinaten müssen auf die tatsächliche
                                    // Bitmap-Größe des Fotos gemappt werden!
                                    Point[] quadBitmap = ImageUtil.mapViewPointsToBitmap(previewView, bitmap, quadInViewCoords);
                                    performCropAndReturn(bitmap, quadBitmap, 1.2);
                                }
                            } catch (Exception e) {
                                Log.e(KS_TAG, "Processing failed", e);
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        getMainExecutor().execute(() ->
                                showNotification("Fehler bei Aufnahme"));
                    }
                });
    }

    private void performCropAndReturn(Bitmap bitmap, Point[] quad, double expansionFactor) {
        // Hier passiert die Magie: Wir vergrößern das Feld für das Foto.
        Point[] expandedQuad = expansionFactor > 1.0 ? detector.expandQuad(quad, expansionFactor) : quad;

        // Wir nutzen das Quad für den Zuschnitt
        Bitmap plate = detector.cropAndWarp(bitmap, expandedQuad);

        if (plate == null) {
            if (isAdded()) {
                showNotification("Fehler bei der Entzerrung");
            }
            return;
        }

        Context ctx = getContext();
        if (ctx == null) return;

        try {
            File out = new File(ctx.getCacheDir(), "plate_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(out);
            plate.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();

            Uri uri = FileProvider.getUriForFile(ctx,
                    "de.haainz.kennzeichenerkennung.fileprovider", out);

            sendResultAndClose(uri);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startCrop(@NonNull Uri uri) {
        String destinationFileName = "cropped_image_" + System.currentTimeMillis() + ".jpg";
        Uri destinationUri = Uri.fromFile(new File(requireContext().getCacheDir(), destinationFileName));

        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);
        options.setToolbarColor(Color.WHITE);
        options.setActiveControlsWidgetColor(ContextCompat.getColor(requireContext(), R.color.yellow));
        options.setToolbarWidgetColor(Color.BLACK);
        options.setToolbarTitle("Bild zuschneiden");

        Intent intent = UCrop.of(uri, destinationUri)
                .withOptions(options)
                .getIntent(requireContext());
        cropImageLauncher.launch(intent);
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return new File(requireContext().getCacheDir(), "IMG_" + timeStamp + ".jpg");
    }

    private void sendResultAndClose(Uri uri) {
        Bundle result = new Bundle();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            result.putParcelable("image_uri", uri);
        } else {
            result.putParcelable("image_uri", uri);
        }
        result.putString("scanned_plate_uri", uri.toString());

        requireActivity().runOnUiThread(() -> {
            try {
                getParentFragmentManager().setFragmentResult("kennzeichen_scan_result", result);
                getActivity().getSupportFragmentManager().setFragmentResult("kennzeichen_scan_result", result);

                getActivity().grantUriPermission(getActivity().getPackageName(), uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);

                NavHostFragment.findNavController(this).popBackStack();
            } catch (Exception e) {
                Log.e(KS_TAG, "Failed to deliver result", e);
            }
        });
    }    

    // Methode zum Anzeigen der Nachricht (ersetzt Toast):
    private void showNotification(String message) {
        notificationHandler.removeCallbacks(notificationRunnable);
        notificationText.setText(message);
        notificationText.setVisibility(View.VISIBLE);
        notificationText.setAlpha(1.0f);

        notificationRunnable = () -> {
            notificationText.animate().alpha(0f).setDuration(500).withEndAction(() ->
                    notificationText.setVisibility(View.GONE)
            ).start();
        };
        notificationHandler.postDelayed(notificationRunnable, 2000); // 2 Sekunden anzeigen
    }

    // Methode zum Zoomen:
    private void setZoom(float ratio) {
        if (camera != null) {
            camera.getCameraControl().setZoomRatio(ratio);
            showNotification("Zoom: " + (int)ratio + "x");
        }
    }

    private void updateZoomButtons(float currentRatio) {
        float tolerance = 0.1f;
        int activeColor = ContextCompat.getColor(requireContext(), R.color.yellow);

        highlightButton(zoom1x, Math.abs(currentRatio - 1.0f) < tolerance, activeColor);
        highlightButton(zoom3x, Math.abs(currentRatio - 3.0f) < tolerance, activeColor);
        highlightButton(zoom5x, Math.abs(currentRatio - 5.0f) < tolerance, activeColor);
    }

    private void highlightButton(Button button, boolean active, int activeColor) {
        if (active) {
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(activeColor));
            button.setTextColor(Color.BLACK);
        } else {
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.blue_500)));
            button.setTextColor(Color.WHITE);
        }
    }

    private boolean isWithinScanRange(Point[] mappedPoints) {
        if (mappedPoints == null || mappedPoints.length < 4 || overlayView == null) return false;

        android.graphics.RectF guideRect = overlayView.getGuideRect();
        float density = getResources().getDisplayMetrics().density;
        float limitPx = 50 * density;

        float allowedTop = guideRect.top - limitPx;
        float allowedBottom = guideRect.bottom + limitPx;

        // Prüfe ob der Mittelpunkt des erkannten Quads im erlaubten Bereich liegt
        double centerY = 0;
        for (Point p : mappedPoints) centerY += p.y;
        centerY /= 4.0;

        return centerY >= allowedTop && centerY <= allowedBottom;
    }
}
