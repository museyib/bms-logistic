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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LocationService extends Service
{
    public static final String CHANNEL_ID = "location";
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 60000;
    private final LocationCallback locationCallback = new LocationCallback()
    {
        @Override
        public void onLocationResult(LocationResult locationResult)
        {
            super.onLocationResult(locationResult);
            Location currentLocation = locationResult.getLastLocation();
            new Thread(() -> sendLocation(currentLocation.getLatitude(), currentLocation.getLongitude())).start();
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
        Map<String, String> parameters = new HashMap<>();
        parameters.put("latitude", String.valueOf(lat));
        parameters.put("aptitude", String.valueOf(apt));
        parameters.put("address", address);
        parameters.put("user-id", ((App) getApplication()).getConfig().getUser().getId());
        url = addRequestParameters(url, parameters);
        RestTemplate template = new RestTemplate();
        ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                .setConnectTimeout(((App) getApplication()).getConfig().getConnectionTimeout() * 1000);
        template.getMessageConverters().add(new StringHttpMessageConverter());
        try
        {
            template.postForObject(url, null, String.class);
        }
        catch (RuntimeException ex)
        {
            ex.printStackTrace();
        }
    }

    public String url(String... value)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(((App) getApplication()).getConfig().getServerUrl());
        for (String s : value)
        {
            sb.append("/").append(s);
        }
        return sb.toString();
    }

    public String addRequestParameters(String url, Map<String, String> requestParameters)
    {
        StringBuilder builder = new StringBuilder(url);
        builder.append("?");

        for (Map.Entry<String, String> entry : requestParameters.entrySet())
        {
            builder.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue())
                    .append("&");
        }

        builder.delete(builder.length() - 1, builder.length());
        return builder.toString();
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
                    PendingIntent.getActivity(this, 0, notificationIntent, 0);
            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle(getText(R.string.app_name))
                    .setContentText("Location")
                    .setSmallIcon(R.mipmap.ic_launcher)
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
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

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