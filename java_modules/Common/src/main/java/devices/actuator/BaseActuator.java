/* SafeTunnels Base Actuator Class */

package devices.actuator;

/* ================================== IMPORTS ================================== */

/* --------------------------- SafeTunnels Resources --------------------------- */
import devices.BaseDevice;
import static devices.BaseDevice.DevType.actuator;


/* ============================== CLASS DEFINITION ============================== */
public abstract class BaseActuator extends BaseDevice
 {
  /* ============================ ENUMS DEFINITIONS ============================ */

  /* --------------------------- Actuators Quantities --------------------------- */
  public enum ActuatorQuantity
   {
    // Fan relative speed
    FANRELSPEED  { @Override public String toString() { return "fan relative speed"; } },

    // Temperature (°C)
    LIGHTSTATE { @Override public String toString() { return "light state"; } },
   }

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
    LIGHT_STATE_INVALID;

    // LightState Values Lookup Array
    public static final LightState[] values = LightState.values();
   }


  /* ================================ ATTRIBUTES ================================ */

  // Actuators Resources Relative Paths
  protected static final String actuatorFanRelSpeedResRelPath = "fan";
  protected static final String actuatorLightStateResRelPath = "light";
  protected static final String actuatorErrorsResRelPath = "actuatorErrors";


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


  /**
   * Sets the actuator's fan relative speed
   * @param newFanRelSpeed The fan relative speed to be set [0-100]
   */
  public abstract void setFanRelSpeed(int newFanRelSpeed);


  /**
   * Sets the actuator's light state
   * @param newLightState The light state to be set
   */
  public abstract void setLightState(LightState newLightState);
 }