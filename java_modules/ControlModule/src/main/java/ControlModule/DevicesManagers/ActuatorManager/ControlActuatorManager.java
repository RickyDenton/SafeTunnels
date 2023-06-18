/* An actuator managed by the Control Module */

package ControlModule.DevicesManagers.ActuatorManager;

/* ================================== IMPORTS ================================== */

/* --------------------- Java Standard Libraries Resources --------------------- */
import javax.swing.*;
import java.util.Timer;
import java.util.TimerTask;

/* --------------------- Californium CoAP Client Resources --------------------- */
import org.eclipse.californium.core.CoapClient;

/* --------------------------- SafeTunnels Resources --------------------------- */
import logging.Log;
import ControlModule.ControlModule;
import devices.actuator.BaseActuator;
import ControlModule.ControlMySQLConnector.ControlMySQLConnector;
import ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.RequestsHandlers.fan.CoAPClientFanReqHandler;
import ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.RequestsHandlers.light.CoAPClientLightReqHandler;
import ControlModule.DevicesManagers.ActuatorManager.GUIUpdateTasks.FanIconSpinTask;
import ControlModule.DevicesManagers.ActuatorManager.GUIUpdateTasks.LightBlinkTask;
import static ControlModule.OpState.*;
import static devices.actuator.BaseActuator.ActuatorQuantity.FANRELSPEED;
import static devices.actuator.BaseActuator.ActuatorQuantity.LIGHTSTATE;
import static devices.actuator.BaseActuator.LightState.LIGHT_STATE_INVALID;


