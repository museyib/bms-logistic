package az.inci.bmslogistic.security;

import static az.inci.bmslogistic.GlobalParameters.connectionTimeout;
import static az.inci.bmslogistic.GlobalParameters.serviceUrl;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import az.inci.bmslogistic.model.CustomResponse;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class JwtResolver
{
    private final String username;
    private final String password;
    private final String secretKey;

    public JwtResolver(Context context)
    {

        Properties properties = new Properties();
        try
        {
            properties.load(context.getAssets().open("app.properties"));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        username = properties.getProperty("app.username");
        password = properties.getProperty("app.password");
        secretKey = properties.getProperty("app.secret-key");
    }

    public String resolve()
    {
        String jwt = "";
        try
        {
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .connectTimeout(connectionTimeout, TimeUnit.SECONDS)
                    .build();
            AuthenticationRequest authenticationRequest = new AuthenticationRequest();
            authenticationRequest.setUsername(username);
            authenticationRequest.setPassword(password);
            authenticationRequest.setSecretKey(secretKey);
            URL url = new URL(serviceUrl + "/v3/authenticate");
            RequestBody requestBody = RequestBody.create(new Gson().toJson(authenticationRequest),
                                                         MediaType.get("application/json;charset=UTF-8"));
            Request request = new Request.Builder()
                    .method("POST", requestBody)
                    .url(url)
                    .header("Content-Type", "application/json")
                    .build();

            try (Response httpResponse = httpClient.newCall(request).execute())
            {
                ResponseBody responseBody = httpResponse.body();

                Gson gson = new Gson();
                CustomResponse response = gson.fromJson(responseBody.string(),
                                                        new TypeToken<CustomResponse>() {}.getType());
                AuthenticationResponse authenticationResponse = gson.fromJson(
                        gson.toJson(response.getData()),
                        new TypeToken<AuthenticationResponse>() {}.getType());
                jwt = authenticationResponse.getToken();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return jwt;
    }
}
