/*
 * Actuator Watchdog Timer Task, checking whether the actuator is online
 * and attempting to establish observing relationships on its resources
 */

package ControlModule.DevicesManagers.ActuatorManager;

/* ================================== IMPORTS ================================== */

/* --------------------- Java Standard Libraries Resources --------------------- */
import java.util.TimerTask;

/* --------------------- Californium CoAP Client Resources --------------------- */
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.ClientObserveRelation;

/* --------------------------- SafeTunnels Resources --------------------------- */
import logging.Log;
import ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.ObservingHandlers.actuatorErrors.CoAPClientErrorsObsHandler;
import ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.ObservingHandlers.fan.CoAPClientFanObsHandler;
import ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.ObservingHandlers.light.CoAPClientLightObsHandler;


/* ============================== CLASS DEFINITION ============================== */
final class ActuatorWatchdogTimerTask extends TimerTask
 {
  /* ============================ PRIVATE ATTRIBUTES ============================ */

  // The CoAP standard 'accept' JSON value
  private static final int COAP_ACCEPT_JSON = 50;

  // A reference to the associated ControlActuatorManager
  private final ControlActuatorManager ctrlActuatorManager;

  // Whether the actuator was ever pinged (used to push
  // the first 'OFFLINE' connState into the database)
  private boolean actuatorWasPinged;

  /* --------------------------- Californium Objects --------------------------- */

  // Actuator Resources Californium CoAP Clients References
  private final CoapClient coapClientFan;
  private final CoapClient coapClientLight;
  private final CoapClient coapClientErrors;

  // Californium CoAP Clients Observe Relationships
  private ClientObserveRelation coapClientFanObserveRel;
  private ClientObserveRelation coapClientLightObserveRel;
  private ClientObserveRelation coapClientErrorsObserveRel;

  // Californium CoAP Client Observe Handlers
  private final CoAPClientFanObsHandler    fanObsHandler;
  private final CoAPClientLightObsHandler  lightObsHandler;
  private final CoAPClientErrorsObsHandler errorsObsHandler;


  /* ============================= PACKAGE METHODS ============================= */

  /**
   * Actuator Watchdog TimerTask constructor, initializing its attributes
   * to their default values and the CoAP resources' observing handlers
   * @param ctrlActuatorManager A reference to the associated ControlActuatorManager
   * @param coapClientFan       A reference to the Californium's "Fan" CoAP Client
   * @param coapClientLight     A reference to the Californium's "Light" CoAP Client
   * @param coapClientErrors    A reference to the Californium's "Errors" CoAP Client
   */
  ActuatorWatchdogTimerTask(ControlActuatorManager ctrlActuatorManager, CoapClient coapClientFan, CoapClient coapClientLight, CoapClient coapClientErrors)
   {
    // Initialize the object's attribute to their default value
    this.ctrlActuatorManager = ctrlActuatorManager;
    actuatorWasPinged = false;
    this.coapClientFan = coapClientFan;
    this.coapClientLight = coapClientLight;
    this.coapClientErrors = coapClientErrors;
    coapClientFanObserveRel = null;
    coapClientLightObserveRel = null;
    coapClientErrorsObserveRel = null;

    // Initialize the CoAP resources' observers handlers
    fanObsHandler = new CoAPClientFanObsHandler(ctrlActuatorManager);
    lightObsHandler = new CoAPClientLightObsHandler(ctrlActuatorManager);
    errorsObsHandler = new CoAPClientErrorsObsHandler(ctrlActuatorManager);
   }


  /**
   * Actuator Watchdog TimerTask run() method, executed at the fixed
   * "actuatorWatcherTimerPeriod" specified in the "ControlActuatorManager" class
   */
  @Override
  public void run()
   {
    // Whether the actuator has replied to a CoAP Ping
    boolean actuatorRepliedCoAPPing;

    // If all CoAP clients observe relationships are establishing, no operation is necessary
    if((coapClientFanObserveRel    != null && !coapClientFanObserveRel.isCanceled())     &&
       (coapClientLightObserveRel  != null && !coapClientLightObserveRel.isCanceled())   &&
       (coapClientErrorsObserveRel != null && !coapClientErrorsObserveRel.isCanceled()))
     return;

    // Otherwise, if at least one observe relationship is not established,
    // ascertain whether the actuator is online by CoAP-pinging it
    actuatorRepliedCoAPPing = coapClientFan.ping(3000); // Ping timeout in milliseconds

    // If the actuator didn't reply to the ping and
    // was either online or this was the first CoAP ping
    if(!actuatorRepliedCoAPPing && (ctrlActuatorManager.getConnState() || !actuatorWasPinged))
     {
      // Call the actuator's disconnection handler
      ctrlActuatorManager.setConnStateOffline();

      // Set that the actuator was pinged
      actuatorWasPinged = true;

      /*
       * No other operations can be performed
       * being the actuator supposedly offline
       */
     }

    // Otherwise, if the actuator replied to the ping and is so online
    else
     if(actuatorRepliedCoAPPing)
      {
       // If the actuator was previously
       // offline, call its connection handler
       if(!ctrlActuatorManager.getConnState())
        ctrlActuatorManager.setConnStateOnline();

       /*
        * Attempt to make all Californium CoAP client observe their associated
        * resources if not already doing so,  specifying via the CoAP
        * 'accept' attribute that they expected responses in JSON format
        */

       // "Fan" CoAP Client observe establishment
       if(coapClientFanObserveRel == null || coapClientFanObserveRel.isCanceled())
        coapClientFanObserveRel = coapClientFan.observe(fanObsHandler,COAP_ACCEPT_JSON);

       // "Light" CoAP Client observe establishment
       if(coapClientLightObserveRel == null || coapClientLightObserveRel.isCanceled())
        coapClientLightObserveRel = coapClientLight.observe(lightObsHandler,COAP_ACCEPT_JSON);

       // "Errors" CoAP Client observe establishment
       if(coapClientErrorsObserveRel == null || coapClientErrorsObserveRel.isCanceled())
        coapClientErrorsObserveRel = coapClientErrors.observe(errorsObsHandler,COAP_ACCEPT_JSON);

       // If all Californium CoAP clients are now observing their associated resources, log it
       if((coapClientFanObserveRel    != null && !coapClientFanObserveRel.isCanceled())    &&
          (coapClientLightObserveRel  != null && !coapClientLightObserveRel.isCanceled())  &&
          (coapClientErrorsObserveRel != null && !coapClientErrorsObserveRel.isCanceled()))
        Log.dbg("Successfully observing all actuator" + ctrlActuatorManager.ID + " resources");
      }
   }
 }