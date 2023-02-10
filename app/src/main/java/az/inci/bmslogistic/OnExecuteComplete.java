package az.inci.bmslogistic;

import az.inci.bmslogistic.model.ResponseMessage;

public interface OnExecuteComplete
{
    void executeComplete(ResponseMessage message);
}
