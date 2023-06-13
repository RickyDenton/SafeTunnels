/* SafeTunnels Control Module Actuator CoAP Client Light Handler Errors Definitions */

package ControlModule.DevicesManagers.ActuatorManager.CoAPClientsObservingHandlers.CoAPClientLightHandler;

/* ================================== IMPORTS ================================== */

/* --------------------- Java Standard Libraries Resources --------------------- */
import java.util.EnumMap;
import java.util.Map;

/* --------------------------- SafeTunnels Resources --------------------------- */
import errors.ErrCodeInfo;
import errors.ModuleErrCode;
import static errors.ErrCodeSeverity.ERROR;


/* ============================== ENUM DEFINITION ============================== */
public enum CoAPClientLightHandlerErrCode implements ModuleErrCode
 {
  /* ====================== Enumeration Values Definition ====================== */

  /* ------------- CoAP Client Light Responses General Error Codes ------------- */
  
  // An unsuccessful CoAP client light state response was received
  ERR_COAP_CLI_LIGHT_RESP_UNSUCCESSFUL,

  // A received CoAP client light state response
  // could not be interpreted in JSON format
  ERR_COAP_CLI_LIGHT_RESP_NOT_JSON,

  /* -------------- CoAP Client Light Invalid Values Error Codes -------------- */

  // A received CoAP client light state response
  // does not contain the "lightState" attribute
  ERR_COAP_CLI_LIGHT_LIGHTSTATE_MISSING,

  // The "lightState" attribute in a received CoAP client light 
  // state response could not be interpreted as an integer
  ERR_COAP_CLI_LIGHT_LIGHTSTATE_NOT_INT,

  // The "lightState" attribute in a received CoAP client light
  // response could not be mapped to a valid actuator light state
  ERR_COAP_CLI_LIGHT_LIGHTSTATE_UNKNOWN;


  /* ================== CoAPClientLightHandler ErrCodeInfo Map ================== */

  private static final EnumMap<CoAPClientLightHandlerErrCode,ErrCodeInfo> CoAPClientLightHandlerErrCodeInfoMap = new EnumMap<>(Map.ofEntries
   (
    /* ------------ CoAP Client Light Responses General Error Codes ------------ */
    Map.entry(ERR_COAP_CLI_LIGHT_RESP_UNSUCCESSFUL,new ErrCodeInfo(ERROR,"An unsuccessful CoAP client light state response was received")),
    Map.entry(ERR_COAP_CLI_LIGHT_RESP_NOT_JSON,new ErrCodeInfo(ERROR,"A received CoAP client light state response could not be interpreted in JSON format")),

    /* ------------- CoAP Client Light Invalid Values Error Codes ------------- */
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
   { return "CoAPClientLightHandler"; }
 }