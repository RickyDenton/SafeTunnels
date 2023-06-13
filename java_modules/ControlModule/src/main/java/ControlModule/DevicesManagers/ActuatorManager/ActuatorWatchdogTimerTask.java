package ControlModule.DevicesManagers.ActuatorManager;

import ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.ObservingHandlers.actuatorErrors.CoAPClientErrorsObsHandler;
import ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.ObservingHandlers.fan.CoAPClientFanObsHandler;
import ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.ObservingHandlers.light.CoAPClientLightObsHandler;
import logging.Log;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.ClientObserveRelation;

import java.util.TimerTask;

class ActuatorWatchdogTimerTask extends TimerTask
 {
  // The CoAP standard 'accept' JSON value
  public static final int COAP_ACCEPT_JSON = 50;

  // Control actuator manager reference
  ControlActuatorManager ctrlActuatorManager;

  // Whether the actuator was ever pinged (used to push
  // the first 'OFFLINE' connState into the database)
  boolean actuatorWasPinged;

  // Actuator Resources CoAP Clients References
  final CoapClient coapClientFan;
  final CoapClient coapClientLight;
  final CoapClient coapClientErrors;

  // CoAP Clients Observe Relationships
  ClientObserveRelation coapClientFanObserveRel;
  ClientObserveRelation coapClientLightObserveRel;
  ClientObserveRelation coapClientErrorsObserveRel;

  ActuatorWatchdogTimerTask(ControlActuatorManager ctrlActuatorManager, CoapClient coapClientFan, CoapClient coapClientLight, CoapClient coapClientErrors)
   {
    this.ctrlActuatorManager = ctrlActuatorManager;
    actuatorWasPinged = false;
    this.coapClientFan = coapClientFan;
    this.coapClientLight = coapClientLight;
    this.coapClientErrors = coapClientErrors;
    coapClientFanObserveRel = null;
    coapClientLightObserveRel = null;
    coapClientErrorsObserveRel = null;
   }

  @Override
  public void run()
   {
    boolean actuatorRepliedPing;

    // If all CoAP clients observe relationships are established, nothing needs to be done
    if((coapClientFanObserveRel!=null && !coapClientFanObserveRel.isCanceled()) && (coapClientLightObserveRel!=null && !coapClientLightObserveRel.isCanceled()) && (coapClientErrorsObserveRel!=null
      && !coapClientErrorsObserveRel.isCanceled()))
     return;

    // Otherwise, verify whether the actuator is
    // online by CoAP-pinging it using any CoAP client
    actuatorRepliedPing = coapClientFan.ping(3000); // in ms

    // If the actuator is online and was online or this is the first ping,
    // attempt to push its updated 'OFFLINE' connState into the database
    if(!actuatorRepliedPing && (ctrlActuatorManager.getConnState() || !actuatorWasPinged))
     {
      ctrlActuatorManager.setConnStateOffline();
      actuatorWasPinged = true;
     }

    // Otherwise, if the actuator is now online
    else
     if(actuatorRepliedPing)
      {
       // If the actuator was previously offline, attempt to push
       // its updated 'ONLINE' connState into the database
       if(!ctrlActuatorManager.getConnState())
        ctrlActuatorManager.setConnStateOnline();

       // In any case, attempt to make the CoAP clients observe their associated
       // resources if not already observing, specifying via the CoAP 'accept'
       // attribute that they expected responses to be in JSON format
       if(coapClientFanObserveRel == null || coapClientFanObserveRel.isCanceled())
        coapClientFanObserveRel = coapClientFan.observe(new CoAPClientFanObsHandler(ctrlActuatorManager,coapClientFanObserveRel),COAP_ACCEPT_JSON);

       if(coapClientLightObserveRel == null || coapClientLightObserveRel.isCanceled())
        coapClientLightObserveRel = coapClientLight.observe(new CoAPClientLightObsHandler(ctrlActuatorManager,coapClientLightObserveRel),COAP_ACCEPT_JSON);

       if(coapClientErrorsObserveRel == null || coapClientErrorsObserveRel.isCanceled())
        coapClientErrorsObserveRel = coapClientErrors.observe(new CoAPClientErrorsObsHandler(ctrlActuatorManager,coapClientErrorsObserveRel),COAP_ACCEPT_JSON);

       // If all CoAP clients are now observing their associated resources, log it
       if((coapClientFanObserveRel!=null && !coapClientFanObserveRel.isCanceled()) && (coapClientLightObserveRel!=null && !coapClientLightObserveRel.isCanceled()) && (coapClientErrorsObserveRel!=null
         && !coapClientErrorsObserveRel.isCanceled()))
        Log.dbg("Successfully observing all actuator" + ctrlActuatorManager.ID + " resources");
      }
   }
 }