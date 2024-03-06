package az.inci.bmslogistic.model;

import lombok.Data;

@Data
public class UpdateDeliveryRequestItem
{
    private String trxNo;
    private String note;
    private String deliverPerson;
    private String driverCode;
    private boolean transitionFlag;
}
