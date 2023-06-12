package ControlModule.DevicesManagers.ActuatorManager.CoAPClientsObservingHandlers.CoAPClientErrorsHandler;

import ControlModule.DevicesManagers.ActuatorManager.ControlActuatorManager;
import devices.actuator.BaseActuatorErrCode;
import errors.ErrCodeExcp;
import logging.Log;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.ClientObserveRelation;
import org.json.JSONException;
import org.json.JSONObject;


public class CoAPClientErrorsHandler implements CoapHandler
 {
  ControlActuatorManager controlActuatorManager;
  ClientObserveRelation coapClientErrorsObserveRel;


  private BaseActuatorErrCode getActuatorErrCode(JSONObject coapResponseJSON, String coapResponseStr) throws  ErrCodeExcp
   {
    // Ensure the CoAP response to contain the required "errCode" attribute
    if(!coapResponseJSON.has("errCode"))
     throw new ErrCodeExcp(ERR_COAP_CLI_ERRORS_ERRCODE_MISSING,"(\"" + coapResponseStr + "\")");

    try
     {
      // Attempt to extract the response's "errCode" attribute
      BaseActuatorErrCode actuatorErrCode = BaseActuatorErrCode.values()[coapResponseJSON.getInt("errCode")];

      // If the received integer does not map to any valid base actuator error codes, throw an error
      if(actuatorErrCode == null)
       throw new ErrCodeExcp(ERR_COAP_CLI_ERRORS_ERRCODE_UNKNOWN,"(\"" + coapResponseStr + "\")");

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



  public CoAPClientErrorsHandler(ControlActuatorManager controlActuatorManager,ClientObserveRelation coapClientErrorsObserveRel)
   {
    this.controlActuatorManager = controlActuatorManager;
    this.coapClientErrorsObserveRel = coapClientErrorsObserveRel;
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
      Log.code(ERR_COAP_CLI_ERRORS_RESP_UNSUCCESSFUL,"(response = " + coapResponse.getCode() + ")");
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
    Log.code(ERR_COAP_CLI_ERRORS_OBSERVE_ERROR);

    // Proactively cancel the resource observing to attempt to recover
    // from the error at the next actuatorWatchdogTimer execution
    coapClientErrorsObserveRel.proactiveCancel();
   }
 }
