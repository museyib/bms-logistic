package az.inci.bmslogistic.model;

import lombok.Data;

@Data
public class CustomResponse
{
    private int statusCode;
    private String systemMessage;
    private String developerMessage;
    private Object data;
}
