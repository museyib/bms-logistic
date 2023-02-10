package az.inci.bmslogistic;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import az.inci.bmslogistic.model.UpdateDocLocationRequest;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class LocationService extends Service
{
    public static final String CHANNEL_ID = "location";
    static final long UPDATE_INTERVAL_IN_MILLISECONDS = 60000;
    private final LocationCallback locationCallback = new LocationCallback()
    {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult)
        {
            super.onLocationResult(locationResult);
            Location currentLocation = locationResult.getLastLocation();
            new Thread(() -> {
                if (currentLocation != null)
                {
                    sendLocation(currentLocation.getLatitude(), currentLocation.getLongitude());
                }
            }).start();
        }
    };
    Notification notification;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;

    @Override
    public void onCreate()
    {
        super.onCreate();
        initData();
    }

    private void sendLocation(double lat, double apt)
    {
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        String address = "";
        try
        {
            List<Address> addressList = geocoder.getFromLocation(lat, apt, 1);
            address = addressList.get(0).getAddressLine(0);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        String url = url("logistics", "update-location");

        UpdateDocLocationRequest request = new UpdateDocLocationRequest();
        request.setLongitude(apt);
        request.setLatitude(lat);
        request.setAddress(address);
        request.setUserId(((App) getApplication()).getConfig().getUser().getId());
        executeUpdate(url, request);
    }

    okhttp3.Response sendRequest(URL url, @Nullable Object requestBodyData)
            throws IOException
    {
        OkHttpClient httpClient = new OkHttpClient.Builder().connectTimeout(
                ((App) getApplication()).getConfig().getConnectionTimeout(), TimeUnit.SECONDS).build();

        RequestBody requestBody = RequestBody.create(new Gson().toJson(requestBodyData),
                                                     MediaType.get(
                                                             "application/json;charset=UTF-8"));
        Request request = new Request.Builder().method("POST", requestBody).url(url).build();

        return httpClient.newCall(request).execute();
    }

    public void executeUpdate(String urlString, Object requestData)
    {
        try (okhttp3.Response httpResponse = sendRequest(new URL(urlString),
                                                         requestData))
        {
            Log.i("INFO", httpResponse.toString());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public String url(String... value)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(((App) getApplication()).getConfig().getServerUrl()).append("/v2");
        for (String s : value)
        {
            sb.append("/").append(s);
        }
        return sb.toString();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        prepareForegroundNotification();
        startLocationUpdates();

        return START_STICKY;
    }

    private void startLocationUpdates()
    {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            fusedLocationProviderClient
                    .requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void prepareForegroundNotification()
    {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            Intent notificationIntent = new Intent(this, LocationService.class);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle(getText(R.string.app_name))
                    .setContentText("Location")
                    .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                    .setContentIntent(pendingIntent)
                    .setTicker(getText(R.string.app_name))
                    .build();
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
            manager.notify(1, notification);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    private void initData()
    {
        locationRequest = new LocationRequest.Builder(UPDATE_INTERVAL_IN_MILLISECONDS)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());

    }

    @Override
    public void onTaskRemoved(Intent rootIntent)
    {
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        startService(restartServiceIntent);
        super.onTaskRemoved(rootIntent);
    }
}