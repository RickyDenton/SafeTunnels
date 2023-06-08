/* SafeTunnels Cloud Module Sensors MQTT Handler Errors Definitions */

package CloudModule;

/* ================================== IMPORTS ================================== */

/* -------------------------- Java Standard Libraries -------------------------- */
import java.util.EnumMap;
import java.util.Map;

/* --------------------------- SafeTunnels Packages --------------------------- */
import errors.ErrCodeInfo;
import errors.ModuleErrCode;
import static errors.ErrCodeSeverity.ERROR;


/* ============================== ENUM DEFINITION ============================== */
public enum CloudSensorsMQTTHandlerErrCode implements ModuleErrCode
 {
  /* ====================== Enumeration Values Definition ====================== */

  /* ------------ Sensors Connection Status Database Updates Errors ------------ */

  // Failed to push a sensor connection update into the database
  ERR_CLOUD_SENSOR_ONLINE_UPDATE_FAILED,

  // Failed to push a sensor disconnection update into the database
  ERR_CLOUD_SENSOR_OFFLINE_UPDATE_FAILED,

  /* -------------------- Sensors Quantities Updates Errors -------------------- */

  // Failed to push a sensor C02 update into the database
  ERR_CLOUD_SENSOR_C02_UPDATE_FAILED,

  // Failed to push a sensor temperature update into the database
  ERR_CLOUD_SENSOR_TEMP_UPDATE_FAILED;


  /* ================= CloudSensorsMQTTHandler ErrCodeInfo Map ================= */

  private static final EnumMap<CloudSensorsMQTTHandlerErrCode,ErrCodeInfo> CloudModuleMQTTHandlerErrCodeInfoMap = new EnumMap<>(Map.ofEntries
   (
    /* ---------- Sensors Connection Status Database Updates Errors ---------- */
    Map.entry(ERR_CLOUD_SENSOR_ONLINE_UPDATE_FAILED,new ErrCodeInfo(ERROR,"Failed to push a sensor connection update into the database")),
    Map.entry(ERR_CLOUD_SENSOR_OFFLINE_UPDATE_FAILED,new ErrCodeInfo(ERROR,"Failed to push a sensor disconnection update into the database")),

    /* ------------------ Sensors Quantities Updates Errors ------------------ */
    Map.entry(ERR_CLOUD_SENSOR_C02_UPDATE_FAILED,new ErrCodeInfo(ERROR,"Failed to push a sensor C02 update into the database")),
    Map.entry(ERR_CLOUD_SENSOR_TEMP_UPDATE_FAILED,new ErrCodeInfo(ERROR,"Failed to push a sensor temperature update into the database"))
   ));


  /* ========================== Enumeration Methods  ========================== */

  /**
   * @return The errCodeInfo object associated with an enum
   */
  public ErrCodeInfo getErrCodeInfo()
   { return CloudModuleMQTTHandlerErrCodeInfoMap.get(this); }

  /**
   * @return The ModuleErrCode's name
   */
  public String getModuleName()
   { return "CloudMQTTHandlerErrCode"; }
 }