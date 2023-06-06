package devices.actuator;

import devices.Device;


public class BaseActuator implements Device
 {
  public final short deviceID;

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

  public BaseActuator(short deviceID)
   {this.deviceID = deviceID;}
 }
