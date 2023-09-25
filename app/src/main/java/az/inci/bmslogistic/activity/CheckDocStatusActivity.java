package az.inci.bmslogistic.activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static az.inci.bmslogistic.GlobalParameters.cameraScanning;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import java.util.HashMap;
import java.util.Map;

import az.inci.bmslogistic.R;
import az.inci.bmslogistic.model.ShipDocInfo;

public class CheckDocStatusActivity extends ScannerSupportActivity
{

    private EditText trxNoEdit;
    private EditText driverCodeEdit;
    private EditText driverNameEdit;
    private EditText vehicleCodeEdit;
    private EditText statusEdit;
    private EditText noteEdit;
    private String trxNo;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_doc_status);

        Button scanCam = findViewById(R.id.scan_cam);
        trxNoEdit = findViewById(R.id.trx_no);
        driverCodeEdit = findViewById(R.id.driver_code);
        driverNameEdit = findViewById(R.id.driver_name);
        vehicleCodeEdit = findViewById(R.id.vehicle_code);
        statusEdit = findViewById(R.id.status);
        noteEdit = findViewById(R.id.note);

        scanCam.setVisibility(cameraScanning ? VISIBLE : GONE);
        scanCam.setOnClickListener(v -> barcodeResultLauncher.launch(0));

        loadFooter();
    }

    @Override
    public void onScanComplete(String barcode)
    {
        trxNo = barcode;
        showProgressDialog(true);
        new Thread(() -> {
            String url = url("logistics", "doc-info");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("trx-no", barcode);
            url = addRequestParameters(url, parameters);
            ShipDocInfo docInfo = getSimpleObject(url, "GET", null, ShipDocInfo.class);
            runOnUiThread(() -> publishResult(docInfo));
        }).start();
    }

    private void publishResult(ShipDocInfo docInfo)
    {
        if(docInfo != null)
        {
            trxNoEdit.setText(trxNo);
            driverCodeEdit.setText(docInfo.getDriverCode());
            driverNameEdit.setText(docInfo.getDriverName());
            vehicleCodeEdit.setText(docInfo.getVehicleCode());
            noteEdit.setText(docInfo.getDeliverNotes());

            String statusText = String.format("%s: %s", docInfo.getShipStatus(),
                                              docInfo.getShipStatusDescription());
            statusEdit.setText(statusText);

        }
        else
        {
            clearFields();
        }
    }

    private void clearFields()
    {
        trxNoEdit.setText("");
        driverCodeEdit.setText("");
        driverNameEdit.setText("");
        vehicleCodeEdit.setText("");
        noteEdit.setText("");
        statusEdit.setText("");
    }
}