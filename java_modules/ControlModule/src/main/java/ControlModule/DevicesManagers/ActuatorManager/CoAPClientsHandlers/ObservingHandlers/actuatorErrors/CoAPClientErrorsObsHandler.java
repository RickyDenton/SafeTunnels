/* Californium CoAP Client "Errors" Resource Observing Handler */

package ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.ObservingHandlers.actuatorErrors;

/* ================================== IMPORTS ================================== */

/* ----------------------- Maven Dependencies Resources ----------------------- */

// Californium CoAP Client
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;

// JSON parser
import org.json.JSONException;
import org.json.JSONObject;

/* --------------------------- SafeTunnels Resources --------------------------- */
import logging.Log;
import errors.ErrCodeExcp;
import devices.actuator.BaseActuatorErrCode;
import ControlModule.DevicesManagers.ActuatorManager.ControlActuatorManager;
import static ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.ObservingHandlers.actuatorErrors.CoAPClientErrorsHandlerObsErrCode.*;

/* ============================== CLASS DEFINITION ============================== */
public final class CoAPClientErrorsObsHandler implements CoapHandler
 {
  /* ============================ PRIVATE ATTRIBUTES ============================ */

  // A reference to the associated ControlActuatorManager object
  private final ControlActuatorManager controlActuatorManager;

  // Whether the error that the actuator cannot
  // accept further observers has been notified
  private boolean notifyTooManyObserversError;


  /* ============================== PRIVATE METHODS ============================== */

  /* ---------------- Received CoAP GET Response Parsing Methods ---------------- */

  /**
   * Retrieves the "BaseActuatorErrCode" attribute from a CoAP error
   * response received by an actuator interpreted as a JSON object
   * @param coapResponseJSON The received CoAP error response
   *                         interpreted as a JSON object
   * @param coapResponseStr  The received CoAP error response
   *                         interpreted as a String
   * @return The "BaseActuatorErrCode" attribute value
   *         in the CoAP response error message
   * @throws ErrCodeExcp The "BaseActuatorErrCode" attribute is missing or its value
   *                     could not be interpreted as a valid BaseActuatorErrCode
   */
  private BaseActuatorErrCode getActuatorErrCode(JSONObject coapResponseJSON, String coapResponseStr) throws  ErrCodeExcp
   {
    // Ascertain the received CoAP error response to contain the
    // required "errCode" attribute, throwing an exception otherwise
    if(!coapResponseJSON.has("errCode"))
     throw new ErrCodeExcp(ERR_COAP_CLI_ERRORS_ERRCODE_MISSING,
                    "(\"" + coapResponseStr + "\")");

    try
     {
      // The CoAP response's "errCode" mapped into a BaseActuatorErrCode
      BaseActuatorErrCode actuatorErrCode;

      // Attempt to interpret the CoAP error response's "errCode" attribute as an integer and map it into
      // a BaseActuatorErrCode, throwing an exception if no BaseActuatorErrCode of such index exists
      try
       { actuatorErrCode = BaseActuatorErrCode.values[coapResponseJSON.getInt("errCode")]; }
      catch(ArrayIndexOutOfBoundsException outOfBoundsException)
       { throw new ErrCodeExcp(ERR_COAP_CLI_ERRORS_ERRCODE_UNKNOWN,
                       "(\"" + coapResponseStr + "\")"); }

      // Return the valid BaseActuatorErrCode
      return actuatorErrCode;
     }

    // If the CoAP error response's "errCode" attribute could
    // not be interpreted as an integer, throw an exception
    catch(JSONException jsonExcp)
     { throw new ErrCodeExcp(ERR_COAP_CLI_ERRORS_ERRCODE_NOT_INT,
                     "(\"" + coapResponseStr + "\")"); }
   }


  /**
   * Retrieves the "CliIP" attribute from a CoAP error response
   * received by an actuator interpreted as a JSON object
   * @param coapResponseJSON The received CoAP error response
   *                         interpreted as a JSON object
   * @param coapResponseStr  The received CoAP error response
   *                         interpreted as a String
   * @return The "CliIP" attribute value in the CoAP response error message
   * @throws ErrCodeExcp The "CliIP" attribute in the CoAP error
   *                     response is missing or is not a non-null String
   */
  private String getActuatorErrClientIP(JSONObject coapResponseJSON,String coapResponseStr) throws  ErrCodeExcp
   {
    // Ascertain the received CoAP error response to contain the
    // required "clientIP" attribute, throwing an exception otherwise
    if(!coapResponseJSON.has("clientIP"))
     throw new ErrCodeExcp(ERR_COAP_CLI_ERRORS_CLIIP_MISSING,
                    "(\"" + coapResponseStr + "\")");

    // Attempt to extract the "clientIP" attribute as a String and
    // ensure it to be non-null, throwing an exception otherwise
    try
     { return coapResponseJSON.getString("clientIP"); }
    catch(JSONException jsonExcp)
     { throw new ErrCodeExcp(ERR_COAP_CLI_ERRORS_CLIIP_NOT_NONNULL_STRING,
                      "(\"" + coapResponseStr + "\")"); }
   }


  /**
   * Retrieves the "errDscr" attribute from a CoAP error response
   * received by an actuator interpreted as a JSON object
   * @param coapResponseJSON The received CoAP error response
   *                         interpreted as a JSON object
   * @param coapResponseStr  The received CoAP error response
   *                         interpreted as a String
   * @return The "errDscr" attribute value in the CoAP response error message
   * @throws ErrCodeExcp The "errDscr" attribute value could not
   *                     be interpreted as a non-null String
   */
  private String getActuatorErrDscr(JSONObject coapResponseJSON,String coapResponseStr) throws  ErrCodeExcp
   {
    // Check whether the received CoAP error response contains
    // the optional "errDscr" attribute and, if it does
    if(coapResponseJSON.has("errDscr"))
     {
      // Attempt to extract the "errDscr" attribute as a String and
      // ensure it to be non-null, throwing an exception otherwise
      try
       { return coapResponseJSON.getString("errDscr"); }
      catch(JSONException jsonExcp)
       { throw new ErrCodeExcp(ERR_COAP_CLI_ERRORS_ERRDSCR_NOT_NONNULL_STRING,
                       "(\"" + coapResponseStr + "\")"); }
     }

    // Otherwise, if the "errDscr" attribute is missing, return null
    else
     return null;
   }


  /* ============================= PUBLIC METHODS ============================= */

  /**
   * CoAP Client "Errors" Resource Observing Handler, initializing its attributes
   * @param controlActuatorManager A reference to the associated
   *                               ControlActuatorManager object
   */
  public CoAPClientErrorsObsHandler(ControlActuatorManager controlActuatorManager)
   {
    this.controlActuatorManager = controlActuatorManager;
    this.notifyTooManyObserversError = true;
   }


  /* ------------ Californium CoAP Client Observing Callback Methods ------------ */

  /**
   * Callback method invoked by the Californium CoAP client whenever a CoAP
   * (error) response is received, i.e. an error was sent by the actuator
   * @param coapResponse A reference to the received CoAP (error) response
   */
  @Override
  public void onLoad(CoapResponse coapResponse)
   {
    // The CoAP error response payload interpreted
    // as a JSON object and as a String
    JSONObject coapResponseJSON;
    String coapResponseStr;

    // The received actuator error code
    BaseActuatorErrCode actuatorErrCode;

    // The IP address of the client whose
    // request to the actuator raised the error
    String actuatorErrClientIP;

    // The received optional error description
    String actuatorErrDscr;

    // Ensure that a successful CoAP response
    // (as for the protocol) was received
    if(!coapResponse.isSuccess())
     {
      // If the actuator rejected observing the resource because it has
      // too many observers registered already (5.03, Too Many Observers)
      if(coapResponse.getCode().value == 163)
       {
        // If it is the first time this error was received,
        // log it and notify that actuator errors will
        // not be reported until the device is rebooted
        if(notifyTooManyObserversError)
         {
          Log.warn("actuator" + controlActuatorManager.ID + " rejected observing "
            + "the \"errors\" resource because it has too many observers already, its "
            + "errors will NOT be reported (reboot the actuator to fix the problem)");

          // Set that the observer subscription error has been notified
          notifyTooManyObserversError = false;
         }

        /*
         * If it is not the first time the error
         * was received, it is silently ignored
         */
       }

      // Otherwise, if it is another error, log it
      else
       Log.err("The errors resource observe handler on actuator" + controlActuatorManager.ID
                + " received an unsuccessful response " + (coapResponse.getCode()));

      // In any case, abort the handler in case
      // of an unsuccessful CoAP response
      return;
     }

    // Interpret the CoAP error response's payload as a String
    coapResponseStr = coapResponse.getResponseText();

    // Empty CoAP error responses are associated with the initial "Errors"
    // observing response and periodic refreshing, and can so be ignored
    if(coapResponseStr.isEmpty())
     return;

    try
     {
      // Attempt to interpret the CoAP error response as
      // a JSON object, throwing an exception otherwise
      try
       { coapResponseJSON = new JSONObject(coapResponseStr); }
      catch(JSONException jsonExcp)
       { throw new ErrCodeExcp(ERR_COAP_CLI_ERRORS_RESP_NOT_JSON,
                       "(\"" + coapResponseStr + "\")"); }

      // Attempt to extract the required "errCode"
      // attribute from the CoAP error response
      actuatorErrCode = getActuatorErrCode(coapResponseJSON,coapResponseStr);

      // Attempt to extract the required "clientIP"
      // attribute from the CoAP error response
      actuatorErrClientIP = getActuatorErrClientIP(coapResponseJSON,coapResponseStr);

      // Attempt to extract the optional "errDscr"
      // attribute from the CoAP error response
      actuatorErrDscr = getActuatorErrDscr(coapResponseJSON,coapResponseStr);

      // Log the reported actuator error depending on
      // whether an additional description was provided
      if(actuatorErrDscr == null)
       Log.code(actuatorErrCode,controlActuatorManager.ID,
         "(clientIP = " + actuatorErrClientIP + ")");
      else
       Log.code(actuatorErrCode,controlActuatorManager.ID,
         actuatorErrDscr + " (clientIP = " + actuatorErrClientIP + ")");
     }

    // If an error has occurred in parsing the
    // received CoAP error response, log it
    catch(ErrCodeExcp errCodeExcp)
     { Log.excp(errCodeExcp); }
   }




  /**
   * Callback method invoked by the Californium CoAP client if observing the
   * resources failed (which is typically due to the actuator being offline)
   */
  @Override
  public void onError()
   {
    // Log the error
    Log.err("An error occurred in observing the \"errors\" resource on actuator"
      + controlActuatorManager.ID + ", attempting to re-establish the observing relationship");

    /*
     * Note that in this case the observing handler is automatically cancelled
     * by Californium, with the Actuator Watchdog TimerTask, if the actuator
     * is online, that will attempt to reinitialize it at its next execution
     */
   }
 }