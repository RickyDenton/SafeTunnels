/* "Light" CoAP Client Asynchronous PUT Request Handler */

package ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.RequestsHandlers.light;

/* ================================== IMPORTS ================================== */

/* --------------------- Californium CoAP Client Resources --------------------- */
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;

/* --------------------------- SafeTunnels Resources --------------------------- */
import logging.Log;
import devices.actuator.BaseActuator.LightState;
import ControlModule.DevicesManagers.ActuatorManager.ControlActuatorManager;

/* ============================== CLASS DEFINITION ============================== */
public final class CoAPClientLightReqHandler implements CoapHandler
 {
  /* ============================ PRIVATE ATTRIBUTES ============================ */

  // A reference to the associated ControlActuatorManager object
  private final ControlActuatorManager ctrlActuatorManager;

  // The light state value that was sent to the actuator
  private final LightState sendLightState;


  /* ============================= PUBLIC METHODS ============================= */

  /**
   * CoAPClientLightReqHandler constructor, initializing its attributes
   * @param ctrlActuatorManager A reference to the associated
   *                            ControlActuatorManager object
   * @param sendLightState The light state value that was sent to the actuator
   */
  public CoAPClientLightReqHandler(ControlActuatorManager ctrlActuatorManager, LightState sendLightState)
   {
    this.ctrlActuatorManager = ctrlActuatorManager;
    this.sendLightState = sendLightState;
   }


  /* ------------ Californium Received PUT Response Callback Methods ------------ */

  /**
   * Callback method invoked by the Californium CoAP client whenever
   * the asynchronous (in this case, PUT) response is received
   * @param coapResponse A reference to the received CoAP response
   */
  @Override
  public void onLoad(CoapResponse coapResponse)
   {
    // If the CoAP response is successful
    if(coapResponse.isSuccess())
     {
      // Log the new light state value sent to the actuator
      Log.dbg("Successfully sent new light state ("
               + sendLightState + ") to actuator" + ctrlActuatorManager.ID);

      /*
       * Directly invoke the actuator's new light state handler without
       * waiting for the associated observer's response, which also allow
       * to update its value if observing on the "light" resource failed
       */
      ctrlActuatorManager.setLightState(sendLightState);
     }

    // Otherwise, if the CoAP response was unsuccessful, log the error
    else
     {
      Log.err("Failed to send new light state (" + sendLightState
               + ") to actuator" + ctrlActuatorManager.ID + " (response "
               + "= " + coapResponse.getCode().toString() + ")");
      Log.err("|-- Response Code: " + coapResponse.getCode().toString());
      Log.err("|-- Payload: " + coapResponse.getResponseText());
     }
   }


  /**
   * Callback method invoked by the Californium CoAP client whenever an
   * error occurred in sending the asynchronous (in this case, PUT) request
   * to the actuator (which is typically due to the actuator being offline)
   */
  @Override
  public void onError()
   {
    // Just log the error
    Log.err("An error occurred in sending the new light "
            + "state (" + sendLightState + ")" + " to actuator"
            + ctrlActuatorManager.ID + " (probably it is offline)");
   }
 }