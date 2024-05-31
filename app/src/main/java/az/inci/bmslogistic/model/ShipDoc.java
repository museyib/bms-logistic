package az.inci.bmslogistic.model;

import lombok.Data;

@Data
public class ShipDoc
{
    private String docNo;
    private String trxNo;
    private String trxDate;
    private String bpCode;
    private String bpName;
    private String sbeCode;
    private String sbeName;
    private String shipStatus;
    private String driverCode;
    private String driverName;
}
