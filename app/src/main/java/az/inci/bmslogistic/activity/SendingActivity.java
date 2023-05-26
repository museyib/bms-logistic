package az.inci.bmslogistic.activity;

import static android.R.drawable.ic_dialog_info;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import az.inci.bmslogistic.R;
import az.inci.bmslogistic.model.ShipDocInfo;
import az.inci.bmslogistic.model.UpdateDeliveryRequest;
import az.inci.bmslogistic.model.UpdateDeliveryRequestItem;

public class SendingActivity extends ScannerSupportActivity
{

    Button scanCam;
    Button confirm;
    Button cancel;

    EditText trxNoEdit;
    EditText driverCodeEdit;
    EditText driverNameEdit;
    EditText vehicleCodeEdit;
    EditText noteEdit;
    CheckBox returnCheck;

    boolean filled;
    boolean returnMode;
    private String trxNo;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sending);

        scanCam = findViewById(R.id.scan_cam);
        confirm = findViewById(R.id.confirm);
        cancel = findViewById(R.id.cancel);
        trxNoEdit = findViewById(R.id.trx_no);
        driverCodeEdit = findViewById(R.id.driver_code);
        driverNameEdit = findViewById(R.id.driver_name);
        vehicleCodeEdit = findViewById(R.id.vehicle_code);
        noteEdit = findViewById(R.id.note);
        returnCheck = findViewById(R.id.return_check);

        loadFooter();

        changeFillingStatus();

        scanCam.setOnClickListener(v -> barcodeResultLauncher.launch(0));

        confirm.setOnClickListener(view -> {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage(R.string.want_to_confirm)
                  .setCancelable(false)
                  .setPositiveButton(R.string.yes, (dialogInterface, i) -> changeDocStatus())
                  .setNegativeButton(R.string.no, null);
            dialog.show();
        });

        cancel.setOnClickListener(view -> {
            clearFields();
            changeFillingStatus();
        });

        returnCheck.setOnCheckedChangeListener((compoundButton, b) -> returnMode = b);
    }

    private void changeFillingStatus()
    {
        confirm.setEnabled(filled);
        cancel.setEnabled(filled);
        noteEdit.setEnabled(filled);
    }

    private void getShipDetails(String trxNo)
    {
        showProgressDialog(true);
        new Thread(() -> {
            String action = returnMode ? "doc-info-for-return" : "doc-info-for-sending";
            String url = url("logistics", action);
            Map<String, String> parameters = new HashMap<>();
            parameters.put("trx-no", trxNo);
            url = addRequestParameters(url, parameters);
            ShipDocInfo docInfo = getSimpleObject(url, "GET", null, ShipDocInfo.class);

            if(docInfo != null) runOnUiThread(() -> publishResult(docInfo));
        }).start();
    }

    @Override
    public void onScanComplete(String barcode)
    {
        trxNo = barcode;
        getShipDetails(trxNo);
    }

    private void publishResult(ShipDocInfo docInfo)
    {
        trxNoEdit.setText(trxNo);
        driverCodeEdit.setText(docInfo.getDriverCode());
        driverNameEdit.setText(docInfo.getDriverName());
        vehicleCodeEdit.setText(docInfo.getVehicleCode());
        returnCheck.setEnabled(false);

        String note = docInfo.getDeliverNotes();
        if(note != null)
        {
            String[] split = note.split("; ");
            if(split.length > 1)
            {
                String[] split1 = split[1].split(": ");
                if(split1.length > 1) {note = split1[1];}
                else {note = "";}
            }
            else {note = "";}
        }
        noteEdit.setText(note);

        filled = true;
        changeFillingStatus();

        if(docInfo.getShipStatus().equals("PL"))
        {
            showMessageDialog(getString(R.string.info),
                              getString(R.string.caution_doc_have_sent_already),
                              ic_dialog_info);
        }
    }

    private void clearFields()
    {
        filled = false;
        trxNoEdit.setText("");
        driverCodeEdit.setText("");
        driverNameEdit.setText("");
        vehicleCodeEdit.setText("");
        noteEdit.setText("");
        returnCheck.setChecked(false);
        returnCheck.setEnabled(true);
    }

    private void changeDocStatus()
    {
        showProgressDialog(true);
        new Thread(() -> {
            String status = returnMode ? "PL" : "YC";
            String note = "İstifadəçi: " + config().getUser().getId();
            if(!noteEdit.getText().toString().isEmpty())
            {note += "; Qeyd: " + noteEdit.getText().toString();}
            String url = url("logistics", "change-doc-status");
            Map<String, String> parameters = new HashMap<>();
            url = addRequestParameters(url, parameters);

            UpdateDeliveryRequest request = new UpdateDeliveryRequest();
            request.setStatus(status);
            UpdateDeliveryRequestItem requestItem = new UpdateDeliveryRequestItem();
            requestItem.setTrxNo(trxNo);
            requestItem.setNote(note);
            requestItem.setDeliverPerson("");
            requestItem.setDriverCode(driverCodeEdit.getText().toString());
            request.setRequestItems(Collections.singletonList(requestItem));

            executeUpdate(url, request, message -> {
                showMessageDialog(message.getTitle(), message.getBody(), message.getIconId());
                if(message.getStatusCode() == 0)
                {
                    clearFields();
                    changeFillingStatus();
                }
                else playSound(SOUND_FAIL);
            });
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem switchTo = menu.findItem(R.id.switch_to);
        switchTo.setOnMenuItemClickListener(menuItem -> {
            Intent intent = new Intent(this, SendingListActivity.class);
            startActivity(intent);
            finish();
            return true;
        });
        MenuItem itemSearch = menu.findItem(R.id.check_doc_status);
        itemSearch.setOnMenuItemClickListener(menuItem -> {
            Intent intent = new Intent(this, CheckDocStatusActivity.class);
            startActivity(intent);
            return true;
        });

        MenuItem itemList = menu.findItem(R.id.list);
        itemList.setOnMenuItemClickListener(menuItem -> {
            Intent intent = new Intent(this, DocListActivity.class);
            startActivity(intent);
            return true;
        });
        return true;
    }
}