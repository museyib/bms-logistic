package az.inci.bmslogistic;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import az.inci.bmslogistic.model.ShipDocInfo;

public class CheckDocStatusActivity extends ScannerSupportActivity
{

    Button scanCam;

    EditText trxNoEdit;
    EditText driverCodeEdit;
    EditText driverNameEdit;
    EditText vehicleCodeEdit;
    EditText statusEdit;
    EditText noteEdit;

    private String trxNo;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_doc_status);

        scanCam = findViewById(R.id.scan_cam);

        trxNoEdit = findViewById(R.id.trx_no);
        driverCodeEdit = findViewById(R.id.driver_code);
        driverNameEdit = findViewById(R.id.driver_name);
        vehicleCodeEdit = findViewById(R.id.vehicle_code);
        statusEdit = findViewById(R.id.status);
        noteEdit = findViewById(R.id.note);

        loadFooter();

        scanCam.setOnClickListener(
                view -> startActivityForResult(new Intent(this, BarcodeScannerCamera.class), 1));
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
        String statusText;
        if (docInfo != null)
        {
            trxNoEdit.setText(trxNo);
            driverCodeEdit.setText(docInfo.getDriverCode());
            driverNameEdit.setText(docInfo.getDriverName());
            vehicleCodeEdit.setText(docInfo.getVehicleCode());
            String note = docInfo.getDeliverNotes();
            noteEdit.setText(note);

            String status = docInfo.getShipStatus();

            statusText = status + ": " + docInfo.getShipStatusDescription();

        }
        else
        {
            clearFields();
            statusText = "Sənəd yükləməyə verilməyib";
        }

        statusEdit.setText(statusText);
    }

    private void clearFields()
    {
        trxNoEdit.setText("");
        driverCodeEdit.setText("");
        driverNameEdit.setText("");
        vehicleCodeEdit.setText("");
        noteEdit.setText("");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != -1)
        {
            if (data != null)
            {
                if (resultCode == 1)
                {
                    String barcode = data.getStringExtra("barcode");
                    onScanComplete(barcode);
                }
            }
        }
    }
}