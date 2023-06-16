package ControlModule.DevicesManagers.ActuatorManager;

import ControlModule.ControlModule;
import ControlModule.ControlMySQLConnector.ControlMySQLConnector;
import ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.RequestsHandlers.fan.CoAPClientFanReqHandler;
import ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.RequestsHandlers.light.CoAPClientLightReqHandler;
import ControlModule.DevicesManagers.ActuatorManager.GUIUpdateTasks.FanIconSpinTask;
import ControlModule.DevicesManagers.ActuatorManager.GUIUpdateTasks.LightBlinkTask;
import devices.actuator.BaseActuator;
import logging.Log;
import org.eclipse.californium.core.CoapClient;

import javax.swing.*;
import java.util.Timer;
import java.util.TimerTask;

import static ControlModule.OpState.*;
import static devices.actuator.BaseActuator.ActuatorQuantity.FANRELSPEED;
import static devices.actuator.BaseActuator.ActuatorQuantity.LIGHTSTATE;
import static devices.actuator.BaseActuator.LightState.LIGHT_STATE_INVALID;


public class ControlActuatorManager extends BaseActuator
 {
  private final static int actuatorWatcherTimerInitDelay = 3 * 1000;  // milliseconds

  // MUST BE > COAP CLIENT TIMEOUTS
  private static final int actuatorWatcherTimerPeriod = 10 * 1000;    // milliseconds
  private static final int fanIconSpinTimerBasePeriod = 130;          // milliseconds
  private static final int autoAdjustOnConnTimerDelay = 200;          // milliseconds

  // Actuator quantities
  private short fanRelSpeed;
  private LightState lightState;

  // Controller Module reference
  private final ControlModule controlModule;

  // Control Module MySQL Connector
  private final ControlMySQLConnector controlMySQLConnector;

  // Actuator Resources CoAP Clients
  final CoapClient coapClientFan;
  final CoapClient coapClientLight;
  final CoapClient coapClientErrors;

  // GUI Update timers
  Timer fanIconSpinTimer;
  Timer lightBlinkTimer;

  // Whether the actuator is bound to an actuator widget in the GUI
  private boolean GUIBound;

  // The GUI's JLabels associated with the actuator, if any
  private JLabel connStateLEDIcon;
  private JLabel fanRelSpeedLabel;
  private JLabel lightStateLabel;
  private JLabel fanIcon;
  private JLabel lightIcon;
  private JSlider fanRelSpeedSlider;
  private JButton lightStateButtonOFF;
  private JButton lightStateButtonWARNING;
  private JButton lightStateButtonALERT;
  private JButton lightStateButtonEMERGENCY;


  // Binds this actuator to an actuator widget in the GUI
  public void bindToGUI(JLabel connStateLEDIcon, JLabel fanRelSpeedLabel, JLabel lightStateLabel,
                        JLabel fanIcon, JLabel lightIcon, JSlider fanRelSpeedSlider,
                        JButton lightStateButtonOFF, JButton lightStateButtonWARNING,
                        JButton lightStateButtonALERT, JButton lightStateButtonEMERGENCY)
   {
    if(connStateLEDIcon == null || fanRelSpeedLabel == null || lightStateLabel == null || fanIcon == null ||
       lightIcon == null || fanRelSpeedSlider == null || lightStateButtonOFF == null ||
       lightStateButtonWARNING == null || lightStateButtonALERT == null || lightStateButtonEMERGENCY == null)
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
    this.lightStateButtonOFF = lightStateButtonOFF;
    this.lightStateButtonWARNING = lightStateButtonWARNING;
    this.lightStateButtonALERT = lightStateButtonALERT;
    this.lightStateButtonEMERGENCY = lightStateButtonEMERGENCY;

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

    // If bound to a GUI sensor widget
    if(GUIBound)
     {
      // Update the connection state LED icon
      connStateLEDIcon.setIcon(ControlModule.connStateLEDOFFImg);

      // Stop the fan spinning and the LED blinking, if any
      if(fanIconSpinTimer != null)
       fanIconSpinTimer.cancel();

      if(lightBlinkTimer != null)
       lightBlinkTimer.cancel();

      // Deactivate the other actuator widget's components
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



  public void setConnStateOnline()
   {
    // Set the actuator as online
    connState = true;

    // Attempt to push the 'ONLINE' actuator connState into the database
    controlMySQLConnector.pushActuatorConnState(ID, true);

    // Log that the actuator is now online
    Log.info("actuator" + ID + " is now online");

    // If bound to a GUI sensor widget
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

    // Start a timer which, after a short delay, checks whether
    // the automatic fan adjustment is enabled and, if it is,
    // sends the commands to adjust the fan's quantities
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


  @Override
  public void setFanRelSpeed(int newFanRelSpeed)
   {
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

      // If bound to a GUI widget, update its associated
      // fan speed value and set the slider to match
      if(GUIBound)
       {
        fanRelSpeedLabel.setText(newFanRelSpeed + " %");
        fanRelSpeedLabel.setForeground(fanRelSpeedToOpStateColor(newFanRelSpeed));
        fanRelSpeedSlider.setValue(newFanRelSpeed);

        // Stop the fanIconSpinTimer
        if(fanIconSpinTimer != null)
         fanIconSpinTimer.cancel();

        // If the fan has stopped, restore its fixed icon
        if(newFanRelSpeed == 0)
         fanIcon.setIcon(ControlModule.actuatorFanIcons[0]);

         // Otherwise initialize the fanIconSpinTimer with a fixed
         // period inversely proportional to the fan relative speed
        else
         {
          fanIconSpinTimer = new Timer();
          fanIconSpinTimer.scheduleAtFixedRate(new FanIconSpinTask(fanIcon),0,fanIconSpinTimerBasePeriod-newFanRelSpeed);
         }
       }

      // Update the system's average fan relative speed
      controlModule.updateAvgFanRelSpeed();
     }
   }


  @Override
  public void setLightState(LightState newLightState)
   {
    // If this is not a periodic light state
    // observing refresh, i.e. its value has changed
    if(newLightState != this.lightState)
     {
      // Update the actuator's light state value
      this.lightState = newLightState;

      // If bound to a GUI widget, update its
      // associated label and light state icon
      if(GUIBound)
       {
        // Stop the LightBlinkTimer, if any
        if(lightBlinkTimer!= null)
         lightBlinkTimer.cancel();

        // Depending on the new light state
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

           // Start the LED blinking timer
           lightBlinkTimer = new Timer();
           lightBlinkTimer.scheduleAtFixedRate(new LightBlinkTask(lightIcon,ControlModule.actuatorLightALERTImg),0,1000);
           break;

          case LIGHT_BLINK_EMERGENCY:
           lightStateLabel.setText("EMER.");
           lightStateLabel.setForeground(EMERGENCY.getColor());

           // Start the LED blinking timer
           lightBlinkTimer = new Timer();
           lightBlinkTimer.scheduleAtFixedRate(new LightBlinkTask(lightIcon,ControlModule.actuatorLightEMERGENCYImg),0,300);
           break;
         }
       }

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
    coapClientFan.put(new CoAPClientFanReqHandler(this,sendFanRelSpeed), "fanRelSpeed=" + sendFanRelSpeed,0); // 0 = text/plain
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
    coapClientLight.put(new CoAPClientLightReqHandler(this,sendLightState), "lightState=" + sendLightState.toString(),0); // 0 = text/plain
   }

 }
