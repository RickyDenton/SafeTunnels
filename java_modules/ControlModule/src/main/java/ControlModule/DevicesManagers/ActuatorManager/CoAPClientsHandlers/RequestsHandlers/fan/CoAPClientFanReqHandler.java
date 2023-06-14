package ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.RequestsHandlers.fan;

import ControlModule.DevicesManagers.ActuatorManager.ControlActuatorManager;
import logging.Log;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;


public class CoAPClientFanReqHandler implements CoapHandler
 {
  final private ControlActuatorManager ctrlActuatorManager;
  final private int sendFanRelSpeed;

  public CoAPClientFanReqHandler(ControlActuatorManager ctrlActuatorManager, int sendFanRelSpeed)
   {
    this.ctrlActuatorManager = ctrlActuatorManager;
    this.sendFanRelSpeed = sendFanRelSpeed;
   }

   // We don't parse the response's contents
   @Override
   public void onLoad(CoapResponse coapResponse)
   {
    if(coapResponse.isSuccess())
     {
      Log.dbg("Successfully sent new fan relative speed (" + sendFanRelSpeed + ") to actuator" + ctrlActuatorManager.ID);

      // Directly set the fan relative speed (to account for observing problems)
      ctrlActuatorManager.setFanRelSpeed(sendFanRelSpeed);
     }
    else
     {
      Log.err("Failed to send new fan relative speed (" + sendFanRelSpeed + ") to "
        + "actuator" + ctrlActuatorManager.ID + " (response = " + coapResponse.getCode().toString() + ")");
      Log.err("|-- Response Code: " + coapResponse.getCode().toString());
      Log.err("|-- Payload: " + coapResponse.getResponseText());
     }
   }

   @Override
   public void onError()
   {
    Log.err("An error occurred in sending the new fan relative speed (" + sendFanRelSpeed + ") "
             + "to actuator" + ctrlActuatorManager.ID + " (probably it is offline)");
   }
  }