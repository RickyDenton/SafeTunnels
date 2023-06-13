package ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.RequestsHandlers.fan;

import logging.Log;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;


public class CoAPClientFanReqHandler implements CoapHandler
 {
  short actuatorID;
  int sendFanRelSpeed;

  public CoAPClientFanReqHandler(short actuatorID, int sendFanRelSpeed)
   {
    this.actuatorID = actuatorID;
    this.sendFanRelSpeed = sendFanRelSpeed;
   }

   // We don't parse the response's contents
   @Override
   public void onLoad(CoapResponse coapResponse)
   {
    if(coapResponse.isSuccess())
     Log.dbg("Successfully sent new fan relative speed (" + sendFanRelSpeed + ") to actuator" + actuatorID);
    else
     {
      Log.err("Failed to send new fan relative speed (" + sendFanRelSpeed + ") to "
        + "actuator" + actuatorID + " (response = " + coapResponse.getCode().toString() + ")");
      Log.err("|-- Response Code: " + coapResponse.getCode().toString());
      Log.err("|-- Payload: " + coapResponse.getResponseText());
     }
   }

   @Override
   public void onError()
   {
    Log.dbg("An error occurred in sending the new fan relative speed (" + sendFanRelSpeed + ") "
             + "to actuator" + actuatorID + " (probably it is offline)");
   }
  }