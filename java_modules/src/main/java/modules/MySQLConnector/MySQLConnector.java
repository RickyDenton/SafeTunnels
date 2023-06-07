package modules.MySQLConnector;

import devices.Device.*;
import devices.actuator.BaseActuator;
import devices.sensor.BaseSensor;
import logging.Log;

import java.sql.*;
import java.util.HashMap;

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

  // SafeTunnels database connection
  protected static Connection STDBConn;


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

  protected void pushDevState(String seriesTable, String devIDColumn, String valueColumn, int devID, String value) throws java.sql.SQLException
   {
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

  // TODO: Check if something useful, otherwise remove

  /*
  // Attempts to push an updated device connection state into the database, return whether the operation was successful
  private boolean pushDevConnStatus(DevType devType, int devID, boolean connStatus)
   {
    // Retrieve the DB connStateSeries table associated with "devType"
    String connStateSeriesTable = getConnStateSeriesTable(devType);

    // Retrieve the name of the ID column associated with "devType"
    String devIDColumn = getDevIDColumn(devType);

    // Convert the boolean connStatus to a (short) bit)
    short connStatusBit = connStatus?(short)1:(short)0;

    // Query to push the updated device connection status
    String pushDevConnStatusQuery = "INSERT INTO " + connStateSeriesTable + "( " + devIDColumn + ",connState) VALUES(" + devID + "," + connStatusBit +")";

    // Attempt to push the updated device connection status into the database
    try
     {
      int numRowsChanged = mySQLStmt.executeUpdate(pushDevConnStatusQuery);

      // Ensure that a row was affected by the update
      if(numRowsChanged == 0)
       {
        // Log the error
        Log.code(ERR_DB_PUSH_DEVCONNSTATE_NOROWS);

        // Return that the device connection state was NOT pushed into the database
        return false;
       }

      // Return that the device connection state
      // was successfully pushed into the database
      return true;
     }
    catch(SQLException sqlExcp)
     {
      // Log the error
      Log.code(ERR_DB_PUSH_DEVCONNSTATE,"(reason = " + sqlExcp + ")");

      // Return that the device connection state was NOT pushed into the database
      return false;
     }
   }

  // Attempts to push an updated device state (C02, temp for sensors, lightState, fanRelSpeed for actuators) into the database
  private boolean pushDevState(DevType devType, int devID, Object updatedState)
   {
    // Retrieve the DB connStateSeries table associated with "devType"
    String connStateSeriesTable = getConnStateSeriesTable(devType);

    // Retrieve the name of the ID column associated with "devType"
    String devIDColumn = getDevIDColumn(devType);

    // Convert the boolean connStatus to a (short) bit)
    short connStatusBit = connStatus?(short)1:(short)0;

    // Query to push the updated device connection status
    String pushDevConnStatusQuery = "INSERT INTO " + connStateSeriesTable + "( " + devIDColumn + ",connState) VALUES(" + devID + "," + connStatusBit +")";

    // Attempt to push the updated device connection status into the database
    try
     {
      int numRowsChanged = mySQLStmt.executeUpdate(pushDevConnStatusQuery);

      // Ensure that a row was affected by the update
      if(numRowsChanged == 0)
       {
        // Log the error
        Log.code(ERR_DB_PUSH_DEVCONNSTATE_NOROWS);

        // Return that the device connection state was NOT pushed into the database
        return false;
       }

      // Return that the device connection state
      // was successfully pushed into the database
      return true;
     }
    catch(SQLException sqlExcp)
     {
      // Log the error
      Log.code(ERR_DB_PUSH_DEVCONNSTATE,"(reason = " + sqlExcp + ")");

      // Return that the device connection state was NOT pushed into the database
      return false;
     }
   }
  */

  /* ================================== PUBLIC METHODS ================================== */

  // TODO: FIXME

  // Constructor
  public MySQLConnector()
   {
    /*

    // Attempt to establish a connection with the database
    try(Connection STDBConn = DriverManager.getConnection(ST_DB_ENDPOINT,ST_DB_USER,ST_DB_PWD))
     { this.STDBConn = STDBConn; }

    // Failing to connect with the database or to initialize the statement is a FATAL error
    catch(SQLException sqlExcp)
     { Log.code(ERR_DB_CONN_FAILED,"(reason = " + sqlExcp + ")"); }

     */
    java.util.Properties connProperties = new java.util.Properties();
    connProperties.put("user",ST_DB_USER);
    connProperties.put("password",ST_DB_PWD);
    connProperties.put("autoReconnect","true");
    connProperties.put("maxReconnects","4");


    try
     { STDBConn = DriverManager.getConnection(ST_DB_ENDPOINT,connProperties);}
    catch(java.sql.SQLException sqlExcp)
     { Log.err("shit"); }
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
         Log.code(ERR_DB_GET_SENSORS);
       }
     }

    // Failing to retrieve the set of sensors in the database is a FATAL error
    catch(SQLException sqlExcp)
     { Log.code(ERR_DB_GET_SENSORS, "(reason = " + sqlExcp + ")"); }

    // Return the <MAC,BaseSensor> map of sensors in the database
    return sensorsMap;
   }

  // Returns the <MAC,BaseActuator> map of actuators in the database
  public HashMap<String,BaseActuator> getDBActuatorsMap()
   {
    // <MAC,BaseActuator> map of sensors in the database to be returned
    HashMap<String,BaseActuator> actuatorsMap = new HashMap<>();

    // Query to retrieve all actuators in the database
    String getAllActuatorsQuery = "SELECT * FROM " + ST_DB_ACTUATORS_TABLE;

    // Attempt to initialize the statement
    try(Statement mySQLStmt = STDBConn.createStatement())
     {
      // Attempt to retrieve the set of actuators in the database
      try(ResultSet actuatorsSet = mySQLStmt.executeQuery(getAllActuatorsQuery))
       {
        // For every actuator tuple returned, initialize and append
        // its associated <MAC,BaseActuator> element in the actuatorsMap
        while(actuatorsSet.next())
         { actuatorsMap.put(actuatorsSet.getString("mac"),new BaseActuator(actuatorsSet.getShort("actuatorID"))); }

        // Retrieving no actuators from the database is a FATAL error
        if(actuatorsMap.isEmpty())
         Log.code(ERR_DB_GET_ACTUATORS);
       }
     }

    // Failing to retrieve the set of actuators in the database is a FATAL error
    catch(SQLException sqlExcp)
     { Log.code(ERR_DB_GET_ACTUATORS, "(reason = " + sqlExcp + ")"); }

    // Return the <MAC,BaseActuator> map of actuators in the database
    return actuatorsMap;
   }
 }
