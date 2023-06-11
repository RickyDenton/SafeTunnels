package modules.MySQLConnector;

import errors.ErrCodeInfo;
import errors.ModuleErrCode;

import java.util.EnumMap;
import java.util.Map;

import static errors.ErrCodeSeverity.FATAL;


public enum MySQLConnectorErrCode implements ModuleErrCode
 {
  /* =========================== Enumeration Values Definition =========================== */

  /* ------------------------------ General Database Errors ------------------------------ */

  // Failed to connect with the SafeTunnels MySQL Database
  ERR_DB_CONN_FAILED,

  /* ------------------------- Database Sensors Retrieval Errors ------------------------- */

  // The set of sensors in the database could not be retrieved
  ERR_DB_GET_DEVICES,

  // No sensors were retrieved from the database
  ERR_DB_NO_DEVICES,

  /* ------------------------ Database Actuators Retrieval Errors ------------------------ */

  // The set of actuators in the database could not be retrieved
  ERR_DB_GET_ACTUATORS,

  // No actuators were retrieved from the database
  ERR_DB_NO_ACTUATORS;


  /* ======================= MySQLConnectorErrCode ErrCodeInfo Map ======================= */

  private static final EnumMap<MySQLConnectorErrCode,ErrCodeInfo> mySQLConnectorErrorsInfoMap = new EnumMap<>(Map.ofEntries
   (
    /* ---------------------------- General Database Errors ---------------------------- */
    Map.entry(ERR_DB_CONN_FAILED,new ErrCodeInfo(FATAL,"Failed to connect with the SafeTunnels MySQL Database")),

    /* ----------------------- Database Sensors Retrieval Errors ----------------------- */
    Map.entry(ERR_DB_GET_DEVICES,new ErrCodeInfo(FATAL,"The set of sensors in the database could not be retrieved")),
    Map.entry(ERR_DB_NO_DEVICES,new ErrCodeInfo(FATAL,"No sensors were retrieved from the database")),

    /* ---------------------- Database Actuators Retrieval Errors ---------------------- */
    Map.entry(ERR_DB_GET_ACTUATORS,new ErrCodeInfo(FATAL,"The set of actuators in the database could not be retrieved")),
    Map.entry(ERR_DB_NO_ACTUATORS,new ErrCodeInfo(FATAL,"No actuators were retrieved from the database"))
   ));

  /* ================================ Enumeration Methods  ================================ */

  public ErrCodeInfo getErrCodeInfo()
   { return mySQLConnectorErrorsInfoMap.get(this); }

  public String getModuleName()
   { return "MySQLConnector"; }
 }