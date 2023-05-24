package az.inci.bmslogistic.activity;

import static android.text.TextUtils.isEmpty;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.device.ScanManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rscja.deviceapi.DeviceConfiguration;
import com.zebra.adc.decoder.Barcode2DWithSoft;

import az.inci.bmslogistic.ScanTask;

public abstract class ScannerSupportActivity extends AppBaseActivity
{

    protected String model;
    protected boolean isContinuous = false;

    protected ScanManager scanManager;
    public Barcode2DWithSoft barcode2DWithSoft;
    protected ScanTask scanTask;
    protected boolean busy = false;
    protected boolean isUrovoOpen = false;
    protected Barcode2DWithSoft.ScanCallback s98ScanCallback;
    ActivityResultLauncher<Integer> barcodeResultLauncher = barcodeResultLauncher();

    private final BroadcastReceiver urovoScanReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            byte[] barcodeArray = intent.getByteArrayExtra(ScanManager.DECODE_DATA_TAG);
            int length = intent.getIntExtra(ScanManager.BARCODE_LENGTH_TAG, 0);
            String barcode = new String(barcodeArray, 0, length);
            onScanComplete(barcode);
            busy = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        initS98Scanner();
    }

    private void initUrovoScanner()
    {
        try
        {
            isUrovoOpen = true;
            scanManager = new ScanManager();
            scanManager.openScanner();
            IntentFilter filter = new IntentFilter();
            filter.addAction(ScanManager.ACTION_DECODE);
            registerReceiver(urovoScanReceiver, filter);
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
        }
    }

    private void toggleUrovoScanner()
    {
        if (!isUrovoOpen)
        {
            initUrovoScanner();
        }

        if (!busy)
        {
            scanManager.startDecode();
            busy = true;
        }
        else
        {
            scanManager.stopDecode();
            busy = false;
        }
    }

    private void initS98Scanner()
    {
        try
        {
            barcode2DWithSoft = Barcode2DWithSoft.getInstance();
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
        scanTask = new ScanTask(this);
        scanTask.execute();
        model = DeviceConfiguration.getModel();

        s98ScanCallback = (i, i2, bArr) ->
        {
            if (bArr != null)
            {
                String barcode = new String(bArr, 0, i2);
                onScanComplete(barcode);
                if (!isContinuous)
                {
                    busy = false;
                }
            }
        };
    }

    private void toggleS98Scanner()
    {
        if (!busy)
        {
            busy = true;
            scan();
        }
        else
        {
            busy = false;
            barcode2DWithSoft.stopScan();
        }
    }

    private void stopScan()
    {
        isUrovoOpen = false;
        try
        {
            if (scanManager != null && scanManager.getScannerState())
            {
                scanManager.closeScanner();
                unregisterReceiver(urovoScanReceiver);
            }
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
        }

        if (scanTask.getStatus() == AsyncTask.Status.FINISHED && barcode2DWithSoft != null && model.equals("C4000_6582"))
        {
            try
            {
                barcode2DWithSoft.close();
                scanTask.cancel(true);
            }
            catch (IllegalArgumentException e)
            {
                e.printStackTrace();
            }
        }
    }

    public abstract void onScanComplete(String barcode);

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == 139)
            toggleS98Scanner();
        if (keyCode == 520 || keyCode == 521 || keyCode == 522)
            toggleUrovoScanner();
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        stopScan();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        stopScan();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        stopScan();
    }

    public void setScanCallback()
    {
        if (barcode2DWithSoft != null)
        {
            barcode2DWithSoft.setScanCallback(s98ScanCallback);
        }
    }

    private synchronized void scan()
    {
        synchronized (this)
        {
            if (barcode2DWithSoft != null)
            {
                ScanThread thread = new ScanThread();
                thread.start();
            }
        }
    }

    private class ScanThread extends Thread
    {
        public void run()
        {
            barcode2DWithSoft.scan();
        }
    }

    protected ActivityResultLauncher<Integer> barcodeResultLauncher()
    {
        return registerForActivityResult(
                new ActivityResultContract<Integer, String>()
                {
                    @NonNull
                    @Override
                    public Intent createIntent(@NonNull Context context, Integer input)
                    {
                        return new Intent(ScannerSupportActivity.this, BarcodeScannerCamera.class);
                    }

                    @Override
                    public String parseResult(int resultCode, @Nullable Intent intent)
                    {
                        String scanResult = "";
                        if (intent != null) scanResult = intent.getStringExtra("barcode");

                        return scanResult;
                    }
                }, barcode -> {
                    if (!isEmpty(barcode)) onScanComplete(barcode);
                });
    }
}