/* SafeTunnels Control MySQL Connector Errors Definitions */

package ControlModule.ControlMySQLConnector;

/* ================================== IMPORTS ================================== */

/* --------------------- Java Standard Libraries Resources --------------------- */
import java.util.EnumMap;
import java.util.Map;

/* --------------------------- SafeTunnels Resources --------------------------- */
import errors.ErrCodeInfo;
import errors.ModuleErrCode;
import static errors.ErrCodeSeverity.ERROR;


/* ============================== ENUM DEFINITION ============================== */
public enum ControlMySQLConnectorErrCode implements ModuleErrCode
 {
  /* ====================== Enumeration Values Definition ====================== */

  // Failed to push an updated actuator connection state into the database
  ERR_CONTROL_PUSH_CONNSTATE_FAILED,

  // Failed to push an updated actuator quantity into the database
  ERR_CONTROL_PUSH_QUANTITY_FAILED;


  /* ================== ControlMySQLConnector ErrCodeInfo Map ================== */

  private static final EnumMap<ControlMySQLConnectorErrCode,ErrCodeInfo> ControlMySQLConnectorErrCodeInfoMap = new EnumMap<>(Map.ofEntries
   (
    Map.entry(ERR_CONTROL_PUSH_CONNSTATE_FAILED,new ErrCodeInfo(ERROR,"Failed to push an updated actuator connection state into the database")),
    Map.entry(ERR_CONTROL_PUSH_QUANTITY_FAILED,new ErrCodeInfo(ERROR,"Failed to push an updated actuator quantity into the database"))
   ));


  /* ========================== Enumeration Methods  ========================== */

  /**
   * @return The errCodeInfo object associated with an enum
   */
  public ErrCodeInfo getErrCodeInfo()
   { return ControlMySQLConnectorErrCodeInfoMap.get(this); }

  /**
   * @return The ModuleErrCode's name
   */
  public String getModuleName()
   { return "ControlMySQLConnector"; }
 }