package ControlModule;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.PrintStream;

import DevicesManagers.SensorManager;
import GUILogging.ANSIColorPane;
import GUILogging.ANSIColorPaneOutputStream;
import Parameters.OperatingState;
import errors.DevErrCodeExcp;
import errors.ErrCodeExcp;
import logging.Log;

import static devices.actuator.BaseActuatorErrCode.ERR_LIGHT_PUT_NO_LIGHTSTATE;
import static devices.sensor.BaseSensorErrCode.ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC;
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
  private JLabel actuator1ConnStateImg;
  private JPanel sensorsListPanel;
  private JSlider slider1;
  private JButton OFFButton;
  private JButton WARNINGButton;
  private JButton ALERTButton;
  private JButton EMERGENCYButton;
  private JLabel TryTry;
  private JLabel TryLamp;
  private ANSIColorPane ANSIColorPane1;
  private JLabel sensor1ConnStateLED;
  private JLabel systemOperatingStateLabel;

  boolean autoMode;
  OperatingState systemOperatingState;

  SensorManager[] sensorsList;



  public ControlModule()
   {
    autoMode = true;
    systemOperatingState = OperatingState.NOMINAL;




    /* Convert map of subclass to superclass

    Map<String, SuperClass> superClassMap = new HashMap<>();
    subclass.forEach((k, v) -> superClassMap.put(k, SuperClass.copyOf(v)));
    */


    setTitle("SafeTunnels Control Module");
    setSize(1200,393);
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setVisible(true);

    // mainPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 385));
    setResizable(false);

    setContentPane(mainPanel);


    ANSIColorPaneOutputStream out = new ANSIColorPaneOutputStream(ANSIColorPane1);
    System.setOut (new PrintStream(out));

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
    TryTry.setText("fuffa");
    TryTry.setForeground(new Color(255,0,0));

    // Change a JPanel icon
    ImageIcon iconLogo = new ImageIcon("ControlModule/src/main/resources/LightBulb_ALERT_Icon_30.png");
    TryLamp.setIcon(iconLogo);
    */

    automaticModeCheckBox.addItemListener(new ItemListener()
     {
      @Override
      public void itemStateChanged(ItemEvent itemEvent)
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
       }
     });
   }


  // Called by a sensor upon receiving a new quantity
  // value to possibly update the system's operating state
  public void updateOperatingState(OperatingState newSensorOpState)
   {
    boolean systemOperatingStateChanged = false;

    // If the new sensor and the system operating state
    // are the same, no other operations are required
    if(newSensorOpState == systemOperatingState)
     return;

    // Otherwise, if the sensor's operating state has
    // a higher severity, directly update it
    else
     if(newSensorOpState.ordinal() > systemOperatingState.ordinal())
      {
       systemOperatingState = newSensorOpState;
       systemOperatingStateChanged = true;
      }

     // Otherwise, if the sensor's operating state has a lower
     // severity, check if the system can pass to a lower severity
     else
      {
       OperatingState maxSensorOperatingState = OperatingState.NOMINAL;

       for(SensorManager sens : sensorsList)
        { maxSensorOperatingState = OperatingState.values()[max(maxSensorOperatingState.ordinal(),sens.getSensorOperatingState().ordinal())]; }

       // If the state has changed (passed to a lower severity)
       if(maxSensorOperatingState != systemOperatingState)
        {
         systemOperatingState = maxSensorOperatingState;
         systemOperatingStateChanged = true;
        }
      }

    // If the system operating state has changed
    if(systemOperatingStateChanged)
     {
      // Update the "system state" label text and color
      systemOperatingStateLabel.setText(systemOperatingState.toString());
      systemOperatingStateLabel.setForeground(systemOperatingState.getColor());

      // In automatic mode, also command the actuators
      if(autoMode)
       {
        // TODO!
        Log.info("implement automatic actuators adjustments in auto mode...");
       }
     }
   }

  public static void main(String[] args)
   { new ControlModule(); }
 }
