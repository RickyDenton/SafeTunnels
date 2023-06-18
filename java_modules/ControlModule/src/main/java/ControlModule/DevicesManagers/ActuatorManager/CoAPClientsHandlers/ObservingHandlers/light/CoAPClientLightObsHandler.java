/* Californium CoAP Client "Light" Resource Observing Handler */

package ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.ObservingHandlers.light;

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
import devices.actuator.BaseActuator.LightState;
import ControlModule.DevicesManagers.ActuatorManager.ControlActuatorManager;
import static ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.ObservingHandlers.light.CoAPClientLightHandlerObsErrCode.*;


/* ============================== CLASS DEFINITION ============================== */
public final class CoAPClientLightObsHandler implements CoapHandler
 {
  // A reference to the associated ControlActuatorManager object
  private final ControlActuatorManager controlActuatorManager;

  // Whether the error that the actuator cannot
  // accept further observers has been notified
  private boolean notifyTooManyObserversError;


  /* ============================== PRIVATE METHODS ============================== */

  /* ---------------- Received CoAP GET Response Parsing Methods ---------------- */

  /**
   * Retrieves the "lightState" attribute from a CoAP response
   * received by an actuator interpreted as a JSON object
   * @param coapResponseJSON The received CoAP response
   *                         interpreted as a JSON object
   * @param coapResponseStr  The received CoAP response
   *                         interpreted as a String
   * @return The "lightState" attribute in the CoAP response
   * @throws ErrCodeExcp The "lightState" value could not be
   *                     interpreted as a valid actuator LightState
   */
  private LightState getActuatorLightState(JSONObject coapResponseJSON, String coapResponseStr) throws ErrCodeExcp
   {
    // Ascertain the received CoAP response to contain the required
    // "lightState" attribute, throwing an exception otherwise
    if(!coapResponseJSON.has("lightState"))
     throw new ErrCodeExcp(ERR_COAP_CLI_LIGHT_LIGHTSTATE_MISSING,
                   "(\"" + coapResponseStr + "\")");

    try
     {
      // The CoAP response's "lightState" mapped into a LightState
      LightState actuatorLightState;

      // Attempt to interpret the CoAP response's "lightState" attribute as an integer and map
      // it into a LightState, throwing an exception if no LightState of such index exists
      try
       { actuatorLightState = LightState.values[coapResponseJSON.getInt("lightState")]; }
      catch(ArrayIndexOutOfBoundsException outOfBoundsException)
       { throw new ErrCodeExcp(ERR_COAP_CLI_LIGHT_LIGHTSTATE_UNKNOWN,
                       "(\"" + coapResponseStr + "\")"); }

      // Return the valid actuator LightState
      return actuatorLightState;
     }

    // If the CoAP response "lightState" attribute could
    // not be interpreted as an integer, throw an exception
    catch(JSONException jsonExcp)
     { throw new ErrCodeExcp(ERR_COAP_CLI_LIGHT_LIGHTSTATE_NOT_INT,
                     "(\"" + coapResponseStr + "\")"); }
   }


  /* ============================= PUBLIC METHODS ============================= */

  /**
   * CoAP Client "Light" Resource Observing Handler, initializing its attributes
   * @param controlActuatorManager A reference to the associated
   *                               ControlActuatorManager object
   */
  public CoAPClientLightObsHandler(ControlActuatorManager controlActuatorManager)
   {
    this.controlActuatorManager = controlActuatorManager;
    this.notifyTooManyObserversError = true;
   }


  /* ------------ Californium CoAP Client Observing Callback Methods ------------ */

  /**
   * Callback method invoked by the Californium CoAP client whenever a
   * CoAP response is received, i.e. a light state was sent by the actuator
   * @param coapResponse A reference to the received CoAP (error) response
   */
  @Override
  public void onLoad(CoapResponse coapResponse)
   {
    // The CoAP response payload interpreted
    // as a JSON object and as a String
    JSONObject coapResponseJSON;
    String coapResponseStr;

    // The received actuator light state value
    LightState lightState;

    // Ensure that a successful CoAP response
    // (as for the protocol) was received
    if(!coapResponse.isSuccess())
     {
      // If the actuator rejected observing the resource because it has
      // too many observers registered already (5.03, Too Many Observers)
      if(coapResponse.getCode().value == 163)
       {
        // If it is the first time this error was received, log it and
        // notify that external actuator light state modifications will
        // not be reflected in the GUI until the device is rebooted
        if(notifyTooManyObserversError)
         {
          Log.warn("actuator" + controlActuatorManager.ID + " rejected "
            + "observing the \"light\" resource because it has too many observers "
            + "already, external actuator light state modifications will NOT be "
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
       Log.err("The light resource observe handler on actuator" + controlActuatorManager.ID
                + " received an unsuccessful response " + (coapResponse.getCode()));

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
       { throw new ErrCodeExcp(ERR_COAP_CLI_LIGHT_RESP_NOT_JSON,
                       "(\"" + coapResponseStr + "\")"); }

      // Attempt to extract the required "lightState"
      // attribute from the CoAP response
      lightState = getActuatorLightState(coapResponseJSON,coapResponseStr);

      // Call the associated actuator's new light state handler
      controlActuatorManager.setLightState(lightState);
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
    Log.err("An error occurred in observing the \"light\" resource on actuator"
            + controlActuatorManager.ID + ", attempting to re-establish the observing relationship");

    /*
     * Note that in this case the observing handler is automatically cancelled
     * by Californium, with the Actuator Watchdog TimerTask, if the actuator
     * is online, that will attempt to reinitialize it at its next execution
     */
   }
 }