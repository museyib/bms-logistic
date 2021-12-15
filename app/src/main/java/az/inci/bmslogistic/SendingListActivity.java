package az.inci.bmslogistic;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private String note;

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
                        loadTrx();
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
        {
            checkShipmentValidation(barcode);
        }
        else
        {
            setDriverCode(barcode);

            docCreated = true;
        }
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
                    checkShipmentValidation(barcode);
                    break;
            }
        }
    }

    public void setDriverCode(String driverCode)
    {
        if (driverCode.startsWith("PER"))
        {
            this.driverCode = driverCode;
            driverCodeEditText.setText(driverCode);
            docCreated = true;
            playSound(SOUND_SUCCESS);
        }
        else
        {
            showMessageDialog(getString(R.string.error), getString(R.string.driver_code_incorrect),
                    android.R.drawable.ic_dialog_alert);
            playSound(SOUND_FAIL);
        }
    }

    void loadTrx()
    {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.list_item_layout, docList);
        docListView.setAdapter(adapter);
    }

    private void checkShipmentValidation(String trxNo)
    {
        if (docList.contains(barcode))
            return;

        showProgressDialog(true);
        new Thread(() -> {
            String url = url("doc", "shipment-is-valid");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("trx-no", trxNo);
            url = addRequestParameters(url, parameters);
            RestTemplate template = new RestTemplate();
            template.getMessageConverters().add(new StringHttpMessageConverter());
            boolean result;
            try
            {
                result = template.getForObject(url, Boolean.class);
            }
            catch (RuntimeException e)
            {
                e.printStackTrace();
                runOnUiThread(() -> showMessageDialog(getString(R.string.error),
                        getString(R.string.connection_error),
                        android.R.drawable.ic_dialog_alert));
                playSound(SOUND_FAIL);
                return;
            }
            if (result) {
                checkShipping(trxNo);
                playSound(SOUND_SUCCESS);
            } else {
                runOnUiThread(() -> {
                    showMessageDialog(getString(R.string.error),
                            getString(R.string.not_valid_doc_for_shipping),
                            android.R.drawable.ic_dialog_alert);
                    showProgressDialog(false);
                });
                playSound(SOUND_FAIL);
            }
        }).start();
    }

    private void checkShipping(String trxNo)
    {
        showProgressDialog(true);
        new Thread(() -> {
            String url = url("trx", "shipped");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("trx-no", trxNo);
            url = addRequestParameters(url, parameters);
            RestTemplate template = new RestTemplate();
            template.getMessageConverters().add(new StringHttpMessageConverter());
            boolean result;
            try
            {
                result = template.getForObject(url, Boolean.class);
            }
            catch (RuntimeException e)
            {
                e.printStackTrace();
                runOnUiThread(() -> {
                    showMessageDialog(getString(R.string.error),
                            getString(R.string.connection_error),
                            android.R.drawable.ic_dialog_alert);
                    showProgressDialog(false);
                });
                playSound(SOUND_FAIL);
                return;
            }

            if (result) {
                getShipDetails(trxNo);
                playSound(SOUND_SUCCESS);
            } else {
                runOnUiThread(() -> {
                    showMessageDialog(getString(R.string.error),
                            getString(R.string.not_shipped_for_current_driver),
                            android.R.drawable.ic_dialog_alert);
                    showProgressDialog(false);
                });
                playSound(SOUND_FAIL);
            }
        }).start();
    }


    private void getShipDetails(String trxNo)
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            String action = returnMode ? "return" : "sending";
            String url = url("logistics", action);
            Map<String, String> parameters = new HashMap<>();
            parameters.put("trx-no", trxNo);
            url = addRequestParameters(url, parameters);
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            String[] result;
            try
            {
                result = template.getForObject(url, String[].class);
                runOnUiThread(() -> addDoc(trxNo, result));
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
                runOnUiThread(() ->
                        showMessageDialog(getString(R.string.error),
                                getString(R.string.connection_error),
                                android.R.drawable.ic_dialog_alert)
                );
            }
            finally {
                runOnUiThread(() -> showProgressDialog(false));
            }
        }).start();
    }

    private void addDoc(String trxNo, String[] result)
    {
        if (result != null)
        {
            if (!driverCode.equals(result[0]))
            {
                showMessageDialog(getString(R.string.info),
                        getString(R.string.not_shipped_for_current_driver)
                                + "\n\nYükləndiyi sürücü  və N/V nömrəsi:\n"
                        + result[0] + " - " + result[1] + "\n" + result[2],
                        android.R.drawable.ic_dialog_info);
                return;
            }

            String status = result[4];

            if (status.equals("PL"))
            {
                showMessageDialog(getString(R.string.info),
                        getString(R.string.caution_doc_have_sent_already),
                        android.R.drawable.ic_dialog_info);
                return;
            }

            docList.add(trxNo);
            scanDriverCode.setVisibility(View.GONE);
            loadTrx();
        }
        else
        {
            showMessageDialog(getString(R.string.info),
                    getString(R.string.doc_status_incorrect),
                    android.R.drawable.ic_dialog_info);
        }
    }

    private void changeDocStatus()
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            for (String trxNo : docList) {
                note = "İstifadəçi: " + config().getUser().getId();
                String url = url("logistics", "change-doc-status");
                Map<String, String> parameters = new HashMap<>();
                parameters.put("trx-no", trxNo);
                parameters.put("status", "YC");
                parameters.put("note", note);
                parameters.put("deliver-person", "");
                url = addRequestParameters(url, parameters);
                RestTemplate template = new RestTemplate();
                ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                        .setConnectTimeout(config().getConnectionTimeout() * 1000);
                template.getMessageConverters().add(new StringHttpMessageConverter());
                boolean result;
                try {
                    result = template.postForObject(url, null, Boolean.class);
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                    runOnUiThread(() -> {
                        showMessageDialog(getString(R.string.error), getString(R.string.connection_error),
                                android.R.drawable.ic_dialog_alert);
                        showProgressDialog(false);
                    });
                    break;
                }
                if (!result) {
                    runOnUiThread(() -> onPostExecute(false));
                    break;
                }
            }
            runOnUiThread(() -> onPostExecute(true));
        }).start();
    }

    private void onPostExecute(boolean result)
    {
        showProgressDialog(false);
        String message;
        String title;
        int icon;
        if (result)
        {
            title = getString(R.string.info);
            message = getString(R.string.docs_confirmed_successfully);
            icon = android.R.drawable.ic_dialog_info;

            clearFields();
        }
        else
        {
            title = getString(R.string.error);
            message = getString(R.string.server_error);
            icon = android.R.drawable.ic_dialog_alert;
        }
        showMessageDialog(title, message, icon);
    }

    private void clearFields()
    {
        driverCode = "";
        driverCodeEditText.setText("");
        docCreated = false;
        docList.clear();
        scanDriverCode.setVisibility(View.VISIBLE);
        loadTrx();
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