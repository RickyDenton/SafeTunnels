package ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.RequestsHandlers.light;


import logging.Log;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import devices.actuator.BaseActuator.LightState;

public class CoAPClientLightReqHandler implements CoapHandler
 {
  short actuatorID;
  LightState sendLightState;

  public CoAPClientLightReqHandler(short actuatorID, LightState sendLightState)
   {
    this.actuatorID = actuatorID;
    this.sendLightState = sendLightState;
   }

  // We don't parse the response's contents
  @Override
  public void onLoad(CoapResponse coapResponse)
   {
    if(coapResponse.isSuccess())
     Log.dbg("Successfully sent new light state (" + sendLightState + ") to actuator" + actuatorID);
    else
     {
      Log.err("Failed to send new light state (" + sendLightState + ") to "
        + "actuator" + actuatorID + " (response = " + coapResponse.getCode().toString() + ")");
      Log.err("|-- Response Code: " + coapResponse.getCode().toString());
      Log.err("|-- Payload: " + coapResponse.getResponseText());
     }
   }

  @Override
  public void onError()
   {
    Log.err("An error occurred in sending the new light state " + "(" + sendLightState + ")"
            + " to actuator" + actuatorID + " (probably it is offline)");
   }
 }