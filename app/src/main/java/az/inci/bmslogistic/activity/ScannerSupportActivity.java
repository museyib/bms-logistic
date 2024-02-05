package az.inci.bmslogistic.activity;

import static android.text.TextUtils.isEmpty;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.device.ScanManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rscja.deviceapi.DeviceConfiguration;
import com.zebra.adc.decoder.Barcode2DWithSoft;

public abstract class ScannerSupportActivity extends AppBaseActivity
{

    public Barcode2DWithSoft barcode2DWithSoft;
    protected String model;
    protected boolean isContinuous = false;
    protected ScanManager scanManager;
    protected boolean scannerBusy = false;
    private final BroadcastReceiver urovoScanReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            byte[] barcodeArray = intent.getByteArrayExtra(ScanManager.DECODE_DATA_TAG);
            int length = intent.getIntExtra(ScanManager.BARCODE_LENGTH_TAG, 0);
            String barcode = new String(barcodeArray, 0, length);
            onScanComplete(barcode);
            scannerBusy = false;
        }
    };
    protected boolean isUrovoOpen = false;
    protected boolean isS98Open = false;
    ActivityResultLauncher<Integer> barcodeResultLauncher = barcodeResultLauncher();

    @Override
    protected void onResume()
    {
        super.onResume();
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                registerReceiver(urovoScanReceiver, filter, RECEIVER_EXPORTED);
            }
            else
                registerReceiver(urovoScanReceiver, filter);
        }
        catch(RuntimeException e)
        {
            e.printStackTrace();
        }
    }

    private void toggleUrovoScanner()
    {
        if(!isUrovoOpen)
            initUrovoScanner();

        if(!scannerBusy)
        {
            scanManager.startDecode();
            scannerBusy = true;
        }
        else
        {
            scanManager.stopDecode();
            scannerBusy = false;
        }
    }

    private void initS98Scanner()
    {
        try
        {
            barcode2DWithSoft = Barcode2DWithSoft.getInstance();

            if(barcode2DWithSoft.open(this))
                setS98ScanCallback();
        }
        catch(Throwable e)
        {
            e.printStackTrace();
        }
        model = DeviceConfiguration.getModel();
    }

    private void toggleS98Scanner()
    {
        if(!scannerBusy)
        {
            scannerBusy = true;
            isS98Open = true;
            s98Scan();
        }
        else
        {
            scannerBusy = false;
            isS98Open = false;
            barcode2DWithSoft.stopScan();
        }
    }

    private void stopScan()
    {
        stopUrovoScan();
        stopS98Scan();
    }

    private void stopUrovoScan()
    {
        isUrovoOpen = false;
        try
        {
            if(scanManager != null && scanManager.getScannerState())
            {
                scanManager.closeScanner();
                unregisterReceiver(urovoScanReceiver);
            }
        }
        catch(RuntimeException e)
        {
            e.printStackTrace();
        }
    }

    private void stopS98Scan()
    {
        if(!isS98Open && barcode2DWithSoft != null && model.equals("C4000_6582"))
            try
            {
                barcode2DWithSoft.close();
            }
            catch(IllegalArgumentException e)
            {
                e.printStackTrace();
            }
    }

    public abstract void onScanComplete(String barcode);

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if(keyCode == 139) toggleS98Scanner();
        if(keyCode == 520 || keyCode == 521 || keyCode == 522) toggleUrovoScanner();
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

    public void setS98ScanCallback()
    {
        if(barcode2DWithSoft != null)
            barcode2DWithSoft.setScanCallback((symbology, length, dataBytes) -> {
                if(dataBytes != null)
                {
                    String barcode = new String(dataBytes, 0, length);
                    onScanComplete(barcode);
                    if(!isContinuous)
                        scannerBusy = false;
                }
            });
    }

    private synchronized void s98Scan()
    {
        synchronized(this)
        {
            if(barcode2DWithSoft != null)
                new Thread(() -> barcode2DWithSoft.scan()).start();
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
                        if(intent != null) scanResult = intent.getStringExtra("barcode");

                        return scanResult;
                    }
                }, barcode -> {
                    if(!isEmpty(barcode)) onScanComplete(barcode);
                });
    }
}