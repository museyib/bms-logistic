package az.inci.bmslogistic.model;

import lombok.Data;

@Data
public class UpdateDocLocationRequest
{
    private double latitude;
    private double longitude;
    private String address;
    private String userId;
}
