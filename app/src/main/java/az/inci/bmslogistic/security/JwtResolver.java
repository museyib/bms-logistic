package az.inci.bmslogistic.security;

import android.content.Context;

import java.io.IOException;
import java.util.Properties;

import az.inci.bmslogistic.activity.AppBaseActivity;

public class JwtResolver
{
    private final Context context;
    private final String username;
    private final String password;
    private final String secretKey;

    public JwtResolver(Context context)
    {
        this.context = context;

        Properties properties = new Properties();
        try
        {
            properties.load(context.getAssets().open("app.properties"));
        }
        catch(IOException ignored) {}

        username = properties.getProperty("app.username");
        password = properties.getProperty("app.password");
        secretKey = properties.getProperty("app.secret-key");
    }

    public String resolve()
    {
        AuthenticationRequest request = new AuthenticationRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setSecretKey(secretKey);
        String url = ((AppBaseActivity) context).url("authenticate");
        AuthenticationResponse authenticationResponse = ((AppBaseActivity) context).getSimpleObject(url, "POST", request, AuthenticationResponse.class);

        return authenticationResponse.getToken();
    }
}
