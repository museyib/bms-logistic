package az.inci.bmslogistic;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

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
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppBaseActivity {

    String id;
    String password;
    String serverUrl;
    int connectionTimeout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        enableStorageAccess();
        loadConfig();
        String[] lastLogin = dbHelper.getLastLogin();
        id = lastLogin[0];
        password = lastLogin[1];
    }

    private void loadConfig() {
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

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        MenuItem itemSettings = menu.findItem(R.id.settings);
        itemSettings.setOnMenuItemClickListener(item1 -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        });

        MenuItem itemUpdate = menu.findItem(R.id.update);
        itemUpdate.setOnMenuItemClickListener(item1 -> {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Proqram versiyasını yenilə")
                    .setMessage("Dəyişiklikdən asılı olaraq məlumatlar silinə bilər. Yeniləmək istəyirsinizmi?")
                    .setNegativeButton("Bəli", (dialogInterface, i) -> {
                        String url = url("download");
                        Map<String, String> parameters = new HashMap<>();
                        parameters.put("file-name", "BMSLogistic");
                        url = addRequestParameters(url, parameters);
                        new Updater(this).execute(url);
                    })
                    .setPositiveButton("Xeyr", null)
                    .create();

            dialog.show();
            return true;
        });
        return true;
    }

    protected void enableStorageAccess() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    public void openSending(View view) {
        showLoginDialog(AppConfig.SEND_MODE);
    }

    public void openDelivery(View view) {
        showLoginDialog(AppConfig.DLV_MODE);
    }

    private void showLoginDialog(int mode) {
        this.mode = mode;
        View view = getLayoutInflater().inflate(R.layout.login_page,
                findViewById(android.R.id.content), false);

        EditText idEdit = view.findViewById(R.id.id_edit);
        EditText passwordEdit = view.findViewById(R.id.password_edit);

        idEdit.setText(id);
        idEdit.selectAll();
        passwordEdit.setText(password);

        AlertDialog loginDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.enter)
                .setView(view)
                .setPositiveButton(R.string.enter, (dialog, which) -> {
                    id = idEdit.getText().toString().toUpperCase();
                    password = passwordEdit.getText().toString();

                    if (id.isEmpty() || password.isEmpty()) {
                        showToastMessage(getString(R.string.username_or_password_not_entered));
                        showLoginDialog(mode);
                        playSound(SOUND_FAIL);
                    } else {
                        User user = dbHelper.getUser(id);
                        if (user != null) {
                            loadUserInfo(user, false);
                            attemptLogin(user);
                        } else {
                            String url = url("user", "login");
                            Map<String, String> parameters = new HashMap<>();
                            parameters.put("id", id);
                            parameters.put("password", password);
                            url = addRequestParameters(url, parameters);
                            new ServerLoginExecutor(MainActivity.this).execute(url);
                        }

                        dialog.dismiss();
                    }
                }).create();

        Objects.requireNonNull(loginDialog.getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        loginDialog.show();
    }

    private void attemptLogin(User user) {
        if (!user.getPassword().equals(password)) {
            String url = url("user", "login");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("id", id);
            parameters.put("password", password);
            url = addRequestParameters(url, parameters);
            new ServerLoginExecutor(MainActivity.this).execute(url);
        } else {
            dbHelper.updateLastLogin(id, password);
            Class<?> aClass;
            switch (mode) {
                case AppConfig.SEND_MODE:
                    aClass = SendingActivity.class;
                    break;
                case AppConfig.DLV_MODE:
                    aClass = DeliveryActivity.class;
                    break;
                default:
                    aClass = null;
            }
            Intent intent = new Intent(MainActivity.this, aClass);
            startActivity(intent);
        }
    }

    static class ServerLoginExecutor extends AsyncTask<String, Boolean, String> {
        private WeakReference<MainActivity> reference;

        ServerLoginExecutor(MainActivity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(String... url) {
            MainActivity activity = reference.get();
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(activity.config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            String result;
            try {
                result = template.postForObject(url[0], null, String.class);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                return null;
            }
            return result;
        }

        @Override
        protected void onPreExecute() {
            reference.get().showProgressDialog(true);
        }

        @Override
        protected void onPostExecute(String result) {
            MainActivity activity = reference.get();
            if (result == null) {
                activity.showMessageDialog(activity.getString(R.string.error),
                        activity.getString(R.string.connection_error),
                        android.R.drawable.ic_dialog_alert);
                activity.playSound(SOUND_FAIL);
            } else {
                Gson gson = new Gson();
                User user = gson.fromJson(result, User.class);
                if (user.getId() == null) {
                    activity.showMessageDialog(activity.getString(R.string.error),
                            activity.getString(R.string.username_or_password_incorrect),
                            android.R.drawable.ic_dialog_alert);
                    activity.playSound(SOUND_FAIL);
                } else {
                    user.setId(user.getId().toUpperCase());
                    activity.loadUserInfo(user, true);
                    activity.attemptLogin(user);
                }
            }
            activity.showProgressDialog(false);
        }
    }

    static class Updater extends AsyncTask<String, Boolean, byte[]> {
        private WeakReference<MainActivity> reference;

        Updater(MainActivity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        protected byte[] doInBackground(String... url) {
            MainActivity activity = reference.get();
            RestTemplate template = new RestTemplate();
            ((SimpleClientHttpRequestFactory) template.getRequestFactory())
                    .setConnectTimeout(activity.config().getConnectionTimeout() * 1000);
            template.getMessageConverters().add(new StringHttpMessageConverter());
            byte[] result;
            try {
                result = template.getForObject(url[0], byte[].class);
                if (result.length == 0)
                    return result;
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                return null;
            }
            return result;
        }

        @Override
        protected void onPreExecute() {
            reference.get().showProgressDialog(true);
        }

        @Override
        protected void onPostExecute(byte[] result) {
            MainActivity activity = reference.get();
            if (result == null) {
                activity.showMessageDialog(activity.getString(R.string.info),
                        activity.getString(R.string.no_new_version),
                        android.R.drawable.ic_dialog_info);
                activity.showProgressDialog(false);
                return;
            }
            File file = new File(Environment.getExternalStorageDirectory().getPath() + "/BMSLogistic.apk");
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            FileOutputStream stream;
            try {
                stream = new FileOutputStream(file);
                stream.write(result);
            } catch (IOException e) {
                e.printStackTrace();
            }

            PackageManager pm = activity.getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(file.getAbsolutePath(), 0);
            int version = 0;
            try {
                version = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if (file.length() > 0 && info.versionCode > version) {

                Intent installIntent;
                Uri uri;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    installIntent = new Intent(Intent.ACTION_VIEW);
                    uri = Uri.fromFile(file);
                    installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
                } else {
                    installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                    uri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", file);
                    installIntent.setData(uri);
                    installIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                activity.startActivity(installIntent);
            } else {
                activity.showMessageDialog(activity.getString(R.string.info),
                        activity.getString(R.string.no_new_version),
                        android.R.drawable.ic_dialog_info);
            }
            activity.showProgressDialog(false);
        }
    }
}