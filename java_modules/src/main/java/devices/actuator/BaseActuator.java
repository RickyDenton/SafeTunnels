/* SafeTunnels Base Actuator Class */

package devices.actuator;

/* ================================== IMPORTS ================================== */

/* --------------------------- SafeTunnels Packages --------------------------- */
import devices.BaseDevice;

import static devices.BaseDevice.DevType.actuator;


/* ============================== CLASS DEFINITION ============================== */
public abstract class BaseActuator extends BaseDevice
 {
  /* -------------------------- Actuator Light States -------------------------- */
  public enum LightState
   {
    // The light is OFF (NOMINAL)
    LIGHT_OFF,

    // The light is ON (WARNING)
    LIGHT_ON,

    // The light is blinking slowly (ALERT)
    LIGHT_BLINK_ALERT,

    // The light is blinking fast (EMERGENCY)
    LIGHT_BLINK_EMERGENCY,

    // Invalid light state used for validating a received new light state
    LIGHT_STATE_INVALID
   }

  /* ============================= PUBLIC METHODS ============================= */

  /**
   * BaseActuator constructor, initializing its attributes
   * @param MAC The actuator's (unique) MAC
   * @param ID The actuator's unique ID in the SafeTunnels database
   */
  public BaseActuator(String MAC, short ID)
   { super(MAC,ID); }


  /**
   * @return The actuator's device type
   */
  public DevType getDevType()
   { return actuator; }
 }
