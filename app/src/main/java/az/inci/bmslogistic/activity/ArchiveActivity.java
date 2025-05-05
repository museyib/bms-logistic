package az.inci.bmslogistic.activity;

import static android.R.drawable.ic_dialog_alert;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static az.inci.bmslogistic.GlobalParameters.cameraScanning;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import az.inci.bmslogistic.R;
import az.inci.bmslogistic.model.ArchiveDeliveryRequest;
import az.inci.bmslogistic.model.ShipDocInfo;

public class ArchiveActivity extends ScannerSupportActivity {

    private Button archive;
    private Button cancel;
    private EditText trxNoEdit;
    private EditText driverCodeEdit;
    private EditText driverNameEdit;
    private EditText vehicleCodeEdit;
    private EditText statusEdit;
    private String trxNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);

        Button scanCam = findViewById(R.id.scan_cam);
        archive = findViewById(R.id.archive);
        cancel = findViewById(R.id.cancel);
        trxNoEdit = findViewById(R.id.trx_no);
        driverCodeEdit = findViewById(R.id.driver_code);
        driverNameEdit = findViewById(R.id.driver_name);
        vehicleCodeEdit = findViewById(R.id.vehicle_code);
        statusEdit = findViewById(R.id.status);

        loadFooter();

        scanCam.setVisibility(cameraScanning ? VISIBLE : GONE);
        scanCam.setOnClickListener(v -> barcodeResultLauncher.launch(0));

        archive.setOnClickListener(v -> archiveDelivery());
        cancel.setOnClickListener(v -> clearFields());
    }

    private void disableControls(boolean disable) {
        archive.setEnabled(!disable);
        cancel.setEnabled(!disable);
    }

    @Override
    public void onScanComplete(String barcode) {
        trxNo = barcode;
        getShipDetails(trxNo);
    }

    private void getShipDetails(String trxNo) {
        showProgressDialog(true);
        new Thread(() -> {
            String url = url("logistics", "doc-info-for-archive");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("trx-no", trxNo);
            url = addRequestParameters(url, parameters);
            ShipDocInfo docInfo = getSimpleObject(url, "GET", null, ShipDocInfo.class);

            if(docInfo != null) runOnUiThread(() -> publishResult(docInfo));
        }).start();
    }

    private void publishResult(ShipDocInfo docInfo) {
        trxNoEdit.setText(trxNo);
        driverCodeEdit.setText(docInfo.getDriverCode());
        driverNameEdit.setText(docInfo.getDriverName());
        vehicleCodeEdit.setText(docInfo.getVehicleCode());
        statusEdit.setText(docInfo.getShipStatus());
        cancel.setEnabled(true);
        archive.setEnabled(docInfo.isConfirmFlag());

        if (!docInfo.isConfirmFlag()) {
            showMessageDialog(getString(R.string.info), "Bu sənədin çatdırılması təsdiqlənməyib.", ic_dialog_alert);
        }
    }

    private void clearFields() {
        trxNoEdit.setText("");
        driverCodeEdit.setText("");
        driverNameEdit.setText("");
        vehicleCodeEdit.setText("");
        statusEdit.setText("");
        cancel.setEnabled(false);
        archive.setEnabled(false);
    }

    private void archiveDelivery() {
        showProgressDialog(true);
        new Thread(() -> {
            String url = url("logistics", "archive-delivery");
            Map<String, String> parameters = new HashMap<>();
            url = addRequestParameters(url, parameters);

            ArchiveDeliveryRequest request = new ArchiveDeliveryRequest();
            request.setTrxNo(trxNo);
            request.setDriverCode(driverCodeEdit.getText().toString());
            List<ArchiveDeliveryRequest> requestList = Collections.singletonList(request);

            executeUpdate(url, requestList, message -> {
                showMessageDialog(message.getTitle(), message.getBody(), message.getIconId());
                if(message.getStatusCode() == 0) {
                    clearFields();
                }
            });
        }).start();
    }
}