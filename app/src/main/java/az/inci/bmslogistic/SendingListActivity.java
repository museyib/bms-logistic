package az.inci.bmslogistic;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import az.inci.bmslogistic.model.ShipDocInfo;
import az.inci.bmslogistic.model.UpdateDeliveryRequest;
import az.inci.bmslogistic.model.UpdateDeliveryRequestItem;

public class SendingListActivity extends ScannerSupportActivity
{

    static final int SCAN_DRIVER_CODE = 0;
    static final int SCAN_NEW_DOC = 1;
    String driverCode;
    String barcode;
    ListView docListView;
    Button scanDriverCode;
    Button scanNewDoc;
    Button cancel;
    EditText driverCodeEditText;
    ImageButton send;
    List<String> docList;
    boolean docCreated = false;

    boolean returnMode;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sending_list_layout);

        driverCodeEditText = findViewById(R.id.driver);
        scanDriverCode = findViewById(R.id.scan_driver_code);
        scanNewDoc = findViewById(R.id.scan_new_doc);
        docListView = findViewById(R.id.ship_trx_list_view);
        send = findViewById(R.id.send);
        cancel = findViewById(R.id.cancel_button);

        docList = new ArrayList<>();

        send.setOnClickListener(v ->
        {
            if (docList.size() > 0)
            {
                changeDocStatus();
            }
        });

        docListView.setOnItemLongClickListener((parent, view, position, id) ->
        {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage(R.string.want_to_delete)
                    .setPositiveButton(R.string.delete, (dialogInterface, i) ->
                    {
                        String trxNo = (String) parent.getItemAtPosition(position);
                        docList.remove(trxNo);
                        loadData();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create();
            dialog.show();
            return true;
        });

        scanDriverCode.setOnClickListener(v ->
        {
            Intent intent = new Intent(SendingListActivity.this, BarcodeScannerCamera.class);
            startActivityForResult(intent, SCAN_DRIVER_CODE);
        });

        scanNewDoc.setOnClickListener(v ->
        {
            if (!docCreated)
            {
                showMessageDialog(getString(R.string.info),
                        getString(R.string.driver_not_defined),
                        android.R.drawable.ic_dialog_info);
                return;
            }
            Intent intent = new Intent(SendingListActivity.this, BarcodeScannerCamera.class);
            startActivityForResult(intent, SCAN_NEW_DOC);
        });

        cancel.setOnClickListener(v -> clearFields());

        loadFooter();
    }

    @Override
    public void onScanComplete(String barcode)
    {
        this.barcode = barcode;

        if (docCreated)
            getShipDetails(barcode);
        else
            setDriverCode(barcode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        barcode = data.getStringExtra("barcode");

        if (resultCode == 1 && barcode != null)
        {
            switch (requestCode)
            {
                case SCAN_DRIVER_CODE:
                    setDriverCode(barcode);
                    break;
                case SCAN_NEW_DOC:
                    getShipDetails(barcode);
                    break;
            }
        }
    }

    public void setDriverCode(String driverCode)
    {
        if (driverCode.startsWith("PER"))
        {
            showProgressDialog(true);
            new Thread(() -> {
                String url = url("personnel", "get-name");
                Map<String, String> parameters = new HashMap<>();
                parameters.put("per-code", driverCode);
                url = addRequestParameters(url, parameters);
                Log.e("URL", url);
                String perName = getSimpleObject(url, "GET", null, String.class);
                if (perName != null)
                {
                    runOnUiThread(() -> {
                        if (!perName.isEmpty())
                        {

                            docCreated = true;
                            this.driverCode = driverCode;
                            driverCodeEditText.setText(driverCode);
                            ((TextView) findViewById(R.id.driver_name)).setText(perName);
                            playSound(SOUND_SUCCESS);
                        }
                        else
                        {
                            showMessageDialog(getString(R.string.error),
                                              getString(R.string.driver_code_incorrect),
                                              android.R.drawable.ic_dialog_alert);
                            playSound(SOUND_FAIL);
                        }
                    });
                }
            }).start();
        }
        else
        {
            showMessageDialog(getString(R.string.error), getString(R.string.driver_code_incorrect),
                    android.R.drawable.ic_dialog_alert);
            playSound(SOUND_FAIL);
        }
    }

    void loadData()
    {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.list_item_layout, docList);
        docListView.setAdapter(adapter);
    }


    private void getShipDetails(String trxNo)
    {
        if (docList.contains(barcode))
            return;

        showProgressDialog(true);
        new Thread(() ->
        {
            String action = returnMode ? "doc-info-for-return" : "doc-info-for-sending";
            String url = url("logistics", action);
            Map<String, String> parameters = new HashMap<>();
            parameters.put("trx-no", trxNo);
            url = addRequestParameters(url, parameters);
            ShipDocInfo docInfo = getSimpleObject(url, "GET", null, ShipDocInfo.class);
            if (docInfo != null)
                runOnUiThread(() -> addDoc(trxNo, docInfo));
        }).start();
    }

    private void addDoc(String trxNo, ShipDocInfo docInfo)
    {
        if (!driverCode.equals(docInfo.getDriverCode()))
        {
            showMessageDialog(getString(R.string.info),
                              getString(R.string.not_shipped_for_current_driver) +
                              "\n\nYükləndiyi sürücü  və N/V nömrəsi:\n" +
                              docInfo.getDriverName() + " - " + docInfo.getVehicleCode() +
                              "\n" + docInfo.getDeliverNotes(),
                              android.R.drawable.ic_dialog_info);
            playSound(SOUND_FAIL);
            return;
        }

//            String status = docInfo.getShipStatus();
//
//            if (status.equals("PL"))
//            {
//                showMessageDialog(getString(R.string.info),
//                        getString(R.string.caution_doc_have_sent_already),
//                        android.R.drawable.ic_dialog_info);
//                return;
//            }

        playSound(SOUND_SUCCESS);
        docList.add(trxNo);
        scanDriverCode.setVisibility(View.GONE);
        loadData();
    }

    private void changeDocStatus()
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            String note = "İstifadəçi: " + config().getUser().getId();
            String url = url("logistics", "change-doc-status");
            Map<String, String> parameters = new HashMap<>();
            url = addRequestParameters(url, parameters);
            UpdateDeliveryRequest request = new UpdateDeliveryRequest();
            request.setStatus("YC");
            List<UpdateDeliveryRequestItem> requestItems = new ArrayList<>();
            for (String trxNo : docList) {
                UpdateDeliveryRequestItem requestItem = new UpdateDeliveryRequestItem();
                requestItem.setTrxNo(trxNo);
                requestItem.setNote(note);
                requestItem.setDeliverPerson("");
                requestItem.setDriverCode(driverCode);
                requestItems.add(requestItem);
            }
            request.setRequestItems(requestItems);
            executeUpdate(url, request, message -> {
                showMessageDialog(
                        message.getTitle(),
                        message.getBody(),
                        message.getIconId()
                );
                if (message.getStatusCode() == 0)
                    clearFields();
                else
                    playSound(SOUND_FAIL);
            });
        }).start();
    }

    private void clearFields()
    {
        driverCode = "";
        driverCodeEditText.setText("");
        ((TextView) findViewById(R.id.driver_name)).setText("");
        docCreated = false;
        docList.clear();
        scanDriverCode.setVisibility(View.VISIBLE);
        loadData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem switchTo = menu.findItem(R.id.switch_to);
        switchTo.setOnMenuItemClickListener(menuItem ->
        {
            Intent intent = new Intent(this, SendingActivity.class);
            startActivity(intent);
            finish();
            return true;
        });
        MenuItem itemSearch = menu.findItem(R.id.check_doc_status);
        itemSearch.setOnMenuItemClickListener(menuItem ->
        {
            Intent intent = new Intent(this, CheckDocStatusActivity.class);
            startActivity(intent);
            return true;
        });

        MenuItem itemList = menu.findItem(R.id.list);
        itemList.setOnMenuItemClickListener(menuItem ->
        {
            Intent intent = new Intent(this, DocListActivity.class);
            startActivity(intent);
            return true;
        });
        return true;
    }
}