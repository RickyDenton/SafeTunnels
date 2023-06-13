package ControlModule.DevicesManagers.ActuatorManager;

import ControlModule.ControlModule;
import ControlModule.ControlMySQLConnector.ControlMySQLConnector;
import ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.RequestsHandlers.fan.CoAPClientFanReqHandler;
import ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.RequestsHandlers.light.CoAPClientLightReqHandler;
import devices.actuator.BaseActuator;
import errors.ErrCodeExcp;
import logging.Log;

import javax.swing.*;

import java.util.Timer;
import java.util.TimerTask;


import ControlModule.OpState;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.ClientObserveRelation;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Response;
import org.json.JSONException;
import org.json.JSONObject;

import static ControlModule.OpState.*;
import static devices.actuator.BaseActuator.ActuatorQuantity.FANRELSPEED;
import static devices.actuator.BaseActuator.ActuatorQuantity.LIGHTSTATE;
import static devices.actuator.BaseActuator.LightState.LIGHT_OFF;
import static devices.sensor.BaseSensor.SensorQuantity.C02;
import static modules.SensorsMQTTHandler.SensorsMQTTHandlerErrCode.ERR_MQTT_AVGFANRELSPEED_VALUE_INVALID;
import static modules.SensorsMQTTHandler.SensorsMQTTHandlerErrCode.ERR_MQTT_MSG_NOT_JSON;


