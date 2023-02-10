package az.inci.bmslogistic.model;

public class ShipDocInfo
{
    private String driverCode;
    private String driverName;
    private String vehicleCode;
    private String deliverNotes;
    private String shipStatus;
    private String shipStatusDescription;
    private String targetCode;
    private String targetName;
    private double longitude;
    private double latitude;

    public String getDriverCode()
    {
        return driverCode;
    }

    public void setDriverCode(String driverCode)
    {
        this.driverCode = driverCode;
    }

    public String getDriverName()
    {
        return driverName;
    }

    public void setDriverName(String driverName)
    {
        this.driverName = driverName;
    }

    public String getVehicleCode()
    {
        return vehicleCode;
    }

    public void setVehicleCode(String vehicleCode)
    {
        this.vehicleCode = vehicleCode;
    }

    public String getDeliverNotes()
    {
        return deliverNotes;
    }

    public void setDeliverNotes(String deliverNotes)
    {
        this.deliverNotes = deliverNotes;
    }

    public String getShipStatus()
    {
        return shipStatus;
    }

    public void setShipStatus(String shipStatus)
    {
        this.shipStatus = shipStatus;
    }

    public String getShipStatusDescription()
    {
        return shipStatusDescription;
    }

    public void setShipStatusDescription(String shipStatusDescription)
    {
        this.shipStatusDescription = shipStatusDescription;
    }

    public String getTargetCode()
    {
        return targetCode;
    }

    public void setTargetCode(String targetCode)
    {
        this.targetCode = targetCode;
    }

    public String getTargetName()
    {
        return targetName;
    }

    public void setTargetName(String targetName)
    {
        this.targetName = targetName;
    }

    public double getLongitude()
    {
        return longitude;
    }

    public void setLongitude(double longitude)
    {
        this.longitude = longitude;
    }

    public double getLatitude()
    {
        return latitude;
    }

    public void setLatitude(double latitude)
    {
        this.latitude = latitude;
    }
}
