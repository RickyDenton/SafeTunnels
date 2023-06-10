package DevicesManagers;

import ControlModule.ControlModule;
import Parameters.OperatingState;
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
  private OperatingState C02OperatingState;
  private OperatingState tempOperatingState;

  // Sensor overall operating state
  private OperatingState sensorOperatingState;

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

    // Initialize the other SensorManager attributes
    temp = -1;
    C02 = -1;
    this.controlModule = controlModule;
    GUIBound = false;
    connStateLED = null;
    C02Label = null;
    tempLabel = null;
    C02OperatingState = OperatingState.NOMINAL;
    tempOperatingState = OperatingState.NOMINAL;
    sensorOperatingState = OperatingState.NOMINAL;
   }



  // Possibly update the sensor's operating state
  private void updateSensorOperatingState()
   { sensorOperatingState = OperatingState.values()[max(C02OperatingState.ordinal(),tempOperatingState.ordinal())]; }


  // Possibly updates the C02 and the sensor's operating states
  private void updateC02OperatingState()
   {
    if(C02 < C02ThresholdWARNING)
     C02OperatingState = OperatingState.NOMINAL;
    else
     if(C02 < C02ThresholdALERT)
      C02OperatingState = OperatingState.WARNING;
     else
      if(C02 < C02ThresholdEMERGENCY)
       C02OperatingState = OperatingState.ALERT;
      else
       C02OperatingState = OperatingState.EMERGENCY;

    updateSensorOperatingState();
   }

  // Possibly updates the temperature and the sensor's operating states
  private void updateTempOperatingState()
   {
    if(temp < tempThresholdWARNING)
     tempOperatingState = OperatingState.NOMINAL;
    else
     if(temp < tempThresholdALERT)
      tempOperatingState = OperatingState.WARNING;
     else
      if(temp < tempThresholdEMERGENCY)
       tempOperatingState = OperatingState.ALERT;
      else
       tempOperatingState = OperatingState.EMERGENCY;

    updateSensorOperatingState();
   }

  public OperatingState getSensorOperatingState()
   { return sensorOperatingState; }



  @Override
  public void setConnStateOffline()
   {
    // Log that the sensor appears to be offline
    Log.warn("sensor" + ID + " appears to be offline");

    // If bound to a GUI widget, update the
    // associated connection status LED icon
    if(GUIBound)
     { connStateLED.setIcon(ControlModule.connStateLEDOFFImg); }
   }

  @Override
  protected void setConnStateOnline()
   {
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
    OperatingState oldSensorOperatingState = sensorOperatingState;

    // Update the C02
    C02 = newC02;

    // Log the updated C02
    Log.info("Received sensor" + ID + " updated C02 value (" + newC02 + ")");

    // Possibly update the C02 and the sensor's operating state
    updateC02OperatingState();

    // If the sensor's operating state has changed, possibly update the system's operating state
    if(sensorOperatingState != oldSensorOperatingState)
     controlModule.updateOperatingState(sensorOperatingState);

    // If bound to a GUI element, update the sensor's widget C02 value
    if(GUIBound)
     {
      C02Label.setText(C02 + " ppm");
      C02Label.setForeground(C02OperatingState.getColor());
     }
   }

  @Override
  public void setTemp(int newTemp)
   {
    OperatingState oldSensorOperatingState = sensorOperatingState;

    // Update the temperature
    temp = newTemp;

    // Log the updated temperature
    Log.info("Received sensor" + ID + " updated temperature value (" + newTemp + ")");

    // Possibly update the temperature and the sensor's operating state
    updateTempOperatingState();

    // If the sensor's operating state has changed, possibly update the system's operating state
    if(sensorOperatingState != oldSensorOperatingState)
     controlModule.updateOperatingState(sensorOperatingState);

    // If bound to a GUI element, update the sensor's widget temperature value
    if(GUIBound)
     {
      tempLabel.setText(temp + " °C");
      tempLabel.setForeground(tempOperatingState.getColor());
     }
   }
 }
