package az.inci.bmslogistic.activity;

import static android.R.drawable.ic_dialog_alert;
import static android.R.drawable.ic_dialog_info;
import static android.text.TextUtils.isEmpty;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static az.inci.bmslogistic.GlobalParameters.cameraScanning;
import static az.inci.bmslogistic.LocationService.UPDATE_INTERVAL_IN_MILLISECONDS;

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
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import az.inci.bmslogistic.LocationService;
import az.inci.bmslogistic.model.Point;
import az.inci.bmslogistic.R;
import az.inci.bmslogistic.model.ConfirmDeliveryRequest;
import az.inci.bmslogistic.model.ShipDocInfo;
import az.inci.bmslogistic.model.UpdateDocLocationRequest;

public class DeliveryActivity extends ScannerSupportActivity
{
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private Button confirm;
    private EditText trxNoEdit;
    private EditText driverCodeEdit;
    private EditText driverNameEdit;
    private EditText vehicleCodeEdit;
    private EditText targetCodeEdit;
    private EditText targetNameEdit;
    private EditText noteEdit;
    private EditText deliverPersonEdit;
    private EditText confirmationCodeEdit;
    private CheckBox transitionCheck;
    private boolean filled;
    private String trxNo;
    private String note;
    private double targetLatitude;
    private double targetLongitude;
    private double currentLongitude;
    private double currentLatitude;
    private boolean transitionFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery);

        Button scanBtn = findViewById(R.id.scan_btn);
        confirm = findViewById(R.id.confirm);
        Button cancel = findViewById(R.id.cancel);

        trxNoEdit = findViewById(R.id.trx_no);
        driverCodeEdit = findViewById(R.id.driver_code);
        driverNameEdit = findViewById(R.id.driver_name);
        vehicleCodeEdit = findViewById(R.id.vehicle_code);
        targetCodeEdit = findViewById(R.id.target_code);
        targetNameEdit = findViewById(R.id.target_name);
        noteEdit = findViewById(R.id.note);
        deliverPersonEdit = findViewById(R.id.deliver_person);
        confirmationCodeEdit = findViewById(R.id.confirmation_code);
        transitionCheck = findViewById(R.id.transition_check);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback()
        {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult)
            {
                for(Location location : locationResult.getLocations())
                {
                    currentLongitude = location.getLongitude();
                    currentLatitude = location.getLatitude();

                    Geocoder geocoder = new Geocoder(DeliveryActivity.this);

                    try
                    {
                        updateDocLocation(
                                geocoder.getFromLocation(currentLatitude, currentLongitude, 1)
                                        .get(0)
                                        .getAddressLine(0));
                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        };

        createLocationRequest();

        getLocationUpdates();

        changeFillingStatus();

        scanBtn.setVisibility(cameraScanning ? VISIBLE : GONE);
        scanBtn.setOnClickListener(v -> {
            barcodeResultLauncher.launch(0);
        });

        cancel.setOnClickListener(view -> {
            clearFields();
            changeFillingStatus();
        });

        confirm.setOnClickListener(view -> {
            if (isEmpty(deliverPersonEdit.getText()))
            {
                showMessageDialog(getString(R.string.info),
                        "Təhvil alan şəxs qeyd edilməyib!",
                        ic_dialog_alert);
            }
            else if (isEmpty(confirmationCodeEdit.getText()))
            {
                showMessageDialog(getString(R.string.info),
                        "Təsdiq kodu qeyd edilməyib!",
                        ic_dialog_alert);
            }
            else
            {
                Point currentPoint = new Point(currentLongitude, currentLatitude);
                Point targetPoint = new Point(targetLongitude, targetLatitude);

                if (Point.getDistance(targetPoint, currentPoint) <= 100 ||
                        targetCodeEdit.getText().toString().equals("B0025210")) {

                    AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                    dialog.setMessage(R.string.want_to_confirm)
                            .setCancelable(false)
                            .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                                confirmDelivery();
                            })
                            .setNegativeButton(R.string.no, null);
                    dialog.show();
                } else {
                    showMessageDialog(getString(R.string.info),
                            "Hədəf nöqtəsinə kifayət qədər yaxın deyilsiniz.",
                            ic_dialog_info);
                }
            }
        });

        transitionCheck.setOnCheckedChangeListener((buttonView, isChecked) -> transitionFlag = isChecked);
        loadFooter();

        Intent intent = new Intent(this, LocationService.class);
        startService(intent);
    }

    private void changeFillingStatus()
    {
        confirm.setEnabled(filled);
        noteEdit.setEnabled(filled);
        deliverPersonEdit.setEnabled(filled);
        transitionCheck.setEnabled(filled);
    }

    private void getShipDetails(String trxNo)
    {
        showProgressDialog(true);
        new Thread(() -> {
            String url = url("logistics", "doc-info-for-delivery");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("trx-no", trxNo);
            url = addRequestParameters(url, parameters);
            ShipDocInfo docInfo = getSimpleObject(url, "GET", null, ShipDocInfo.class);

            if(docInfo != null)
                runOnUiThread(() -> publishResult(docInfo));
            else
                this.trxNo = "";
        }).start();
    }

    @Override
    public void onScanComplete(String barcode)
    {
        if (isEmpty(trxNo))
        {
            trxNo = barcode;
            getShipDetails(barcode);
        }
        else
        {
            confirmationCodeEdit.setText(barcode);
        }
    }

    private void publishResult(ShipDocInfo docInfo)
    {
        trxNoEdit.setText(trxNo);
        driverCodeEdit.setText(docInfo.getDriverCode());
        driverNameEdit.setText(docInfo.getDriverName());
        vehicleCodeEdit.setText(docInfo.getVehicleCode());

        String note = docInfo.getDeliverNotes();
        if(!note.isEmpty())
        {
            String[] split = note.split("; ");
            if(split.length > 1)
            {
                String[] split1 = split[1].split(": ");
                if(split1.length > 1) note = split1[1];
                else note = "";
            }
            else note = "";
        }
        noteEdit.setText(note.equals("null") ? "" : note);

        String targetCode = docInfo.getTargetCode();
        String targetName = docInfo.getTargetName();
        targetCodeEdit.setText(targetCode);
        targetNameEdit.setText(targetName);
        if((docInfo.getLongitude() == 0 || docInfo.getLatitude() == 0) &&
           !targetCode.equals("B0025210"))
        {
            showMessageDialog(getString(R.string.info),
                              getString(R.string.not_found_location_for_target),
                              ic_dialog_info);
            confirm.setEnabled(false);
            filled = false;
        }
        else
        {
            try
            {
                targetLongitude = docInfo.getLongitude();
                targetLatitude = docInfo.getLatitude();
            }
            catch(NumberFormatException e)
            {
                targetLongitude = 0;
                targetLatitude = 0;
            }
            filled = true;
        }
        changeFillingStatus();
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
        confirmationCodeEdit.setText("");
        transitionCheck.setChecked(false);
        trxNo = "";
    }

    private void getLocationUpdates()
    {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
           PackageManager.PERMISSION_GRANTED &&
           ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
           PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                                              new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                                      Manifest.permission.ACCESS_COARSE_LOCATION},
                                              1);
        }
        else fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback,
                                                                Looper.getMainLooper());
    }


    protected void createLocationRequest()
    {
        locationRequest = new LocationRequest.Builder(UPDATE_INTERVAL_IN_MILLISECONDS).setPriority(
                Priority.PRIORITY_HIGH_ACCURACY).build();
    }

    private void confirmDelivery()
    {
        showProgressDialog(true);
        new Thread(() -> {
            note = "İstifadəçi: " + config().getUser().getId();
            if(!noteEdit.getText().toString().isEmpty())
                note += "; Qeyd: " + noteEdit.getText().toString();
            String deliverPerson = deliverPersonEdit.getText().toString();
            String url = url("logistics", "confirm-delivery");
            ConfirmDeliveryRequest request = new ConfirmDeliveryRequest();
            request.setTrxNo(trxNo);
            request.setNote(note);
            request.setDeliverPerson(deliverPerson);
            request.setDriverCode(driverCodeEdit.getText().toString());
            request.setTransitionFlag(transitionFlag);
            request.setConfirmatioinCode(confirmationCodeEdit.getText().toString());
            executeUpdate(url, request, message -> {
                if (message.getStatusCode() == 0) {
                    clearFields();
                    changeFillingStatus();
                }
                showMessageDialog(message.getTitle(), message.getBody(), message.getIconId());
            });
        }).start();
    }

    private void updateDocLocation(String address)
    {
        new Thread(() -> {
            String url = url("logistics", "update-location");
            UpdateDocLocationRequest request = new UpdateDocLocationRequest();
            request.setLongitude(currentLongitude);
            request.setLatitude(currentLatitude);
            request.setAddress(address);
            request.setUserId(config().getUser().getId());
            executeUpdate(url, request, message -> {});
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.sending_activity_menu, menu);
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