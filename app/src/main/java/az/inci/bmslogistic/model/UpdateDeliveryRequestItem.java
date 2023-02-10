package az.inci.bmslogistic.model;

public class UpdateDeliveryRequestItem
{
    private String trxNo;
    private String note;
    private String deliverPerson;
    private String driverCode;

    public String getTrxNo()
    {
        return trxNo;
    }

    public void setTrxNo(String trxNo)
    {
        this.trxNo = trxNo;
    }

    public String getNote()
    {
        return note;
    }

    public void setNote(String note)
    {
        this.note = note;
    }

    public String getDeliverPerson()
    {
        return deliverPerson;
    }

    public void setDeliverPerson(String deliverPerson)
    {
        this.deliverPerson = deliverPerson;
    }

    public String getDriverCode()
    {
        return driverCode;
    }

    public void setDriverCode(String driverCode)
    {
        this.driverCode = driverCode;
    }
}
