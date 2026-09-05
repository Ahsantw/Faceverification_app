package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Surface;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;

import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.multidex.BuildConfig;

import com.google.common.util.concurrent.ListenableFuture;
import org.tensorflow.lite.Interpreter;
import android.util.Size;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FaceVerificationActivity extends AppCompatActivity {
    private String packageName;
    private PreviewView previewView;
    private OverlayView overlayView;
    private TextView verificationStatus;
    private Button captureButton;
    private TextView statusLabel;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private boolean isCameraReady = false;
    private boolean isFaceDetected = false;
    private Interpreter detectorInterpreter;
    private Interpreter recognizerInterpreter;

    private List<RectF> lastDetectedBoxes = new ArrayList<>();
    private Bitmap lastFrameWithBoxes;
    private boolean isRegisterMode;
    private float[] knownEmbedding;
    private float currentCosSim = 0;
    private boolean imageSaved = false;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (BuildConfig.DEBUG) {
            // Disable strict mode for vendor properties
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_verification);
        prefs = getSharedPreferences("locked_apps_prefs", Context.MODE_PRIVATE);


//        prefs = PreferenceManager.getDefaultSharedPreferences(this);
//        isRegisterMode = getIntent().getBooleanExtra("mode", false);
        String mode = getIntent().getStringExtra("mode");
        isRegisterMode = "register".equals(mode);

        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);
        verificationStatus = findViewById(R.id.verificationStatus);
        captureButton = findViewById(R.id.captureButton);
        packageName = getIntent().getStringExtra("packageName");

        try {
            Interpreter.Options options = new Interpreter.Options();
            options.setUseXNNPACK(true);  // Enable faster inference
            detectorInterpreter = new Interpreter(loadModelFile("yolov8n-face_float16.tflite"),options);
            recognizerInterpreter = new Interpreter(loadModelFile("facenet_512.tflite"),options);
        } catch (Exception e) {
            Log.e("FaceVerification", "Error loading models", e);
            finish();
        }

        if (isRegisterMode) {
            verificationStatus.setText("Register your face");
            captureButton.setText("Save Face");
        } else {
            verificationStatus.setText("Verify your face");
            captureButton.setText("Verify");

            // Load known embedding
            String embeddingString = prefs.getString("face_embedding", null);
            if (embeddingString != null) {
                knownEmbedding = stringToFloatArray(embeddingString);
            } else {
                Toast.makeText(this, "No registered face found", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        captureButton.setOnClickListener(v -> {
            if (!isCameraReady) {
                Toast.makeText(this, "Camera is initializing...", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isFaceDetected) {
                Toast.makeText(this, "Please position your face in frame", Toast.LENGTH_SHORT).show();
                return;
            }
            if (lastFrameWithBoxes != null && !lastDetectedBoxes.isEmpty()) {
                RectF box = lastDetectedBoxes.get(0);
                Bitmap faceBitmap = Bitmap.createBitmap(
                        lastFrameWithBoxes,
                        (int) box.left,
                        (int) box.top,
                        (int) box.width(),
                        (int) box.height()
                );

                Matrix matrix = new Matrix();
                matrix.postScale(-1, 1); // Mirror the image
                Bitmap rotatedFace = Bitmap.createBitmap(faceBitmap, 0, 0,
                        faceBitmap.getWidth(), faceBitmap.getHeight(), matrix, true);

                float[] embedding = getFaceEmbedding(rotatedFace);

                if (isRegisterMode) {
                    // Save the embedding
                    prefs.edit()
                            .putString("face_embedding", floatArrayToString(embedding))
                            .apply();
                    Toast.makeText(this, "Face registered successfully", Toast.LENGTH_SHORT).show();
                    finish();
//                    finishAndRemoveTask();
                } else {
                    // Verify against known embedding
                    currentCosSim = cosineSimilarity(embedding, knownEmbedding);
                    if (currentCosSim > 0.8) {
                        if (!imageSaved) {
                        saveImageToStorage(rotatedFace);
                        imageSaved = true;
                        }
                        prefs.edit()
                                .putBoolean("unlocked_" + packageName, true)
                                .apply();

                        // Launch the app properly
                        try {
                            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                            Log.d("AppLocker_faceverification", "Window changed to: " + packageName);
                            if (launchIntent != null) {
                                Log.d("AppLocker_verify_2", "Window changed to: " + packageName);
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                Log.d("AppLocker_verify_3", "Window changed to: " + packageName);
                                startActivity(launchIntent);
                                prefs.edit()
                                        .putBoolean("unlocked_" + packageName, false)
                                        .apply();


                            }
                            finish();

                        } catch (Exception e) {
                            Toast.makeText(this, "Error opening app", Toast.LENGTH_SHORT).show();
                        }
                        finish();

                    } else {
                        verificationStatus.setText("Verification failed. Similarity: " + String.format("%.2f", currentCosSim));
                    }
//                    if (currentCosSim > 0.8) {
//                        verificationStatus.setText("Verified! Similarity: " + String.format("%.2f", currentCosSim));
//                        setResult(RESULT_OK);
//                        finish();
////                        Intent resultData = new Intent();
////                        resultData.putExtra("verified_package", packageName);
////                        setResult(RESULT_OK, resultData);
////                        finish();
////                        Intent resultIntent = new Intent("FACE_VERIFICATION_RESULT");
////                        resultIntent.putExtra("verified", true);
////                        resultIntent.putExtra("packageName", packageName);
////                        sendBroadcast(resultIntent);
////                        finish();
//
//                    } else {
//                        verificationStatus.setText("Verification failed. Similarity: " + String.format("%.2f", currentCosSim));
//                    }
                }
            }
        });
        Log.d("Faceverification_run", "Window changed to: " + packageName);
        startCamera();
    }

//    private void startCamera() {
//        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
//        cameraProviderFuture.addListener(() -> {
//            try {
//                cameraProvider = cameraProviderFuture.get();
//                bindPreview(cameraProvider);
//            } catch (ExecutionException | InterruptedException e) {
//                Log.e("FaceVerification", "Error starting camera", e);
//            }
//        }, ContextCompat.getMainExecutor(this));
//    }

    private void startCamera() {
        try {
            cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            cameraProviderFuture.addListener(() -> {
                try {
                    cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (Exception e) {
                    Log.e("CameraX", "Camera failed", e);
                    // Attempt recovery
                    new Handler().postDelayed(this::startCamera, 1000);
                }
            }, ContextCompat.getMainExecutor(this));
        } catch (Exception e) {
            Log.e("CameraX", "Camera initialization failed", e);
        }
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();
        // Create resolution selector
//        ResolutionSelector resolutionSelector = new ResolutionSelector.Builder()
//                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
//                .setPreferredResolution(new Size(640, 480)) // Optional: Set preferred resolution
//                .build();
//
//// Build preview use case
//        Preview preview = new Preview.Builder()
//                .setResolutionSelector(resolutionSelector)
//                .setTargetRotation(Surface.ROTATION_0)
//                .build();
//
//        ResolutionSelector analysisResolutionSelector = new ResolutionSelector.Builder()
//                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
//                .setResolutionStrategy(new ResolutionStrategy(
//                        new Size(640, 480),
//                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER))
//                .build();
//
//        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
//                .setResolutionSelector(analysisResolutionSelector)
//                .setTargetRotation(Surface.ROTATION_0)
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .build();



// ImageAnalysis use case
//        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
//                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                .setTargetRotation(Surface.ROTATION_0) // or currentDisplay.getRotation()
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .build();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
//        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
//                .setTargetResolution(new Size(640, 480))  // Lower resolution
//                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)  // More efficient
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)  // Drop frames if busy
//                .build();

        Executor analysisExecutor = Executors.newSingleThreadExecutor();
        imageAnalysis.setAnalyzer(analysisExecutor, this::analyzeImage);

        camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalysis
        );
        isCameraReady = true;
        runOnUiThread(() -> {
            verificationStatus.setText(isRegisterMode ?
                    "Camera ready - Align your face" :
                    "Camera ready - Align face to verify");
        });
    }
//private void bindPreview(ProcessCameraProvider cameraProvider) {
//    try {
//        // Unbind previous use cases first
//        cameraProvider.unbindAll();
//
//        // Camera selector - front camera
//        CameraSelector cameraSelector = new CameraSelector.Builder()
//                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
//                .build();
//
//        // Resolution selector for preview (updated)
//        ResolutionSelector previewResolutionSelector = new ResolutionSelector.Builder()
//                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
//                .setResolutionStrategy(new ResolutionStrategy(
//                        new Size(640, 480),
//                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER))
//                .build();
//
//        // Build preview use case
//        Preview preview = new Preview.Builder()
//                .setResolutionSelector(previewResolutionSelector)
//                .setTargetRotation(previewView.getDisplay().getRotation())
//                .build();
//
//        // Connect preview to view
//        preview.setSurfaceProvider(previewView.getSurfaceProvider());
//
//        // Resolution selector for analysis (same approach)
//        ResolutionSelector analysisResolutionSelector = new ResolutionSelector.Builder()
//                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
//                .setResolutionStrategy(new ResolutionStrategy(
//                        new Size(640, 480),
//                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER))
//                .build();
//
//        // Rest of your code remains the same...
//        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
//                .setResolutionSelector(analysisResolutionSelector)
//                .setTargetRotation(previewView.getDisplay().getRotation())
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .build();
//
//        Executor analysisExecutor = Executors.newSingleThreadExecutor();
//        imageAnalysis.setAnalyzer(analysisExecutor, this::analyzeImage);
//
//        camera = cameraProvider.bindToLifecycle(
//                this,
//                cameraSelector,
//                preview,
//                imageAnalysis
//        );
//
//        isCameraReady = true;
//        updateVerificationStatus();
//
//    } catch (Exception e) {
//        Log.e("FaceVerification", "Error binding preview", e);
//        runOnUiThread(() -> {
//            verificationStatus.setText("Camera error: " + e.getMessage());
//        });
//    }
//}
//
//    private void updateVerificationStatus() {
//        runOnUiThread(() -> {
//            verificationStatus.setText(isRegisterMode ?
//                    "Camera ready - Align your face" :
//                    "Camera ready - Align face to verify");
//        });
//    }
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeImage(ImageProxy image) {
        try {
            if (image.getImage() == null) {
                image.close();
                return;
            }
            Runtime.getRuntime().gc();
            Bitmap bitmap = imageProxyToBitmap(image);
            if (bitmap == null) {
                return;
            }
            Matrix matrix = new Matrix();
            matrix.postRotate(270);
            matrix.postScale(-1, 1);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//            if (!imageSaved) {
//                saveImageToStorage(rotatedBitmap);
//                imageSaved = true;
//            }
            int width = image.getWidth();
            int height = image.getHeight();
            Log.e("checking_width_height", "width " +width+ "height"+ height);

            Bitmap resizedBitmap = Bitmap.createScaledBitmap(rotatedBitmap, 640, 640, true);
            List<RectF> boxes = detectFaces(resizedBitmap, rotatedBitmap.getWidth(), rotatedBitmap.getHeight());

            lastFrameWithBoxes = rotatedBitmap.copy(Bitmap.Config.ARGB_8888, true);
            lastDetectedBoxes = boxes;

//            runOnUiThread(() -> {
//                if (boxes != null && !boxes.isEmpty()) {
//                    overlayView.setBoxes(boxes, knownEmbedding == null ? 0 : currentCosSim);
//                    overlayView.invalidate();
//                    captureButton.setEnabled(true);
//                } else {
//                    captureButton.setEnabled(false);
//                }
//            });
//        } catch (Exception e) {
//            Log.e("FaceVerification", "Error analyzing image", e);
//        } finally {
//            image.close();
//        }
            runOnUiThread(() -> {
                if (boxes != null && !boxes.isEmpty()) {
                    isFaceDetected = true;
//                    overlayView.setBoxes(boxes, knownEmbedding == null ? 0 : currentCosSim);
//                    overlayView.invalidate();
                    captureButton.setEnabled(true);
                    verificationStatus.setText(isRegisterMode ?
                            "Face detected - Ready to register" :
                            "Face detected - Ready to verify");
                } else {
                    isFaceDetected = false;
                    captureButton.setEnabled(false);
                    verificationStatus.setText(isRegisterMode ?
                            "Align your face in the frame" :
                            "Align your face to verify");
                }
            });
        } finally {
            image.close();
        }
    }

    private String floatArrayToString(float[] array) {
        StringBuilder sb = new StringBuilder();
        for (float f : array) {
            sb.append(f).append(",");
        }
        // Remove the last comma
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
    private float[] stringToFloatArray(String string) {
        String[] strings = string.split(",");
        float[] array = new float[strings.length];
        for (int i = 0; i < strings.length; i++) {
            try {
                array[i] = Float.parseFloat(strings[i]);
            } catch (NumberFormatException e) {
                array[i] = 0f; // Default value if parsing fails
                Log.e("FaceVerification", "Error parsing float value: " + strings[i]);
            }
        }
        return array;
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        int width = image.getWidth();
        int height = image.getHeight();

        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride();
        int uvPixelStride = planes[1].getPixelStride();

        byte[] yuvBytes = new byte[yBuffer.remaining() + uBuffer.remaining() + vBuffer.remaining()];
        yBuffer.get(yuvBytes, 0, yBuffer.remaining());
        uBuffer.get(yuvBytes, yBuffer.remaining(), uBuffer.remaining());
        vBuffer.get(yuvBytes, yBuffer.remaining() + uBuffer.remaining(), vBuffer.remaining());

        int[] outPixels = new int[width * height];

        yBuffer.rewind();
        uBuffer.rewind();
        vBuffer.rewind();

        for (int y = 0; y < height; y++) {
            int yOffset = y * yRowStride;
            int uvRow = (y >> 1) * uvRowStride;

            for (int x = 0; x < width; x++) {
                int yIndex = yOffset + x;

                // Integer division for chroma subsampling
                int uvIndex = uvRow + (x >> 1) * uvPixelStride;
                int yValue = (yBuffer.get(yIndex) & 0xFF);
                int uValue = (uBuffer.get(uvIndex) & 0xFF) - 128;
                int vValue = (vBuffer.get(uvIndex) & 0xFF) - 128;

                // YUV to RGB conversion
                int r = (int) (yValue + 1.370705f * vValue);
                int g = (int) (yValue - 0.337633f * uValue - 0.698001f * vValue);
                int b = (int) (yValue + 1.732446f * uValue);

                r = Math.min(255, Math.max(0, r));
                g = Math.min(255, Math.max(0, g));
                b = Math.min(255, Math.max(0, b));

                outPixels[y * width + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }

        return Bitmap.createBitmap(outPixels, width, height, Bitmap.Config.ARGB_8888);
    }


//    private Bitmap imageProxyToBitmap(ImageProxy image) {
//        ImageProxy.PlaneProxy[] planes = image.getPlanes();
//        ByteBuffer yBuffer = planes[0].getBuffer();
//        ByteBuffer uBuffer = planes[1].getBuffer();
//        ByteBuffer vBuffer = planes[2].getBuffer();
//
//        int ySize = yBuffer.remaining();
//        int uSize = uBuffer.remaining();
//        int vSize = vBuffer.remaining();
//
//        byte[] nv21 = new byte[ySize + uSize + vSize];
//        yBuffer.get(nv21, 0, ySize);
//        vBuffer.get(nv21, ySize, vSize);
//        uBuffer.get(nv21, ySize + vSize, uSize);
//
//        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);
//        byte[] imageBytes = out.toByteArray();
//        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
//    }

//    private List<RectF> detectFaces(Bitmap bitmap, int originalWidth, int originalHeight) {
//
//        if (bitmap == null || bitmap.isRecycled()) {
//            return Collections.emptyList();
//        }
//        // Preprocess input
//        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(640 * 640 * 3 * 4);
//        inputBuffer.order(ByteOrder.nativeOrder());
//        for (int y = 0; y < 640; y++) {
//            for (int x = 0; x < 640; x++) {
//                int pixel = bitmap.getPixel(x, y);
//                inputBuffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f);
//                inputBuffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);
//                inputBuffer.putFloat((pixel & 0xFF) / 255.0f);
//            }
//        }
//
//        // Run detection
//        float[][][] output = new float[1][5][8400];
//        try {
//            detectorInterpreter.run(inputBuffer, output);
//        } catch (Exception e) {
//            Log.e("yolov8Error", "Model run failed: " + e.getMessage());
//            e.printStackTrace();
//        }
//
//        Log.d("TFLite", "Original shape: " + output.length + "x" + output[0].length + "x" + output[0][0].length);
//
//// Transpose [1, 8400, 5] → [1, 5, 8400]
//        float[][][] transposedOutput = new float[1][8400][5];
//        for (int i = 0; i < 8400; i++) {
//            for (int j = 0; j < 5; j++) {
//                transposedOutput[0][i][j] = output[0][j][i];
//            }
//        }
//// Log transposed shape
//        Log.d("TFLite", "Transposed shape: " + transposedOutput.length + "x" + transposedOutput[0].length + "x" + transposedOutput[0][0].length);
//
//        // Process output
//        List<RectF> boxes = new ArrayList<>();
//        List<Float> scores = new ArrayList<>();
//
//        for (int i = 0; i < 8400; i++) {
//            float confidence = transposedOutput[0][i][4];
//            if (confidence > 0.2f) {
//
//
////                float x = transposedOutput[0][i][0];
////                float y = transposedOutput[0][i][1];
////                float w = transposedOutput[0][i][2];
////                float h = transposedOutput[0][i][3];
////
////                float x1 = x - w / 2;
////                float y1 = y - h / 2;
////                float x2 = x + w / 2;
////                float y2 = y + h / 2;
////
////                boxes.add(new RectF(x1, y1, x2, y2));
//                float x = transposedOutput[0][i][0] * 640f;
//                float y = transposedOutput[0][i][1] * 640f;
//                float w = transposedOutput[0][i][2] * 640f;
//                float h = transposedOutput[0][i][3] * 640f;
//
//                float x1 = x - w / 2;
//                float y1 = y - h / 2;
//                float x2 = x + w / 2;
//                float y2 = y + h / 2;
//
//// Optional clamping
//                x1 = Math.max(0, x1);
//                y1 = Math.max(0, y1);
//                x2 = Math.min(640, x2);
//                y2 = Math.min(640, y2);
//
//                boxes.add(new RectF(x1, y1, x2, y2));
//                scores.add(confidence);
//                Log.d("FaceDetection", "Face detected! Box: " + boxes+ ", Confidence: " + confidence);
//                Log.d("original_Dimension", "width " + originalWidth + ", Height " + originalHeight);
//            }
//        }
//
//        // Apply NMS
//        List<Integer> indices = nms(boxes, scores, 0.5f);
//
//        // Convert to original image coordinates
//        List<RectF> finalBoxes = new ArrayList<>();
//        float scaleX = (float)originalWidth/640f;
//        float scaleY = (float)originalHeight/640f;
//        float xb,yb,wb,hb;
//        for (int i : indices) {
//            RectF box = boxes.get(i);
//            xb=box.left * scaleX;
//            yb=box.top * scaleY;
//            wb=box.right * scaleX;
//            hb=box.bottom * scaleY;
//            int x11 = (int) Math.max(0, xb);
//            int y11 = (int) Math.max(0, yb);
//            int x21 = (int) Math.min(originalWidth , wb);
//            int y21 = (int) Math.min(originalHeight, hb);
//            RectF scaledBox = new RectF(
//                    x11,
//                    y11,
//                    x21,
//                    y21
//
//            );
//            finalBoxes.add(scaledBox);
//
//            // If we have a known embedding, compare faces
////            if (knownEmbedding != null) {
////                Bitmap faceBitmap = Bitmap.createBitmap(
////                        lastFrameWithBoxes,
////                        (int) scaledBox.left,
////                        (int) scaledBox.top,
////                        (int) scaledBox.width(),
////                        (int) scaledBox.height()
////                );
////
////                Matrix matrix = new Matrix();
//////                matrix.postRotate(270);
////                matrix.postScale(-1, 1);
////                Bitmap rotatedFace = Bitmap.createBitmap(faceBitmap, 0, 0,
////                        faceBitmap.getWidth(), faceBitmap.getHeight(), matrix, true);
//////                if (!imageSaved) {
//////                    saveImageToStorage(rotatedFace);
//////                    imageSaved = true;
//////                }
////
////                float[] embedding = getFaceEmbedding(rotatedFace);
////                currentCosSim = cosineSimilarity(embedding, knownEmbedding);
////
////                runOnUiThread(() -> {
////                    if (currentCosSim > 0.8) {
////                        statusLabel.setText(String.format("Known. %.2f", currentCosSim));
////                    } else {
////                        statusLabel.setText(String.format("Unknown. %.2f", currentCosSim));
////                    }
////                });
////            }
//        }
////        float displayCosSim = knownEmbedding == null ? 0 : currentCosSim;
////        overlayView.setBoxes(boxes, displayCosSim);
//
//        return finalBoxes;
//    }
private List<RectF> detectFaces(Bitmap bitmap, int originalWidth, int originalHeight) {
    // Initialize with empty list as default return value
    List<RectF> finalBoxes = new ArrayList<>();

    try {
        // Input validation
        if (bitmap == null || bitmap.isRecycled()) {
            return finalBoxes;
        }

        // Preprocess input with bounds checking
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(640 * 640 * 3 * 4);
        inputBuffer.order(ByteOrder.nativeOrder());

        for (int y = 0; y < 640; y++) {
            for (int x = 0; x < 640; x++) {
                // Add bounds checking
                if (x < bitmap.getWidth() && y < bitmap.getHeight()) {
                    int pixel = bitmap.getPixel(x, y);
                    inputBuffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f);
                    inputBuffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);
                    inputBuffer.putFloat((pixel & 0xFF) / 255.0f);
                } else {
                    // Pad with zeros if out of bounds
                    inputBuffer.putFloat(0);
                    inputBuffer.putFloat(0);
                    inputBuffer.putFloat(0);
                }
            }
        }

        // Safer model execution
        synchronized (this) {
            if (detectorInterpreter != null) {
                float[][][] output = new float[1][5][8400];
                detectorInterpreter.run(inputBuffer, output);

                Log.d("TFLite", "Original shape: " + output.length + "x" + output[0].length + "x" + output[0][0].length);

                // Transpose [1, 8400, 5] → [1, 5, 8400]
                float[][][] transposedOutput = new float[1][8400][5];
                for (int i = 0; i < 8400; i++) {
                    for (int j = 0; j < 5; j++) {
                        transposedOutput[0][i][j] = output[0][j][i];
                    }
                }
                Log.d("TFLite", "Transposed shape: " + transposedOutput.length + "x" + transposedOutput[0].length + "x" + transposedOutput[0][0].length);

                // Process output
                List<RectF> boxes = new ArrayList<>();
                List<Float> scores = new ArrayList<>();
                Log.d("TFLite", "Transposed shape: " + transposedOutput.length + "x" + transposedOutput[0].length + "x" + transposedOutput[0][0].length);
                for (int i = 0; i < 8400; i++) {
                    float confidence = transposedOutput[0][i][4];
                    if (confidence > 0.2f) {
                        float x = transposedOutput[0][i][0] * 640f;
                        float y = transposedOutput[0][i][1] * 640f;
                        float w = transposedOutput[0][i][2] * 640f;
                        float h = transposedOutput[0][i][3] * 640f;

                        float x1 = x - w / 2;
                        float y1 = y - h / 2;
                        float x2 = x + w / 2;
                        float y2 = y + h / 2;

                        // Optional clamping
                        x1 = Math.max(0, x1);
                        y1 = Math.max(0, y1);
                        x2 = Math.min(640, x2);
                        y2 = Math.min(640, y2);

                        boxes.add(new RectF(x1, y1, x2, y2));
                        scores.add(confidence);
                        Log.d("FaceDetection", "Face detected! Box: " + boxes + ", Confidence: " + confidence);
                        Log.d("original_Dimension", "width " + originalWidth + ", Height " + originalHeight);
                    }
                }

                // Apply NMS
                List<Integer> indices = nms(boxes, scores, 0.5f);

                // Convert to original image coordinates
                float scaleX = (float)originalWidth/640f;
                float scaleY = (float)originalHeight/640f;
                for (int i : indices) {
                    RectF box = boxes.get(i);
                    float xb = box.left * scaleX;
                    float yb = box.top * scaleY;
                    float wb = box.right * scaleX;
                    float hb = box.bottom * scaleY;
                    int x11 = (int) Math.max(0, xb);
                    int y11 = (int) Math.max(0, yb);
                    int x21 = (int) Math.min(originalWidth, wb);
                    int y21 = (int) Math.min(originalHeight, hb);
                    finalBoxes.add(new RectF(x11, y11, x21, y21));
                }
            }
        }
    } catch (Exception e) {
        Log.e("FaceDetection", "Detection failed", e);
    }

    return finalBoxes;
}
    private float[] getFaceEmbedding(Bitmap faceBitmap) {
        // Resize to 160x160
        Bitmap resizedFace = Bitmap.createScaledBitmap(faceBitmap, 160, 160, true);

        // Preprocess
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(160 * 160 * 3 * 4);
        inputBuffer.order(ByteOrder.nativeOrder());
        for (int y = 0; y < 160; y++) {
            for (int x = 0; x < 160; x++) {
                int pixel = resizedFace.getPixel(x, y);
                float r = ((pixel >> 16) & 0xFF) - 127.5f;
                float g = ((pixel >> 8) & 0xFF) - 127.5f;
                float b = (pixel & 0xFF) - 127.5f;

                inputBuffer.putFloat(r / 128.0f);
                inputBuffer.putFloat(g / 128.0f);
                inputBuffer.putFloat(b / 128.0f);
            }
        }

        // Run recognition
        float[][] output = new float[1][512];
        try {
            recognizerInterpreter.run(inputBuffer, output);
        } catch (Exception e) {
            Log.e("TFLiteError", "Model run failed: " + e.getMessage());
            e.printStackTrace();
        }

        // Normalize embedding
        float[] embedding = output[0];
//        float norm = (float) Math.sqrt(Arrays.stream(embedding).map(x -> x * x).sum());
//        float norm = (float) Math.sqrt(
//                IntStream.range(0, embedding.length)
//                        .mapToDouble(i -> embedding[i] * embedding[i])
//                        .sum()
//        );
        float sumOfSquares = 0f;
        for (float value : embedding) {
            sumOfSquares += value * value;
        }
        float norm = (float) Math.sqrt(sumOfSquares);
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] /= norm;
        }

        return embedding;
    }

    private List<Integer> nms(List<RectF> boxes, List<Float> scores, float iouThreshold) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < boxes.size(); i++) {
            indices.add(i);
        }

        // Sort by score
        Collections.sort(indices, (a, b) -> Float.compare(scores.get(b), scores.get(a)));

        List<Integer> selected = new ArrayList<>();
        while (!indices.isEmpty()) {
            int current = indices.remove(0);
            selected.add(current);

            List<Integer> toRemove = new ArrayList<>();
            for (int i = 0; i < indices.size(); i++) {
                int idx = indices.get(i);
                float iou = calculateIoU(boxes.get(current), boxes.get(idx));
                if (iou > iouThreshold) {
                    toRemove.add(i);
                }
            }

            // Remove from back to front to avoid index shifting
            for (int i = toRemove.size() - 1; i >= 0; i--) {
                indices.remove((int) toRemove.get(i));
            }
        }

        return selected;
    }
    private void saveImageToStorage(Bitmap bitmap) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "FACE_" + timeStamp + ".jpg";

        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File imageFile = new File(storageDir, fileName);

        try (FileOutputStream out = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

            // Show Toast safely on the main thread
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(getApplicationContext(), "Image saved: " + imageFile.getAbsolutePath(), Toast.LENGTH_SHORT).show()
            );

        } catch (IOException e) {
            Log.e("FaceDetection", "Error saving image", e);
        }
    }
    private float calculateIoU(RectF a, RectF b) {
        float intersectionArea = Math.max(0, Math.min(a.right, b.right) - Math.max(a.left, b.left)) *
                Math.max(0, Math.min(a.bottom, b.bottom) - Math.max(a.top, b.top));

        float areaA = (a.right - a.left) * (a.bottom - a.top);
        float areaB = (b.right - b.left) * (b.bottom - b.top);

        return intersectionArea / (areaA + areaB - intersectionArea);
    }

    private float cosineSimilarity(float[] a, float[] b) {
        float dotProduct = 0;
        float normA = 0;
        float normB = 0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return dotProduct / ((float) Math.sqrt(normA) * (float) Math.sqrt(normB));
    }

    private ByteBuffer loadModelFile(String modelName) throws IOException {
        AssetManager assetManager = getAssets();
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // Include all the helper methods from your original code:
    // imageProxyToBitmap, detectFaces, getFaceEmbedding, cosineSimilarity,
    // nms, calculateIoU, loadModelFile, stringToFloatArray, floatArrayToString
    // (Copy these from your original implementation)

    @Override
    protected void onDestroy() {
        super.onDestroy();

//        if (detectorInterpreter != null) detectorInterpreter.close();
        if (detectorInterpreter != null) {
            synchronized (this) {
                detectorInterpreter.close();
                detectorInterpreter = null;
            }
        }
        if (recognizerInterpreter!= null) {
            synchronized (this) {
                recognizerInterpreter.close();
                recognizerInterpreter = null;
            }
        }
//        if (recognizerInterpreter != null) recognizerInterpreter.close();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }
}
