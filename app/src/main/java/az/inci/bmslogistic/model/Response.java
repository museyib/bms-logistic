package az.inci.bmslogistic.model;

public class Response
{
    private int statusCode;
    private String systemMessage;
    private String developerMessage;
    private Object data;

    public int getStatusCode()
    {
        return statusCode;
    }

    public String getSystemMessage()
    {
        return systemMessage;
    }

    public String getDeveloperMessage()
    {
        return developerMessage;
    }

    public Object getData()
    {
        return data;
    }
}
