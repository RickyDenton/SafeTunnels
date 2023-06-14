package ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.ObservingHandlers.light;

import ControlModule.DevicesManagers.ActuatorManager.ControlActuatorManager;
import devices.actuator.BaseActuator.LightState;
import errors.ErrCodeExcp;
import logging.Log;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.json.JSONException;
import org.json.JSONObject;

import static ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.ObservingHandlers.light.CoAPClientLightHandlerObsErrCode.*;


public class CoAPClientLightObsHandler implements CoapHandler
 {
  ControlActuatorManager controlActuatorManager;
  public boolean notifyTooManyObserversError;


  private LightState getActuatorLightState(JSONObject coapResponseJSON, String coapResponseStr) throws ErrCodeExcp
   {
    // Ensure the CoAP response to contain the required "lightState" attribute
    if(!coapResponseJSON.has("lightState"))
     throw new ErrCodeExcp(ERR_COAP_CLI_LIGHT_LIGHTSTATE_MISSING,"(\"" + coapResponseStr + "\")");

    try
     {
      // Attempt to extract the response's "LightState" attribute
      LightState actuatorLightState = LightState.values()[coapResponseJSON.getInt("lightState")];

      // If the received integer does not map to any valid actuator light states, throw an error
      if(actuatorLightState == null)
       throw new ErrCodeExcp(ERR_COAP_CLI_LIGHT_LIGHTSTATE_UNKNOWN,"(\"" + coapResponseStr + "\")");

      // Otherwise return the valid actuator's light state
      return actuatorLightState;
     }
    catch(JSONException jsonExcp)
     { throw new ErrCodeExcp(ERR_COAP_CLI_LIGHT_LIGHTSTATE_NOT_INT,"(\"" + coapResponseStr + "\")"); }
   }




  public CoAPClientLightObsHandler(ControlActuatorManager controlActuatorManager)
   {
    this.controlActuatorManager = controlActuatorManager;
    this.notifyTooManyObserversError = true;
   }




  @Override
  public void onLoad(CoapResponse coapResponse)
   {
    // The CoAP response payload as JSON object
    JSONObject coapResponseJSON;

    // The received lightState value
    LightState lightState;

    // Ensure that a successful response was received
    if(!coapResponse.isSuccess())
     {
      // If the actuator rejected observing the resource because it has
      // too many registered observers already (5.03, Too Many Observers)
      if(coapResponse.getCode().value == 163)
       {
        // If it was globally the first time the error was received, log
        // that the actuator's light state in the GUI may not be synchronized
        if(notifyTooManyObserversError)
         {
          Log.warn("actuator" + controlActuatorManager.ID + " rejected observing the \"light\" "
            + "resource because it has too many observers already, its light state value "
            + "in the GUI may not be synchronized (restart the node to fix the problem)");
          notifyTooManyObserversError = false;
         }
        // Otherwise, just silently ignore the error (the handler is automatically cancelled)
       }
      else
       Log.err("The light resource observe handler on actuator" + controlActuatorManager.ID
                + " received an unsuccessful response " + (coapResponse.getCode()));
      return;
     }

    // Retrieve the CoAP response's payload as a String
    String coapResponseStr = coapResponse.getResponseText();

    try
     {
      // Attempt to interpret the CoAP response as a JSON object
      try
       { coapResponseJSON = new JSONObject(coapResponseStr); }
      catch(JSONException jsonExcp)
       { throw new ErrCodeExcp(ERR_COAP_CLI_LIGHT_RESP_NOT_JSON,"(\"" + coapResponseStr + "\")"); }

      // Attempt to extract the required "lightState" attribute from the CoAP response
      lightState = getActuatorLightState(coapResponseJSON,coapResponseStr);

      // Set the new actuator light state
      controlActuatorManager.setLightState(lightState);
     }
    catch(ErrCodeExcp errCodeExcp)
     { Log.excp(errCodeExcp); }
   }


  @Override
  public void onError()
   {
    // Log the error
    Log.err("An error occurred in observing the \"light\" resource on actuator"
            + controlActuatorManager.ID + ", attempting to re-establish the observing relationship");
   }
 }
