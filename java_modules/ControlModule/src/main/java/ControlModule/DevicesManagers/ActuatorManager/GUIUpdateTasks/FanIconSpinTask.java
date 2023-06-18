/* Actuator Widget Fan Icon Spin Animation TimerTask */

package ControlModule.DevicesManagers.ActuatorManager.GUIUpdateTasks;

/* ================================== IMPORTS ================================== */

/* --------------------- Java Standard Libraries Resources --------------------- */
import javax.swing.*;
import java.util.TimerTask;

/* --------------------------- SafeTunnels Resources --------------------------- */
import ControlModule.ControlModule;


/* ============================== CLASS DEFINITION ============================== */
public class FanIconSpinTask extends TimerTask
 {
  /* ============================ PRIVATE ATTRIBUTES ============================ */

  // The current fan icon index in the Control Module's "actuatorFanIcons" array
  private int currFanIconIndex;

  // A reference to the actuator widget's fan icon
  private final JLabel fanIcon;


  /* ============================= PUBLIC METHODS ============================= */

  /**
   * Fan Icon Spin Animation TimerTask constructor, initializing its attributes
   * @param fanIcon A reference to the actuator widget's  fan icon
   */
  public FanIconSpinTask(JLabel fanIcon)
   {
    currFanIconIndex = 0;
    this.fanIcon = fanIcon;
   }


  /**
   * Fan Icon Spin Animation TimerTask run() method, executed at the
   * fixed rate directly proportional to the current fan's relative speed
   */
  @Override
  public void run()
   {
    // Change the actuator widget's fan icon with the next
    // one in the Control Module's "actuatorFanIcons" array
    currFanIconIndex = (currFanIconIndex+1) % ControlModule.actuatorFanIcons.length;
    fanIcon.setIcon(ControlModule.actuatorFanIcons[currFanIconIndex]);
   }
 }