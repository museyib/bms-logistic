package az.inci.bmslogistic;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;


public class ScanTask extends AsyncTask<String, Integer, Boolean> {
    WeakReference<ScannerSupportActivity> reference;

    public ScanTask(ScannerSupportActivity activity) {
        reference=new WeakReference<>(activity);
    }

    public Boolean doInBackground(String... strArr) {
        ScannerSupportActivity activity=reference.get();
        if (activity.barcode2DWithSoft != null) {
            return activity.barcode2DWithSoft.open(activity);
        }
        return false;
    }

    public void onPostExecute(Boolean bool) {
        super.onPostExecute(bool);
        if (!bool) {
            reference.get().showToastMessage("Device not found!");
        }
        else {
            reference.get().setScanCallback();
        }
    }

}
