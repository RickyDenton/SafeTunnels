package ControlModule;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;

import ControlModule.ControlMySQLConnector.ControlMySQLConnector;
import ControlModule.DevicesManagers.ActuatorManager.ControlActuatorManager;
import ControlModule.DevicesManagers.SensorManager.ControlSensorManager;
import ControlModule.GUILogging.ANSIColorPane;
import ControlModule.GUILogging.ANSIColorPaneOutputStream;
import devices.actuator.BaseActuator.LightState;
import errors.ErrCodeSeverity;
import logging.Log;
import modules.MySQLConnector.DevMACIDPair;
import modules.SensorsMQTTHandler.SensorsMQTTHandler;

import static devices.BaseDevice.DevType.actuator;
import static devices.BaseDevice.DevType.sensor;
import static java.lang.Math.max;


public class ControlModule extends JFrame
 {
  /* ------------------------------ Updatable Icons ------------------------------ */
  // Connection LEDs icons
  public static final ImageIcon connStateLEDONImg = new ImageIcon("ControlModule/src/main/resources/ConnState_ONLINE_10.png");
  public static final ImageIcon connStateLEDOFFImg = new ImageIcon("ControlModule/src/main/resources/ConnState_OFFLINE_10.png");

  // Actuator Light Icons
  public static final ImageIcon actuatorLightOFFImg = new ImageIcon("ControlModule/src/main/resources/LightBulb_OFF_Icon_30.png");
  public static final ImageIcon actuatorLightWARNINGImg = new ImageIcon("ControlModule/src/main/resources/LightBulb_WARNING_Icon_30.png");
  public static final ImageIcon actuatorLightALERTImg = new ImageIcon("ControlModule/src/main/resources/LightBulb_ALERT_Icon_30.png");
  public static final ImageIcon actuatorLightEMERGENCYImg = new ImageIcon("ControlModule/src/main/resources/LightBulb_EMERGENCY_Icon_30.png");

  private JPanel mainPanel;
  private JCheckBox automaticModeCheckBox;
  private JPanel systemPanel;
  private JLabel stateLabel;
  private JPanel logPanel;
  private JLabel LogLabel;
  private JPanel devicesPanel;
  private JPanel logLabelPanel;
  private JPanel devicesListsPanel;
  private JPanel actuatorsListPanel;
  private JPanel actuator1Panel;
  private JPanel actuator1HeaderPanel;
  private JLabel actuator1Name;
  private JLabel actuator1ConnStateLEDIcon;
  private JPanel sensorsListPanel;
  private JSlider actuator1FanRelSpeedSlider;
  private JButton actuator1LightStateButtonOFF;
  private JButton actuator1LightStateButtonWARNING;
  private JButton actuator1LightStateButtonALERT;
  private JButton actuator1LightStateButtonEMERGENCY;
  private JLabel sensor2C02DensityValue;
  private JLabel actuator2LightIcon;
  private ANSIColorPane ANSIColorPane1;
  private JLabel sensor1ConnStateLEDIcon;
  private JLabel systemOperatingStateLabel;
  private JPanel sensor2Panel;
  private JLabel sensor1C02DensityValue;
  private JLabel sensor1TempValue;
  private JLabel sensor2ConnStateLEDIcon;
  private JLabel sensor2TempValue;
  private JSlider actuator2FanRelSpeedSlider;
  private JLabel actuator2ConnStateLEDIcon;
  private JLabel actuator1FanRelSpeedValue;
  private JLabel actuator2FanRelSpeedValue;
  private JLabel actuator1LightStateStr;
  private JLabel actuator2LightStateStr;
  private JLabel actuator1FanIcon;
  private JLabel actuator2FanIcon;
  private JLabel actuator1LightIcon;
  private JButton actuator2LightStateButtonOFF;
  private JButton actuator2LightStateButtonWARNING;
  private JButton actuator2LightStateButtonALERT;
  private JButton actuator2LightStateButtonEMERGENCY;
  private JPanel actuator2Panel;
  private JLabel avgFanRelSpeedLabel;

  boolean autoMode;
  OpState systemOpState;

  // The system's average fan relative speed (also known by sensors)
  private int avgFanRelSpeed;

  ControlMySQLConnector controlMySQLConnector;
  SensorsMQTTHandler controlMQTTHandler;

  ArrayList<ControlSensorManager> ctrlSensorManagersList;
  ArrayList<ControlActuatorManager> ctrlActuatorManagersList;

  private void initGUI()
   {
    // Set the GUI window title
    setTitle("SafeTunnels Control Module");

    // Set the GUI window (fixed) size
    setSize(1300,393);
    setResizable(false);

    // Set the application to close after closing the GUI window
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    // Initialize the Log window output stream
    ANSIColorPaneOutputStream logOutputStream = new ANSIColorPaneOutputStream(ANSIColorPane1);

    // Redirect stdout to the Low window
    System.setOut (new PrintStream(logOutputStream));

    // Automatic mode checkbox listener
    // TODO: Description
    automaticModeCheckBox.addItemListener(itemEvent ->
     {
      if(itemEvent.getStateChange() == ItemEvent.SELECTED)
       {
        Log.info("Automatic Mode engaged");
        autoMode = true;

        // Trigger the fan quantities automatic adjustmen
        automaticModeFanControl();
       }
      else
       if(itemEvent.getStateChange() == ItemEvent.DESELECTED)
        {
         Log.info("Automatic Mode disengaged");
         autoMode = false;
        }
       else
        Log.warn("Unhandled Automatic Mode state change: " + itemEvent.getStateChange());
     });

    // Draw the GUI window
    setVisible(true);
    setContentPane(mainPanel);
   }


  private void initSensors()
   {
    // Attempt to retrieve the <MAC,sensorID> list of sensors stored in the database
    ArrayList<DevMACIDPair> sensorsList = controlMySQLConnector.getDBDevicesList(sensor);

    // Depending on the number of sensors that were retrieved from the database
    // (which is ascertained to be >0 by the getDBDevicesList() method)
    switch(sensorsList.size())
     {
      /* ----------- A single sensor was retrieved from the database ----------- */
      case 1:

       // Warn that a single sensor was retrieved from the database and
       // that the second sensor widget will so be hidden from the GUI
       Log.warn("A single sensor was retrieved from the database,"
                      + "hiding the second sensor widget from the GUI");

       // Hide the "sensor2" JPanel
       sensor2Panel.setVisible(false);
       break;

      /* ----- The (expected) two sensors were retrieved from the database ----- */
      case 2:
       Log.dbg("2 sensors were retrieved from the database:");
       break;

      /* ------- More than two sensors were retrieved from the database ------- */
      default:

       // Warn that the Control Module GUI currently supports displaying only two sensors
       Log.warn(sensorsList.size() + " sensors were retrieved from the database, "
         + "with the GUI currently supporting displaying only the first two");
     }

    // If LOG_LEVEL = DEBUG, log the list of sensors retrieved from the database
    if(Log.LOG_LEVEL == ErrCodeSeverity.DEBUG)
     sensorsList.forEach((sensor) -> Log.dbg("|- <" + sensor.ID + "," + sensor.MAC + ">"));

    // Initialize and populate the ArrayList of ControlSensorManagers
    ctrlSensorManagersList = new ArrayList<>();
    sensorsList.forEach(sensor -> ctrlSensorManagersList.add
      (new ControlSensorManager(sensor.MAC,sensor.ID,this)));

    // Sort the ControlSensorManagers list by increasing sensorID
    Collections.sort(ctrlSensorManagersList);

    // Bind the sensors with sensorID 1 and 2 to the GUI
    ctrlSensorManagersList.get(0).bindToGUI(sensor1ConnStateLEDIcon,sensor1C02DensityValue,
                                                                 sensor1TempValue);
    if(ctrlSensorManagersList.size() > 1)
     ctrlSensorManagersList.get(1).bindToGUI(sensor2ConnStateLEDIcon,sensor2C02DensityValue,
                                                                  sensor2TempValue);
   }


  private void initActuators()
   {
    // Attempt to retrieve the <MAC,actuatorID> list of actuators stored in the database
    ArrayList<DevMACIDPair> actuatorsList = controlMySQLConnector.getDBDevicesList(actuator);

    // Depending on the number of actuators that were retrieved from the database
    // (which is ascertained to be >0 by the getDBDevicesList() method)
    switch(actuatorsList.size())
     {
      /* ---------- A single actuator was retrieved from the database ---------- */
      case 1:

       // Warn that a single actuator was retrieved from the database and
       // that the second actuator widget will so be hidden from the GUI
       Log.warn("A single actuator was retrieved from the database,"
         + "hiding the second actuator widget from the GUI");

       // Hide the "actuator2" JPanel
       actuator2Panel.setVisible(false);
       break;

      /* ---- The (expected) two actuators were retrieved from the database ---- */
      case 2:
       Log.dbg("2 actuators were retrieved from the database:");
       break;

      /* ------ More than two actuators were retrieved from the database ------ */
      default:

       // Warn that the Control Module GUI currently supports displaying only two actuators
       Log.warn(actuatorsList.size() + " actuators were retrieved from the "
         + "database, with the GUI currently supporting displaying only the first two");
     }

    // If LOG_LEVEL = DEBUG, log the list of actuators retrieved from the database
    if(Log.LOG_LEVEL == ErrCodeSeverity.DEBUG)
     actuatorsList.forEach((actuator) -> Log.dbg("|- <" + actuator.ID + "," + actuator.MAC + ">"));

    // Initialize and populate the ArrayList of ControlActuatorManagers
    ctrlActuatorManagersList = new ArrayList<>();
    actuatorsList.forEach(actuator -> ctrlActuatorManagersList.add
      (new ControlActuatorManager(actuator.MAC,actuator.ID,this,controlMySQLConnector)));

    // Sort the ControlActuatorManagers list by increasing actuatorID
    Collections.sort(ctrlActuatorManagersList);

    // Bind the actuators with actuatorID 1 and 2 to the GUI
    ctrlActuatorManagersList.get(0).bindToGUI(actuator1ConnStateLEDIcon,actuator1FanRelSpeedValue,
                                              actuator1LightStateStr,actuator1FanIcon,
                                              actuator1LightIcon,actuator1FanRelSpeedSlider);
    if(ctrlActuatorManagersList.size() > 1)
     ctrlActuatorManagersList.get(1).bindToGUI(actuator2ConnStateLEDIcon,actuator2FanRelSpeedValue,
                                               actuator2LightStateStr,actuator2FanIcon,
                                               actuator2LightIcon,actuator2FanRelSpeedSlider);
   }


  private ControlModule()
   {
    // Initialize the system's attributes
    autoMode = true;
    systemOpState = OpState.NOMINAL;
    avgFanRelSpeed = -1;

    // Initialize the GUI window
    initGUI();

    // Attempt to connect with the SafeTunnels MySQL database
    controlMySQLConnector = new ControlMySQLConnector();

    // Initialize the system's sensors
    initSensors();

    // Initialize the system's actuators
    initActuators();

    // Attempt to instantiate the Control Module MQTT Client Handler
    controlMQTTHandler = new SensorsMQTTHandler("ControlModule",ctrlSensorManagersList);

    // Log that the Control Module has been successfully initialized
    Log.info("Control Module successfully initialized");




    // TODO: MAC ADDRESS -> IPV6 ADDR TRY

    /* TRY 1 (WORKING)

    String MAC = "00:04:00:04:00:04:00:04";
    int byteIndex = 1; // the second byte


    String MACNoColons = MAC.replace(":","");

    Log.info("MACNoColons = " + MACNoColons);

    byte secByte = (byte)Integer.parseInt(MACNoColons.substring(0,2));

    Log.info("secByteBeforeXOR = " + secByte);

    secByte = (byte)(secByte ^ 0x02);

    Log.info("secByteAfterXOR = " + secByte);

    int secByteInt = secByte;

    // Pad with zero
    String firstGroup = String.format("%02d", secByteInt);

    Log.info("firstGroup = " + firstGroup);

    String IID = firstGroup + MACNoColons.substring(2,4) + ":" + MACNoColons.substring(4,8) + ":" + MACNoColons.substring(8,12) + ":" + MACNoColons.substring(12,16);

    Log.info("IID = " + IID);

    String IPv6Addr = "fd00::" + IID;

    Log.info("IPv6Addr = " + IPv6Addr);

    */





    /*

    System.out.println("LOGGING TESTING");
    System.out.println("===============");

    Log.dbg("This is a debug message");
    Log.info("This is a info message");
    Log.warn("This is a warning message");
    Log.err("This is a error message");
    // Log.fatal("This is a fatal message");
    Log.dbg("This should not be printed with EXIT_IF_FATAL == true");

    Log.code(ERR_LIGHT_PUT_NO_LIGHTSTATE,1);
    Log.code(ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC,2,"(<additional description>)");

    try
     { throw new ErrCodeExcp(ERR_LIGHT_PUT_NO_LIGHTSTATE); }
    catch(ErrCodeExcp excp)
     { Log.excp(excp); }

    try
     { throw new ErrCodeExcp(ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC,"(<additional description>)"); }
    catch(ErrCodeExcp excp)
     { Log.excp(excp); }

    try
     { throw new DevErrCodeExcp(ERR_LIGHT_PUT_NO_LIGHTSTATE,1); }
    catch(ErrCodeExcp excp)
     { Log.excp(excp); }

    try
     { throw new DevErrCodeExcp(ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC,2,"(<additional description>)"); }
    catch(ErrCodeExcp excp)
     { Log.excp(excp); }

    /* TODO: WORKING!

    // Change a JPanel text and color
    sensor2C02DensityValue.setText("fuffa");
    sensor2C02DensityValue.setForeground(new Color(255,0,0));

    // Change a JPanel icon
    ImageIcon iconLogo = new ImageIcon("ControlModule/src/main/resources/LightBulb_ALERT_Icon_30.png");
    actuator2LightIcon.setIcon(iconLogo);
    */

    actuator1FanRelSpeedSlider.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       { GUIFanRelSpeedSliderUpdate(1, actuator1FanRelSpeedSlider.getValue());}
     });

    actuator2FanRelSpeedSlider.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       { GUIFanRelSpeedSliderUpdate(2, actuator2FanRelSpeedSlider.getValue());}
     });

    actuator1LightStateButtonOFF.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       { GUILightStateButtonUpdate(1, LightState.LIGHT_OFF);   }
     });

    actuator1LightStateButtonWARNING.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       { GUILightStateButtonUpdate(1, LightState.LIGHT_ON);   }
     });

    actuator1LightStateButtonALERT.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       { GUILightStateButtonUpdate(1, LightState.LIGHT_BLINK_ALERT);   }
     });

    actuator1LightStateButtonEMERGENCY.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       { GUILightStateButtonUpdate(1, LightState.LIGHT_BLINK_EMERGENCY);   }
     });

    actuator2LightStateButtonOFF.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       { GUILightStateButtonUpdate(2, LightState.LIGHT_OFF);   }
     });

    actuator2LightStateButtonWARNING.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       { GUILightStateButtonUpdate(2, LightState.LIGHT_ON);   }
     });

    actuator2LightStateButtonALERT.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       { GUILightStateButtonUpdate(2, LightState.LIGHT_BLINK_ALERT);   }
     });

    actuator2LightStateButtonEMERGENCY.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       { GUILightStateButtonUpdate(2, LightState.LIGHT_BLINK_EMERGENCY);   }
     });
   }


  private void GUIFanRelSpeedSliderUpdate(int actuatorID, int newFanSpeed)
   {
    try
     { ctrlActuatorManagersList.get(actuatorID-1).sendFanRelSpeed(newFanSpeed); }
    catch(IndexOutOfBoundsException indexOutOfBoundsException)
     { Log.err("Attempting to update via slider " + actuatorID
       + " the fan relative speed of non-existing actuator" + actuatorID);}
   }

  private void GUILightStateButtonUpdate(int actuatorID, LightState newLightState)
    {
     try
      { ctrlActuatorManagersList.get(actuatorID-1).sendLightState(newLightState); }
     catch(IndexOutOfBoundsException indexOutOfBoundsException)
      { Log.err("Attempting to update via GUI button the light state to '" + newLightState.toString()
        + "' of non-existing actuator" + actuatorID);}
    }



  private void automaticModeFanControl()
   {
    // Retrieve the automatic mode values associated
    // with the current system operating state
    int autoModeFanRelSpeed = systemOpState.getAutoFanRelSpeed();
    LightState autoModeLightState = systemOpState.getAutoLightState();

    // Attempt to send such values to all online actuators
    for(ControlActuatorManager ctrlActuatorMgr : ctrlActuatorManagersList)
     {
      if(ctrlActuatorMgr.getConnState())
       {
        ctrlActuatorMgr.sendFanRelSpeed(autoModeFanRelSpeed);
        ctrlActuatorMgr.sendLightState(autoModeLightState);
       }
     }
   }

  // Called by a sensor upon updating their operating
  // state to possibly update the system's operating state
  public void updateSystemOpState(OpState newSensorOpState)
   {
    boolean systemOperatingStateChanged = false;

    // If the sensor's updated and the system operating state
    // are the same, the latter does not require to be updated
    if(newSensorOpState == systemOpState)
     return;

    // Otherwise, if the sensor's updated is more severe than the
    // system's operating state, update the latter to the former
    else
     if(newSensorOpState.ordinal() > systemOpState.ordinal())
      {
       systemOpState = newSensorOpState;
       systemOperatingStateChanged = true;
      }

     // Finally, if the sensor's updated is less severe than the
     // system's operating check, it must be verified whether
     // the latter can switch to a less severe operating state
     else
      {
       // Used to store the maximum among
       // all the sensor's operating states
       OpState maxSensorOpState = OpState.NOMINAL;

       // Compute the maximum among all the sensor's operating states
       for(ControlSensorManager ctrlSensorMgr : ctrlSensorManagersList)
        maxSensorOpState = OpState.values()
                            [
                             max(maxSensorOpState.ordinal(),
                                 ctrlSensorMgr.getSensorOperatingState().ordinal())
                            ];

       // If the maximum among all the sensor's operating
       // state is less severe than the current system
       // severity, the system can switch to such state
       if(maxSensorOpState != systemOpState)
        {
         systemOpState = maxSensorOpState;
         systemOperatingStateChanged = true;
        }
      }

    // If the system's operating state has changed
    if(systemOperatingStateChanged)
     {
      // Update the GUI's system state label with the new operating state
      systemOperatingStateLabel.setText(systemOpState.toString());
      systemOperatingStateLabel.setForeground(systemOpState.getColor());

      // Log that the system has switched state
      Log.info("System now in " + systemOpState.toString() + " mode");

      // If automatic mode is enabled, also adjust the
      // actuators' fan relative speeds and light states
      if(autoMode)
       { automaticModeFanControl(); }
     }
   }

  public void updateAvgFanRelSpeed()
   {
    int numFansToCount = 0;
    int totFanRelSpeed = 0;

    for (ControlActuatorManager ctrlActuatorManager : ctrlActuatorManagersList)
     {
      // The actuator's fan relative speed
      int actFanRelSpeed = ctrlActuatorManager.getFanRelSpeed();

      // If a valid fan relative speed was received by the actuator
      if(actFanRelSpeed != -1)
       {
        totFanRelSpeed += actFanRelSpeed;
        numFansToCount++;
       }
     }

    // New average fan relative speed
    int newAvgFanRelSpeed = totFanRelSpeed / numFansToCount;

    // If the new average fan relative speed differs from its previous value
    if(avgFanRelSpeed != newAvgFanRelSpeed)
     {
      // Update the system's average fan relative speed
      avgFanRelSpeed = newAvgFanRelSpeed;

      // Update the system's average fan relative speed GUI label
      avgFanRelSpeedLabel.setText(avgFanRelSpeed + " %");

      // Attempt to publish the new system average fan relative
      // speed on the sensor's MQTT 'TOPIC_AVG_FAN_REL_SPEED' topic
      controlMQTTHandler.publishAvgFanRelSpeed(avgFanRelSpeed);

      // Log the new system average fan relative speed
      Log.info("Published new system average fan relative speed: " + avgFanRelSpeed);
     }
   }

  /**
   * Control Module application entry point
   */
  public static void main(String[] args)
   { new ControlModule(); }
 }
