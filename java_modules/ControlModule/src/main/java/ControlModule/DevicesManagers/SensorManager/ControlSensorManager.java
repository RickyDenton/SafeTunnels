package ControlModule.DevicesManagers.SensorManager;

import ControlModule.ControlModule;
import ControlModule.OpState;
import devices.sensor.BaseSensor;
import logging.Log;

import javax.swing.*;

import static java.lang.Math.max;


public class ControlSensorManager extends BaseSensor
 {
  // C02 Thresholds
  private static final int C02ThresholdWARNING = 2000;
  private static final int C02ThresholdALERT = 5000;
  private static final int C02ThresholdEMERGENCY = 10000;

  // Temperature Thresholds
  private static final int tempThresholdWARNING = 35;
  private static final int tempThresholdALERT = 40;
  private static final int tempThresholdEMERGENCY = 45;

  // Sensor quantities
  private int C02;
  private int temp;

  // Sensor quantities operating states
  private OpState C02OpState;
  private OpState tempOpState;

  // Sensor overall operating state
  private OpState sensorOpState;

  // Controller Module reference
  private final ControlModule controlModule;

  // Whether the sensor is bound to a sensor widget in the GUI
  private boolean GUIBound;

  // The GUI's JLabels associated with the sensor, if any
  private JLabel connStateLED;
  private JLabel C02Label;
  private JLabel tempLabel;




  // Binds this sensor to a sensor widget in the GUI
  public void bindToGUI(JLabel connStateLED, JLabel C02Label, JLabel tempLabel)
   {
    if(connStateLED == null || C02Label == null || tempLabel == null)
     {
      Log.err("Attempting to bind sensor" + ID + "to null elements in the GUI");
      return;
     }

    // Assign the GUI's JLabels associated with the sensor
    this.connStateLED = connStateLED;
    this.C02Label = C02Label;
    this.tempLabel = tempLabel;

    // Set that the sensor is now bound to a sensor widget in the GUI
    GUIBound = true;
   }


  public ControlSensorManager(String MAC, short ID, ControlModule controlModule)
   {
    // Call the parent's constructor, initializing the sensor's connState to false
    super(MAC,ID);

    // Initialize the other ControlSensorManager's attributes
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



  // Possibly update the sensor's and the system's operating states
  private void updateSensorOperatingState()
   {
    // Save the current sensor operating state
    OpState oldOpState = sensorOpState;

    // Set the sensor operating state as its maximum
    // between its C02 and temperature operating states
    sensorOpState = OpState.values()[max(C02OpState.ordinal(),tempOpState.ordinal())];

    // If the sensor operating state has changed,
    // possibly update the system's operating state
    if(sensorOpState != oldOpState)
     controlModule.updateSystemOpState(sensorOpState);
   }


  // Possibly updates the C02 and the sensor's operating states
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

    // Possibly update the sensor's operating state
    updateSensorOperatingState();
   }

  // Possibly updates the temperature and the sensor's operating states
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

    // Possibly update the sensor's operating state
    updateSensorOperatingState();
   }

  public OpState getSensorOperatingState()
   { return sensorOpState; }



  @Override
  public void setConnStateOffline()
   {
    // Set the sensor as offline
    connState = false;

    // Log that the sensor appears to be offline
    Log.warn("sensor" + ID + " appears to be offline");

    // If bound to a GUI widget, update the
    // associated connection status LED icon
    if(GUIBound)
     { connStateLED.setIcon(ControlModule.connStateLEDOFFImg); }
   }

  @Override
  public void setConnStateOnline()
   {
    // Set the sensor as online
    connState = true;

    // Log that the sensor is now online
    Log.info("sensor" + ID + " is now online");

    // If bound to a GUI widget, update the
    // associated connection status LED icon
    if(GUIBound)
     { connStateLED.setIcon(ControlModule.connStateLEDONImg); }
   }

  @Override
  public void setC02(int newC02)
   {
    // If the sensor was offline, set it online
    if(!connState)
     setConnStateOnline();

    // Log the sensor's updated C02 value
    Log.dbg("Received sensor" + ID + " updated C02 value (" + newC02 + ")");

    // If its updated differs from its current C02 value
    if(newC02 != C02)
     {
      // Update the sensor's C02 value
      C02 = newC02;

      // Possibly update the C02 and the sensor's overall operating states
      updateC02OperatingState();

      // If bound to a GUI element, update the sensor's widget C02 value
      if(GUIBound)
       {
        C02Label.setText(C02 + " ppm");
        C02Label.setForeground(C02OpState.getColor());
       }
     }
   }

  @Override
  public void setTemp(int newTemp)
   {
    // If the sensor was offline, set it online
    if(!connState)
     setConnStateOnline();

    // Log the sensor's updated temperature value
    Log.dbg("Received sensor" + ID + " updated temperature value (" + newTemp + ")");

    // If its updated differs from its current temperature value
    if(newTemp != temp)
     {
      // Update the sensor's temperature value
      temp = newTemp;

      // Possibly update the temperature and the sensor's overall operating states
      updateTempOperatingState();

      // If bound to a GUI element, update the sensor's widget temperature value
      if(GUIBound)
       {
        tempLabel.setText(temp + " °C");
        tempLabel.setForeground(tempOpState.getColor());
       }
     }
   }

 }
