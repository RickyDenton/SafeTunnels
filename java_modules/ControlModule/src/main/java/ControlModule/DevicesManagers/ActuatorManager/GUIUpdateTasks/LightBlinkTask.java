package ControlModule.DevicesManagers.ActuatorManager.GUIUpdateTasks;

import ControlModule.ControlModule;

import javax.swing.*;
import java.util.TimerTask;


public class LightBlinkTask extends TimerTask
 {
  private final JLabel lightIcon;
  private final ImageIcon lightStateImgON;
  boolean lightOn;

  public LightBlinkTask(JLabel lightIcon,ImageIcon lightStateImgON)
   {
    this.lightIcon = lightIcon;
    this.lightStateImgON = lightStateImgON;
    lightOn = false;
   }

  @Override
  public void run()
   {
    if(lightOn)
     {
      lightIcon.setIcon(ControlModule.actuatorLightOFFImg);
      lightOn = false;
     }
    else
     {
      lightIcon.setIcon(lightStateImgON);
      lightOn = true;
     }
   }
 }