public class ControlActuatorManager extends BaseActuator
 {
  private final static int actuatorWatcherTimerInitDelay = 3 * 1000;  // milliseconds

  // MUST BE > COAP CLIENT TIMEOUTS
  private final static int actuatorWatcherTimerPeriod = 10 * 1000;  // milliseconds

  // TODO: Probably not required
  // private final static int coapClientsReqTimeout = 10 * 1000;

  // Actuator quantities
  private short fanRelSpeed;
  private LightState lightState;

  // Controller Module reference
  private final ControlModule controlModule;

  // Control Module MySQL Connector
  private final ControlMySQLConnector controlMySQLConnector;

  // The actuator's IPv6 address (e.g. fd00::0202:0002:0002:0002)
  private final String actuatorIPv6Addr;

  // The actuator's CoAP endpoint (e.g. coap://[fd00::0202:0002:0002:0002]/)
  private final String actuatorCoAPEndpoint;

  // Actuator Resources CoAP Clients
  final CoapClient coapClientFan;
  final CoapClient coapClientLight;
  final CoapClient coapClientErrors;


  /*

  // CoAP Client Observing Establishment Timer
  private final Timer coapClientFanObsTimer;
  private final Timer coapClientLightObsTimer;
  private final Timer coapClientErrorsObsTimer;

  // The time in Unix epochs the CoAP actuator was last pinged
  Long lastPingTime;

  // Whether the actuator is online as a wrapper
  Boolean connStateBoolean;
  */

  // Whether the actuator is bound to an actuator widget in the GUI
  private boolean GUIBound;

  // The GUI's JLabels associated with the actuator, if any
  private JLabel connStateLEDIcon;
  private JLabel fanRelSpeedLabel;
  private JLabel lightStateLabel;
  private JLabel fanIcon;
  private JLabel lightIcon;
  private JSlider fanRelSpeedSlider;


  // Binds this actuator to an actuator widget in the GUI
  public void bindToGUI(JLabel connStateLEDIcon, JLabel fanRelSpeedLabel, JLabel lightStateLabel,
                        JLabel fanIcon, JLabel lightIcon, JSlider fanRelSpeedSlider)
   {
    if(connStateLEDIcon == null || fanRelSpeedLabel == null || lightStateLabel == null ||
      fanIcon == null || lightIcon == null || fanRelSpeedSlider == null)
     {
      Log.err("Attempting to bind actuator" + ID + "to null elements in the GUI");
      return;
     }

    // Assign the GUI's components associated with the actuator
    this.connStateLEDIcon = connStateLEDIcon;
    this.fanRelSpeedLabel = fanRelSpeedLabel;
    this.lightStateLabel = lightStateLabel;
    this.fanIcon = fanIcon;
    this.lightIcon = lightIcon;
    this.fanRelSpeedSlider = fanRelSpeedSlider;

    // Set that the actuator is now bound to an actuator widget in the GUI
    GUIBound = true;
   }

  public ControlActuatorManager(String MAC,short ID,ControlModule controlModule,ControlMySQLConnector controlMySQLConnector)
   {
    // Call the parent's constructor, initializing the sensor's connState to false
    super(MAC,ID);

    // Initialize the other ControlActuatorManager's attributes
    this.controlMySQLConnector = controlMySQLConnector;
    this.controlModule = controlModule;
    fanRelSpeed = -1;
    lightState = LIGHT_OFF;
    GUIBound = false;
    this.connStateLEDIcon = null;
    this.fanRelSpeedLabel = null;
    this.lightStateLabel = null;
    this.fanIcon = null;
    this.lightIcon = null;
    this.fanRelSpeedSlider = null;

    /* ---- Actuator Global IPv6 Address and CoAP Endpoint Initialization ---- */

    // Initialize a StringBuilder pruning all ':'
    // characters from the actuator's MAC address
    StringBuilder IID = new StringBuilder(MAC.replace(":",""));

    // Mimic the Contiki-NG uip algorithm for generating 64-bit
    // interface IDs from the nodes' MAC addresses by XOR-ing
    // the first MAC address byte with the constant '0x02'
    byte secByte = (byte)Integer.parseInt(IID.substring(0,2));
    secByte = (byte)(secByte ^ 0x02);
    IID.replace(0,2,String.format("%02d", (int)secByte));

    // Build the actuator's IPv6 interface ID by
    // adding a colon every 4 hexadecimal characters
    for(int i = 4; i < IID.length(); i+=5)
     IID.insert(i, ":");

    // Set the actuator's global IPv6 address
    actuatorIPv6Addr = "fd00::" + IID;

    // Set the actuator's CoAP endpoint
    actuatorCoAPEndpoint = "coap://[" + actuatorIPv6Addr + "]/";

    /*/ --- Actuator Global IPv6 Address and CoAP Endpoint Initialization --- /*/

    /*
     * Initialize the actuator resources' CoAP Clients
     *
     * NOTE: The CoAP clients use confirmable (CON) messages by default
     */
    coapClientFan = new CoapClient(actuatorCoAPEndpoint + actuatorFanRelSpeedResRelPath);
    coapClientLight = new CoapClient(actuatorCoAPEndpoint + actuatorLightStateResRelPath);
    coapClientErrors = new CoapClient(actuatorCoAPEndpoint + actuatorErrorsResRelPath);

    // Initialize the Actuator Watchdog Timer
    Timer actuatorWatchdogTimer = new Timer();
    actuatorWatchdogTimer.scheduleAtFixedRate(new ActuatorWatchdogTimerTask(this,coapClientFan,
                                                                             coapClientLight,coapClientErrors),
                                                                             actuatorWatcherTimerInitDelay,actuatorWatcherTimerPeriod);
   }


  public void setConnStateOffline()
   {
    // Set the actuator as offline
    connState = false;

    // Attempt to push the 'OFFLINE' actuator connState into the database
    controlMySQLConnector.pushActuatorConnState(ID, false);

    // Log that the actuator appears to be offline
    Log.warn("actuator" + ID + " appears to be offline");

    // If bound to a GUI widget, update the
    // associated connection status LED icon
    if(GUIBound)
     { connStateLEDIcon.setIcon(ControlModule.connStateLEDOFFImg); }
   }



  public void setConnStateOnline()
   {
    // Set the actuator as online
    connState = true;

    // Attempt to push the 'ONLINE' actuator connState into the database
    controlMySQLConnector.pushActuatorConnState(ID, true);

    // Log that the actuator is now online
    Log.info("actuator" + ID + " is now online");

    // If bound to a GUI widget, update the
    // associated connection status LED icon
    if(GUIBound)
     { connStateLEDIcon.setIcon(ControlModule.connStateLEDONImg); }
   }


  @Override
  public void setFanRelSpeed(int newFanRelSpeed)
   {
    // If bound to a GUI widget, update its associated
    // fan speed value and set the slider to match
    if(GUIBound)
     {
      fanRelSpeedLabel.setText(newFanRelSpeed + " %");
      fanRelSpeedSlider.setValue(newFanRelSpeed);

      // TODO: Possibly Rotate the fan speed icon via a timer
     }


    // If this is not a periodic fan relative speed
    // observing refresh, i.e. its value has changed
    if(newFanRelSpeed != this.fanRelSpeed)
     {
      // Update the actuator's fan relative speed value
      this.fanRelSpeed = (short)newFanRelSpeed;

      // Attempt to push the updated actuator fan
      // relative speed value into the database
      controlMySQLConnector.pushActuatorQuantityValue(ID,FANRELSPEED,fanRelSpeed);

      // Log the new actuator fan relative speed
      Log.dbg("New actuator" + ID + " fan relative speed: " + fanRelSpeed);

      // Update the system's average fan relative speed
      controlModule.updateAvgFanRelSpeed();
     }
   }


  @Override
  public void setLightState(LightState newLightState)
   {
    // If bound to a GUI widget, update its
    // associated label and light state icon
    // TODO: Possibly use a single, blinking light via a timer instead
    if(GUIBound)
     {
      switch(newLightState)
       {
        case LIGHT_OFF:
         lightStateLabel.setText("OFF");
         lightStateLabel.setForeground(NOMINAL.getColor());
         lightIcon.setIcon(ControlModule.actuatorLightOFFImg);
         break;

        case LIGHT_ON:
         lightStateLabel.setText("WARN");
         lightStateLabel.setForeground(WARNING.getColor());
         lightIcon.setIcon(ControlModule.actuatorLightWARNINGImg);
         break;

        case LIGHT_BLINK_ALERT:
         lightStateLabel.setText("ALERT");
         lightStateLabel.setForeground(ALERT.getColor());
         lightIcon.setIcon(ControlModule.actuatorLightALERTImg);
         break;

        case LIGHT_BLINK_EMERGENCY:
         lightStateLabel.setText("EMER.");
         lightStateLabel.setForeground(EMERGENCY.getColor());
         lightIcon.setIcon(ControlModule.actuatorLightEMERGENCYImg);
         break;
       }
     }

    // If this is not a periodic light state
    // observing refresh, i.e. its value has changed
    if(newLightState != this.lightState)
     {
      // Update the actuator's light state value
      this.lightState = newLightState;

      // Attempt to push the updated actuator light state into the database
      controlMySQLConnector.pushActuatorQuantityValue(ID,LIGHTSTATE,lightState.ordinal());

      // Log the new actuator light state
      Log.dbg("New actuator" + ID + " light state: " + lightState);
     }
   }

  public short getFanRelSpeed()
   { return this.fanRelSpeed; }


  public void sendFanRelSpeed(int sendFanRelSpeed)
   {
    // Ensure the fan relative speed to be sent to the actuator to be valid
    if(sendFanRelSpeed < 0 || sendFanRelSpeed > 100)
     {
      Log.err("Attempting to send to actuator" + ID + " an invalid fan relative speed (" + sendFanRelSpeed + ")");
      return;
     }

    // Ensure the actuator to be online
    if(!connState)
     {
      Log.warn("Cannot send an updated fan relative speed (" + sendFanRelSpeed + ") to actuator" + ID + " because it is offline");
      return;
     }

    // Ensure that the fan relative speed to be sent differs from its current one
    if(sendFanRelSpeed == fanRelSpeed)
     {
      Log.warn("Attempting to send to actuator" + ID + " the same fan relative speed (" + sendFanRelSpeed + ")");
      return;
     }

    // Send the fan relative speed using an asynchronous, confirmable PUT request
    coapClientFan.put(new CoAPClientFanReqHandler(ID,sendFanRelSpeed), "fanRelSpeed=" + sendFanRelSpeed,0); // 0 = text/plain
   }

  public void sendLightState(LightState sendLightState)
   {
    // Ensure the actuator to be online
    if(!connState)
     {
      Log.warn("Cannot send an updated light state (" + sendLightState.toString() + ") to actuator" + ID + " because it is offline");
      return;
     }

    // Ensure that the light state to be sent differs from its current one
    if(sendLightState == lightState)
     {
      Log.warn("Attempting to send to actuator" + ID + " the same light state (" + sendLightState + ")");
      return;
     }

    // Send the light state using an asynchronous, confirmable PUT request
    coapClientLight.put(new CoAPClientLightReqHandler(ID,sendLightState), "lightState=" + sendLightState.toString(),0); // 0 = text/plain
   }

 }
