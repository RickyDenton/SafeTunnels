package ControlModule.DevicesManagers.ActuatorManager.CoAPClientsObservingHandlers.CoAPClientFanHandler;

import ControlModule.DevicesManagers.ActuatorManager.ControlActuatorManager;
import errors.ErrCodeExcp;
import logging.Log;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.ClientObserveRelation;
import org.json.JSONException;
import org.json.JSONObject;

import static ControlModule.DevicesManagers.ActuatorManager.CoAPClientsObservingHandlers.CoAPClientFanHandler.CoAPClientFanHandlerErrCode.*;


public class CoAPClientFanHandler implements CoapHandler
 {
  ControlActuatorManager controlActuatorManager;
  ClientObserveRelation coapClientFanObserveRel;

  private int getActuatorFanRelSpeed(JSONObject coapResponseJSON, String coapResponseStr) throws  ErrCodeExcp
   {
    // Ensure the CoAP response to contain the required "fanRelSpeed" attribute
    if(!coapResponseJSON.has("fanRelSpeed"))
     throw new ErrCodeExcp(ERR_COAP_CLI_FAN_FANRELSPEED_MISSING,"(\"" + coapResponseStr + "\")");

    // Attempt to interpret the "fanRelSpeed" attribute as an int
    try
     { return coapResponseJSON.getInt("fanRelSpeed"); }
    catch(JSONException jsonExcp)
     { throw new ErrCodeExcp(ERR_COAP_CLI_FAN_FANRELSPEED_NOT_INT,"(\"" + coapResponseStr + "\")"); }
   }




  public CoAPClientFanHandler(ControlActuatorManager controlActuatorManager,ClientObserveRelation coapClientFanObserveRel)
   {
    this.controlActuatorManager = controlActuatorManager;
    this.coapClientFanObserveRel = coapClientFanObserveRel;
   }




  @Override
  public void onLoad(CoapResponse coapResponse)
   {
    // The CoAP response payload as JSON object
    JSONObject coapResponseJSON;

    // The received fanRelSpeed value
    int fanRelSpeed;

    // Ensure that a successful response was received
    if(!coapResponse.isSuccess())
     {
      Log.code(ERR_COAP_CLI_FAN_RESP_UNSUCCESSFUL,"(response = " + coapResponse.getCode().toString() + ")");
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
       { throw new ErrCodeExcp(ERR_COAP_CLI_FAN_RESP_NOT_JSON,"(\"" + coapResponseStr + "\")"); }

      // Attempt to extract the required "fanRelSpeed" attribute from the CoAP response
      fanRelSpeed = getActuatorFanRelSpeed(coapResponseJSON,coapResponseStr);

      // Ensure the received fan relative speed value to be valid
      if(fanRelSpeed < 0 || fanRelSpeed > 100)
       throw new ErrCodeExcp(ERR_COAP_CLI_FAN_FANRELSPEED_INVALID,"(" + fanRelSpeed + ")");

      // Set the new actuator fan relative speed
      controlActuatorManager.setFanRelSpeed(fanRelSpeed);
     }
    catch(ErrCodeExcp errCodeExcp)
     { Log.excp(errCodeExcp); }
   }


  @Override
  public void onError()
   {
    // Log the error
    Log.err("An error occurred in observing the \"fan\" resource on actuator"
      + controlActuatorManager.ID + ", attempting to re-establish the observing relationship");

    // Proactively cancel the resource observing to attempt to recover
    // from the error at the next actuatorWatcherTimer execution
    coapClientFanObserveRel.proactiveCancel();
   }
 }