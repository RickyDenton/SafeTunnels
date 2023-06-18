/* Actuator Widget Light Icon Animation TimerTask */

package ControlModule.DevicesManagers.ActuatorManager.GUIUpdateTasks;

/* ================================== IMPORTS ================================== */

/* --------------------- Java Standard Libraries Resources --------------------- */
import javax.swing.*;
import java.util.TimerTask;

/* --------------------------- SafeTunnels Resources --------------------------- */
import ControlModule.ControlModule;

/* ============================== CLASS DEFINITION ============================== */
public class LightBlinkTask extends TimerTask
 {
  /* ============================ PRIVATE ATTRIBUTES ============================ */

  // A reference to the actuator widget's light icon
  private final JLabel lightIcon;

  // A reference to the light "ON" image to be blinked depending on
  // the actuator's current operating state ('ALERT' | 'EMERGENCY')
  private final ImageIcon lightStateImgON;

  // Whether the actuator widget's light icon is on or off
  private boolean lightOn;


  /* ============================= PUBLIC METHODS ============================= */

  /**
   * Light Icon Blink Animation TimerTask constructor, initializing its attributes
   * @param lightIcon       A reference to the actuator widget's light icon
   * @param lightStateImgON A reference to the light "ON" image to be
   *                        blinked depending on the actuator's current
   *                        operating state ('ALERT' | 'EMERGENCY')
   */
  public LightBlinkTask(JLabel lightIcon,ImageIcon lightStateImgON)
   {
    this.lightIcon = lightIcon;
    this.lightStateImgON = lightStateImgON;
    lightOn = false;
   }


  /**
   * Light Icon Blink Animation TimerTask run() method, executed at a fixed
   * rate depending on the light current blinking state ('ALERT' | 'EMERGENCY')
   */
  @Override
  public void run()
   {
    // Toggle the light icon and its state using the
    // "LIGHT OFF" and the provided "LIGHT ON" images
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