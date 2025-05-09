package az.inci.bmslogistic.activity;

import static android.Manifest.permission.READ_MEDIA_AUDIO;
import static android.Manifest.permission.READ_MEDIA_IMAGES;
import static android.Manifest.permission.READ_MEDIA_VIDEO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.R.drawable.ic_dialog_alert;
import static android.R.drawable.ic_dialog_info;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.os.Build.VERSION.SDK_INT;
import static android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION;
import static az.inci.bmslogistic.GlobalParameters.apiVersion;
import static az.inci.bmslogistic.GlobalParameters.cameraScanning;
import static az.inci.bmslogistic.GlobalParameters.connectionTimeout;
import static az.inci.bmslogistic.GlobalParameters.serviceUrl;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import az.inci.bmslogistic.AppConfig;
import az.inci.bmslogistic.R;
import az.inci.bmslogistic.model.User;
import az.inci.bmslogistic.model.LoginRequest;

public class MainActivity extends AppBaseActivity {

    String id;
    String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        enableStorageAccess();

        id = preferences.getString("last_login_id", "");
        password = preferences.getString("last_login_password", "");
        Button sendButton = findViewById(R.id.sending);
        Button deliveryButton = findViewById(R.id.delivery);
        Button archiveButton = findViewById(R.id.archive);

