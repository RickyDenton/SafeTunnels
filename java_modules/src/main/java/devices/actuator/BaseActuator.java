/* SafeTunnels Base Actuator Class */

package devices.actuator;

/* ================================== IMPORTS ================================== */

/* --------------------------- SafeTunnels Packages --------------------------- */
import devices.Device;
import static devices.Device.DevType.actuator;


/* ============================== CLASS DEFINITION ============================== */
public class BaseActuator implements Device
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


  /* ============================ PUBLIC ATTRIBUTES ============================ */

  // The actuator's unique ID in the SafeTunnels database
  public final short ID;

  // The actuator's current connection state (false -> offline, true -> online)
  public boolean connState;


  /* ============================= PUBLIC METHODS ============================= */

  /**
   * BaseActuator constructor, initializing its attributes
   * @param ID The actuator's unique ID in the SafeTunnels database
   */
  public BaseActuator(short ID)
   {
    this.ID = ID;
    this.connState = false;
   }

  /**
   * @return The actuator's unique ID in the SafeTunnels database
   */
  public short getID()
   { return ID; }

  /**
   * @return The actuator's device type
   */
  public DevType getDevType()
   { return actuator; }
 }
