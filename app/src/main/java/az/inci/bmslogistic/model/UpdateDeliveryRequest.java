package az.inci.bmslogistic.model;

import java.util.List;

public class UpdateDeliveryRequest
{
    private String status;
    private List<UpdateDeliveryRequestItem> requestItems;

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public List<UpdateDeliveryRequestItem> getRequestItems()
    {
        return requestItems;
    }

    public void setRequestItems(
            List<UpdateDeliveryRequestItem> requestItems)
    {
        this.requestItems = requestItems;
    }
}
