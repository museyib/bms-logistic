package az.inci.bmslogistic.model;

import java.util.List;

import lombok.Data;

@Data
public class UpdateDeliveryRequest
{
    private String status;
    private List<UpdateDeliveryRequestItem> requestItems;
}
