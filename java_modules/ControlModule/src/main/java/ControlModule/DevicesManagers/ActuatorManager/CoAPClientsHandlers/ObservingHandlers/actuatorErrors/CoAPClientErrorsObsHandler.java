package ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.ObservingHandlers.actuatorErrors;

import ControlModule.DevicesManagers.ActuatorManager.ControlActuatorManager;
import devices.actuator.BaseActuatorErrCode;
import errors.ErrCodeExcp;
import logging.Log;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.json.JSONException;
import org.json.JSONObject;

import static ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.ObservingHandlers.actuatorErrors.CoAPClientErrorsHandlerObsErrCode.*;


public class CoAPClientErrorsObsHandler implements CoapHandler
 {
  ControlActuatorManager controlActuatorManager;
  public boolean notifyTooManyObserversError;

  private BaseActuatorErrCode getActuatorErrCode(JSONObject coapResponseJSON, String coapResponseStr) throws  ErrCodeExcp
   {
    // Ensure the CoAP response to contain the required "errCode" attribute
    if(!coapResponseJSON.has("errCode"))
     throw new ErrCodeExcp(ERR_COAP_CLI_ERRORS_ERRCODE_MISSING,"(\"" + coapResponseStr + "\")");

    try
     {
      // The CoAP response's "errCode" mapped into a BaseActuatorErrCode
      BaseActuatorErrCode actuatorErrCode;

      // Attempt to interpret the response's "errCode" attribute as an integer and map it into a
      // BaseActuatorErrCode, throwing an exception if no BaseActuatorErrCode of such index exists
      try
       { actuatorErrCode = BaseActuatorErrCode.values[coapResponseJSON.getInt("errCode")]; }
      catch(ArrayIndexOutOfBoundsException outOfBoundsException)
       { throw new ErrCodeExcp(ERR_COAP_CLI_ERRORS_ERRCODE_UNKNOWN,"(\"" + coapResponseStr + "\")"); }

      // Otherwise return the valid actuator error code
      return actuatorErrCode;
     }
    catch(JSONException jsonExcp)
     { throw new ErrCodeExcp(ERR_COAP_CLI_ERRORS_ERRCODE_NOT_INT,"(\"" + coapResponseStr + "\")"); }
   }


  private String getActuatorErrClientIP(JSONObject coapResponseJSON,String coapResponseStr) throws  ErrCodeExcp
   {
    // Ensure the CoAP response to contain the required "clientIP" attribute
    if(!coapResponseJSON.has("clientIP"))
     throw new ErrCodeExcp(ERR_COAP_CLI_ERRORS_CLIIP_MISSING,"(\"" + coapResponseStr + "\")");

    // Attempt to extract the response's "clientIP"
    // attribute as a String and ensure it to be non-null
    try
     { return coapResponseJSON.getString("clientIP"); }
    catch(JSONException jsonExcp)
     { throw new ErrCodeExcp(ERR_COAP_CLI_ERRORS_CLIIP_NOT_NONNULL_STRING,"(\"" + coapResponseStr + "\")"); }
   }


  private String getActuatorErrDscr(JSONObject coapResponseJSON,String coapResponseStr) throws  ErrCodeExcp
   {
    // If the CoAP response contains the optional actuator "errDscr" attribute
    if(coapResponseJSON.has("errDscr"))
     {
      // Attempt to extract the response's "errDscr"
      // attribute as a String and ensure it to be non-null
      try
       { return coapResponseJSON.getString("errDscr"); }
      catch(JSONException jsonExcp)
       { throw new ErrCodeExcp(ERR_COAP_CLI_ERRORS_ERRDSCR_NOT_NONNULL_STRING,"(\"" + coapResponseStr + "\")"); }
     }

    // Otherwise, return null
    else
     return null;
   }



  public CoAPClientErrorsObsHandler(ControlActuatorManager controlActuatorManager)
   {
    this.controlActuatorManager = controlActuatorManager;
    this.notifyTooManyObserversError = true;
   }


  @Override
  public void onLoad(CoapResponse coapResponse)
   {
    // The CoAP response payload as JSON object
    JSONObject coapResponseJSON;

    // The received actuator error code
    BaseActuatorErrCode actuatorErrCode;

    // The IP address of the client whose
    // request to the actuator raised the error
    String actuatorErrClientIP;

    // The received optional actuator error description
    String actuatorErrDscr;

    // Ensure that a successful response was received
    if(!coapResponse.isSuccess())
     {
      // If the actuator rejected observing the resource because it has
      // too many registered observers already (5.03, Too Many Observers)
      if(coapResponse.getCode().value == 163)
       {
        // If it is the first of such errors received, log
        // that the actuator errors will not be reported
        if(notifyTooManyObserversError)
         {
          Log.warn("actuator" + controlActuatorManager.ID + " rejected observing "
            + "the \"errors\" resource because it has too many observers already, its "
            + "errors will NOT be reported (restart the node to fix the problem)");
          notifyTooManyObserversError = false;
         }
        // Otherwise, just silently ignore the error (the handler is automatically cancelled)
       }
      else
       Log.err("The errors resource observe handler on actuator" + controlActuatorManager.ID
                + " received an unsuccessful response " + (coapResponse.getCode()));
      return;
     }

    // Retrieve the CoAP response's payload as a String
    String coapResponseStr = coapResponse.getResponseText();

    // Empty CoAP responses are associated with the "actuatorErrors"
    // observing confirmation and periodic refreshing, and so can be ignored
    if(coapResponseStr.isEmpty())
     { return; }

    try
     {
      // Attempt to interpret the CoAP response as a JSON object
      try
       { coapResponseJSON = new JSONObject(coapResponseStr); }
      catch(JSONException jsonExcp)
       { throw new ErrCodeExcp(ERR_COAP_CLI_ERRORS_RESP_NOT_JSON,"(\"" + coapResponseStr + "\")"); }

      // Attempt to extract the required "errCode" attribute from the CoAP response
      actuatorErrCode = getActuatorErrCode(coapResponseJSON,coapResponseStr);

      // Attempt to extract the required "clientIP" attribute from the CoAP response
      actuatorErrClientIP = getActuatorErrClientIP(coapResponseJSON,coapResponseStr);

      // Attempt to extract the optional "errDscr" attribute from the CoAP response
      actuatorErrDscr = getActuatorErrDscr(coapResponseJSON,coapResponseStr);

      // Log the reported actuator error depending on whether an additional description was provided
      if(actuatorErrDscr == null)
       Log.code(actuatorErrCode,controlActuatorManager.ID,"(clientIP = " + actuatorErrClientIP + ")");
      else
       Log.code(actuatorErrCode,controlActuatorManager.ID,actuatorErrDscr + " (clientIP = " + actuatorErrClientIP + ")");
     }
    catch(ErrCodeExcp errCodeExcp)
     { Log.excp(errCodeExcp); }
   }


  @Override
  public void onError()
   {
    // Log the error
    Log.err("An error occurred in observing the \"errors\" resource on actuator"
      + controlActuatorManager.ID + ", attempting to re-establish the observing relationship");
   }
 }
