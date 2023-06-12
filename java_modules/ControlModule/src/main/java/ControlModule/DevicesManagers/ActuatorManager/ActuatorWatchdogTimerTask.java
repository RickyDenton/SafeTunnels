package ControlModule.DevicesManagers.ActuatorManager;

import ControlModule.DevicesManagers.ActuatorManager.CoAPClientsObservingHandlers.CoAPClientErrorsHandler.CoAPClientErrorsHandler;
import ControlModule.DevicesManagers.ActuatorManager.CoAPClientsObservingHandlers.CoAPClientFanHandler.CoAPClientFanHandler;
import ControlModule.DevicesManagers.ActuatorManager.CoAPClientsObservingHandlers.CoAPClientLightHandler.CoAPClientLightHandler;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.ClientObserveRelation;

import java.util.TimerTask;

class ActuatorWatchdogTimerTask extends TimerTask
 {
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
    actuatorRepliedPing = coapClientFan.ping(2000); // in ms

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

       // In any case, attempt to make the CoAP clients observe their
       // associated resources if they are not already doing so
       if(coapClientFanObserveRel == null || coapClientFanObserveRel.isCanceled())
        coapClientFanObserveRel = coapClientFan.observe(new CoAPClientFanHandler(ctrlActuatorManager,coapClientFanObserveRel));

       if(coapClientLightObserveRel == null || coapClientLightObserveRel.isCanceled())
        coapClientLightObserveRel = coapClientLight.observe(new CoAPClientLightHandler(ctrlActuatorManager,coapClientLightObserveRel));

       if(coapClientErrorsObserveRel == null || coapClientErrorsObserveRel.isCanceled())
        coapClientErrorsObserveRel = coapClientErrors.observe(new CoAPClientErrorsHandler(ctrlActuatorManager,coapClientErrorsObserveRel));
      }
   }
 }
