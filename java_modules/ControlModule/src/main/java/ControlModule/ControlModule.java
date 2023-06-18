/* Control Module Main Class, managing its GUI and components */

package ControlModule;

/* ================================== IMPORTS ================================== */

/* --------------------- Java Standard Libraries Resources --------------------- */
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import static java.lang.Math.max;

/* --------------------------- SafeTunnels Resources --------------------------- */
import logging.Log;
import errors.ErrCodeSeverity;
import modules.MySQLConnector.DevMACIDPair;
import modules.InputArgsParser.InputArgsParser;
import devices.actuator.BaseActuator.LightState;
import modules.SensorsMQTTHandler.SensorsMQTTHandler;
import ControlModule.ControlMySQLConnector.ControlMySQLConnector;
import ControlModule.DevicesManagers.ActuatorManager.ControlActuatorManager;
import ControlModule.DevicesManagers.SensorManager.ControlSensorManager;
import ControlModule.GUILogging.ANSIColorPane;
import ControlModule.GUILogging.ANSIColorPaneOutputStream;
import static ControlModule.OpState.fanRelSpeedToOpStateColor;
import static devices.BaseDevice.DevType.actuator;
import static devices.BaseDevice.DevType.sensor;


/* ============================== CLASS DEFINITION ============================== */
public final class ControlModule extends JFrame
 {
  /* ============================== GUI RESOURCES ============================== */

  /* ----------------------------- Preloaded Icons ----------------------------- */

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

  // Array of fan icons with an 18° degree offset
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

  /* ------------------------ GUI Main Panel Components ------------------------ */
  private JPanel mainPanel;                   // Main Panel
  private JLabel systemOperatingStateLabel;   // Operating State Panel
  private JLabel avgFanRelSpeedLabel;         // System Average Fan Speed Value
  private JCheckBox automaticModeCheckBox;    // Automatic Mode checkbox
  private JPanel sensor2Panel;                // Sensor 2 widget
  private JPanel actuator2Panel;              // Actuator 2 widget
  private ANSIColorPane ANSIColorPane1;       // Log Window

  /* ----------------------------- Sensors Widgets ----------------------------- */

  // ------------------------------- Sensor1 Widget -------------------------------
  private JLabel sensor1ConnStateLEDIcon;   // Connection state LED
  private JLabel sensor1C02DensityValue;    // C02 value
  private JLabel sensor1TempValue;          // Temperature value
  private JLabel sensor1C02Icon;            // C02 icon
  private JLabel sensor1TempIcon;           // Temperature icon

  // ------------------------------- Sensor2 Widget -------------------------------
  private JLabel sensor2ConnStateLEDIcon;   // Connection state LED
  private JLabel sensor2C02DensityValue;    // C02 value
  private JLabel sensor2TempValue;          // Temperature value
  private JLabel sensor2C02Icon;            // C02 icon
  private JLabel sensor2TempIcon;           // Temperature icon

  /* ---------------------------- Actuators Widgets ---------------------------- */

  // ------------------------------ Actuator1 Widget ------------------------------
  private JLabel actuator1ConnStateLEDIcon;            // Connection state LED
  private JLabel actuator1FanRelSpeedValue;            // Fan relative speed value
  private JLabel actuator1LightStateLabel;             // Light state value
  private JLabel actuator1FanIcon;                     // Fan icon
  private JLabel actuator1LightIcon;                   // Light icon
  private JSlider actuator1FanRelSpeedSlider;          // Fan relative speed slider
  private JButton actuator1LightStateButtonOFF;        // Light "OFF" button
  private JButton actuator1LightStateButtonWARNING;    // Light "ON"/"WARN" button
  private JButton actuator1LightStateButtonALERT;      // Light "ALERT" button
  private JButton actuator1LightStateButtonEMERGENCY;  // Light "EMERGENCY" button


  // ------------------------------ Actuator2 Widget ------------------------------
  private JLabel actuator2ConnStateLEDIcon;            // Connection state LED
  private JLabel actuator2FanRelSpeedValue;            // Fan relative speed value
  private JLabel actuator2LightStateLabel;             // Light state value
  private JLabel actuator2FanIcon;                     // Fan icon
  private JLabel actuator2LightIcon;                   // Light icon
  private JSlider actuator2FanRelSpeedSlider;          // Fan relative speed slider
  private JButton actuator2LightStateButtonOFF;        // Light "OFF" button
  private JButton actuator2LightStateButtonWARNING;    // Light "ON"/"WARN" button
  private JButton actuator2LightStateButtonALERT;      // Light "ALERT" button
  private JButton actuator2LightStateButtonEMERGENCY;  // Light "EMERGENCY" button


  /* ============================ PRIVATE ATTRIBUTES ============================ */

  // A reference to the Control Module MySQL Connector
  private final ControlMySQLConnector controlMySQLConnector;

  // A reference to the Control Module MQTT Handler
  private final SensorsMQTTHandler controlMQTTHandler;

  // The lists of sensors and actuators managed by the application
  private ArrayList<ControlSensorManager> ctrlSensorManagersList;
  private ArrayList<ControlActuatorManager> ctrlActuatorManagersList;

  // The system's average fan relative speed, which is also published to sensors
  private int avgFanRelSpeed;


  /* ============================ PUBLIC ATTRIBUTES ============================ */

  // The current's system operating state
  public OpState systemOpState;

  // Whether the system's automatic mode, or automatic actuator
  // adjustment depending on its current operating state, is enabled
  public boolean autoMode;


  /* ============================== PRIVATE METHODS ============================== */

  /* ----------------------- System Initialization Methods ----------------------- */

  /**
   * Initializes the Control Module's GUI
   */
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

    // Register the "Automatic Mode" checkbox event listeners
    automaticModeCheckBox.addItemListener(itemEvent ->
     {
      // If automatic mode has been engaged
      if(itemEvent.getStateChange()==ItemEvent.SELECTED)
       {
        // Enable the automatic mode
        autoMode = true;

        // Log that automatic mode has been engaged
        Log.info("Automatic Mode engaged");

        // Directly trigger the actuator's
        // quantities automatic adjustments
        autoModeFanAdjustment();
       }

      // Otherwise, if automatic mode has been disengaged
      else
       if(itemEvent.getStateChange()==ItemEvent.DESELECTED)
        {
         // Disable the automatic mode
         autoMode = false;

         // Log that automatic mode has been disengaged
         Log.info("Automatic Mode disengaged");
        }

       // Unhandled automatic mode state change
       else
        Log.warn("Unhandled Automatic Mode state change: "
                 + itemEvent.getStateChange());
     });

    // Draw the GUI window
    setVisible(true);
    setContentPane(mainPanel);
   }


  /**
   * Initializes the Control Module's sensors and
   * associated ControlSensorManagers objects
   */
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
       Log.warn("A single sensor was retrieved from the database, "
                + "hiding the second sensor widget from the GUI");

       // Hide the sensor2 widget
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
     sensorsList.forEach((sensor) -> Log.dbg("|- <" + sensor.ID +
                                             "," + sensor.MAC + ">"));

    // Initialize and populate the ArrayList of ControlSensorManagers
    ctrlSensorManagersList = new ArrayList<>();
    sensorsList.forEach(sensor -> ctrlSensorManagersList.add(
      new ControlSensorManager(sensor.MAC,sensor.ID,this)));

    // Sort the ControlSensorManagers list by increasing sensorID
    Collections.sort(ctrlSensorManagersList);

    // Bind the sensor with sensorID == 1 and, if
    // available, the one with sensorID == 2 to the GUI
    ctrlSensorManagersList.get(0).bindToGUI(sensor1ConnStateLEDIcon,sensor1C02DensityValue,
                                            sensor1TempValue,sensor1C02Icon,sensor1TempIcon);
    if(ctrlSensorManagersList.size() > 1)
     ctrlSensorManagersList.get(1).bindToGUI(sensor2ConnStateLEDIcon,sensor2C02DensityValue,
                                             sensor2TempValue,sensor2C02Icon,sensor2TempIcon);
   }


  /**
   * Initializes the Control Module's actuators
   * and associated ControlActuatorManagers objects
   */
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
       Log.warn("A single actuator was retrieved from the database, "
                + "hiding the second actuator widget from the GUI");

       // Hide the actuator2 widget
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
     actuatorsList.forEach((actuator) -> Log.dbg("|- <" + actuator.ID +
                                                 "," + actuator.MAC + ">"));

    // Initialize and populate the ArrayList of ControlActuatorManagers
    ctrlActuatorManagersList = new ArrayList<>();
    actuatorsList.forEach(actuator -> ctrlActuatorManagersList.add
      (new ControlActuatorManager(actuator.MAC,actuator.ID,
                      this,controlMySQLConnector)));

    // Sort the ControlActuatorManagers list by increasing actuatorID
    Collections.sort(ctrlActuatorManagersList);

    // Bind the actuator with actuatorID == 1 and, if
    // available, the one with actuatorID == 2 to the GUI
    ctrlActuatorManagersList.get(0)
      .bindToGUI(actuator1ConnStateLEDIcon,actuator1FanRelSpeedValue,
                 actuator1LightStateLabel,actuator1FanIcon,
                 actuator1LightIcon,actuator1FanRelSpeedSlider,
                 actuator1LightStateButtonOFF,actuator1LightStateButtonWARNING,
                 actuator1LightStateButtonALERT,actuator1LightStateButtonEMERGENCY);
    if(ctrlActuatorManagersList.size() > 1)
     ctrlActuatorManagersList.get(1)
       .bindToGUI(actuator2ConnStateLEDIcon,actuator2FanRelSpeedValue,
                  actuator2LightStateLabel,actuator2FanIcon,
                  actuator2LightIcon,actuator2FanRelSpeedSlider,
                  actuator2LightStateButtonOFF,actuator2LightStateButtonWARNING,
                  actuator2LightStateButtonALERT,actuator2LightStateButtonEMERGENCY);
   }


  /**
   * Initializes the GUI actuator widgets' mouse listeners
   */
  private void initActuatorsWidgetsMouseListeners()
   {
    /* ------------------ Actuator 1 Widget Initialization ------------------ */

    // Fan relative speed slider
    actuator1FanRelSpeedSlider.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       { GUIFanRelSpeedSliderUpdate(1,actuator1FanRelSpeedSlider.getValue()); }
     });

    // Light "OFF" button
    actuator1LightStateButtonOFF.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       {GUILightStateButtonUpdate(1,LightState.LIGHT_OFF);}
     });

    // Light "ON"/"WARN" button
    actuator1LightStateButtonWARNING.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       {GUILightStateButtonUpdate(1,LightState.LIGHT_ON);}
     });

    // Light "ALERT" button
    actuator1LightStateButtonALERT.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       {GUILightStateButtonUpdate(1,LightState.LIGHT_BLINK_ALERT);}
     });

    // Light "EMERGENCY" button
    actuator1LightStateButtonEMERGENCY.addMouseListener(new MouseAdapter()
     {
      @Override
      public void mouseReleased(MouseEvent e)
       {GUILightStateButtonUpdate(1,LightState.LIGHT_BLINK_EMERGENCY);}
     });

    /* ------------------ Actuator 2 Widget Initialization ------------------ */

    if(ctrlActuatorManagersList.size() > 1)
     {
      // Fan relative speed slider
      actuator2FanRelSpeedSlider.addMouseListener(new MouseAdapter()
       {
        @Override
        public void mouseReleased(MouseEvent e)
         {GUIFanRelSpeedSliderUpdate(2,actuator2FanRelSpeedSlider.getValue());}
       });

      // Light "OFF" button
      actuator2LightStateButtonOFF.addMouseListener(new MouseAdapter()
       {
        @Override
        public void mouseReleased(MouseEvent e)
         {GUILightStateButtonUpdate(2,LightState.LIGHT_OFF);}
       });

      // Light "ON"/"WARN" button
      actuator2LightStateButtonWARNING.addMouseListener(new MouseAdapter()
       {
        @Override
        public void mouseReleased(MouseEvent e)
         {GUILightStateButtonUpdate(2,LightState.LIGHT_ON);}
       });

      // Light "ALERT" button
      actuator2LightStateButtonALERT.addMouseListener(new MouseAdapter()
       {
        @Override
        public void mouseReleased(MouseEvent e)
         {GUILightStateButtonUpdate(2,LightState.LIGHT_BLINK_ALERT);}
       });

      // Light "EMERGENCY" button
      actuator2LightStateButtonEMERGENCY.addMouseListener(new MouseAdapter()
       {
        @Override
        public void mouseReleased(MouseEvent e)
         {GUILightStateButtonUpdate(2,LightState.LIGHT_BLINK_EMERGENCY);}
       });
     }
   }


  /**
   * Control Module Constructor, initializing the
   * application's GUI and its other components
   */
  private ControlModule()
   {
    // Initialize the system's base attributes
    autoMode = true;
    systemOpState = OpState.NOMINAL;
    avgFanRelSpeed = -1;

    // Initialize the application's GUI window
    initGUI();

    // Attempt to connect with the SafeTunnels MySQL database
    controlMySQLConnector = new ControlMySQLConnector();

    // Initialize the system's sensors
    initSensors();

    // Initialize the system's actuators
    initActuators();

    // Initialize the GUI actuator widgets' mouse listeners
    initActuatorsWidgetsMouseListeners();

    // Attempt to instantiate the Control Module MQTT Client Handler
    controlMQTTHandler = new SensorsMQTTHandler("ControlModule",ctrlSensorManagersList);

    // Log that the Control Module has been successfully initialized
    Log.info("Control Module successfully initialized");
   }


  /* ------------------------- System Operation Methods ------------------------- */

  /**
   * Actuator Widgets' fan slider callback function, attempting to send the
   * specified fan relative speed value to the actuator associated with the widget
   * @param actuatorID     The ID of the actuator to send the "newFanRelSpeed" value to
   * @param newFanRelSpeed The new fan relative speed value to be sent to the actuator
   */
  private void GUIFanRelSpeedSliderUpdate(int actuatorID,int newFanRelSpeed)
   {
    // Attempt to retrieve the ControlActuatorManager of specified
    // index/ID and send the new fan relative speed value to the actuator,
    // logging an error if an actuator of such index/ID does not exist
    try
     { ctrlActuatorManagersList.get(actuatorID - 1).sendFanRelSpeed(newFanRelSpeed); }
    catch(IndexOutOfBoundsException indexOutOfBoundsException)
     { Log.err("Attempting to update via slider " + actuatorID +
               " the fan relative speed of non-existing actuator" + actuatorID); }
   }

  /**
   * Actuator Widgets' light buttons callback function, attempting to send the
   * specified light state value to the actuator associated with the widget
   * @param actuatorID    The ID of the actuator to send the "newFanRelSpeed" value to
   * @param newLightState The new light state value to be sent to the actuator
   */
  private void GUILightStateButtonUpdate(int actuatorID,LightState newLightState)
   {
    // Attempt to retrieve the ControlActuatorManager of specified
    // index/ID and send the new light state value to the actuator,
    // logging an error if an actuator of such index/ID does not exist
    try
     { ctrlActuatorManagersList.get(actuatorID - 1).sendLightState(newLightState); }
    catch(IndexOutOfBoundsException indexOutOfBoundsException)
     {
      Log.err("Attempting to update via GUI button the light state to '"
        + newLightState.toString() + "' of non-existing actuator" + actuatorID);
     }
   }


  /**
   *  System automatic mode fan quantities adjustment method, sending
   *  to every actuator the fan relative speed and light state
   *  value associated with the current system's operating state
   */
  private void autoModeFanAdjustment()
   {
    // Retrieve the fan relative speed and light state values
    // associated with the current system operating state
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


  /* ============================= PUBLIC METHODS ============================= */

  /**
   * Possibly updates the system's operating state following a sensor operating
   * state change, also driving the actuators with the values associated
   * with such state if their automatic quantities' adjustment is enabled
   * @param newSensorOpState The SensorManager caller's new operating state
   */
  public void updateSystemOpState(OpState newSensorOpState)
   {
    // Whether the system operating state has changed from its previous value
    boolean systemOpStateChanged = false;

    // If the sensor's new and the system current operating state
    // are the same, the latter does not need to be updated
    if(newSensorOpState == systemOpState)
     return;

    // Otherwise, if the sensor's new is MORE severe than the current
    // system's operating state, directly set the latter to the former
    else
     if(newSensorOpState.ordinal()>systemOpState.ordinal())
      {
       systemOpState = newSensorOpState;
       systemOpStateChanged = true;
      }

    // Otherwise, if the sensor's new is LESS severe than the
    // system's operating state, it must be verified whether
    // the latter can switch to such a lower operating state
     else
      {
       // Stores the maximum among all the sensor's operating states
       OpState maxSensorOpState = OpState.NOMINAL;

       // Compute the maximum among all the sensor's operating states
       for(ControlSensorManager ctrlSensorMgr : ctrlSensorManagersList)
        maxSensorOpState = OpState.values()[max(maxSensorOpState.ordinal(),
          ctrlSensorMgr.getSensorOperatingState().ordinal())];

       // If the maximum among all the sensor's operating states
       // is less severe than the current system operating
       // state, the latter can switch to such lower state
       if(maxSensorOpState!=systemOpState)
        {
         systemOpState = maxSensorOpState;
         systemOpStateChanged = true;
        }
      }

    /* --------------- System Operating State Change Adjustments --------------- */

    // If the system's operating state has changed
    if(systemOpStateChanged)
     {
      // Update the GUI system state label text and
      // color to match the new operating state
     systemOperatingStateLabel.setText(systemOpState.toString());
      systemOperatingStateLabel.setForeground(systemOpState.getColor());

      // Log the system operating state change
      Log.info("System now in the " + systemOpState.toString() + " state");

      // If the fan quantities' automatic adjustment is enabled, drive them
      // with the values associated with the new system operating state
     if(autoMode)
      autoModeFanAdjustment();
     }
   }


  /**
   * Possibly updates the system's average fan relative speed following
   * an actuator fan relative speed change, also publishing an updated
   * quantity on the sensors' MQTT 'TOPIC_SENSORS_ERRORS' topic
   */
  public void updateAvgFanRelSpeed()
   {
    // The number of fans to be counted in computing
    // the system's average fan relative speed
    int numFansToCount = 0;

    // The total system fan relative speed
    int totFanRelSpeed = 0;

    // For every actuator managed by the system
    for(ControlActuatorManager ctrlActuatorManager : ctrlActuatorManagersList)
     {
      // If the actuator is online
      if(ctrlActuatorManager.getConnState())
       {
        // Retrieve the actuator's fan relative speed value
        int actFanRelSpeed = ctrlActuatorManager.getFanRelSpeed();

        // If such value is valid, i.e. the manager received at least
        // a fan relative speed value from when the actuator had connected
        if(actFanRelSpeed != -1)
         {
          // Sum the actuator's fan relative speed value in the total
          // system fan relative speed and increment the number
          // of fans to be counted in computing the mean value
          totFanRelSpeed += actFanRelSpeed;
          numFansToCount++;
         }
       }
     }

    // Compute the new system average fan relative speed
    int newAvgFanRelSpeed = totFanRelSpeed / numFansToCount;

    // If the system new average fan relative
    // speed differs from its current value
    if(avgFanRelSpeed != newAvgFanRelSpeed)
     {
      // Update the system's average fan relative speed
      avgFanRelSpeed = newAvgFanRelSpeed;

      // Update the system's average fan relative speed GUI label
      // and set its color to match its associated "operating state"
      avgFanRelSpeedLabel.setText(avgFanRelSpeed + " %");
      avgFanRelSpeedLabel.setForeground(fanRelSpeedToOpStateColor(avgFanRelSpeed));

      // Attempt to publish the new system average fan relative
      // speed on the sensors' MQTT 'TOPIC_AVG_FAN_REL_SPEED' topic
      controlMQTTHandler.publishAvgFanRelSpeed(avgFanRelSpeed);

      // Log that the new system average fan relative speed has been published
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

    // Attempt to enable hardware acceleration
    System.setProperty("sun.java2d.opengl", "true");

    // Start the Control Module
    new ControlModule();
   }
 }