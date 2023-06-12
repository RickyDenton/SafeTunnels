package ControlModule.DevicesManagers.ActuatorManager.CoAPClientsObservingHandlers.CoAPClientLightHandler;

import ControlModule.DevicesManagers.ActuatorManager.ControlActuatorManager;
import devices.actuator.BaseActuator.LightState;
import errors.ErrCodeExcp;
import logging.Log;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.ClientObserveRelation;
import org.json.JSONException;
import org.json.JSONObject;


public class CoAPClientLightHandler implements CoapHandler
 {
  ControlActuatorManager controlActuatorManager;
  ClientObserveRelation coapClientLightObserveRel;


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




  public CoAPClientLightHandler(ControlActuatorManager controlActuatorManager,ClientObserveRelation coapClientLightObserveRel)
   {
    this.controlActuatorManager = controlActuatorManager;
    this.coapClientLightObserveRel = coapClientLightObserveRel;
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
      Log.code(ERR_COAP_CLI_LIGHT_RESP_UNSUCCESSFUL,"(response = " + coapResponse.getCode() + ")");
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
    Log.code(ERR_COAP_CLI_LIGHT_OBSERVE_ERROR);

    // Proactively cancel the resource observing to attempt to recover
    // from the error at the next actuatorWatchdogTimer execution
    coapClientLightObserveRel.proactiveCancel();
   }
 }
