/* SafeTunnels Cloud MySQL Connector Errors Definitions */

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
public enum CloudMySQLConnectorErrCode implements ModuleErrCode
 {
  /* ====================== Enumeration Values Definition ====================== */

  // Failed to push an updated sensor connection state into the database
  ERR_CLOUD_PUSH_CONNSTATE_FAILED,

  // Failed to push an updated sensor quantity into the database
  ERR_CLOUD_PUSH_QUANTITY_FAILED;


  /* ================= CloudSensorsMQTTHandler ErrCodeInfo Map ================= */

  private static final EnumMap<CloudMySQLConnectorErrCode,ErrCodeInfo> CloudMySQLConnectorErrCodeInfoMap = new EnumMap<>(Map.ofEntries
   (
    Map.entry(ERR_CLOUD_PUSH_CONNSTATE_FAILED,new ErrCodeInfo(ERROR,"Failed to push an updated sensor connection state into the database")),
    Map.entry(ERR_CLOUD_PUSH_QUANTITY_FAILED,new ErrCodeInfo(ERROR,"Failed to push an updated sensor quantity into the database"))
   ));


  /* ========================== Enumeration Methods  ========================== */

  /**
   * @return The errCodeInfo object associated with an enum
   */
  public ErrCodeInfo getErrCodeInfo()
   { return CloudMySQLConnectorErrCodeInfoMap.get(this); }

  /**
   * @return The ModuleErrCode's name
   */
  public String getModuleName()
   { return "CloudMySQLConnector"; }
 }