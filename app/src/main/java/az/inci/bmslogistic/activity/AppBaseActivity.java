package az.inci.bmslogistic.activity;

import static android.R.drawable.ic_dialog_alert;
import static android.R.drawable.ic_dialog_info;
import static android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION;
import static az.inci.bmslogistic.GlobalParameters.apiVersion;
import static az.inci.bmslogistic.GlobalParameters.connectionTimeout;
import static az.inci.bmslogistic.GlobalParameters.jwt;
import static az.inci.bmslogistic.GlobalParameters.serviceUrl;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import az.inci.bmslogistic.App;
import az.inci.bmslogistic.AppConfig;
import az.inci.bmslogistic.DBHelper;
import az.inci.bmslogistic.OnExecuteComplete;
import az.inci.bmslogistic.R;
import az.inci.bmslogistic.model.User;
import az.inci.bmslogistic.model.CustomResponse;
import az.inci.bmslogistic.model.ResponseMessage;
import az.inci.bmslogistic.security.JwtResolver;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AppBaseActivity extends AppCompatActivity
{

    protected static int SOUND_SUCCESS = R.raw.barcodebeep;
    protected static int SOUND_FAIL = R.raw.serror3;
    protected SoundPool soundPool;
    protected AudioManager audioManager;
    protected int sound;
    protected SharedPreferences preferences;
    protected JwtResolver jwtResolver;
    AlertDialog progressDialog;
    int mode;
    DBHelper dbHelper;
    Type responseType = new TypeToken<CustomResponse>() {}.getType();
    Gson gson = new Gson();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        jwtResolver = new JwtResolver(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        dbHelper = new DBHelper(this);
        dbHelper.open();
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder().setMaxStreams(10)
                                           .setAudioAttributes(audioAttributes)
                                           .build();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        jwt = preferences.getString("jwt", "");
    }

    public void loadFooter()
    {
        TextView userId = findViewById(R.id.user_info_id);
        userId.setText(config().getUser().getId());
        userId.append(" - ");
        userId.append(config().getUser().getName());
    }

    public void showProgressDialog(boolean b)
    {
        View view = getLayoutInflater().inflate(R.layout.progress_dialog_layout,
                                                findViewById(android.R.id.content), false);
        if(progressDialog == null)
        {
            progressDialog = new AlertDialog.Builder(this)
                    .setView(view)
                    .setCancelable(false)
                    .create();
        }
        if(b)
        {
            progressDialog.show();
        }
        else
        {
            progressDialog.dismiss();
        }
    }

    public String url(String... value)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(serviceUrl).append("/").append(apiVersion);
        for(String s : value)
        {
            sb.append("/").append(s);
        }
        return sb.toString();
    }

    public String addRequestParameters(String url, Map<String, String> requestParameters)
    {
        StringBuilder builder = new StringBuilder(url);
        builder.append("?");

        for(Map.Entry<String, String> entry : requestParameters.entrySet())
        {
            builder.append(entry.getKey())
                   .append("=")
                   .append(entry.getValue())
                   .append("&");
        }

        builder.delete(builder.length() - 1, builder.length());
        return builder.toString();
    }

    public void loadUserInfo(User user, boolean newUser)
    {
        if(newUser)
        {
            dbHelper.addUser(user);
        }
        config().setUser(user);
    }

    public AppConfig config()
    {
        return ((App) getApplication()).getConfig();
    }

    protected void showToastMessage(String message)
    {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    protected void showMessageDialog(String title, String message, int icon)
    {
        new android.app.AlertDialog.Builder(this)
                .setIcon(icon)
                .setTitle(title)
                .setMessage(message).show();
    }

    protected void playSound(int resourceId)
    {
        int volume = audioManager.getStreamMaxVolume(3);
        sound = soundPool.load(this, resourceId, 1);
        soundPool.setOnLoadCompleteListener((soundPool1, i, i1) ->
                                                    soundPool.play(sound, volume, volume, 1, 0, 1));
    }

    Response sendRequest(URL url, String method, @Nullable Object requestBodyData)
            throws IOException
    {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        clientBuilder.connectTimeout(connectionTimeout, TimeUnit.SECONDS);
        OkHttpClient httpClient = clientBuilder.build();

        Request request;

        if(method.equals("POST")) {
            RequestBody requestBody = RequestBody.create(MediaType.get("application/json;charset=UTF-8"),
                    new Gson().toJson(requestBodyData));
            request = new Request.Builder()
                    .post(requestBody)
                    .header("Authorization", "Bearer " + jwt)
                    .url(url)
                    .build();
        }
        else {
            request = new Request.Builder()
                    .get()
                    .header("Authorization", "Bearer " + jwt)
                    .url(url)
                    .build();
        }

        return httpClient.newCall(request).execute();
    }

    public void executeUpdate(String urlString, Object requestData,
                              OnExecuteComplete executeComplete)
    {
        new Thread(() -> {
            try
            {
                int statusCode;
                String title;
                String message;
                int iconId;

                Response httpResponse = sendRequest(new URL(urlString), "POST", requestData);
                if(httpResponse.code() == 403)
                {
                    jwt = jwtResolver.resolve();
                    preferences.edit().putString("jwt", jwt).apply();
                    httpResponse = sendRequest(new URL(urlString), "POST", requestData);
                }

                if(httpResponse.code() == 200)
                {
                    ResponseBody responseBody = httpResponse.body();
                    CustomResponse response = gson.fromJson(
                            Objects.requireNonNull(responseBody).string(), responseType);
                    statusCode = response.getStatusCode();

                    if(statusCode == 0)
                    {
                        title = getString(R.string.info);
                        message = response.getDeveloperMessage();
                        iconId = ic_dialog_info;
                    }
                    else
                    if(statusCode == 2)
                    {
                        title = getString(R.string.error);
                        message = response.getDeveloperMessage();
                        iconId = ic_dialog_alert;
                    }
                    else
                    {
                        title = getString(R.string.error);
                        message = response.getDeveloperMessage() + ": " + response.getSystemMessage();
                        iconId = ic_dialog_alert;
                    }
                }
                else
                {
                    statusCode = httpResponse.code();
                    title = getString(R.string.error);
                    message = httpResponse.toString();
                    iconId = ic_dialog_alert;
                }

                ResponseMessage responseMessage = new ResponseMessage();
                responseMessage.setStatusCode(statusCode);
                responseMessage.setTitle(title);
                responseMessage.setBody(message);
                responseMessage.setIconId(iconId);

                runOnUiThread(() -> executeComplete.executeComplete(responseMessage));
            }
            catch(IOException e)
            {
                runOnUiThread(() -> {
                    showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert);
                    playSound(SOUND_FAIL);
                });
            }
            finally
            {
                runOnUiThread(() -> {
                    showProgressDialog(false);
                });
            }
        }).start();
    }

    public <T> T getSimpleObject(String url, String method, Object request, Class<T> tClass)
    {
        Log.e("URL", url);
        try
        {
            Response httpResponse = sendRequest(new URL(url), method, request);
            if(httpResponse.code() == 403)
            {
                jwt = jwtResolver.resolve();
                preferences.edit().putString("jwt", jwt).apply();
                httpResponse = sendRequest(new URL(url), method, request);
            }
            if(httpResponse.code() == 200)
            {
                ResponseBody responseBody = httpResponse.body();
                CustomResponse response = gson.fromJson(
                        Objects.requireNonNull(responseBody).string(),
                        responseType);
                if(response.getStatusCode() == 0)
                    return gson.fromJson(gson.toJson(response.getData()), tClass);
                else
                if(response.getStatusCode() == 2)
                {
                    runOnUiThread(() -> {
                        showMessageDialog(getString(R.string.error),
                                response.getDeveloperMessage(),
                                ic_dialog_alert);
                        playSound(SOUND_FAIL);
                    });
                    return null;
                }
                else
                {
                    runOnUiThread(() -> {
                        showMessageDialog(getString(R.string.error),
                                response.getDeveloperMessage() + ": " +
                                        response.getSystemMessage(),
                                ic_dialog_alert);
                        playSound(SOUND_FAIL);
                    });
                    return null;
                }
            }
            else
            {
                String message = httpResponse.toString();
                runOnUiThread(() -> {
                    showMessageDialog(getString(R.string.error), message, ic_dialog_alert);
                    playSound(SOUND_FAIL);
                });
                return null;
            }
        }
        catch(IOException e)
        {
            runOnUiThread(() -> {
                showMessageDialog(getString(R.string.error),
                        getString(R.string.internal_error) + ": " + e,
                        ic_dialog_alert);
                playSound(SOUND_FAIL);
            });
            return null;
        }
        finally
        {
            runOnUiThread(() -> showProgressDialog(false));
        }
    }

    @SuppressWarnings("SameParameterValue")
    protected <T> List<T> getListData(String url, String method, Object request, Class<T[]> tClass) {
        try {
            Response httpResponse = sendRequest(new URL(url), method, request);
            if (httpResponse.code() == 403) {
                jwt = jwtResolver.resolve();
                preferences.edit().putString("jwt", jwt).apply();
                httpResponse = sendRequest(new URL(url), method, request);
            }
            if (httpResponse.code() == 200) {
                ResponseBody responseBody = httpResponse.body();
                CustomResponse response = gson.fromJson(
                        Objects.requireNonNull(responseBody).string(),
                        responseType);
                if (response.getStatusCode() == 0)
                    return new ArrayList<>(
                            Arrays.asList(gson.fromJson(gson.toJson(response.getData()), tClass)));
                else if (response.getStatusCode() == 2) {
                    runOnUiThread(() -> {
                        showMessageDialog(getString(R.string.error),
                                response.getDeveloperMessage(),
                                ic_dialog_alert);
                        playSound(SOUND_FAIL);
                    });
                    return null;
                } else {
                    runOnUiThread(() -> {
                        showMessageDialog(getString(R.string.error),
                                response.getDeveloperMessage() + ": " +
                                        response.getSystemMessage(),
                                ic_dialog_alert);
                        playSound(SOUND_FAIL);
                    });
                    return null;
                }
            } else {
                String message = httpResponse.toString();
                runOnUiThread(() -> {
                    showMessageDialog(getString(R.string.error), message, ic_dialog_alert);
                    playSound(SOUND_FAIL);
                });
                return null;
            }
        } catch (Exception e) {
            runOnUiThread(() -> {
                showMessageDialog(getString(R.string.error),
                        getString(R.string.internal_error) + ": " + e,
                        ic_dialog_alert);
                playSound(SOUND_FAIL);
            });
            return null;
        } finally {
            runOnUiThread(() -> showProgressDialog(false));
        }
    }
}
