/* SafeTunnels Control Module Actuator CoAP Client Errors Handler Errors Definitions */

package ControlModule.DevicesManagers.ActuatorManager.CoAPClientsHandlers.ObservingHandlers.actuatorErrors;

/* ================================== IMPORTS ================================== */

/* --------------------- Java Standard Libraries Resources --------------------- */
import java.util.EnumMap;
import java.util.Map;

/* --------------------------- SafeTunnels Resources --------------------------- */
import errors.ErrCodeInfo;
import errors.ModuleErrCode;
import static errors.ErrCodeSeverity.ERROR;


/* ============================== ENUM DEFINITION ============================== */
public enum CoAPClientErrorsHandlerObsErrCode implements ModuleErrCode
 {
  /* ====================== Enumeration Values Definition ====================== */

  // A received CoAP client error response
  // could not be interpreted in JSON format
  ERR_COAP_CLI_ERRORS_RESP_NOT_JSON,

  // A received CoAP client error response
  // does not contain the "errCode" attribute
  ERR_COAP_CLI_ERRORS_ERRCODE_MISSING,

  // The "errCode" attribute in a received CoAP client
  // error response could not be interpreted as an integer
  ERR_COAP_CLI_ERRORS_ERRCODE_NOT_INT,

  // The "errCode" attribute in a received CoAP client error
  // response could not be mapped to a valid actuator error code
  ERR_COAP_CLI_ERRORS_ERRCODE_UNKNOWN,

  // A received CoAP client error response
  // does not contain the "clientIP" attribute
  ERR_COAP_CLI_ERRORS_CLIIP_MISSING,

  // The "clientIP" attribute in a received CoAP client error
  // response could not be interpreted as a non-null String
  ERR_COAP_CLI_ERRORS_CLIIP_NOT_NONNULL_STRING,

  // The "errDscr" attribute in a received CoAP client error
  // response could not be interpreted as a non-null String
  ERR_COAP_CLI_ERRORS_ERRDSCR_NOT_NONNULL_STRING;


  /* ================= CoAPClientErrorsHandler ErrCodeInfo Map ================= */

  private static final EnumMap<CoAPClientErrorsHandlerObsErrCode,ErrCodeInfo> CoAPClientErrorsHandlerErrCodeInfoMap = new EnumMap<>(Map.ofEntries
   (
    Map.entry(ERR_COAP_CLI_ERRORS_RESP_NOT_JSON,new ErrCodeInfo(ERROR,"A received CoAP client error response could not be interpreted in JSON format")),
    Map.entry(ERR_COAP_CLI_ERRORS_ERRCODE_MISSING,new ErrCodeInfo(ERROR,"A received CoAP client error response does not contain the \"errCode\" attribute")),
    Map.entry(ERR_COAP_CLI_ERRORS_ERRCODE_NOT_INT,new ErrCodeInfo(ERROR,"The \"errCode\" attribute in a received CoAP client error response could not be interpreted as an integer")),
    Map.entry(ERR_COAP_CLI_ERRORS_ERRCODE_UNKNOWN,new ErrCodeInfo(ERROR,"The \"errCode\" attribute in a received CoAP client error response could not be mapped to a valid actuator error code")),
    Map.entry(ERR_COAP_CLI_ERRORS_CLIIP_MISSING,new ErrCodeInfo(ERROR,"A received CoAP client error response does not contain the \"clientIP\" attribute")),
    Map.entry(ERR_COAP_CLI_ERRORS_CLIIP_NOT_NONNULL_STRING,new ErrCodeInfo(ERROR,"The \"clientIP\" attribute in a received CoAP client error response could not be interpreted as a non-null String")),
    Map.entry(ERR_COAP_CLI_ERRORS_ERRDSCR_NOT_NONNULL_STRING,new ErrCodeInfo(ERROR,"The \"errDscr\" attribute in a received CoAP client error response could not be interpreted as a non-null String"))
   ));


  /* ========================== Enumeration Methods  ========================== */

  /**
   * @return The errCodeInfo object associated with an enum
   */
  public ErrCodeInfo getErrCodeInfo()
   { return CoAPClientErrorsHandlerErrCodeInfoMap.get(this); }

  /**
   * @return The ModuleErrCode's name
   */
  public String getModuleName()
   { return "CoAPClientErrorsHandler"; }
 }