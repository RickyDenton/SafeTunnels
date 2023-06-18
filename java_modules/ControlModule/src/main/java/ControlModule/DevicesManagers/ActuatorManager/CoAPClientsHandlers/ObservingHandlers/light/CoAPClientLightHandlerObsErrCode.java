/* "Light" CoAP Client Observe Handler Errors Definitions */

package ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.ObservingHandlers.light;

/* ================================== IMPORTS ================================== */

/* --------------------- Java Standard Libraries Resources --------------------- */
import java.util.EnumMap;
import java.util.Map;

/* --------------------------- SafeTunnels Resources --------------------------- */
import errors.ErrCodeInfo;
import errors.ModuleErrCode;
import static errors.ErrCodeSeverity.ERROR;


/* ============================== ENUM DEFINITION ============================== */
public enum CoAPClientLightHandlerObsErrCode implements ModuleErrCode
 {
  /* ====================== Enumeration Values Definition ====================== */

  /* ------------------- Received CoAP Response Parsing Errors ----------------- */

  // A received CoAP client light state response
  // could not be interpreted in JSON format
  ERR_COAP_CLI_LIGHT_RESP_NOT_JSON,

  // A received CoAP client light state response
  // does not contain the "lightState" attribute
  ERR_COAP_CLI_LIGHT_LIGHTSTATE_MISSING,

  // The "lightState" attribute in a received CoAP client light 
  // state response could not be interpreted as an integer
  ERR_COAP_CLI_LIGHT_LIGHTSTATE_NOT_INT,

  // The "lightState" attribute in a received CoAP client light
  // response could not be mapped to a valid actuator light state
  ERR_COAP_CLI_LIGHT_LIGHTSTATE_UNKNOWN;


  /* ================ CoAPClientLightObsHandler ErrCodeInfo Map ================ */

  private static final EnumMap<CoAPClientLightHandlerObsErrCode,ErrCodeInfo> CoAPClientLightHandlerErrCodeInfoMap = new EnumMap<>(Map.ofEntries
   (
    /* ------------------- Received CoAP Response Parsing Errors ----------------- */
    Map.entry(ERR_COAP_CLI_LIGHT_RESP_NOT_JSON,new ErrCodeInfo(ERROR,"A received CoAP client light state response could not be interpreted in JSON format")),
    Map.entry(ERR_COAP_CLI_LIGHT_LIGHTSTATE_MISSING,new ErrCodeInfo(ERROR,"A received CoAP client light state response does not contain the \"lightState\" attribute")),
    Map.entry(ERR_COAP_CLI_LIGHT_LIGHTSTATE_NOT_INT,new ErrCodeInfo(ERROR,"The \"lightState\" attribute in a received CoAP client light state response could not be interpreted as an integer")),
    Map.entry(ERR_COAP_CLI_LIGHT_LIGHTSTATE_UNKNOWN,new ErrCodeInfo(ERROR,"The \"lightState\" attribute in a received CoAP client light response could not be mapped to a valid actuator light state"))
   ));


  /* ========================== Enumeration Methods  ========================== */

  /**
   * @return The errCodeInfo object associated with an enum
   */
  public ErrCodeInfo getErrCodeInfo()
   { return CoAPClientLightHandlerErrCodeInfoMap.get(this); }

  /**
   * @return The ModuleErrCode's name
   */
  public String getModuleName()
   { return "CoAPClientLightObsHandler"; }
 }