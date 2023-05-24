package az.inci.bmslogistic;

public class AppConfig
{
    static final String DB_NAME = "BMS_LOGISTIC";
    static final int DB_VERSION = 2;

    public static final int SEND_MODE = 0;
    public static final int DLV_MODE = 1;

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
