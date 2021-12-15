package az.inci.bmslogistic;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DeliveryActivity extends ScannerSupportActivity
{

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;
    LocationRequest locationRequest;

    Button scanCam;
    Button confirm;
    Button cancel;

    EditText trxNoEdit;
    EditText driverCodeEdit;
    EditText driverNameEdit;
    EditText vehicleCodeEdit;
    EditText targetCodeEdit;
    EditText targetNameEdit;
    EditText noteEdit;
    EditText deliverPersonEdit;

    boolean filled;
    private String trxNo;
    private String note;
    private String deliverPerson;
    private double targetLatitude;
    private double targetLongitude;
    private double currentLongitude;
    private double currentLatitude;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery);

        scanCam = findViewById(R.id.scan_cam);
        confirm = findViewById(R.id.confirm);
        cancel = findViewById(R.id.cancel);

        trxNoEdit = findViewById(R.id.trx_no);
        driverCodeEdit = findViewById(R.id.driver_code);
        driverNameEdit = findViewById(R.id.driver_name);
        vehicleCodeEdit = findViewById(R.id.vehicle_code);
        targetCodeEdit = findViewById(R.id.target_code);
        targetNameEdit = findViewById(R.id.target_name);
        noteEdit = findViewById(R.id.note);
        deliverPersonEdit = findViewById(R.id.deliver_person);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback()
        {
            @Override
            public void onLocationResult(LocationResult locationResult)
            {
                if (locationResult == null)
                    return;

                for (Location location : locationResult.getLocations())
                {
                    currentLongitude = location.getLongitude();
                    currentLatitude = location.getLatitude();

                    Geocoder geocoder = new Geocoder(DeliveryActivity.this);

                    try {
                        updateDocLocation(geocoder
                                .getFromLocation(currentLatitude, currentLongitude, 1)
                                .get(0)
                                .getAddressLine(0));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        createLocationRequest();

        getLocationUpdates();

        changeFillingStatus();

        scanCam.setOnClickListener(view -> startActivityForResult(new Intent(
                this, BarcodeScannerCamera.class), 1));

        cancel.setOnClickListener(view ->
        {
            clearFields();
            changeFillingStatus();
        });

        confirm.setOnClickListener(view ->
        {
            Point currentPoint = new Point(currentLongitude, currentLatitude);
            Point targetPoint = new Point(targetLongitude, targetLatitude);

            if (currentPoint.getDistance(targetPoint) <= 100
                    || targetCodeEdit.getText().toString().equals("B0013914"))
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
            }
            else
            {
                showMessageDialog(getString(R.string.info),
                        "Hədəf nöqtəsinə kifayət qədər yaxın deyilsiniz.",
                        android.R.drawable.ic_dialog_info);
            }
        });

        loadFooter();

        Intent intent = new Intent(this, LocationService.class);
        startService(intent);
    }

    private void changeFillingStatus()
    {
        confirm.setEnabled(filled);
        cancel.setEnabled(filled);
        noteEdit.setEnabled(filled);
        deliverPersonEdit.setEnabled(filled);
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
            String url = url("logistics", "delivery");
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

            String targetCode = result[5];
            String targetName = result[6];
            targetCodeEdit.setText(targetCode);
            targetNameEdit.setText(targetName);
            if ((result[7].isEmpty() || result[8].isEmpty())
                    && !targetCode.equals("B0013914"))
            {
                showMessageDialog(getString(R.string.info),
                        getString(R.string.not_found_location_for_target),
                        android.R.drawable.ic_dialog_info);
                return;
            }
            else
            {
                try {
                    targetLongitude = Double.parseDouble(result[7]);
                    targetLatitude = Double.parseDouble(result[8]);
                }
                catch (NumberFormatException e)
                {
                    targetLongitude = 0;
                    targetLatitude = 0;
                }
            }
            filled = true;
            changeFillingStatus();
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
        targetCodeEdit.setText("");
        targetNameEdit.setText("");
        noteEdit.setText("");
        deliverPersonEdit.setText("");
    }

    private void getLocationUpdates()
    {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        else
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }


    protected void createLocationRequest()
    {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void changeDocStatus()
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            note = "İstifadəçi: " + config().getUser().getId();
            if (!noteEdit.getText().toString().isEmpty())
                note += "; Qeyd: " + noteEdit.getText().toString();
            deliverPerson = deliverPersonEdit.getText().toString();

            String url = url("logistics", "change-doc-status");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("trx-no", trxNo);
            parameters.put("status", "MC");
            parameters.put("note", note);
            parameters.put("deliver-person", deliverPerson);
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


    private void updateDocLocation(String address)
    {
        new Thread(() ->
        {
            note = "İstifadəçi: " + config().getUser().getId();
            if (!noteEdit.getText().toString().isEmpty())
                note += "; Qeyd: " + noteEdit.getText().toString();
            deliverPerson = deliverPersonEdit.getText().toString();

            String url = url("logistics", "update-location");
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("latitude", currentLatitude)
                    .queryParam("aptitude", currentLongitude)
                    .queryParam("address", address)
                    .queryParam("user-id", config().getUser().getId());
            try
            {
                template.postForObject(builder.toUriString(), null, Boolean.class);
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != -1)
        {
            if (data != null)
            {
                String barcode = data.getStringExtra("barcode");
                onScanComplete(barcode);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main_menu, menu);
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

    @Override
    protected void onStop()
    {
        super.onStop();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        getLocationUpdates();
    }
}