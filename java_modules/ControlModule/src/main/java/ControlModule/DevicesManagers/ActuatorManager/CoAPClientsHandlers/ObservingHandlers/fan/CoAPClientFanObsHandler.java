/* Californium CoAP Client "Fan" Resource Observing Handler */

package ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.ObservingHandlers.fan;

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
import ControlModule.DevicesManagers.ActuatorManager.ControlActuatorManager;


/* ============================== CLASS DEFINITION ============================== */
public class CoAPClientFanObsHandler implements CoapHandler
 {
  // A reference to the associated ControlActuatorManager object
  private final ControlActuatorManager controlActuatorManager;

  // Whether the error that the actuator cannot
  // accept further observers has been notified
  private boolean notifyTooManyObserversError;

  /* ============================== PRIVATE METHODS ============================== */

  /* ---------------- Received CoAP GET Response Parsing Methods ---------------- */

  /**
   * Retrieves the "fanRelSpeed" attribute from a CoAP response
   * received by an actuator interpreted as a JSON object
   * @param coapResponseJSON The received CoAP response
   *                         interpreted as a JSON object
   * @param coapResponseStr  The received CoAP response
   *                         interpreted as a String
   * @return The "fanRelSpeed" attribute in the CoAP response
   * @throws ErrCodeExcp The "fanRelSpeed" attribute is missing or its
   *                     value could not be interpreted as an integer
   */
  private int getActuatorFanRelSpeed(JSONObject coapResponseJSON, String coapResponseStr) throws  ErrCodeExcp
   {
    // Ascertain the received CoAP response message to contain the
    // required "fanRelSpeed" attribute, throwing an exception otherwise
    if(!coapResponseJSON.has("fanRelSpeed"))
     throw new ErrCodeExcp(CoAPClientFanHandlerObsErrCode.ERR_COAP_CLI_FAN_FANRELSPEED_MISSING,
                   "(\"" + coapResponseStr + "\")");

    // Attempt to extract the "fanRelSpeed" attribute
    // as an integer, throwing an exception otherwise
    try
     { return coapResponseJSON.getInt("fanRelSpeed"); }
    catch(JSONException jsonExcp)
     { throw new ErrCodeExcp(
       CoAPClientFanHandlerObsErrCode.ERR_COAP_CLI_FAN_FANRELSPEED_NOT_INT,
                              "(\"" + coapResponseStr + "\")"); }
   }


  /* ============================= PUBLIC METHODS ============================= */

  /**
   * CoAP Client "Fan" Resource Observing Handler, initializing its attributes
   * @param controlActuatorManager A reference to the associated
   *                               ControlActuatorManager object
   */
  public CoAPClientFanObsHandler(ControlActuatorManager controlActuatorManager)
   {
    this.controlActuatorManager = controlActuatorManager;
    this.notifyTooManyObserversError = true;
   }


  /* ------------ Californium CoAP Client Observing Callback Methods ------------ */

  /**
   * Callback method invoked by the Californium CoAP client whenever a CoAP
   * response is received, i.e. a fan relative speed value was sent by the actuator
   * @param coapResponse A reference to the received CoAP (error) response
   */
  @Override
  public void onLoad(CoapResponse coapResponse)
   {
    // The CoAP response payload interpreted
    // as a JSON object and as a String
    JSONObject coapResponseJSON;
    String coapResponseStr;

    // The received actuator fanRelSpeed value
    int fanRelSpeed;

    // Ensure that a successful CoAP response
    // (as for the protocol) was received
    if(!coapResponse.isSuccess())
     {
      // If the actuator rejected observing the resource because it has
      // too many observers registered already (5.03, Too Many Observers)
      if(coapResponse.getCode().value == 163)
       {
        // If it is the first time this error was received, log it and notify
        // that external actuator fan relative speed modifications will
        // not be reflected in the GUI until the device is rebooted
        if(notifyTooManyObserversError)
         {
          Log.warn("actuator" + controlActuatorManager.ID + " rejected observing "
                   + "the \"fan\" resource because it has too many observers already, "
                   + "external actuator fan relative speed modifications will NOT be "
                   + "reflected in the GUI (reboot the actuator to fix the problem)");

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
       Log.err("The fan resource observe handler on actuator" + controlActuatorManager.ID +
                " received an unsuccessful response " + (coapResponse.getCode()));

      // In any case, abort the handler in case
      // of an unsuccessful CoAP response
      return;
     }

    // Interpret the CoAP response's payload as a String
    coapResponseStr = coapResponse.getResponseText();

    try
     {
      // Attempt to interpret the CoAP response as a
      // JSON object, throwing an exception otherwise
      try
       { coapResponseJSON = new JSONObject(coapResponseStr); }
      catch(JSONException jsonExcp)
       { throw new ErrCodeExcp(CoAPClientFanHandlerObsErrCode.ERR_COAP_CLI_FAN_RESP_NOT_JSON,
                       "(\"" + coapResponseStr + "\")"); }

      // Attempt to extract the required "fanRelSpeed"
      // attribute from the CoAP response
      fanRelSpeed = getActuatorFanRelSpeed(coapResponseJSON,coapResponseStr);

      // Ascertain the received fan relative speed value
      // to be valid, throwing an exception otherwise
      if(fanRelSpeed < 0 || fanRelSpeed > 100)
       throw new ErrCodeExcp(CoAPClientFanHandlerObsErrCode.ERR_COAP_CLI_FAN_FANRELSPEED_INVALID,
                     "(" + fanRelSpeed + ")");

      // Call the associated actuator's new fan relative speed handler
      controlActuatorManager.setFanRelSpeed(fanRelSpeed);
     }

    // If an error has occurred in parsing
    // the received CoAP response, log it
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
    Log.err("An error occurred in observing the \"fan\" resource on actuator"
      + controlActuatorManager.ID + ", attempting to re-establish the observing relationship");

    /*
     * Note that in this case the observing handler is automatically cancelled
     * by Californium, with the Actuator Watchdog TimerTask, if the actuator
     * is online, that will attempt to reinitialize it at its next execution
     */
   }
 }