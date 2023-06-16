package ControlModule;

import ControlModule.ControlMySQLConnector.ControlMySQLConnector;
import ControlModule.DevicesManagers.ActuatorManager.ControlActuatorManager;
import ControlModule.DevicesManagers.SensorManager.ControlSensorManager;
import ControlModule.GUILogging.ANSIColorPane;
import ControlModule.GUILogging.ANSIColorPaneOutputStream;
import devices.actuator.BaseActuator.LightState;
import errors.ErrCodeSeverity;
import logging.Log;
import modules.InputArgsParser.InputArgsParser;
import modules.MySQLConnector.DevMACIDPair;
import modules.SensorsMQTTHandler.SensorsMQTTHandler;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;

import static ControlModule.OpState.fanRelSpeedToOpStateColor;
import static devices.BaseDevice.DevType.actuator;
import static devices.BaseDevice.DevType.sensor;
import static java.lang.Math.max;


public class ControlModule extends JFrame
 {
  /* ------------------------------ Updatable Icons ------------------------------ */
  // Connection LEDs icons
  public static final ImageIcon connStateLEDONImg = new ImageIcon(
    "ControlModule/src/main/resources/icons/ConnState_ONLINE_10.png");
  public static final ImageIcon connStateLEDOFFImg = new ImageIcon(
    "ControlModule/src/main/resources/icons/ConnState_OFFLINE_10.png");

  // Actuator Light Icons
  public static final ImageIcon actuatorLightOFFImg = new ImageIcon(
    "ControlModule/src/main/resources/icons/LightBulb_OFF_Icon_30.png");
  public static final ImageIcon actuatorLightWARNINGImg = new ImageIcon(
    "ControlModule/src/main/resources/icons/LightBulb_WARNING_Icon_30.png");
  public static final ImageIcon actuatorLightALERTImg = new ImageIcon(
    "ControlModule/src/main/resources/icons/LightBulb_ALERT_Icon_30.png");
  public static final ImageIcon actuatorLightEMERGENCYImg = new ImageIcon(
    "ControlModule/src/main/resources/icons/LightBulb_EMERGENCY_Icon_30.png");

  public static final ImageIcon[] actuatorFanIcons =
   {
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_0.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_1.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_2.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_3.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_4.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_5.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_6.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_7.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_8.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_9.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_10.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_11.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_12.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_13.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_14.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_15.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_16.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_17.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_18.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_19.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_20.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_21.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_22.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_23.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_24.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_25.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_26.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_27.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_28.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_29.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_30.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_31.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_32.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_33.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_34.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_35.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_36.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_37.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_38.png"),
    new ImageIcon("ControlModule/src/main/resources/icons/fanIcons/Fan_Icon_30_39.png"),
   };


  // General GUI Elements
  private JPanel mainPanel;
  private JLabel systemOperatingStateLabel;
  private JLabel avgFanRelSpeedLabel;
  private JCheckBox automaticModeCheckBox;
  private JPanel actuator2Panel;
  private JPanel sensor2Panel;
  private ANSIColorPane ANSIColorPane1;

  // Sensor1 Widget
  private JLabel sensor1ConnStateLEDIcon;
  private JLabel sensor1C02DensityValue;
  private JLabel sensor1TempValue;
  private JLabel sensor1C02Icon;
  private JLabel sensor1TempIcon;

  // Sensor2 Widget
  private JLabel sensor2ConnStateLEDIcon;
  private JLabel sensor2C02DensityValue;
  private JLabel sensor2TempValue;
  private JLabel sensor2C02Icon;
  private JLabel sensor2TempIcon;

  // Actuator1 Widget
  private JLabel actuator1ConnStateLEDIcon;
  private JLabel actuator1FanRelSpeedValue;
  private JLabel actuator1LightStateLabel;
  private JSlider actuator1FanRelSpeedSlider;
  private JButton actuator1LightStateButtonOFF;
  private JButton actuator1LightStateButtonWARNING;
  private JButton actuator1LightStateButtonALERT;
  private JButton actuator1LightStateButtonEMERGENCY;
  private JLabel actuator1FanIcon;
  private JLabel actuator1LightIcon;

  // Actuator2 Widget
  private JLabel actuator2ConnStateLEDIcon;
  private JLabel actuator2FanRelSpeedValue;
  private JLabel actuator2LightStateLabel;
  private JSlider actuator2FanRelSpeedSlider;
  private JButton actuator2LightStateButtonOFF;
  private JButton actuator2LightStateButtonWARNING;
  private JButton actuator2LightStateButtonALERT;
  private JButton actuator2LightStateButtonEMERGENCY;
  private JLabel actuator2FanIcon;
  private JLabel actuator2LightIcon;


  public boolean autoMode;
  public OpState systemOpState;

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
    setSize(1230,430);
    setResizable(false);

    // Set the application to close after closing the GUI window
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    // Initialize the Log window output stream
    ANSIColorPaneOutputStream logOutputStream = new ANSIColorPaneOutputStream(ANSIColorPane1);

    // Redirect stdout to the Low window
    System.setOut(new PrintStream(logOutputStream));

    // Automatic mode checkbox listener
    // TODO: Description
    automaticModeCheckBox.addItemListener(itemEvent ->
     {
      if(itemEvent.getStateChange()==ItemEvent.SELECTED)
       {
        Log.info("Automatic Mode engaged");
        autoMode = true;

        // Trigger the fan quantities automatic adjustment
        automaticModeFanControl();
       }
      else
       if(itemEvent.getStateChange()==ItemEvent.DESELECTED)
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
       Log.warn("A single sensor was retrieved from the database," + "hiding the second sensor widget from the GUI");

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
    if(Log.LOG_LEVEL==ErrCodeSeverity.DEBUG)
     sensorsList.forEach((sensor) -> Log.dbg("|- <" + sensor.ID + "," + sensor.MAC + ">"));

    // Initialize and populate the ArrayList of ControlSensorManagers
    ctrlSensorManagersList = new ArrayList<>();
    sensorsList.forEach(sensor -> ctrlSensorManagersList.add(new ControlSensorManager(sensor.MAC,sensor.ID,this)));

    // Sort the ControlSensorManagers list by increasing sensorID
    Collections.sort(ctrlSensorManagersList);

    // Bind the sensors with sensorID 1 and 2 to the GUI
    ctrlSensorManagersList.get(0).bindToGUI(sensor1ConnStateLEDIcon,sensor1C02DensityValue,sensor1TempValue,sensor1C02Icon,sensor1TempIcon);
    if(ctrlSensorManagersList.size()>1)
     ctrlSensorManagersList.get(1).bindToGUI(sensor2ConnStateLEDIcon,sensor2C02DensityValue,sensor2TempValue,sensor2C02Icon,sensor2TempIcon);
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
       Log.warn("A single actuator was retrieved from the database," + "hiding the second actuator widget from the GUI");

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
    if(Log.LOG_LEVEL==ErrCodeSeverity.DEBUG)
     actuatorsList.forEach((actuator) -> Log.dbg("|- <" + actuator.ID + "," + actuator.MAC + ">"));

    // Initialize and populate the ArrayList of ControlActuatorManagers
    ctrlActuatorManagersList = new ArrayList<>();
    actuatorsList.forEach(actuator -> ctrlActuatorManagersList.add(new ControlActuatorManager(actuator.MAC,actuator.ID,this,controlMySQLConnector)));

    // Sort the ControlActuatorManagers list by increasing actuatorID
    Collections.sort(ctrlActuatorManagersList);

    // Bind the actuators with actuatorID 1 and 2 to the GUI
    ctrlActuatorManagersList.get(0)
      .bindToGUI(actuator1ConnStateLEDIcon,actuator1FanRelSpeedValue,actuator1LightStateLabel,actuator1FanIcon,
                 actuator1LightIcon,actuator1FanRelSpeedSlider,actuator1LightStateButtonOFF,
                 actuator1LightStateButtonWARNING,actuator1LightStateButtonALERT,actuator1LightStateButtonEMERGENCY);
    if(ctrlActuatorManagersList.size()>1)
     ctrlActuatorManagersList.get(1)
       .bindToGUI(actuator2ConnStateLEDIcon,actuator2FanRelSpeedValue,actuator2LightStateLabel,actuator2FanIcon,
                  actuator2LightIcon,actuator2FanRelSpeedSlider,actuator2LightStateButtonOFF,
                  actuator2LightStateButtonWARNING,actuator2LightStateButtonALERT,actuator2LightStateButtonEMERGENCY);
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


    actuator1FanRelSpeedSlider.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       {GUIFanRelSpeedSliderUpdate(1,actuator1FanRelSpeedSlider.getValue());}
     });

    actuator2FanRelSpeedSlider.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       {GUIFanRelSpeedSliderUpdate(2,actuator2FanRelSpeedSlider.getValue());}
     });

    actuator1LightStateButtonOFF.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       {GUILightStateButtonUpdate(1,LightState.LIGHT_OFF);}
     });

    actuator1LightStateButtonWARNING.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       {GUILightStateButtonUpdate(1,LightState.LIGHT_ON);}
     });

    actuator1LightStateButtonALERT.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       {GUILightStateButtonUpdate(1,LightState.LIGHT_BLINK_ALERT);}
     });

    actuator1LightStateButtonEMERGENCY.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       {GUILightStateButtonUpdate(1,LightState.LIGHT_BLINK_EMERGENCY);}
     });

    actuator2LightStateButtonOFF.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       {GUILightStateButtonUpdate(2,LightState.LIGHT_OFF);}
     });

    actuator2LightStateButtonWARNING.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       {GUILightStateButtonUpdate(2,LightState.LIGHT_ON);}
     });

    actuator2LightStateButtonALERT.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       {GUILightStateButtonUpdate(2,LightState.LIGHT_BLINK_ALERT);}
     });

    actuator2LightStateButtonEMERGENCY.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       {GUILightStateButtonUpdate(2,LightState.LIGHT_BLINK_EMERGENCY);}
     });
   }

  private void GUIFanRelSpeedSliderUpdate(int actuatorID,int newFanSpeed)
   {
    try
     {
      ctrlActuatorManagersList.get(actuatorID - 1).sendFanRelSpeed(newFanSpeed);
     }
    catch(IndexOutOfBoundsException indexOutOfBoundsException)
     {
      Log.err("Attempting to update via slider " + actuatorID + " the fan relative speed of non-existing actuator" + actuatorID);
     }
   }

  private void GUILightStateButtonUpdate(int actuatorID,LightState newLightState)
   {
    try
     {
      ctrlActuatorManagersList.get(actuatorID - 1).sendLightState(newLightState);
     }
    catch(IndexOutOfBoundsException indexOutOfBoundsException)
     {
      Log.err("Attempting to update via GUI button the light state to '" + newLightState.toString()
        + "' of non-existing actuator" + actuatorID);
     }
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
    if(newSensorOpState==systemOpState)
     return;

     // Otherwise, if the sensor's updated is more severe than the
     // system's operating state, update the latter to the former
    else
     if(newSensorOpState.ordinal()>systemOpState.ordinal())
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
        maxSensorOpState = OpState.values()[max(maxSensorOpState.ordinal(),
          ctrlSensorMgr.getSensorOperatingState().ordinal())];

       // If the maximum among all the sensor's operating
       // state is less severe than the current system
       // severity, the system can switch to such state
       if(maxSensorOpState!=systemOpState)
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
      Log.info("System now in the " + systemOpState.toString() + " state");

      // If automatic mode is enabled, also adjust the
      // actuators' fan relative speeds and light states
      if(autoMode)
       {
        automaticModeFanControl();
       }
     }
   }

  public void updateAvgFanRelSpeed()
   {
    int numFansToCount = 0;
    int totFanRelSpeed = 0;

    for(ControlActuatorManager ctrlActuatorManager : ctrlActuatorManagersList)
     {
      // The actuator's fan relative speed
      int actFanRelSpeed = ctrlActuatorManager.getFanRelSpeed();

      // If a valid fan relative speed was received by the actuator
      if(actFanRelSpeed!=-1)
       {
        totFanRelSpeed += actFanRelSpeed;
        numFansToCount++;
       }
     }

    // New average fan relative speed
    int newAvgFanRelSpeed = totFanRelSpeed / numFansToCount;

    // If the new average fan relative speed differs from its previous value
    if(avgFanRelSpeed!=newAvgFanRelSpeed)
     {
      // Update the system's average fan relative speed
      avgFanRelSpeed = newAvgFanRelSpeed;

      // Update the system's average fan relative speed GUI label
      avgFanRelSpeedLabel.setText(avgFanRelSpeed + " %");
      avgFanRelSpeedLabel.setForeground(fanRelSpeedToOpStateColor(avgFanRelSpeed));

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
   {
    // Parse the possible command-line input arguments
    InputArgsParser.parseCMDInputArgs("ControlModule",args);

    // If the command-line input arguments
    // are valid, start the Control Module
    new ControlModule();
   }
 }