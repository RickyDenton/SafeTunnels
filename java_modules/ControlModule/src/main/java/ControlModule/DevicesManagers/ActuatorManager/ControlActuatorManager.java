package ControlModule.DevicesManagers.ActuatorManager;

import ControlModule.ControlModule;
import devices.actuator.BaseActuator;
import errors.ErrCodeExcp;
import logging.Log;

import javax.swing.*;

import java.util.Timer;
import java.util.TimerTask;


import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.ClientObserveRelation;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Response;
import org.json.JSONException;
import org.json.JSONObject;

import static devices.actuator.BaseActuator.LightState.LIGHT_OFF;
import static modules.SensorsMQTTHandler.SensorsMQTTHandlerErrCode.ERR_MQTT_MSG_NOT_JSON;


public class ControlActuatorManager extends BaseActuator
 {
  // MUST BE > COAP CLIENT TIMEOUTS
  private final static int actuatorWatcherTimerPeriod = 15 * 1000;  // milliseconds

  // TODO: Probably not required
  // private final static int coapClientsReqTimeout = 10 * 1000;

  // Actuator quantities
  private short fanRelSpeed;
  private LightState lightState;

  // Controller Module reference
  private final ControlModule controlModule;

  // The actuator's IPv6 address (e.g. fd00::0202:0002:0002:0002)
  private final String actuatorIPv6Addr;

  // The actuator's CoAP endpoint (e.g. coap://[fd00::0202:0002:0002:0002]/)
  private final String actuatorCoAPEndpoint;

  // Actuator Resources CoAP Clients
  final CoapClient coapClientFan;
  final CoapClient coapClientLight;
  final CoapClient coapClientErrors;



  // Whether the actuator was ever pinged
  boolean actuatorWasPinged;

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

  public ControlActuatorManager(String MAC,short ID,ControlModule controlModule)
   {
    // Call the parent's constructor, initializing the sensor's connState to false
    super(MAC,ID);

    // Initialize the other ControlActuatorManager's attributes
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
    this.actuatorWasPinged = false;

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

    // Initialize the actuator resources' CoAP Clients
    coapClientFan = new CoapClient(actuatorCoAPEndpoint + actuatorFanRelSpeedResRelPath);
    coapClientLight = new CoapClient(actuatorCoAPEndpoint + actuatorLightStateResRelPath);
    coapClientErrors = new CoapClient(actuatorCoAPEndpoint + actuatorErrorsResRelPath);

    // Initialize the Actuator Watchdog Timer
    Timer actuatorWatchdogTimer = new Timer();
    actuatorWatchdogTimer.scheduleAtFixedRate(new ActuatorWatchdogTimerTask(this,coapClientFan,
                                                                             coapClientFan,coapClientErrors),
                                                                             1000,actuatorWatcherTimerPeriod);
   }


  public void setConnStateOffline()
   {

   }

  public void setConnStateOnline()
   {

   }

  @Override
  public void setFanRelSpeed(int newFanRelSpeed)
   {

   }

  @Override
  public void setLightState(LightState newLightState)
   {

   }
 }
