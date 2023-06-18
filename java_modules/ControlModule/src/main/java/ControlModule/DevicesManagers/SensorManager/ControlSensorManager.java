/* A sensor managed by the Control Module */

package ControlModule.DevicesManagers.SensorManager;

/* ================================== IMPORTS ================================== */

/* --------------------- Java Standard Libraries Resources --------------------- */
import javax.swing.*;
import static java.lang.Math.max;

/* --------------------------- SafeTunnels Resources --------------------------- */
import logging.Log;
import ControlModule.ControlModule;
import ControlModule.OpState;
import devices.sensor.BaseSensor;


/* ============================== CLASS DEFINITION ============================== */
public final class ControlSensorManager extends BaseSensor
 {
  /* ============== SENSOR QUANTITIES OPERATING STATES THRESHOLDS ============== */

  // C02 Thresholds
  private static final int C02ThresholdWARNING = 2000;
  private static final int C02ThresholdALERT = 5000;
  private static final int C02ThresholdEMERGENCY = 10000;

  // Temperature Thresholds
  private static final int tempThresholdWARNING = 35;
  private static final int tempThresholdALERT = 40;
  private static final int tempThresholdEMERGENCY = 45;

  /* ============================ PRIVATE ATTRIBUTES ============================ */

  /* -------------- Sensor Quantities Operating States Thresholds -------------- */

  // Sensor quantities
  private int C02;
  private int temp;

  // Sensor quantities operating states
  private OpState C02OpState;
  private OpState tempOpState;

  // Sensor overall operating state
  private OpState sensorOpState;

  // Control Module reference
  private final ControlModule controlModule;

  /* ----------------------- GUI Sensor Widget Management ----------------------- */

  // Whether the sensor is bound to a sensor widget in the GUI
  private boolean GUIBound;

  // The GUI's JLabels associated with the sensor, if any
  private JLabel connStateLED;   // Connection state LED
  private JLabel C02Label;       // C02 value
  private JLabel tempLabel;      // Temperature value
  private JLabel C02Icon;        // C02 icon
  private JLabel tempIcon;       // Temperature icon


  /* ============================== PRIVATE METHODS ============================== */

  /**
   *  Updates the sensor's operating state based on its C02 and
   *  temperature values and, if it has changed, notifies the
   *  Control Module to possibly update the overall system's state
   */
  private void updateSensorOperatingState()
   {
    // Save the current sensor operating state
    OpState oldOpState = sensorOpState;

    // Set the sensor operating state as its maximum
    // between its C02 and temperature operating states
    sensorOpState = OpState.values()[max(C02OpState.ordinal(),tempOpState.ordinal())];

    // If the sensor operating state has changed, notify the control
    // module to possibly update the overall system's operating state
    if(sensorOpState != oldOpState)
     controlModule.updateSystemOpState(sensorOpState);
   }


  /**
   * Depending on its current value, updates the sensor's C02 and possibly
   * in cascade the sensor and the overall system's operating state
   */
  private void updateC02OperatingState()
   {
    if(C02 < C02ThresholdWARNING)
     C02OpState = OpState.NOMINAL;
    else
     if(C02 < C02ThresholdALERT)
      C02OpState = OpState.WARNING;
     else
      if(C02 < C02ThresholdEMERGENCY)
       C02OpState = OpState.ALERT;
      else
       C02OpState = OpState.EMERGENCY;

    // Possibly update in cascade the sensor
    // and the system's overall operating state
    updateSensorOperatingState();
   }

  /**
   * Depending on its current value, updates the sensor's temperature and
   * possibly in cascade the sensor and the overall system's operating state
   */
  private void updateTempOperatingState()
   {
    if(temp < tempThresholdWARNING)
     tempOpState = OpState.NOMINAL;
    else
     if(temp < tempThresholdALERT)
      tempOpState = OpState.WARNING;
     else
      if(temp < tempThresholdEMERGENCY)
       tempOpState = OpState.ALERT;
      else
       tempOpState = OpState.EMERGENCY;

    // Possibly update in cascade the sensor
    // and the system's overall operating state
    updateSensorOperatingState();
   }


  /* ============================= PUBLIC METHODS ============================= */

  /**
   * ControlSensorManager constructor, initializing its attributes
   * @param MAC The sensor's (unique) MAC address
   * @param ID  The sensor's unique ID in the SafeTunnels database
   * @param controlModule A reference to the Control Module object
   */
  public ControlSensorManager(String MAC, short ID, ControlModule controlModule)
   {
    // Call the parent constructor, initializing
    // the sensor's MAC, ID and connState to false
    super(MAC,ID);

    // Initialize the other ControlSensorManager
    // attributes to their default values
    this.controlModule = controlModule;
    temp = -1;
    C02 = -1;
    GUIBound = false;
    connStateLED = null;
    C02Label = null;
    tempLabel = null;
    C02OpState = OpState.NOMINAL;
    tempOpState = OpState.NOMINAL;
    sensorOpState = OpState.NOMINAL;
   }


  /**
   * @return The sensor's current operating state
   */
  public OpState getSensorOperatingState()
   { return sensorOpState; }


  /**
   * Binds the sensor to a sensor widget in the Control Module's GUI
   * @param connStateLED The sensor widget's connection state LEd
   * @param C02Label     The sensor widget's C02 value
   * @param tempLabel    The sensor widget's temperature value
   * @param C02Icon      The sensor widget's C02 icon
   * @param tempIcon     The sensor widget's temperature icon
   */
  public void bindToGUI(JLabel connStateLED, JLabel C02Label,
                        JLabel tempLabel, JLabel C02Icon, JLabel tempIcon)
   {

    // Ensure all the passed sensor widget GUI components to
    // be non-null, logging an error and returning otherwise
    if(connStateLED == null || C02Label == null ||
      tempLabel == null || C02Icon == null || tempIcon == null)
     {
      Log.err("Attempting to bind sensor" + ID
              + "to null elements in the GUI");
      return;
     }

    // Initialize the sensor widget's
    // components to the provided values
    this.connStateLED = connStateLED;
    this.C02Label = C02Label;
    this.tempLabel = tempLabel;
    this.C02Icon = C02Icon;
    this.tempIcon = tempIcon;

    // Set that the sensor is now
    // bound to a GUI sensor widget
    GUIBound = true;
   }


  /* ----------------------------- Setters Methods ----------------------------- */

  /**
   * Marks the sensor as offline and disables
   * its associated GUI sensor widget, if any
   */
  @Override
  public void setConnStateOffline()
   {
    // Set the sensor as offline
    connState = false;

    // Log that the sensor appears to be offline
    Log.warn("sensor" + ID + " appears to be offline");

    // If the sensor is bound to a GUI sensor widget
    if(GUIBound)
     {
      // Update the connection state LED icon
      connStateLED.setIcon(ControlModule.connStateLEDOFFImg);

      // Disable the sensor widget labels and icons
      C02Label.setEnabled(false);
      tempLabel.setEnabled(false);
      C02Icon.setEnabled(false);
      tempIcon.setEnabled(false);
     }
   }


  /**
   * Marks the sensor as online and enables
   * its associated GUI sensor widget, if any
   */
  @Override
  public void setConnStateOnline()
   {
    // Set the sensor as online
    connState = true;

    // Log that the sensor is now online
    Log.info("sensor" + ID + " is now online");

    // If the sensor is bound to a GUI sensor widget
    if(GUIBound)
     {
      // Update the connection state LED icon
      connStateLED.setIcon(ControlModule.connStateLEDONImg);

      // Activate the sensor widget labels and icons
      C02Label.setEnabled(true);
      tempLabel.setEnabled(true);
      C02Icon.setEnabled(true);
      tempIcon.setEnabled(true);
     }
   }


  /**
   * Sets a new C02 value for the sensor, also updating
   * it in its associated GUI sensor widget, if any
   * @param newC02 The sensor's new C02 value
   */
  @Override
  public void setC02(int newC02)
   {
    // If the sensor was offline, set it online
    if(!connState)
     setConnStateOnline();

    // Log the sensor's updated C02 value
    Log.dbg("Received sensor" + ID + " updated C02 value (" + newC02 + ")");

    // If its updated differs from its current C02 value
    if(newC02 != this.C02)
     {
      // Update the sensor's C02 value
      this.C02 = newC02;

      // Possibly update the C02 and in cascade the
      // sensor and the system's overall operating states
      updateC02OperatingState();

      // If the sensor is bound to a GUI widget, update its C02
      // value, setting its color as of its current operating state
      if(GUIBound)
       {
        C02Label.setText(this.C02 + " ppm");
        C02Label.setForeground(C02OpState.getColor());
       }
     }
   }


  /**
   * Sets a new temperature value for the sensor, also
   * updating it in its associated GUI sensor widget, if any
   * @param newTemp The sensor's new temperature value
   */
  @Override
  public void setTemp(int newTemp)
   {
    // If the sensor was offline, set it online
    if(!connState)
     setConnStateOnline();

    // Log the sensor's updated temperature value
    Log.dbg("Received sensor" + ID + " updated temperature value (" + newTemp + ")");

    // If its updated differs from its current temperature value
    if(newTemp != this.temp)
     {
      // Update the sensor's temperature value
      temp = newTemp;

      // Possibly update the temperature and in cascade the
      // sensor and the system's overall operating states
      updateTempOperatingState();

      // If the sensor is bound to a GUI widget, update its temperature
      // value, setting its color as of its current operating state
      if(GUIBound)
       {
        tempLabel.setText(temp + " °C");
        tempLabel.setForeground(tempOpState.getColor());
       }
     }
   }
 }