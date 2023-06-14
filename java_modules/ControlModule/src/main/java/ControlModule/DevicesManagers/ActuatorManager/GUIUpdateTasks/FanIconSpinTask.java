package ControlModule.DevicesManagers.ActuatorManager.GUIUpdateTasks;

import ControlModule.ControlModule;

import javax.swing.*;
import java.util.TimerTask;


public class FanIconSpinTask extends TimerTask
 {
  int currFanIconIndex;
  JLabel fanIcon;

  public FanIconSpinTask(JLabel fanIcon)
   {
    currFanIconIndex = 0;
    this.fanIcon = fanIcon;
   }

  @Override
  public void run()
   {
    currFanIconIndex = (currFanIconIndex+1) % ControlModule.actuatorFanIcons.length;
    fanIcon.setIcon(ControlModule.actuatorFanIcons[currFanIconIndex]);
   }
 }
