package az.inci.bmslogistic;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.lang.reflect.Field;

public class BarcodeScannerCamera extends AppBaseActivity {

    private SurfaceView surfaceView;
    private CameraSource cameraSource;
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private SurfaceHolder surfaceHolder;
    Camera camera;
    BarcodeDetector barcodeDetector;
    boolean isContinuous;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scanner_camera);

        isContinuous =getIntent().getBooleanExtra("serialScan", false);

        soundPool =new SoundPool(10, 3, 5);
        audioManager=(AudioManager) getSystemService(Context.AUDIO_SERVICE);
        surfaceView = findViewById(R.id.surface_view);
        surfaceHolder=surfaceView.getHolder();

        surfaceView.setOnClickListener(v -> {
            if (setCamera(cameraSource))
                camera.autoFocus(myAutoFocusCallback);
        });

        initialiseDetectorsAndSources();
    }

    public void onScanComplete(String barcode) {
        Intent intent=new Intent();
        intent.putExtra("barcode", barcode);
        setResult(1, intent);
        finish();
    }

    private boolean setCamera(CameraSource cameraSource)
    {
        Field[] declaredFields=CameraSource.class.getDeclaredFields();

        for (Field field : declaredFields)
        {
            if (field.getType() == Camera.class)
            {
                field.setAccessible(true);

                try {
                    camera=(Camera)field.get(cameraSource);
                    if (camera !=null)
                        return true;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        return false;
    }

    private void initialiseDetectorsAndSources() {

        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(1920, 1080)
                .setAutoFocusEnabled(true)
                .build();


        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(BarcodeScannerCamera.this,
                            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(surfaceView.getHolder());
                    } else {
                        ActivityCompat.requestPermissions(BarcodeScannerCamera.this, new
                                String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });


        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {
                    String barcode = barcodes.valueAt(0).displayValue;
                    onScanComplete(barcode);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        setResult(-1, new Intent());
        finish();
    }

    Camera.AutoFocusCallback myAutoFocusCallback = (arg0, arg1) -> {

    };
}