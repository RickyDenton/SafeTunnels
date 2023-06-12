package ControlModule;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;

import ControlModule.ControlMySQLConnector.ControlMySQLConnector;
import ControlModule.DevicesManagers.SensorManager.ControlSensorManager;
import ControlModule.GUILogging.ANSIColorPane;
import ControlModule.GUILogging.ANSIColorPaneOutputStream;
import errors.ErrCodeSeverity;
import logging.Log;
import modules.MySQLConnector.DevMACIDPair;
import modules.SensorsMQTTHandler.SensorsMQTTHandler;

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

  boolean autoMode;
  OpState systemOpState;

  ControlMySQLConnector controlMySQLConnector;
  SensorsMQTTHandler controlMQTTHandler;

  ArrayList<ControlSensorManager> ctrlSensorsManagersList;


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
       // that the second sensor panel will so be hidden from the GUI
       Log.warn("A single sensor was retrieved from the database,"
                      + "hiding the second sensor panel from the GUI");

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
       Log.warn(sensorsList.size() + " sensors were retrieved from the database,"
                + "with only the first two that will be displayed in the GUI");
     }

    // If LOG_LEVEL = DEBUG, log the list of sensors retrieved from the database
    if(Log.LOG_LEVEL == ErrCodeSeverity.DEBUG)
     sensorsList.forEach((sensor) -> Log.dbg("|- <" + sensor.ID + "," + sensor.MAC + ">"));

    // Initialize and populate the ArrayList of ControlSensorManagers
    ctrlSensorsManagersList = new ArrayList<>();
    sensorsList.forEach(sensor -> ctrlSensorsManagersList.add
      (new ControlSensorManager(sensor.MAC,sensor.ID,this)));

    // Sort the ControlSensorManagers by increasing sensorID
    Collections.sort(ctrlSensorsManagersList);

    // Bind the sensors with sensorID 1 and 2 to the GUI
    ctrlSensorsManagersList.get(0).bindToGUI(sensor1ConnStateLEDIcon,sensor1C02DensityValue,
                                                                 sensor1TempValue);
    if(ctrlSensorsManagersList.size() > 1)
     ctrlSensorsManagersList.get(1).bindToGUI(sensor2ConnStateLEDIcon,sensor2C02DensityValue,
                                                                  sensor2TempValue);
   }


  private ControlModule()
   {
    // Initialize the system's attributes
    autoMode = true;
    systemOpState = OpState.NOMINAL;

    // Initialize the GUI window
    initGUI();

    // Attempt to connect with the SafeTunnels MySQL database
    controlMySQLConnector = new ControlMySQLConnector();

    // Initialize the system's sensors
    initSensors();

    // Initialize the system's actuators
    // TODO

    // Attempt to instantiate the Control Module MQTT Client Handler
    controlMQTTHandler = new SensorsMQTTHandler("ControlModule",ctrlSensorsManagersList);

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
       for(ControlSensorManager ctrlSensorMgr : ctrlSensorsManagersList)
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
       {
        // TODO!
       }
     }
   }

  /**
   * Control Module application entry point
   */
  public static void main(String[] args)
   { new ControlModule(); }
 }
