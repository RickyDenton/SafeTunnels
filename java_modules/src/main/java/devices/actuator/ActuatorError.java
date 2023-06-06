package devices.actuator;

public enum ActuatorError
 {
  // No error
  COAP_REQ_OK                       { @Override public String toString() { return "NO_ERROR"; } },

  // ----------------- Light Resource CoAP Requests Application Error Codes -----------------

  // The "lightState" variable is missing from a light PUT request
  ERR_LIGHT_PUT_NO_LIGHTSTATE       { @Override public String toString() { return "\"lightState\" variable missing from a light PUT request"; } },

  // An invalid "lightState" value was received in a light PUT request
  ERR_LIGHT_PUT_LIGHTSTATE_INVALID  { @Override public String toString() { return "Invalid \"lightState\" value received in a light PUT request"; } },

  // ------------------ Fan Resource CoAP Requests Application Error Codes ------------------

  // The "fanRelSpeed" variable is missing from a fan PUT request
  ERR_FAN_PUT_NO_FANRELSPEED { @Override public String toString() { return "\"fanRelSpeed\" variable missing from a fan PUT request"; } },

  // An invalid "fanRelSpeed" value was received in a fan PUT request
  ERR_FAN_PUT_FANRELSPEED_INVALID   { @Override public String toString() { return "Invalid \"fanRelSpeed\" value received in a fan PUT request"; } }
 }