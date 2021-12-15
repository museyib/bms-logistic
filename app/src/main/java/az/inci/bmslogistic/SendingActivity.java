package az.inci.bmslogistic;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.Nullable;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

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
    private String note;
    private String status;

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

        scanCam.setOnClickListener(view -> startActivityForResult(new Intent(
                this, BarcodeScannerCamera.class), 1));

        confirm.setOnClickListener(view ->
        {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage("Təsdiqləmək istəyirsinizmi?")
                    .setCancelable(false)
                    .setPositiveButton("Bəli", (dialogInterface, i) ->
                    {
                        changeDocStatus();
                        clearFields();
                        changeFillingStatus();
                    })
                    .setNegativeButton("Xeyr", null)
                    .create();
            dialog.show();
        });

        cancel.setOnClickListener(view ->
        {
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

    private void checkShipmentValidation(String barcode)
    {
        trxNo = barcode;
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
                getShipDetails(trxNo);
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

    private void getShipDetails(String trxNo)
    {
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
                runOnUiThread(() -> publishResult(result));
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

    @Override
    public void onScanComplete(String trxNo)
    {
        checkShipmentValidation(trxNo);
    }

    private void publishResult(String[] result)
    {
        if (result != null)
        {
            trxNoEdit.setText(trxNo);
            driverCodeEdit.setText(result[0]);
            driverNameEdit.setText(result[1]);
            vehicleCodeEdit.setText(result[2]);
            returnCheck.setEnabled(false);

            note = result[3];
            if (!note.isEmpty())
            {
                String[] split = note.split("; ");
                if (split.length > 1)
                {
                    String[] split1 = split[1].split(": ");
                    if (split1.length > 1)
                        note = split1[1];
                    else
                        note = "";
                }
                else
                    note = "";
            }
            noteEdit.setText(note.equals("null") ? "" : note);

            status = result[4];
            filled = true;
            changeFillingStatus();

            if (status.equals("PL"))
            {
                showMessageDialog(getString(R.string.info),
                        getString(R.string.caution_doc_have_sent_already),
                        android.R.drawable.ic_dialog_info);
            }
        }
        else
        {
            clearFields();
            changeFillingStatus();
            showMessageDialog(getString(R.string.info),
                    getString(R.string.doc_status_incorrect),
                    android.R.drawable.ic_dialog_info);
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
        status = returnMode ? "PL" : "YC";
        showProgressDialog(true);
        new Thread(() ->
        {
            note = "İstifadəçi: " + config().getUser().getId();
            if (!noteEdit.getText().toString().isEmpty())
                note += "; Qeyd: " + noteEdit.getText().toString();
            String url = url("logistics", "change-doc-status");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("trx-no", trxNo);
            parameters.put("status", status);
            parameters.put("note", note);
            parameters.put("deliver-person", "");
            url = addRequestParameters(url, parameters);
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            boolean result;
            try
            {
                result = template.postForObject(url, null, Boolean.class);
                runOnUiThread(() -> onPostExecute(result));
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
                showMessageDialog(getString(R.string.error), getString(R.string.connection_error),
                        android.R.drawable.ic_dialog_alert);
            }
            finally
            {
                runOnUiThread(() -> showProgressDialog(false));
            }
        }).start();
    }

    private void onPostExecute(boolean result)
    {
        String message;
        String title;
        int icon;
        if (result)
        {
            title = getString(R.string.info);
            message = getString(R.string.doc_confirmed_successfully);
            icon = android.R.drawable.ic_dialog_info;
        }
        else
        {
            title = getString(R.string.error);
            message = getString(R.string.server_error);
            icon = android.R.drawable.ic_dialog_alert;
        }
        showMessageDialog(title, message, icon);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem switchTo = menu.findItem(R.id.switch_to);
        switchTo.setOnMenuItemClickListener(menuItem ->
        {
            Intent intent = new Intent(this, SendingListActivity.class);
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