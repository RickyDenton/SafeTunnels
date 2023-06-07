package CloudModule;

import errors.ErrCodeInfo;
import errors.ModuleErrCode;

import java.util.EnumMap;
import java.util.Map;

import static errors.ErrCodeSeverity.ERROR;


public enum CloudModuleErrCode implements ModuleErrCode
 {
  /* =========================== Enumeration Values Definition =========================== */

  /* ----------------- Sensors Connection Status Database Updates Errors ----------------- */

  // A sensor disconnection update could not be pushed into the database
  ERR_CLOUD_SENSOR_OFFLINE_UPDATE_FAILED,

  // A sensor connection update could not be pushed into the database
  ERR_CLOUD_SENSOR_ONLINE_UPDATE_FAILED,

  /* ------------------------- Sensors Quantities Updates Errors ------------------------- */

  // A sensor C02 update could not be pushed into the database
  ERR_CLOUD_SENSOR_C02_UPDATE_FAILED,

  // A sensor temperature update could not be pushed into the database
  ERR_CLOUD_SENSOR_TEMP_UPDATE_FAILED;


  /* ========================= CloudModuleErrCode ErrCodeInfo Map ========================= */

  private static final EnumMap<CloudModuleErrCode,ErrCodeInfo> CloudModuleErrorsInfoMap = new EnumMap<>(Map.ofEntries
   (
    /* --------------- Sensors Connection Status Database Updates Errors --------------- */
    Map.entry(ERR_CLOUD_SENSOR_OFFLINE_UPDATE_FAILED,new ErrCodeInfo(ERROR,"A sensor disconnection update could not be pushed into the database")),
    Map.entry(ERR_CLOUD_SENSOR_ONLINE_UPDATE_FAILED,new ErrCodeInfo(ERROR,"A sensor connection update could not be pushed into the database")),

    /* ----------------------- Sensors Quantities Updates Errors ----------------------- */
    Map.entry(ERR_CLOUD_SENSOR_C02_UPDATE_FAILED,new ErrCodeInfo(ERROR,"A sensor C02 update could not be pushed into the database")),
    Map.entry(ERR_CLOUD_SENSOR_TEMP_UPDATE_FAILED,new ErrCodeInfo(ERROR,"A sensor temperature update could not be pushed into the database"))
   ));


  /* ================================ Enumeration Methods  ================================ */

  public ErrCodeInfo getErrCodeInfo()
   { return CloudModuleErrorsInfoMap.get(this); }

  public String getModuleName()
   { return "CloudModule"; }
 }