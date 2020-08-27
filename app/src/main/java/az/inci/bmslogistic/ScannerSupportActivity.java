package az.inci.bmslogistic;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;

import com.rscja.deviceapi.DeviceConfiguration;
import com.zebra.adc.decoder.Barcode2DWithSoft;

public abstract class ScannerSupportActivity extends AppBaseActivity {

    protected String model;
    protected boolean isContinuous=false;

    protected Barcode2DWithSoft barcode2DWithSoft;
    protected ScanTask scanTask;
    protected boolean busy =false;
    protected Barcode2DWithSoft.ScanCallback scanCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            barcode2DWithSoft = Barcode2DWithSoft.getInstance();
        } catch (Throwable e)
        {
            e.printStackTrace();
        }
        scanTask = new ScanTask(this);
        scanTask.execute();
        model= DeviceConfiguration.getModel();

        scanCallback = (i, i2, bArr) -> {
            if (bArr!=null) {
                String barcode = new String(bArr, 0, i2);
                onScanComplete(barcode);
                if (!isContinuous)
                {
                    busy=false;
                }
            }
        };
    }

    public abstract void onScanComplete(String barcode);

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode==139) {
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
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
        cancel();
    }

    public void cancel() {
        if (scanTask.getStatus() == AsyncTask.Status.FINISHED && barcode2DWithSoft != null && model.equals("C4000_6582")) {
            try {
                barcode2DWithSoft.close();
                scanTask.cancel(true);
            } catch (IllegalArgumentException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void setScanCallback() {
        if (barcode2DWithSoft != null) {
            barcode2DWithSoft.setScanCallback(scanCallback);
        }
    }

    protected synchronized void scan() {
        synchronized (this) {
            if (barcode2DWithSoft != null) {
                ScanThread thread = new ScanThread();
                thread.start();
            }
        }
    }

    protected class ScanThread extends Thread {
        public void run() {
            barcode2DWithSoft.scan();
        }
    }
}