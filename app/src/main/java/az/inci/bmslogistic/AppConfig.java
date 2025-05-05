package az.inci.bmslogistic;

import az.inci.bmslogistic.model.User;

public class AppConfig
{
    public static final int SEND_MODE = 0;
    public static final int DELIVERY_MODE = 1;
    public static final int ARCHIVE_MODE = 2;
    static final String DB_NAME = "BMS_LOGISTIC";
    static final int DB_VERSION = 2;
    private User user;

    public User getUser()
    {
        return user;
    }

    public void setUser(User user)
    {
        this.user = user;
    }
}
