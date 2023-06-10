package modules.MySQLConnector;

import devices.actuator.BaseActuator;
import devices.sensor.BaseSensor;
import logging.Log;

import java.sql.*;
import java.util.HashMap;
import java.util.Properties;

import static modules.MySQLConnector.MySQLConnectorErrCode.*;


public abstract class MySQLConnector
 {
  /* ======================= SAFETUNNELS MySQL DATABASE PARAMETERS ======================= */

  /* ------------------------------- Connection Parameters ------------------------------- */
  protected final static String ST_DB_HOST = "jdbc:mysql://localhost:3306/";
  protected final static String ST_DB_NAME = "SafeTunnelsCoojaDB";
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
  protected final static String ST_DB_ACTUATORS_TABLE_LIGHTSTATE = "actuatorsConnLightSeries";
  protected final static String ST_DB_ACTUATORS_TABLE_FANRELSPEED = "actuatorsConnFanSeries";

  protected final static String ST_DB_ACTUATORS_COLUMN_ID = "actuatorID";
  protected final static String ST_DB_ACTUATORS_COLUMN_LIGHTSTATE = "lightState";
  protected final static String ST_DB_ACTUATORS_COLUMN_FANRELSPEED = "fanRelSpeed";

  /* ==================================== ATTRIBUTES ==================================== */

  // SafeTunnels Database connection
  protected Connection STDBConn;

  // SafeTunnels Database connection properties
  Properties STDBConnProperties;

  /* ================================= PROTECTED METHODS ================================= */

  // TODO: See if needed
  /*
  protected String getConnStateSeriesTable(DevType devType)
   {
    if(devType == DevType.sensor)
     return ST_DB_TABLE_SENSORS_CONNSTATE_SERIES;
    else
     return ST_DB_TABLE_ACTUATORS_CONNSTATE_SERIES;
   }

  protected String getDevIDColumn(DevType devType)
   {
    if(devType == DevType.sensor)
     return "sensorID";
    else
     return "actuatorID";
   }
  */

  private void connectToDB()
   {
    try
     { STDBConn = DriverManager.getConnection(ST_DB_ENDPOINT,STDBConnProperties);}

    // Failing to connect with the database is a FATAL error
    catch(java.sql.SQLException sqlExcp)
     { Log.code(ERR_DB_CONN_FAILED,"(reason = " + sqlExcp.getMessage() + ")"); }
   }

  protected void checkDBConn()
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
    // Initialize the database connection properties
    STDBConnProperties = new Properties();
    STDBConnProperties.put("user",ST_DB_USER);
    STDBConnProperties.put("password",ST_DB_PWD);
    STDBConnProperties.put("autoReconnect","true");
    STDBConnProperties.put("maxReconnects","4");

    // Attempt to connect with the database
    connectToDB();
   }


  /*
  THIS SHOULD MAYBE BE CALLED ALWAYS?

  public static Connection getConnection() throws Exception
   {
    if(STDBConn==null || STDBConn.isClosed())
     {
      java.util.Properties connProperties = new java.util.Properties();
      connProperties.put("user",ST_DB_USER);
      connProperties.put("password",ST_DB_PWD);
      connProperties.put("autoReconnect","true");
      connProperties.put("maxReconnects","4");

      STDBConn = DriverManager.getConnection(ST_DB_ENDPOINT,connProperties);
     }
    return STDBConn;
   }

   */



  // Returns the <MAC,BaseSensor> map of sensors in the database
  public HashMap<String,BaseSensor> getDBSensorsMap()
   {
    // <MAC,BaseSensor> map of sensors in the database to be returned
    HashMap<String,BaseSensor> sensorsMap = new HashMap<>();

    // Ensure the database connection to be active
    checkDBConn();

    // Query to retrieve all sensors in the database
    String getAllSensorsQuery = "SELECT * FROM " + ST_DB_SENSORS_TABLE;

    // Attempt to initialize the statement
    try(Statement mySQLStmt = STDBConn.createStatement())
     {
      // Attempt to retrieve the set of sensors in the database
      try(ResultSet sensorsSet = mySQLStmt.executeQuery(getAllSensorsQuery))
       {
        // For every sensor tuple returned, initialize and append
        // its associated <MAC,BaseSensor> element in the sensorMap
        while(sensorsSet.next())
         { sensorsMap.put(sensorsSet.getString("mac"),new BaseSensor(sensorsSet.getShort("sensorID"))); }

        // Retrieving no sensors from the database is a FATAL error
        if(sensorsMap.isEmpty())
         Log.code(ERR_DB_NO_SENSORS);
       }
     }

    // Failing to retrieve the set of sensors in the database is a FATAL error
    catch(SQLException sqlExcp)
     { Log.code(ERR_DB_GET_SENSORS, "(reason = " + sqlExcp.getMessage() + ")"); }

    // Return the <MAC,BaseSensor> map of sensors in the database
    return sensorsMap;
   }
 }