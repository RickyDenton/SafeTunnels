package ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.ObservingHandlers.fan;

import ControlModule.DevicesManagers.ActuatorManager.ControlActuatorManager;
import errors.ErrCodeExcp;
import logging.Log;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.json.JSONException;
import org.json.JSONObject;


public class CoAPClientFanObsHandler implements CoapHandler
 {
  ControlActuatorManager controlActuatorManager;
  public boolean notifyTooManyObserversError;

  private int getActuatorFanRelSpeed(JSONObject coapResponseJSON, String coapResponseStr) throws  ErrCodeExcp
   {
    // Ensure the CoAP response to contain the required "fanRelSpeed" attribute
    if(!coapResponseJSON.has("fanRelSpeed"))
     throw new ErrCodeExcp(CoAPClientFanHandlerObsErrCode.ERR_COAP_CLI_FAN_FANRELSPEED_MISSING,"(\"" + coapResponseStr + "\")");

    // Attempt to interpret the "fanRelSpeed" attribute as an int
    try
     { return coapResponseJSON.getInt("fanRelSpeed"); }
    catch(JSONException jsonExcp)
     { throw new ErrCodeExcp(
       CoAPClientFanHandlerObsErrCode.ERR_COAP_CLI_FAN_FANRELSPEED_NOT_INT,"(\"" + coapResponseStr + "\")"); }
   }


  public CoAPClientFanObsHandler(ControlActuatorManager controlActuatorManager)
   {
    this.controlActuatorManager = controlActuatorManager;
    this.notifyTooManyObserversError = true;
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
      // If the actuator rejected observing the resource because it has
      // too many registered observers already (5.03, Too Many Observers)
      if(coapResponse.getCode().value == 163)
       {
        // If it was globally the first time the error was received, log that
        // the actuator's fan relative speed in the GUI may not be synchronized
        if(notifyTooManyObserversError)
         {
          Log.warn("actuator" + controlActuatorManager.ID + " rejected observing the \"fan\" "
                    + "resource because it has too many observers already, its fan relative speed value "
                    + "in the GUI may not be synchronized (restart the node to fix the problem)");
          notifyTooManyObserversError = false;
         }
        // Otherwise, just silently ignore the error (the handler is automatically cancelled)
       }
      else
       Log.err("The fan resource observe handler on actuator" + controlActuatorManager.ID +
                " received an unsuccessful response " + (coapResponse.getCode()));
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
       { throw new ErrCodeExcp(CoAPClientFanHandlerObsErrCode.ERR_COAP_CLI_FAN_RESP_NOT_JSON,"(\"" + coapResponseStr + "\")"); }

      // Attempt to extract the required "fanRelSpeed" attribute from the CoAP response
      fanRelSpeed = getActuatorFanRelSpeed(coapResponseJSON,coapResponseStr);

      // Ensure the received fan relative speed value to be valid
      if(fanRelSpeed < 0 || fanRelSpeed > 100)
       throw new ErrCodeExcp(CoAPClientFanHandlerObsErrCode.ERR_COAP_CLI_FAN_FANRELSPEED_INVALID,"(" + fanRelSpeed + ")");

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
   }
 }