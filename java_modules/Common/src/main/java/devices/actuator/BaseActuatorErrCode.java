/* SafeTunnels Base Actuator Errors Definitions */

package devices.actuator;

/* ================================== IMPORTS ================================== */

/* --------------------- Java Standard Libraries Resources --------------------- */
import java.util.EnumMap;
import java.util.Map;

/* --------------------------- SafeTunnels Resources --------------------------- */
import devices.BaseDevice.DevType;
import devices.DevErrCode;
import errors.ErrCodeInfo;
import static devices.BaseDevice.DevType.actuator;
import static errors.ErrCodeSeverity.WARNING;


/* ============================== ENUM DEFINITION ============================== */
public enum BaseActuatorErrCode implements DevErrCode
 {
  /* ====================== Enumeration Values Definition ====================== */

  // No error
  COAP_REQ_OK,

  /* ---------- Light Resource CoAP Requests Application Error Codes ---------- */

  // The "lightState" variable is missing from a light PUT request
  ERR_LIGHT_PUT_NO_LIGHTSTATE,

  // An invalid "lightState" value was received in a light PUT request
  ERR_LIGHT_PUT_LIGHTSTATE_INVALID,

  /* ----------- Fan Resource CoAP Requests Application Error Codes ----------- */

  // The "fanRelSpeed" variable is missing from a fan PUT request
  ERR_FAN_PUT_NO_FANRELSPEED,

  // An invalid "fanRelSpeed" value was received in a fan PUT request
  ERR_FAN_PUT_FANRELSPEED_INVALID;


  /* ================== BaseSensorErrCode Values Lookup Array ================== */

  public static final BaseActuatorErrCode[] values = BaseActuatorErrCode.values();


  /* ====================== BaseActuator ErrCodeInfo Map ====================== */

  private static final EnumMap<BaseActuatorErrCode,ErrCodeInfo> baseActuatorErrorCodeInfoMap = new EnumMap<>(Map.ofEntries
    (
      // No error
      Map.entry(COAP_REQ_OK,                      new ErrCodeInfo(WARNING,"NO_ERROR")),

      /* -------- Light Resource CoAP Requests Application Error Codes -------- */
      Map.entry(ERR_LIGHT_PUT_NO_LIGHTSTATE,      new ErrCodeInfo(WARNING,"\"lightState\" variable missing from a light PUT request")),
      Map.entry(ERR_LIGHT_PUT_LIGHTSTATE_INVALID, new ErrCodeInfo(WARNING,"Invalid \"lightState\" value received in a light PUT request")),

      /* --------- Fan Resource CoAP Requests Application Error Codes --------- */
      Map.entry(ERR_FAN_PUT_NO_FANRELSPEED,       new ErrCodeInfo(WARNING,"\"fanRelSpeed\" variable missing from a fan PUT request")),
      Map.entry(ERR_FAN_PUT_FANRELSPEED_INVALID,  new ErrCodeInfo(WARNING,"Invalid \"fanRelSpeed\" value received in a fan PUT request"))
    ));


  /* ========================== Enumeration Methods  ========================== */

  /**
   * @return The errCodeInfo object associated with an enum
   */
  public ErrCodeInfo getErrCodeInfo()
   { return baseActuatorErrorCodeInfoMap.get(this); }

  /**
   * @return The DevErrCode's devType
   */
  public DevType getDevType()
   { return actuator; }
 }