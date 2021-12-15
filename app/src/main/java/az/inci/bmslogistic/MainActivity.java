package az.inci.bmslogistic;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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

import com.google.gson.Gson;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppBaseActivity
{

    String id;
    String password;
    String serverUrl;
    int connectionTimeout;
    private String result;
    private byte[] fileBytes;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        enableStorageAccess();
        loadConfig();
        String[] lastLogin = dbHelper.getLastLogin();
        id = lastLogin[0];
        password = lastLogin[1];
    }

    private void loadConfig()
    {
        serverUrl = dbHelper.getParameter("serverUrl");
        if (serverUrl.isEmpty())
            serverUrl = config().getServerUrl();
        config().setServerUrl(serverUrl);

        connectionTimeout = dbHelper.getParameter("connectionTimeout").isEmpty() ? 0 :
                Integer.parseInt(dbHelper.getParameter("connectionTimeout"));
        if (connectionTimeout == 0)
            connectionTimeout = config().getConnectionTimeout();
        config().setConnectionTimeout(connectionTimeout);
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        MenuItem itemSettings = menu.findItem(R.id.settings);
        itemSettings.setOnMenuItemClickListener(item1 ->
        {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        });

        MenuItem itemUpdate = menu.findItem(R.id.update);
        itemUpdate.setOnMenuItemClickListener(item1 ->
        {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Proqram versiyasını yenilə")
                    .setMessage("Dəyişiklikdən asılı olaraq məlumatlar silinə bilər. Yeniləmək istəyirsinizmi?")
                    .setNegativeButton("Bəli", (dialogInterface, i) -> checkForNewVersion())
                    .setPositiveButton("Xeyr", null)
                    .create();

            dialog.show();
            return true;
        });
        return true;
    }

    protected void enableStorageAccess()
    {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    public void openSending(View view)
    {
        showLoginDialog(AppConfig.SEND_MODE);
    }

    public void openDelivery(View view)
    {
        showLoginDialog(AppConfig.DLV_MODE);
    }

    private void showLoginDialog(int mode)
    {
        this.mode = mode;
        View view = getLayoutInflater().inflate(R.layout.login_page,
                findViewById(android.R.id.content), false);

        EditText idEdit = view.findViewById(R.id.id_edit);
        EditText passwordEdit = view.findViewById(R.id.password_edit);
        CheckBox fromServerCheck = view.findViewById(R.id.from_server_check);

        AtomicBoolean loginViaServer = new AtomicBoolean(false);
        fromServerCheck.setOnCheckedChangeListener((buttonView, isChecked) -> loginViaServer.set(isChecked));

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

                    if (id.isEmpty() || password.isEmpty())
                    {
                        showToastMessage(getString(R.string.username_or_password_not_entered));
                        showLoginDialog(mode);
                        playSound(SOUND_FAIL);
                    }
                    else
                    {
                        User user = dbHelper.getUser(id);
                        if (user == null || loginViaServer.get())
                        {
                            loginViaServer();
                        }
                        else
                        {
                            loadUserInfo(user, false);
                            attemptLogin(user);
                        }

                        dialog.dismiss();
                    }
                }).create();

        Objects.requireNonNull(loginDialog.getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        loginDialog.show();
    }

    private void attemptLogin(User user)
    {
        if (!user.getPassword().equals(password))
        {
            loginViaServer();
        }
        else
        {
            dbHelper.updateLastLogin(id, password);
            switch (mode)
            {
                case AppConfig.SEND_MODE:
                    View view = getLayoutInflater().inflate(R.layout.select_sending_mode_layout,
                            findViewById(android.R.id.content), false);
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
                case AppConfig.DLV_MODE:
                    Intent intent = new Intent(MainActivity.this, DeliveryActivity.class);
                    startActivity(intent);
                    break;
            }
        }
    }

    private void loginViaServer()
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            String url = url("user", "login");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("id", id);
            parameters.put("password", password);
            url = addRequestParameters(url, parameters);
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            try
            {
                result = template.postForObject(url, null, String.class);
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
            }
            runOnUiThread(() ->
            {
                showProgressDialog(false);
                if (result == null)
                {
                    showMessageDialog(getString(R.string.error),
                            getString(R.string.connection_error),
                            android.R.drawable.ic_dialog_alert);
                    playSound(SOUND_FAIL);
                }
                else
                {
                    Gson gson = new Gson();
                    User user = gson.fromJson(result, User.class);
                    if (user.getId() == null)
                    {
                        showMessageDialog(getString(R.string.error),
                                getString(R.string.username_or_password_incorrect),
                                android.R.drawable.ic_dialog_alert);
                        playSound(SOUND_FAIL);
                    }
                    else
                    {
                        user.setId(user.getId().toUpperCase());
                        loadUserInfo(user, true);
                        attemptLogin(user);
                    }
                }
            });
        }).start();
    }

    private void checkForNewVersion()
    {
        showProgressDialog(true);
        new Thread(() ->
        {
            String url = url("download");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("file-name", "BMSLogistic");
            url = addRequestParameters(url, parameters);
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            try
            {
                fileBytes = template.getForObject(url, byte[].class);
            }
            catch (RuntimeException ex)
            {
                ex.printStackTrace();
            }
            runOnUiThread(() ->
            {
                showProgressDialog(false);
                updateVersion(fileBytes);
            });
        }).start();

    }

    private void updateVersion(byte[] result)
    {
        if (result == null)
        {
            showMessageDialog(getString(R.string.info),
                    getString(R.string.no_new_version),
                    android.R.drawable.ic_dialog_info);
            return;
        }
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/BMSLogistic.apk");
        if (!file.exists())
        {
            try
            {
                boolean newFile = file.createNewFile();
                if (!newFile)
                {
                    showMessageDialog(getString(R.string.info),
                            getString(R.string.error_occurred),
                            android.R.drawable.ic_dialog_info);
                    return;
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        FileOutputStream stream;
        try
        {
            stream = new FileOutputStream(file);
            stream.write(result);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        PackageManager pm = getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(file.getAbsolutePath(), 0);
        int version = 0;
        try
        {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }
        if (file.length() > 0 && info != null && info.versionCode > version)
        {

            Intent installIntent;
            Uri uri;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            {
                installIntent = new Intent(Intent.ACTION_VIEW);
                uri = Uri.fromFile(file);
                installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
            }
            else
            {
                installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file);
                installIntent.setData(uri);
                installIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            startActivity(installIntent);
        }
        else
        {
            showMessageDialog(getString(R.string.info),
                    getString(R.string.no_new_version),
                    android.R.drawable.ic_dialog_info);
        }
    }
}