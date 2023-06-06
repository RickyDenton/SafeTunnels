package devices.actuator;

import devices.DevErrCode;
import errors.ErrCodeInfo;

import java.util.EnumMap;
import java.util.Map;

import static errors.ErrCodeSeverity.WARNING;


public enum ActuatorErrCode implements DevErrCode
 {
  /* =========================== Enumeration Values Definition =========================== */

  // No error
  COAP_REQ_OK,

  /* --------------- Light Resource CoAP Requests Application Error Codes --------------- */

  // The "lightState" variable is missing from a light PUT request
  ERR_LIGHT_PUT_NO_LIGHTSTATE,

  // An invalid "lightState" value was received in a light PUT request
  ERR_LIGHT_PUT_LIGHTSTATE_INVALID,

  /* ---------------- Fan Resource CoAP Requests Application Error Codes ---------------- */

  // The "fanRelSpeed" variable is missing from a fan PUT request
  ERR_FAN_PUT_NO_FANRELSPEED,

  // An invalid "fanRelSpeed" value was received in a fan PUT request
  ERR_FAN_PUT_FANRELSPEED_INVALID;


  /* ========================== ActuatorErrCode ErrCodeInfo Map ========================== */

  private static final EnumMap<ActuatorErrCode,ErrCodeInfo> actuatorsErrorsInfoMap = new EnumMap<>(Map.ofEntries
    (
      // No error
      Map.entry(COAP_REQ_OK,                      new ErrCodeInfo(WARNING,"NO_ERROR")),

      /* ------------- Light Resource CoAP Requests Application Error Codes ------------- */
      Map.entry(ERR_LIGHT_PUT_NO_LIGHTSTATE,      new ErrCodeInfo(WARNING,"\"lightState\" variable missing from a light PUT request")),
      Map.entry(ERR_LIGHT_PUT_LIGHTSTATE_INVALID, new ErrCodeInfo(WARNING,"Invalid \"lightState\" value received in a light PUT request")),

      /* -------------- Fan Resource CoAP Requests Application Error Codes -------------- */
      Map.entry(ERR_FAN_PUT_NO_FANRELSPEED,       new ErrCodeInfo(WARNING,"\"fanRelSpeed\" variable missing from a fan PUT request")),
      Map.entry(ERR_FAN_PUT_FANRELSPEED_INVALID,  new ErrCodeInfo(WARNING,"Invalid \"fanRelSpeed\" value received in a fan PUT request"))
    ));


  /* ================================ Enumeration Methods  ================================ */

  public ErrCodeInfo getErrCodeInfo()
   { return actuatorsErrorsInfoMap.get(this); }

  public String getDevType()
   { return "actuator"; }
 }