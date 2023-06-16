package modules.MySQLConnector;

import devices.BaseDevice.DevType;
import logging.Log;

import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;

import static devices.BaseDevice.DevType.sensor;
import static modules.MySQLConnector.MySQLConnectorErrCode.*;


public abstract class MySQLConnector
 {
  /* ======================= SAFETUNNELS MySQL DATABASE PARAMETERS ======================= */

  /* ------------------------------- Connection Parameters ------------------------------- */
  protected final static String ST_DB_HOST = "jdbc:mysql://localhost:3306/";

  public static String ST_DB_NAME = "SafeTunnelsPhyDB";
  // public static String ST_DB_NAME = = "SafeTunnelsCoojaDB";

  protected final static String ST_DB_ENDPOINT = ST_DB_HOST + ST_DB_NAME;
  protected final static String ST_DB_USER = "root";
  protected final static String ST_DB_PWD = "iot2023";

  /* ------------------------- Database Tables And Columns Names ------------------------- */

  // --------------------------------------- Common ---------------------------------------
  public final static String ST_DB_COMMON_COLUMN_MAC = "mac";
  public final static String ST_DB_COMMON_COLUMN_CONNSTATE = "connState";

  // -------------------------------------- Sensors --------------------------------------
  public final static String ST_DB_SENSORS_TABLE = "sensors";
  public final static String ST_DB_SENSORS_TABLE_CONNSTATE = "sensorsConnStateSeries";
  public final static String ST_DB_SENSORS_TABLE_C02 = "sensorsC02Series";
  public final static String ST_DB_SENSORS_TABLE_TEMP = "sensorsTempSeries";

  public final static String ST_DB_SENSORS_COLUMN_ID = "sensorID";
  public final static String ST_DB_SENSORS_COLUMN_C02 = "C02Density";
  public final static String ST_DB_SENSORS_COLUMN_TEMP = "temp";

  // ------------------------------------- Actuators -------------------------------------
  protected final static String ST_DB_ACTUATORS_TABLE = "actuators";
  protected final static String ST_DB_ACTUATORS_TABLE_CONNSTATE = "actuatorsConnStateSeries";
  protected final static String ST_DB_ACTUATORS_TABLE_LIGHTSTATE = "actuatorsLightSeries";
  protected final static String ST_DB_ACTUATORS_TABLE_FANRELSPEED = "actuatorsFanSeries";

  protected final static String ST_DB_ACTUATORS_COLUMN_ID = "actuatorID";
  protected final static String ST_DB_ACTUATORS_COLUMN_LIGHTSTATE = "lightState";
  protected final static String ST_DB_ACTUATORS_COLUMN_FANRELSPEED = "fanRelSpeed";

  /* ==================================== ATTRIBUTES ==================================== */

  // SafeTunnels Database connection
  protected Connection STDBConn;

  // SafeTunnels Database connection properties
  Properties STDBConnProperties;


  /* ================================== PRIVATE METHODS ================================== */

  /**
   * @param devType The device type to retrieve the
   *                database table name (sensor || actuator)
   * @return The SafeTunnels database table name
   *         associated with a device type
   */
  private String getDevTableName(DevType devType)
   {
    if(devType == sensor)
     return ST_DB_SENSORS_TABLE;
    else
     return ST_DB_ACTUATORS_TABLE;
   }

  /**
   * @param devType The device type to retrieve the database
   *                column ID name (sensor || actuator)
   * @return The SafeTunnels database column ID name
   *         associated with a device type
   */
  private String getDevColumnIDName(DevType devType)
   {
    if(devType == sensor)
     return ST_DB_SENSORS_COLUMN_ID;
    else
     return ST_DB_ACTUATORS_COLUMN_ID;
   }


  private void connectToDB()
   {
    try
     { STDBConn = DriverManager.getConnection(ST_DB_ENDPOINT,STDBConnProperties);}

    // Failing to connect with the database is a FATAL error
    catch(java.sql.SQLException sqlExcp)
     { Log.code(ERR_DB_CONN_FAILED,"(reason = " + sqlExcp.getMessage() + ")"); }
   }

  private void checkDBConn()
   {
    try
     {
      // If the database connection has been closed, attempt to reopen it
      if(STDBConn.isClosed())
       connectToDB();
     }
    catch(java.sql.SQLException sqlExcp)
     { Log.code(ERR_DB_CONN_FAILED,"(reason = " + sqlExcp.getMessage() + ")"); }
   }


  /* ================================= PROTECTED METHODS ================================= */

  protected void pushDevState(String seriesTable, String devIDColumn, String valueColumn, int devID, String value) throws java.sql.SQLException
   {
    // Ensure the database connection to be active
    checkDBConn();

    // Build the query to push the updated device state into the database
    String pushDevConnStatusQuery = "INSERT INTO " + seriesTable + "( " + devIDColumn + "," + valueColumn + ") VALUES(" + devID + "," + value +")";

    // Attempt to initialize the statement
    try(Statement mySQLStmt = STDBConn.createStatement())
     {
      // Attempt to push the updated device state into the database
      int numRowsChanged = mySQLStmt.executeUpdate(pushDevConnStatusQuery);

      // Ensure that a row was affected by the update
      if(numRowsChanged==0)
       throw new java.sql.SQLException("Pushing an updated device state to the database affected no rows");
     }
   }


  /* ================================== PUBLIC METHODS ================================== */

  // Constructor
  public MySQLConnector()
   {
    // Initialize the database mutex

    // Initialize the database connection properties
    STDBConnProperties = new Properties();
    STDBConnProperties.put("user",ST_DB_USER);
    STDBConnProperties.put("password",ST_DB_PWD);
    STDBConnProperties.put("autoReconnect","true");
    STDBConnProperties.put("maxReconnects","4");

    // Attempt to connect with the database
    connectToDB();
   }


  // Returns the DevMACIDPair (<MAC,sensorID> pairs) list of
  // the devices of a given devType stored in the database
  public ArrayList<DevMACIDPair> getDBDevicesList(DevType devType)
   {
    // DevMACIDPair list of devType devices
    // stored in the database to be returned
    ArrayList<DevMACIDPair> devicesList = new ArrayList<>();

    // Retrieve the names of the database table and column ID
    // associated with the devType of devices to be retrieved
    String devTableName = getDevTableName(devType);
    String devColumnIDName = getDevColumnIDName(devType);

    // Ensure the database connection to be active
    checkDBConn();

    // Build the query to retrieve the list
    // of devType devices from the database
    String getDevicesQuery = "SELECT * FROM " + devTableName;

    // Attempt to initialize the statement for retrieving
    // the list of devType devices from the database
    try(Statement mySQLStmt = STDBConn.createStatement())
     {
      // Attempt to execute the statement
      try(ResultSet devSet = mySQLStmt.executeQuery(getDevicesQuery))
       {
        // For every device tuple returned, initialize and append
        // its associated DevMACIDPair element in the devicesList
        while(devSet.next())
         devicesList.add(new DevMACIDPair(devSet.getString("mac"),devSet.getShort(devColumnIDName)));

        // Retrieving no devices from the database is a FATAL error
        if(devicesList.isEmpty())
         Log.code(ERR_DB_NO_DEVICES,"(" + devType + "s)");
       }
     }

    // Failing to retrieve the list of devType devices in the database is a FATAL error
    catch(SQLException sqlExcp)
     { Log.code(ERR_DB_GET_DEVICES, "(" + devType + "s, reason = " + sqlExcp.getMessage() + ")"); }

    // Return the DevMACIDPair list of devType devices retrieved from the database
    return devicesList;
   }
 }