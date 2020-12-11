package az.inci.bmslogistic;

import android.app.Application;

public class App extends Application
{
    private AppConfig config;

    @Override
    public void onCreate()
    {
        super.onCreate();
        setConfig(new AppConfig());
    }

    public AppConfig getConfig()
    {
        return config;
    }

    public void setConfig(AppConfig config)
    {
        this.config = config;
    }
}
