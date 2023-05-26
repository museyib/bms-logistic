package az.inci.bmslogistic.activity;

import static android.Manifest.permission.CAMERA;
import static android.R.drawable.ic_dialog_alert;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Detector.Detections;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

import az.inci.bmslogistic.R;

public class BarcodeScannerCamera extends AppBaseActivity
{
    private static final int CAMERA_PERMISSION = 200;
    private CameraDevice cameraDevice;
    private SurfaceView surfaceView;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private CameraSource cameraSource;
    private CameraDevice.StateCallback stateCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    public void onScanComplete(String barcode)
    {
        Intent intent = new Intent();
        intent.putExtra("barcode", barcode);
        setResult(1, intent);
        finish();
    }

    protected void startBackgroundThread()
    {
        backgroundThread = new HandlerThread("Camera Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    protected void stopBackgroundThread()
    {
        backgroundThread.quitSafely();
        try
        {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        }
        catch(InterruptedException e)
        {
            showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert);
        }
    }

    private void openCamera()
    {
        setContentView(R.layout.activity_barcode_scanner_camera);
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try
        {
            String cameraId = manager.getCameraIdList()[0];
            if(checkCameraPermission())
            {
                startCameraSource();
                manager.openCamera(cameraId, stateCallback, backgroundHandler);
                startBackgroundThread();
            }
            else
                requestCameraPermission();
        }
        catch(CameraAccessException e)
        {
            showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert);
        }
    }

    private void closeCamera()
    {
        if(cameraDevice != null)
        {
            cameraDevice.close();
            cameraDevice = null;
            stopBackgroundThread();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        openCamera();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        closeCamera();
    }

    private void startCameraSource()
    {
        surfaceView = findViewById(R.id.surface_view);
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();

        CameraSource.Builder sourceBuilder = new CameraSource.Builder(this, barcodeDetector);

        cameraSource = sourceBuilder.setRequestedPreviewSize(1920, 1080)
                                    .setAutoFocusEnabled(true)
                                    .build();
        Callback surfaceCallback = new Callback()
        {
            @Override
            public void surfaceCreated(SurfaceHolder holder)
            {
                try
                {
                    if(checkCameraPermission())
                        cameraSource.start(surfaceView.getHolder());
                    else
                        requestCameraPermission();
                }
                catch(IOException e)
                {
                    showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
            {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder)
            {
                cameraSource.stop();
            }
        };

        stateCallback = new CameraDevice.StateCallback()
        {
            @Override
            public void onOpened(CameraDevice camera)
            {
                cameraDevice = camera;
            }

            @Override
            public void onDisconnected(CameraDevice camera)
            {
                cameraDevice.close();
            }

            @Override
            public void onError(CameraDevice camera, int error)
            {
                cameraDevice.close();
                cameraDevice = null;
            }
        };
        surfaceView.getHolder().addCallback(surfaceCallback);


        barcodeDetector.setProcessor(new Detector.Processor<Barcode>()
        {
            @Override
            public void release()
            {
            }

            @Override
            public void receiveDetections(@NonNull Detections<Barcode> detections)
            {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if(barcodes.size() != 0)
                {
                    String barcode = barcodes.valueAt(0).displayValue;
                    onScanComplete(barcode);
                }
            }
        });
    }

    private boolean checkCameraPermission()
    {
        return ActivityCompat.checkSelfPermission(this, CAMERA) == PERMISSION_GRANTED;
    }

    private void requestCameraPermission()
    {
        ActivityCompat.requestPermissions(this, new String[]{CAMERA}, CAMERA_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == CAMERA_PERMISSION)
        {
            if(grantResults[0] == PERMISSION_DENIED)
                finish();
            else
                openCamera();
        }
    }
}