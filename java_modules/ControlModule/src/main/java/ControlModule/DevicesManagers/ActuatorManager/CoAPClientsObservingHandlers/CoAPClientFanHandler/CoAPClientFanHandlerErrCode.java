/* SafeTunnels Control Module Actuator CoAP Client Fan Handler Errors Definitions */

package ControlModule.DevicesManagers.ActuatorManager.CoAPClientsObservingHandlers.CoAPClientFanHandler;

/* ================================== IMPORTS ================================== */

/* --------------------- Java Standard Libraries Resources --------------------- */
import java.util.EnumMap;
import java.util.Map;

/* --------------------------- SafeTunnels Resources --------------------------- */
import errors.ErrCodeInfo;
import errors.ModuleErrCode;
import static errors.ErrCodeSeverity.ERROR;


/* ============================== ENUM DEFINITION ============================== */
public enum CoAPClientFanHandlerErrCode implements ModuleErrCode
 {
  /* ====================== Enumeration Values Definition ====================== */

  /* -------------- CoAP Client Fan Responses General Error Codes -------------- */

  // An unsuccessful CoAP client fan relative speed response was received
  ERR_COAP_CLI_FAN_RESP_UNSUCCESSFUL,

  // A received CoAP client fan relative speed response
  // could not be interpreted in JSON format
  ERR_COAP_CLI_FAN_RESP_NOT_JSON,

  /* --------------- CoAP Client Fan Invalid Values Error Codes --------------- */

  // A received CoAP client fan relative speed response
  // does not contain the "fanRelSpeed" attribute
  ERR_COAP_CLI_FAN_FANRELSPEED_MISSING,

  // The "fanRelSpeed" attribute in a received CoAP client fan
  // relative speed response could not be interpreted as an integer
  ERR_COAP_CLI_FAN_FANRELSPEED_NOT_INT,

  // Received an invalid "fanRelSpeed" value in
  // a CoAP client fan relative speed response
  ERR_COAP_CLI_FAN_FANRELSPEED_INVALID;


  /* =================== CoAPClientFanHandler ErrCodeInfo Map =================== */

  private static final EnumMap<CoAPClientFanHandlerErrCode,ErrCodeInfo> CoAPClientFanHandlerErrCodeInfoMap = new EnumMap<>(Map.ofEntries
   (
    /* ------------- CoAP Client Fan Responses General Error Codes ------------- */
    Map.entry(ERR_COAP_CLI_FAN_RESP_UNSUCCESSFUL,new ErrCodeInfo(ERROR,"An unsuccessful CoAP client fan relative speed response was received")),
    Map.entry(ERR_COAP_CLI_FAN_RESP_NOT_JSON,new ErrCodeInfo(ERROR,"A received CoAP client fan relative speed response could not be interpreted in JSON format")),

    /* -------------- CoAP Client Fan Invalid Values Error Codes -------------- */
    Map.entry(ERR_COAP_CLI_FAN_FANRELSPEED_MISSING,new ErrCodeInfo(ERROR,"A received CoAP client fan relative speed response does not contain the \"fanRelSpeed\" attribute")),
    Map.entry(ERR_COAP_CLI_FAN_FANRELSPEED_NOT_INT,new ErrCodeInfo(ERROR,"The \"fanRelSpeed\" attribute in a received CoAP client fan relative speed response could not be interpreted as an integer")),
    Map.entry(ERR_COAP_CLI_FAN_FANRELSPEED_INVALID,new ErrCodeInfo(ERROR,"Received an invalid \"fanRelSpeed\" value in a CoAP client fan relative speed response"))
   ));


  /* ========================== Enumeration Methods  ========================== */

  /**
   * @return The errCodeInfo object associated with an enum
   */
  public ErrCodeInfo getErrCodeInfo()
   { return CoAPClientFanHandlerErrCodeInfoMap.get(this); }

  /**
   * @return The ModuleErrCode's name
   */
  public String getModuleName()
   { return "CoAPClientFanHandler"; }
 }