package CloudModule;

import devices.sensor.BaseSensor;
import errors.ErrCodeSeverity;
import logging.Log;
import modules.SensorsMQTTHandler.SensorsMQTTHandler;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import static CloudModule.CloudModuleErrCode.*;
import static modules.MySQLConnector.MySQLConnector.*;


final class CloudModule extends SensorsMQTTHandler
 {
  // Max sensors MQTT inactivity in seconds
  private final static int MQTT_CLI_MAX_INACTIVITY = 50;

  private final CloudMySQLConnector cloudMySQLConnector;
  private final ReentrantLock mutexDB;


  /* ==================================== ATTRIBUTES ==================================== */

  // Constructor
  public CloudModule(CloudMySQLConnector cloudMySQLConnector, HashMap<String,BaseSensor> sensorMap)
   {
    // Attempt to initialize the Cloud Module MQTT Handler
    super("CloudModule",sensorMap);

    // Set the Cloud MySQL connector
    this.cloudMySQLConnector = cloudMySQLConnector;

    // Initialize the DB mutex
    mutexDB = new ReentrantLock();

    // Initialize the timer for updating to "false" the connState of all
    // sensors a MQTT publication was not received from in the database
    Timer sensorsOfflineUpdateTimer = new Timer();
    sensorsOfflineUpdateTimer.schedule(new TimerTask()
     {
      public void run()
       {
        try
         {
          mutexDB.lock();
          sensorMap.forEach((MAC,sensor) ->
           {
            if(!sensor.connState)
             handleSensorDisconnect(sensor.ID);
           });
         }
        finally
         { mutexDB.unlock(); }
       }
     },MQTT_CLI_MAX_INACTIVITY * 1000); // In milliseconds

    // Log that the Cloud Module initialization was successful
    Log.info("Cloud Module successfully initialized");
   }


  // Handle sensor disconnection
  public void handleSensorConnect(int sensorID)
   {
    mutexDB.lock();
    try
     {
      // Attempt to push the online sensor state into the database
      cloudMySQLConnector.pushSensorConnStatus(sensorID,true);
      Log.info("sensor" + sensorID + " is now online");
     }
    catch(SQLException sqlExcp)
     { Log.code(ERR_CLOUD_SENSOR_ONLINE_UPDATE_FAILED,"(sensorID = " + sensorID + ", reason = " + sqlExcp + ")"); }
    finally
     { mutexDB.unlock(); }
   }


  // Handle sensor disconnection
  public void handleSensorDisconnect(int sensorID)
   {
    mutexDB.lock();
    try
     {
      // Attempt to push the offline sensor state into the database
      cloudMySQLConnector.pushSensorConnStatus(sensorID,false);
      Log.warn("sensor" + sensorID + " appears to be offline (pushed into the database)");
     }
    catch(SQLException sqlExcp)
     { Log.code(ERR_CLOUD_SENSOR_OFFLINE_UPDATE_FAILED,"(sensorID = " + sensorID + ", reason = " + sqlExcp + ")"); }
    finally
     { mutexDB.unlock(); }
   }



  // Handle C02 reading
  public void handleSensorC02Reading(int sensorID,int newC02)
   {
    mutexDB.lock();
    try
     {
      // Attempt to push the updated C02 value into the database
      cloudMySQLConnector.pushSensorQuantity(ST_DB_SENSORS_TABLE_C02,ST_DB_SENSORS_COLUMN_C02,sensorID,newC02);
      Log.info("Pushed sensor" + sensorID + " updated C02 density value (" + newC02 + ") into the database");
     }
    catch(SQLException sqlExcp)
     { Log.code(ERR_CLOUD_SENSOR_C02_UPDATE_FAILED,"(sensorID = " + sensorID + ", value = " + newC02 + ", reason = " + sqlExcp + ")"); }
    finally
     { mutexDB.unlock(); }
   }

  // Handle temperature reading
  public void handleSensorTempReading(int sensorID,int newTemp)
   {
    mutexDB.lock();
    try
     {
      // Attempt to push the updated temperature value into the database
      cloudMySQLConnector.pushSensorQuantity(ST_DB_SENSORS_TABLE_TEMP,ST_DB_SENSORS_COLUMN_TEMP,sensorID,newTemp);
      Log.info("Pushed sensor" + sensorID + " updated temperature value (" + newTemp + ") into the database");
     }
    catch(SQLException sqlExcp)
     { Log.code(ERR_CLOUD_SENSOR_TEMP_UPDATE_FAILED,"(sensorID = " + sensorID + ", value = " + newTemp + ", reason = " + sqlExcp + ")"); }
    finally
     { mutexDB.unlock(); }
   }

  public static void main(String[] args)
   {
    // Attempt to connect with the SafeTunnels MySQL database
    CloudMySQLConnector cloudMySQLConnector = new CloudMySQLConnector();

    // Attempt to retrieve the <MAC,BaseSensor> map of sensors in the database
    HashMap<String,BaseSensor> sensorMap = cloudMySQLConnector.getDBSensorsMap();

    // If LOG_LEVEL = DEBUG, log the map of sensors retrieved from the database (which is surely not null)
    if(Log.LOG_LEVEL == ErrCodeSeverity.DEBUG)
     {
      Log.dbg(sensorMap.size() + " sensors were retrieved from the database <MAC,sensorID>:");
      sensorMap.forEach((MAC,sensor) -> Log.dbg(" - <" + MAC + "," + sensor.ID + ">"));
     }

    // Attempt to initialize the Cloud Sensors MQTT Handler
    new CloudModule(cloudMySQLConnector,sensorMap);
   }
 }