/* ============================== CLASS DEFINITION ============================== */
public final class ControlActuatorManager extends BaseActuator
 {
  /* =========================== ACTUATOR PARAMETERS =========================== */

  // The initial delay in milliseconds before starting the actuator's watchdog timer
  private final static int actuatorWatcherTimerInitDelay = 3 * 1000;

  // The actuator watchdog timer period in milliseconds
  private static final int actuatorWatcherTimerPeriod = 10 * 1000;

  // The initial delay in milliseconds from when an actuator
  // connects to when its quantities are automatically adjusted
  // if the control module's automatic mode is enabled
  private static final int autoAdjustOnConnTimerDelay = 200;

  // The base fan icon spin timer period in milliseconds
  private static final int fanIconSpinTimerBasePeriod = 130;


  /* ============================ PRIVATE ATTRIBUTES ============================ */

  // Actuator quantities
  private short fanRelSpeed;
  private LightState lightState;

  // Control Module and MySQL Connector references
  private final ControlModule controlModule;
  private final ControlMySQLConnector controlMySQLConnector;

  // Actuator Resources Californium CoAP clients
  private final CoapClient coapClientFan;
  private final CoapClient coapClientLight;
  private final CoapClient coapClientErrors;


  /* ---------------------- GUI Actuator Widget Management ---------------------- */

  // Whether the actuator is bound to an actuator widget in the GUI
  private boolean GUIBound;

  // GUI quantities animation timers
  private Timer fanIconSpinTimer;
  private Timer lightBlinkTimer;

  // The GUI's JLabels associated with the actuator, if any
  private JLabel  connStateLEDIcon;          // Connection state LED
  private JLabel  fanRelSpeedLabel;          // Fan relative speed value
  private JLabel  lightStateLabel;           // Light state value
  private JLabel  fanIcon;                   // Fan icon
  private JLabel  lightIcon;                 // Light icon
  private JSlider fanRelSpeedSlider;         // Fan relative speed slider
  private JButton lightStateButtonOFF;       // Light "OFF" button
  private JButton lightStateButtonWARNING;   // Light "ON"/"WARN" button
  private JButton lightStateButtonALERT;     // Light "ALERT" button
  private JButton lightStateButtonEMERGENCY; // Light "EMERGENCY" button


  /* ============================= PUBLIC METHODS ============================= */

  /**
   * ControlActuatorManager constructor, initializing its associated attributes and
   * Californium CoAP clients and scheduling at a fixed rate its Watchdog Timer
   * @param MAC The sensor's (unique) MAC address
   * @param ID  The sensor's unique ID in the SafeTunnels database
   * @param controlModule A reference to the Control Module object
   * @param controlMySQLConnector A reference to the Control MySQL Connector object
   */
  public ControlActuatorManager(String MAC,short ID,ControlModule controlModule,
                                ControlMySQLConnector controlMySQLConnector)
   {
    // Call the parent constructor, initializing
    // the actuator's MAC, ID and connState to false
    super(MAC,ID);

    // Initialize the other ControlActuatorManager base attributes
    this.controlMySQLConnector = controlMySQLConnector;
    this.controlModule = controlModule;
    fanRelSpeed = -1;
    lightState = LIGHT_STATE_INVALID;
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
    byte secByte = (byte)Integer.parseInt(IID.substring(0,2),16);
    secByte = (byte)(secByte ^ 0x02);
    IID.replace(0,2,String.format("%02x", secByte));

    // Build the actuator's IPv6 interface ID by
    // adding a colon every 4 hexadecimal characters
    for(int i = 4; i < IID.length(); i+=5)
     IID.insert(i, ":");

    // Set the actuator's global IPv6 address (e.g. fd00::0202:0002:0002:0002)
    String actuatorIPv6Addr = "fd00::" + IID;

    // Set the actuator's CoAP endpoint (e.g. coap://[fd00::0202:0002:0002:0002]/)
    String actuatorCoAPEndpoint = "coap://[" + actuatorIPv6Addr + "]/";

    /*/ --- Actuator Global IPv6 Address and CoAP Endpoint Initialization --- /*/

    /*
     * Initialize the actuator resources' Californium CoAP Clients
     *
     * NOTE: The CoAP clients use confirmable (CON) messages by default
     */
    coapClientFan = new CoapClient(actuatorCoAPEndpoint + actuatorFanRelSpeedResRelPath);
    coapClientLight = new CoapClient(actuatorCoAPEndpoint + actuatorLightStateResRelPath);
    coapClientErrors = new CoapClient(actuatorCoAPEndpoint + actuatorErrorsResRelPath);

    // Schedule at a fixed rate the actuator's Watchdog Timer
    Timer actuatorWatchdogTimer = new Timer();
    actuatorWatchdogTimer.scheduleAtFixedRate(new ActuatorWatchdogTimerTask
     (this,coapClientFan,coapClientLight,coapClientErrors),
      actuatorWatcherTimerInitDelay,actuatorWatcherTimerPeriod);
   }


  /**
   * @return The actuator's fan relative speed value
   */
  public short getFanRelSpeed()
   { return this.fanRelSpeed; }


  /**
   * Binds the actuator to an actuator widget in the Control Module's GUI
   * @param connStateLEDIcon          The actuator widget's connection state LED
   * @param fanRelSpeedLabel          The actuator widget's fan relative speed value
   * @param lightStateLabel           The actuator widget's light state value
   * @param fanIcon                   The actuator widget's fan icon
   * @param lightIcon                 The actuator widget's light icon
   * @param fanRelSpeedSlider         The actuator widget's fan relative speed slider
   * @param lightStateButtonOFF       The actuator widget's light "OFF" button
   * @param lightStateButtonWARNING   The actuator widget's light "ON"/"WARN" button
   * @param lightStateButtonALERT     The actuator widget's light "ALERT" button
   * @param lightStateButtonEMERGENCY The actuator widget's light "EMERGENCY" button
   */
  public void bindToGUI(JLabel connStateLEDIcon, JLabel fanRelSpeedLabel, JLabel lightStateLabel,
                        JLabel fanIcon, JLabel lightIcon, JSlider fanRelSpeedSlider,
                        JButton lightStateButtonOFF, JButton lightStateButtonWARNING,
                        JButton lightStateButtonALERT, JButton lightStateButtonEMERGENCY)
   {
    // Ensure all the passed actuator widget GUI components to
    // be non-null, logging an error and returning otherwise
    if(connStateLEDIcon == null || fanRelSpeedLabel == null || lightStateLabel == null || fanIcon == null ||
       lightIcon == null || fanRelSpeedSlider == null || lightStateButtonOFF == null ||
       lightStateButtonWARNING == null || lightStateButtonALERT == null || lightStateButtonEMERGENCY == null)
     {
      Log.err("Attempting to bind actuator" + ID + "to null elements in the GUI");
      return;
     }

    // Initialize the actuator widget's
    // components to the provided values
    this.connStateLEDIcon = connStateLEDIcon;
    this.fanRelSpeedLabel = fanRelSpeedLabel;
    this.lightStateLabel = lightStateLabel;
    this.fanIcon = fanIcon;
    this.lightIcon = lightIcon;
    this.fanRelSpeedSlider = fanRelSpeedSlider;
    this.lightStateButtonOFF = lightStateButtonOFF;
    this.lightStateButtonWARNING = lightStateButtonWARNING;
    this.lightStateButtonALERT = lightStateButtonALERT;
    this.lightStateButtonEMERGENCY = lightStateButtonEMERGENCY;

    // Set that the actuator is now bound to a GUI actuator widget
    GUIBound = true;
   }


  /* ----------------------------- Setters Methods ----------------------------- */

  /**
   * Actuator offline connection status handler, which:
   *   1) Marks the sensor as offline
   *   2) Pushes such connState into the database
   *   3) Disables the actuator's associated GUI widget, if any
   */
  public void setConnStateOffline()
   {
    // Set the actuator as offline
    connState = false;

    // Attempt to push the 'OFFLINE' actuator connState into the database
    controlMySQLConnector.pushActuatorConnState(ID, false);

    // Log that the actuator appears to be offline
    Log.warn("actuator" + ID + " appears to be offline");

    // If the actuator is bound to a GUI actuator widget
    if(GUIBound)
     {
      // Update the connection state LED icon
      connStateLEDIcon.setIcon(ControlModule.connStateLEDOFFImg);

      // Stop the fan spinning ant the
      // LED blinking animations, if any
      if(fanIconSpinTimer != null)
       fanIconSpinTimer.cancel();
      if(lightBlinkTimer != null)
       lightBlinkTimer.cancel();

      // Disable all the actuator widget's components
      fanRelSpeedLabel.setEnabled(false);
      lightStateLabel.setEnabled(false);
      fanIcon.setEnabled(false);
      lightIcon.setEnabled(false);
      fanRelSpeedSlider.setEnabled(false);
      lightStateButtonOFF.setEnabled(false);
      lightStateButtonWARNING.setEnabled(false);
      lightStateButtonALERT.setEnabled(false);
      lightStateButtonEMERGENCY.setEnabled(false);
     }
   }


  /**
   * Actuator online connection status handler, which:
   *   1) Marks the sensor as online
   *   2) Pushes such connState into the database
   *   3) Enables the actuator's associated GUI widget, if any
   *   4) Starts the autoAdjustOnConnTimer which, after a short
   *      delay, automatically adjusts the actuator's quantities
   *      if the Control Module automatic mode is enabled
   */
  public void setConnStateOnline()
   {
    // Set the actuator as online
    connState = true;

    // Attempt to push the 'ONLINE' actuator connState into the database
    controlMySQLConnector.pushActuatorConnState(ID, true);

    // Log that the actuator is now online
    Log.info("actuator" + ID + " is now online");

    // If the actuator is bound to a GUI actuator widget
    if(GUIBound)
     {
      // Update the connection state LED icon
      connStateLEDIcon.setIcon(ControlModule.connStateLEDONImg);

      // Activate the other actuator widget's components
      fanRelSpeedLabel.setEnabled(true);
      lightStateLabel.setEnabled(true);
      fanIcon.setEnabled(true);
      lightIcon.setEnabled(true);
      fanRelSpeedSlider.setEnabled(true);
      lightStateButtonOFF.setEnabled(true);
      lightStateButtonWARNING.setEnabled(true);
      lightStateButtonALERT.setEnabled(true);
      lightStateButtonEMERGENCY.setEnabled(true);
     }

    /*
     * Start the autoAdjustOnConnTimer which, after a short
     * delay, automatically adjusts the actuator's quantities
     * if the Control Module automatic mode is enabled
     */
    Timer autoAdjustOnConnTimer = new Timer();
    autoAdjustOnConnTimer.schedule(new TimerTask()
     {
      public void run()
       {
        if(controlModule.autoMode && connState)
         {
          sendFanRelSpeed(controlModule.systemOpState.getAutoFanRelSpeed());
          sendLightState(controlModule.systemOpState.getAutoLightState());
         }
       }
     },autoAdjustOnConnTimerDelay);
   }


  /**
   * Actuator new fan relative speed value handler, which, if the
   * new fan relative speed value differs from its current one:
   *   1) Updates the actuator's fan relative speed value
   *   2) Pushes such new fan relative speed into the database
   *   3) If the actuator is bound to a GUI actuator widget:
   *      3.1) Updates the widget's fan relative speed value
   *      3.2) Sets the widget's fan relative speed slider to match the value
   *      3.3) Adjusts the fan icon spinning animation
   *           depending on the new fan relative speed value
   *   4) Notify the Control Module that the fan relative speed
   *      has changed so as for it to compute the new system's
   *      average fan speed and propagate it via MQTT to sensors
   */
  @Override
  public void setFanRelSpeed(int newFanRelSpeed)
   {
    // If the new fan relative speed value differs from its current
    // one (i.e. this is not a periodic "fan" observer refresh)
    if(newFanRelSpeed != this.fanRelSpeed)
     {
      // Update the actuator's fan relative speed value
      this.fanRelSpeed = (short)newFanRelSpeed;

      // Attempt to push the updated actuator fan
      // relative speed value into the database
      controlMySQLConnector.pushActuatorQuantityValue(ID,FANRELSPEED,fanRelSpeed);

      // Log the new actuator fan relative speed
      Log.dbg("New actuator" + ID + " fan relative speed: " + fanRelSpeed);

      // If the actuator is bound to an actuator widget in the GUI
      if(GUIBound)
       {
        // Update its fan relative speed value, also setting
        // its color as of its current "operating state"
        fanRelSpeedLabel.setText(newFanRelSpeed + " %");
        fanRelSpeedLabel.setForeground(fanRelSpeedToOpStateColor(newFanRelSpeed));

        // Sets the fan relative speed slider to match the new value
        fanRelSpeedSlider.setValue(newFanRelSpeed);

        // Stop the fanIconSpinTimer controlling the fan spinning animation
        if(fanIconSpinTimer != null)
         fanIconSpinTimer.cancel();

        // If the fan has stopped, restore its base, static icon
        if(newFanRelSpeed == 0)
         fanIcon.setIcon(ControlModule.actuatorFanIcons[0]);

        // Otherwise (re)-schedule the fanIconSpinTimer at a fixed
        // rate directly proportional to the fan relative speed
        else
         {
          fanIconSpinTimer = new Timer();
          fanIconSpinTimer.scheduleAtFixedRate(new FanIconSpinTask(fanIcon),
                                          0,fanIconSpinTimerBasePeriod-newFanRelSpeed);
         }
       }

      // Notify the Control Module that the fan relative speed
      // has changed so as for it to compute the new system's
      // average fan speed and propagate it via MQTT to sensors
      controlModule.updateAvgFanRelSpeed();
     }
   }


  /**
   * Actuator new fan relative speed value handler, which, if
   * the new light state value differs from its current one:
   *   1) Updates the actuator's light state value
   *   2) Pushes such new light state value into the database
   *   3) If the actuator is bound to a GUI actuator widget, updates
   *      its light state value and animates the light icon accordingly
   *   4) Notify the Control Module that the fan relative speed
   *      has changed so as for it to compute the new system's
   *      average fan speed and propagate it via MQTT to sensors
   */
  @Override
  public void setLightState(LightState newLightState)
   {
    // If the new light state value differs from its current one
    // (i.e. this is not a periodic "light" observer refresh)
    if(newLightState != this.lightState)
     {
      // Update the actuator's light state value
      this.lightState = newLightState;

      // Attempt to push the updated actuator light state into the database
      controlMySQLConnector.pushActuatorQuantityValue(ID,LIGHTSTATE,lightState.ordinal());

      // Log the new actuator light state
      Log.dbg("New actuator" + ID + " light state: " + lightState);

      // If the actuator is bound to an actuator widget in the GUI
      if(GUIBound)
       {
        // Stop the LightBlinkTimer controlling
        // the light blinking animation, if any
        if(lightBlinkTimer!= null)
         lightBlinkTimer.cancel();

        // Update the GUI widget light state value, also setting its
        // color to match its "operating state" and, if applicable,
        // start the LightBlinkTimer light blinking timer
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

           lightBlinkTimer = new Timer();
           lightBlinkTimer.scheduleAtFixedRate(new LightBlinkTask
             (lightIcon,ControlModule.actuatorLightALERTImg),0,1000);
           break;

          case LIGHT_BLINK_EMERGENCY:
           lightStateLabel.setText("EMER.");
           lightStateLabel.setForeground(EMERGENCY.getColor());

           lightBlinkTimer = new Timer();
           lightBlinkTimer.scheduleAtFixedRate(new LightBlinkTask
             (lightIcon,ControlModule.actuatorLightEMERGENCYImg),0,300);
           break;
         }
       }
     }
   }


  /* ----------------------- CoAP Client Requests Methods ----------------------- */

  /**
   * Sends in an asynchronous confirmable CoAP request
   * a new fan relative speed value to the actuator
   * @param sendFanRelSpeed The new fan relative speed value
   *                        to be sent to the actuator
   */
  public void sendFanRelSpeed(int sendFanRelSpeed)
   {
    // Ascertain the fan relative speed value to be sent to the
    // actuator to be valid, logging the error and returning otherwise
    if(sendFanRelSpeed < 0 || sendFanRelSpeed > 100)
     {
      Log.err("Attempting to send to actuator" + ID + " an invalid "
               + "fan relative speed (" + sendFanRelSpeed + ")");
      return;
     }

    // Ascertain the actuator to be online,
    // logging the error and returning otherwise
    if(!connState)
     {
      Log.warn("Cannot send an updated fan relative speed (" + sendFanRelSpeed
                + ") to actuator" + ID + " because it is offline");
      return;
     }

    // Ascertain the fan relative speed to be sent to differ from
    // its current value, logging the error and returning otherwise
    if(sendFanRelSpeed == fanRelSpeed)
     {
      Log.warn("Attempting to send to actuator" + ID + " the same "
                + "fan relative speed (" + sendFanRelSpeed + ")");
      return;
     }

    // Send the fan relative speed to the actuator
    // via an asynchronous, confirmable PUT request
    coapClientFan.put(new CoAPClientFanReqHandler
      (this,sendFanRelSpeed),
      "fanRelSpeed=" + sendFanRelSpeed,0);   // 0 = text/plain
   }


  /**
   * Sends in an asynchronous confirmable CoAP
   * request a new light state value to the actuator
   * @param sendLightState The new light state value
   *                       to be sent to the actuator
   */
  public void sendLightState(LightState sendLightState)
   {
    // Ascertain the actuator to be online,
    // logging the error and returning otherwise
    if(!connState)
     {
      Log.warn("Cannot send an updated light state (" + sendLightState.toString()
               + ") to actuator" + ID + " because it is offline");
      return;
     }

    // Ascertain the light state to be sent to differ from its
    // current value, logging the error and returning otherwise
    if(sendLightState == lightState)
     {
      Log.warn("Attempting to send to actuator" + ID + " the "
                + "same light state (" + sendLightState + ")");
      return;
     }

    // Send the light state to the actuator via
    // an asynchronous, confirmable PUT request
    coapClientLight.put(new CoAPClientLightReqHandler
      (this,sendLightState),
      "lightState=" + sendLightState.toString(),0);  // 0 = text/plain
   }
 }