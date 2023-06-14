package ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.RequestsHandlers.light;


import ControlModule.DevicesManagers.ActuatorManager.ControlActuatorManager;
import logging.Log;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import devices.actuator.BaseActuator.LightState;

public class CoAPClientLightReqHandler implements CoapHandler
 {
  ControlActuatorManager ctrlActuatorManager;
  LightState sendLightState;

  public CoAPClientLightReqHandler(ControlActuatorManager ctrlActuatorManager, LightState sendLightState)
   {
    this.ctrlActuatorManager = ctrlActuatorManager;
    this.sendLightState = sendLightState;
   }

  // We don't parse the response's contents
  @Override
  public void onLoad(CoapResponse coapResponse)
   {
    if(coapResponse.isSuccess())
     {
      Log.dbg("Successfully sent new light state (" + sendLightState + ") to actuator" + ctrlActuatorManager.ID);

      // Directly set the fan relative speed (to account for observing problems)
      ctrlActuatorManager.setLightState(sendLightState);
     }
    else
     {
      Log.err("Failed to send new light state (" + sendLightState + ") to "
        + "actuator" + ctrlActuatorManager.ID + " (response = " + coapResponse.getCode().toString() + ")");
      Log.err("|-- Response Code: " + coapResponse.getCode().toString());
      Log.err("|-- Payload: " + coapResponse.getResponseText());
     }
   }

  @Override
  public void onError()
   {
    Log.err("An error occurred in sending the new light state " + "(" + sendLightState + ")"
            + " to actuator" + ctrlActuatorManager.ID + " (probably it is offline)");
   }
 }