        sendButton.setOnClickListener(v -> openSending());
        deliveryButton.setOnClickListener(v -> openDelivery());
        archiveButton.setOnClickListener(v -> openArchive());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConfig();
    }

    private void loadConfig() {
        Properties properties = new Properties();
        try {
            properties.load(getAssets().open("app.properties"));
        }
        catch(IOException e) {
            apiVersion = "v4";
        }
        serviceUrl = preferences.getString("service_url", "http://185.129.0.46:8022");
        connectionTimeout = Integer.parseInt(preferences.getString("connection_timeout", "5"));
        cameraScanning = preferences.getBoolean("camera_scanning", false);
        apiVersion = properties.getProperty("app.api-version");
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem itemSearch = menu.findItem(R.id.check_doc_status);
        itemSearch.setOnMenuItemClickListener(menuItem -> {
            Intent intent = new Intent(this, CheckDocStatusActivity.class);
            startActivity(intent);
            return true;
        });

        MenuItem itemNotConfirmedDocList = menu.findItem(R.id.not_confirmed_doc_list);
        itemNotConfirmedDocList.setOnMenuItemClickListener(menuItem -> {
            Intent intent = new Intent(this, NotConfirmedDocListActivity.class);
            startActivity(intent);
            return true;
        });

        MenuItem itemWaitingDocList = menu.findItem(R.id.waiting_doc_list);
        itemWaitingDocList.setOnMenuItemClickListener(menuItem -> {
            Intent intent = new Intent(this, WaitingDocListActivity.class);
            startActivity(intent);
            return true;
        });

        MenuItem itemSettings = menu.findItem(R.id.settings);
        itemSettings.setOnMenuItemClickListener(item1 ->
        {
            startActivity(new Intent(MainActivity.this,
                    SettingsActivity.class));
            return true;
        });

        MenuItem itemUpdate = menu.findItem(R.id.update);
        itemUpdate.setOnMenuItemClickListener(item1 ->
        {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.update_version)
                    .setMessage(R.string.want_to_update)
                    .setNegativeButton(R.string.yes,
                            (dialogInterface, i) -> checkForNewVersion())
                    .setPositiveButton(R.string.no, null)
                    .create();

            dialog.show();
            return true;
        });
        return true;
    }

    protected void enableStorageAccess() {
        String[] permissions;
        if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    READ_MEDIA_AUDIO,
                    READ_MEDIA_IMAGES,
                    READ_MEDIA_VIDEO
            };
        } else
            permissions = new String[]{WRITE_EXTERNAL_STORAGE};
        if (!permissionsGranted(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, 1);
            Intent intent;
            if (SDK_INT >= Build.VERSION_CODES.R) {
                intent = new Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }
    }

    private boolean permissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) == PERMISSION_DENIED)
                return false;
        }

        return true;
    }

    public void openSending() {
        showLoginDialog(AppConfig.SEND_MODE);
    }

    public void openDelivery() {
        showLoginDialog(AppConfig.DELIVERY_MODE);
    }

    public void openArchive() {
        showLoginDialog(AppConfig.ARCHIVE_MODE);
    }

    private void showLoginDialog(int mode) {
        this.mode = mode;
        View view = getLayoutInflater().inflate(R.layout.login_page,
                findViewById(android.R.id.content), false);

        EditText idEdit = view.findViewById(R.id.id_edit);
        EditText passwordEdit = view.findViewById(R.id.password_edit);
        CheckBox fromServerCheck = view.findViewById(R.id.from_server_check);

        AtomicBoolean loginViaServer = new AtomicBoolean(false);
        fromServerCheck.setOnCheckedChangeListener(
                (buttonView, isChecked) -> loginViaServer.set(isChecked));

        idEdit.setText(id);
        idEdit.selectAll();
        passwordEdit.setText(password);

        AlertDialog loginDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.enter)
                .setView(view)
                .setPositiveButton(R.string.enter, (dialog, which) ->
                {
                    id = idEdit.getText().toString().toUpperCase();
                    password = passwordEdit.getText().toString();

                    if (id.isEmpty() || password.isEmpty()) {
                        showToastMessage(getString(R.string.username_or_password_not_entered));
                        showLoginDialog(mode);
                        playSound(SOUND_FAIL);
                    } else {
                        loginViaServer();

                        dialog.dismiss();
                    }
                }).create();

        Objects.requireNonNull(loginDialog.getWindow())
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        loginDialog.show();
    }

    private void attemptLogin(User user) {
        preferences.edit().putString("last_login_id", id).apply();
        preferences.edit().putString("last_login_password", password).apply();
        switch (mode) {
            case AppConfig.SEND_MODE:
                View view = getLayoutInflater().inflate(R.layout.select_sending_mode_layout,
                        findViewById(android.R.id.content),
                        false);
                RadioButton singleButton = view.findViewById(R.id.single_mode);
                RadioButton multipleButton = view.findViewById(R.id.multiple_mode);

                AlertDialog selectModeDialog = new AlertDialog.Builder(this)
                        .setView(view)
                        .setTitle("Rejim seç")
                        .setPositiveButton("OK", (dialog, which) -> {
                            Class<?> aClass = null;
                            if (singleButton.isChecked())
                                aClass = SendingActivity.class;
                            else if (multipleButton.isChecked())
                                aClass = SendingListActivity.class;

                            if (aClass != null) {
                                Intent intent = new Intent(MainActivity.this, aClass);
                                startActivity(intent);
                            }
                        }).create();
                selectModeDialog.show();
                break;
            case AppConfig.DELIVERY_MODE:
                Intent deliveryIntent = new Intent(MainActivity.this, DeliveryActivity.class);
                startActivity(deliveryIntent);
                break;
            case AppConfig.ARCHIVE_MODE:
                if (user.isArchiveFlag()) {
                    Intent archiveIntent = new Intent(MainActivity.this, ArchiveActivity.class);
                    startActivity(archiveIntent);
                }
                else
                    showMessageDialog(getString(R.string.error), getString(R.string.not_allowed), ic_dialog_alert);
                break;
        }
    }

    private void loginViaServer() {
        showProgressDialog(true);
        new Thread(() ->
        {
            String url = url("user", "login");
            LoginRequest request = new LoginRequest();
            request.setUserId(id);
            request.setPassword(password);
            User user = getSimpleObject(url, "POST", request, User.class);
            if (user != null) {
                runOnUiThread(() -> {
                    user.setId(user.getId().toUpperCase());
                    loadUserInfo(user, true);
                    attemptLogin(user);
                });
            }
        }).start();
    }

    private void checkForNewVersion()
    {
        showProgressDialog(true);

        new Thread(() -> {
            int version;
            try
            {
                version = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            }
            catch(PackageManager.NameNotFoundException e)
            {
                version = 0;
            }
            String url = url("app-version", "check");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("app-name", "BMSLogistic");
            parameters.put("current-version", String.valueOf(version));
            url = addRequestParameters(url, parameters);
            Boolean newVersionAvailable = getSimpleObject(url, "GET", null, Boolean.class);
            if (newVersionAvailable != null)
                runOnUiThread(() -> {
                    if(newVersionAvailable)
                        getApkFile();
                    else
                        showMessageDialog(getString(R.string.info), getString(R.string.no_new_version),
                                ic_dialog_info);
                });
        }).start();
    }

    private void getApkFile()
    {
        showProgressDialog(true);
        new Thread(() -> {
            String url = url("download");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("file-name", "BMSLogistic");
            url = addRequestParameters(url, parameters);
            try
            {
                String bytes = getSimpleObject(url, "GET", null, String.class);
                if(bytes != null)
                {
                    byte[] fileBytes = android.util.Base64.decode(bytes, Base64.DEFAULT);
                    if (fileBytes != null)
                        runOnUiThread(() -> installApp(fileBytes));
                }
            }
            catch(RuntimeException e)
            {
                runOnUiThread(() -> showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert));
            }
        }).start();
    }

    private void installApp(byte[] bytes)
    {
        File file = new File(
                Environment.getExternalStorageDirectory().getPath() + "/BMSLogistic.apk");
        if(!file.exists())
        {
            try
            {
                boolean newFile = file.createNewFile();
                if(!newFile)
                {
                    showMessageDialog(getString(R.string.info),
                            getString(R.string.error_occurred),
                            ic_dialog_info);
                    return;
                }
            }
            catch(IOException e)
            {
                showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert);
                return;
            }
        }

        try(FileOutputStream stream = new FileOutputStream(file))
        {
            stream.write(bytes);
        }
        catch(Exception e)
        {
            showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert);
            return;
        }

        Intent installIntent;
        Uri uri;
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
        {
            installIntent = new Intent(Intent.ACTION_VIEW);
            uri = Uri.fromFile(file);
            installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
        }
        else
        {
            installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            uri = FileProvider.getUriForFile(this, "az.inci.bmslogistic.provider",
                    file);
            installIntent.setData(uri);
            installIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        startActivity(installIntent);
    }
}