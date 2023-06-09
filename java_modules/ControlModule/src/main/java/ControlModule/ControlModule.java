package ControlModule;

import javax.swing.*;
import java.io.PrintStream;

import GUILogging.ANSIColorPane;
import GUILogging.ANSIColorPaneOutputStream;
import errors.DevErrCodeExcp;
import errors.ErrCodeExcp;
import logging.Log;

import static devices.actuator.BaseActuatorErrCode.ERR_LIGHT_PUT_NO_LIGHTSTATE;
import static devices.sensor.BaseSensorErrCode.ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC;


public class ControlModule extends JFrame
 {
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
  private JButton ONButton;
  private JButton ALERTButton;
  private JButton EMERButton;
  private JLabel TryTry;
  private JLabel TryLamp;
  private ANSIColorPane ANSIColorPane1;

  public ControlModule()
   {
    setTitle("SafeTunnels Control Module");
    setSize(415,600);
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setVisible(true);

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

   }

  public static void main(String[] args)
   { new ControlModule(); }
 }
