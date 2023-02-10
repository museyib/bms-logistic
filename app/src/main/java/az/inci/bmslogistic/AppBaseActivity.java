package az.inci.bmslogistic;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import az.inci.bmslogistic.model.Response;
import az.inci.bmslogistic.model.ResponseMessage;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public class AppBaseActivity extends AppCompatActivity
{

    protected static int SOUND_SUCCESS = R.raw.barcodebeep;
    protected static int SOUND_FAIL = R.raw.serror3;

    protected SoundPool soundPool;
    protected AudioManager audioManager;
    protected int sound;

    AlertDialog progressDialog;
    int mode;
    DBHelper dbHelper;
    Type responseType = new TypeToken<Response>()
    {}.getType();
    Gson gson = new Gson();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        dbHelper = new DBHelper(this);
        dbHelper.open();

        soundPool = new SoundPool(10, 3, 5);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
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
        if (progressDialog == null)
        {
            progressDialog = new AlertDialog.Builder(this)
                    .setView(view)
                    .setCancelable(false)
                    .create();
        }
        if (b)
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
        sb.append(config().getServerUrl()).append("/v2");
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

    public void loadUserInfo(User user, boolean newUser)
    {
        if (newUser)
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

    okhttp3.Response sendRequest(URL url, String method, @Nullable Object requestBodyData)
            throws IOException
    {
        OkHttpClient httpClient = new OkHttpClient.Builder().connectTimeout(
                config().getConnectionTimeout(), TimeUnit.SECONDS).build();

        RequestBody requestBody = null;

        if (method.equals("POST"))
        {
            requestBody = RequestBody.create(new Gson().toJson(requestBodyData),
                                             MediaType.get("application/json;charset=UTF-8"));
        }
        Request request = new Request.Builder().method(method, requestBody).url(url).build();

        return httpClient.newCall(request).execute();
    }

    public void executeUpdate(String urlString, Object requestData,
                              OnExecuteComplete executeComplete)
    {
        new Thread(() -> {
            try (okhttp3.Response httpResponse = sendRequest(new URL(urlString), "POST",
                                                             requestData))
            {
                ResponseBody responseBody = httpResponse.body();
                Response response = gson.fromJson(responseBody.string(), responseType);
                String title;
                String message;
                int iconId;
                if (response.getStatusCode() == 0)
                {
                    title = getString(R.string.info);
                    message = response.getDeveloperMessage();
                    iconId = android.R.drawable.ic_dialog_info;
                }
                else if (response.getStatusCode() == 2)
                {
                    title = getString(R.string.error);
                    message = response.getDeveloperMessage();
                    iconId = android.R.drawable.ic_dialog_alert;
                }
                else
                {
                    title = getString(R.string.error);
                    message = response.getDeveloperMessage() + ": " + response.getSystemMessage();
                    iconId = android.R.drawable.ic_dialog_alert;
                }

                ResponseMessage responseMessage = new ResponseMessage();
                responseMessage.setStatusCode(response.getStatusCode());
                responseMessage.setTitle(title);
                responseMessage.setBody(message);
                responseMessage.setIconId(iconId);

                runOnUiThread(() -> executeComplete.executeComplete(responseMessage));
            }
            catch (IOException e)
            {
                runOnUiThread(() -> {
                    showMessageDialog(getString(R.string.error), e.getMessage(),
                                      android.R.drawable.ic_dialog_alert);
                    playSound(SOUND_FAIL);
                });
            }
            finally
            {
                runOnUiThread(() -> showProgressDialog(false));
            }
        }).start();
    }

    protected <T> T getSimpleObject(String url, String method, Object request, Class<T> tClass)
    {
        try (okhttp3.Response httpResponse = sendRequest(new URL(url), method, request))
        {
            ResponseBody responseBody = httpResponse.body();
            Response response = gson.fromJson(responseBody.string(), responseType);
            if (response.getStatusCode() == 0)
            {
                return gson.fromJson(gson.toJson(response.getData()), tClass);
            }
            else if (response.getStatusCode() == 2)
            {
                runOnUiThread(() -> {
                    showMessageDialog(getString(R.string.error), response.getDeveloperMessage(),
                                      android.R.drawable.ic_dialog_alert);
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
                                      android.R.drawable.ic_dialog_alert);
                    playSound(SOUND_FAIL);
                });
                return null;
            }
        }
        catch (IOException e)
        {
            runOnUiThread(() -> {
                showMessageDialog(getString(R.string.error),
                                  getString(R.string.internal_error) + ": " + e.getMessage(),
                                  android.R.drawable.ic_dialog_alert);
                playSound(SOUND_FAIL);
            });
            return null;
        }
        finally
        {
            runOnUiThread(() -> showProgressDialog(false));
        }
    }

    protected <T> List<T> getListData(String url, String method, Object request, Class<T[]> tClass)
    {
        try (okhttp3.Response httpResponse = sendRequest(new URL(url), method, request))
        {
            ResponseBody responseBody = httpResponse.body();
            Response response = gson.fromJson(responseBody.string(), responseType);
            if (response.getStatusCode() == 0)
            {
                return new ArrayList<>(
                        Arrays.asList(gson.fromJson(gson.toJson(response.getData()), tClass)));
            }
            else if (response.getStatusCode() == 2)
            {
                runOnUiThread(() -> {
                    showMessageDialog(getString(R.string.error), response.getDeveloperMessage(),
                                      android.R.drawable.ic_dialog_alert);
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
                                      android.R.drawable.ic_dialog_alert);
                    playSound(SOUND_FAIL);
                });
                return null;
            }
        }
        catch (Exception e)
        {
            runOnUiThread(() -> {
                showMessageDialog(getString(R.string.error),
                                  getString(R.string.internal_error) + ": " + e.getMessage(),
                                  android.R.drawable.ic_dialog_alert);
                playSound(SOUND_FAIL);
            });
            return null;
        }
        finally
        {
            runOnUiThread(() -> showProgressDialog(false));
        }
    }
}
