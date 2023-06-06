package devices.actuator;

public enum LightState
 {
  // The light is OFF (NOMINAL)
  LIGHT_OFF             { @Override public String toString() { return "LIGHT_OFF"; } },

  // The light is ON (WARNING)
  LIGHT_ON              { @Override public String toString() { return "LIGHT_ON"; } },

  // The light is blinking slowly (ALERT)
  LIGHT_BLINK_ALERT     { @Override public String toString() { return "LIGHT_BLINK_ALERT"; } },

  // The light is blinking fast (EMERGENCY)
  LIGHT_BLINK_EMERGENCY { @Override public String toString() { return "LIGHT_BLINK_EMERGENCY"; } },

  // Invalid light state used for validating a received new light state
  LIGHT_STATE_INVALID   { @Override public String toString() { return "LIGHT_STATE_INVALID"; } }
 }