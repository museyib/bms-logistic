package az.inci.bmslogistic;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

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

        scanCam.setOnClickListener(view -> startActivityForResult(new Intent(
                this, BarcodeScannerCamera.class), 1));
    }

    @Override
    public void onScanComplete(String barcode)
    {
        trxNo = barcode;
        if (trxNo.startsWith("ITO") || trxNo.startsWith("DLV") || trxNo.startsWith("ITD")
                || trxNo.startsWith("SIN") || trxNo.startsWith("ITI"))
        {
            showProgressDialog(true);
            new Thread(() ->
            {
                String url = url("logistics", "check-doc-status");
                Map<String, String> parameters = new HashMap<>();
                parameters.put("trx-no", barcode);
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
                finally
                {
                    runOnUiThread(() -> showProgressDialog(false));
                }
            }).start();
        }
        else
            showMessageDialog(getString(R.string.info), getString(R.string.invalid_trx_no),
                    android.R.drawable.ic_dialog_info);
    }

    private void publishResult(String[] result)
    {
        String statusText = "";
        if (result != null)
        {
            trxNoEdit.setText(trxNo);
            driverCodeEdit.setText(result[0]);
            driverNameEdit.setText(result[1]);
            vehicleCodeEdit.setText(result[2]);
            noteEdit.setText(result[3].equals("null") ? "" : result[3] + "; " + result[5]);

            String status = result[4];

            switch (status)
            {
                case "PL":
                    statusText = status + ": Bir dəfə çıxışa verilib və anbara qayıdıb";
                    break;
                case "AC":
                    statusText = status + ": Anbardan çıxıb";
                    break;
                case "YC":
                    statusText = status + ": Yoldadır";
                    break;
                case "MC":
                    statusText = status + ": Müştəriyə çatdırılıb";
                    break;
            }

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