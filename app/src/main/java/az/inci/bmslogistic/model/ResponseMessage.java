package az.inci.bmslogistic.model;

import lombok.Data;

@Data
public class ResponseMessage
{
    private int statusCode;
    private String title;
    private String body;
    private int iconId;
}
