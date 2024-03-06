package az.inci.bmslogistic.model;

import lombok.Data;

@Data
public class ConfirmDeliveryRequest
{
    private String trxNo;
    private String note;
    private String deliverPerson;
    private String driverCode;
    private boolean transitionFlag;
    private String confirmatioinCode;
}
