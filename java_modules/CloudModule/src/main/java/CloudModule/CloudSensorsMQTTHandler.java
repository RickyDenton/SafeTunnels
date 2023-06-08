/* SafeTunnels Cloud Module Sensors MQTT Handler */

package CloudModule;

/* ================================== IMPORTS ================================== */

/* -------------------------- Java Standard Libraries -------------------------- */
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;
import java.sql.SQLException;

/* --------------------------- SafeTunnels Packages --------------------------- */
import devices.sensor.BaseSensor;
import logging.Log;
import modules.SensorsMQTTHandler.SensorsMQTTHandler;
import static CloudModule.CloudSensorsMQTTHandlerErrCode.*;
import static modules.MySQLConnector.MySQLConnector.*;


/* ============================== CLASS DEFINITION ============================== */
final class CloudSensorsMQTTHandler extends SensorsMQTTHandler
 {
  /* ============================ PRIVATE ATTRIBUTES ============================ */

  // The estimated maximum sensor MQTT inactivity in seconds for
  // tuning the sensors' boostrap inactivity timer which, once
  // triggered, pushes into database the assumed offline connection
  // state of all sensors publications have not been received from
  private final static int MQTT_CLI_MAX_INACTIVITY = 50;

  // A Mutex used to coordinate database access
  // between the MQTT client received messages
  // callbacks and the sensors' boostrap inactivity timer
  private final ReentrantLock mutexDB;

  // Whether the sensor offline bootstrap timer has run
  private boolean sensorsOfflineBootstrapTimerHasRun;

  // The MySQL connector used for reading
  // and writing from the SafeTunnels database
  private final CloudMySQLConnector cloudMySQLConnector;

  /* ============================= PROTECTED METHODS ============================= */

  /**
   * Cloud MQTT Client Sensor Connection handler, pushing into the database the 'online'
   * connection state of a previously offline sensor that data has been received from
   * @param sensorID The ID of the sensor to push the 'online state into the database
   */
  protected void handleSensorConnect(int sensorID)
   {
    // Acquire the database mutex
    mutexDB.lock();

    try
     {
      // Attempt to push the 'online' connection state
      // of the connected sensor into the database
      cloudMySQLConnector.pushSensorConnStatus(sensorID,true);

      // Log that the sensor is now online and the successful database insertion
      Log.info("sensor" + sensorID + " is now online (pushed into the database)");
     }
    catch(SQLException sqlExcp)
     { Log.code(ERR_CLOUD_SENSOR_ONLINE_UPDATE_FAILED,"(sensorID = " +
                sensorID + ", reason = " + sqlExcp.getMessage() + ")"); }

    // Release the database mutex
    finally
     { mutexDB.unlock(); }
   }


  /**
   * Cloud MQTT Client Sensor Disconnection handler, pushing into the
   * database the 'offline' connection state of an offline sensor, either
   * by the sensors' boostrap inactivity timer or from the sensor's
   * ERR_SENSOR_MQTT_DISCONNECTED last will message published by the broker
   * @param sensorID The ID of the sensor to push the 'offline' state into the database
   */
  protected void handleSensorDisconnect(int sensorID)
   {
    /*
     * If the sensors' bootstrap inactivity timer has not run
     * yet, the method was called upon receiving a sensor's last
     * will disconnection message of a past execution that was
     * retained by the MQTT broker, and that can be ignored
     */
    if(!sensorsOfflineBootstrapTimerHasRun)
     return;

    // Acquire the database mutex
    mutexDB.lock();

    try
     {
      // Attempt to push the 'offline' connection state
      // of the disconnected sensor into the database
      cloudMySQLConnector.pushSensorConnStatus(sensorID,false);

      // Log that the sensor appears to be offline and the successful database insertion
      Log.warn("sensor" + sensorID + " appears to be offline (pushed into the database)");
     }
    catch(SQLException sqlExcp)
     { Log.code(ERR_CLOUD_SENSOR_OFFLINE_UPDATE_FAILED,"(sensorID = "
                + sensorID + ", reason = " + sqlExcp.getMessage() + ")"); }

    // Release the database mutex
    finally
     { mutexDB.unlock(); }
   }


  /**
   * Cloud MQTT Client Sensor C02 reading handler, pushing
   * into the database a sensor's updated C02 density reading
   * @param sensorID The ID of the sensor the reading comes from
   * @param newC02   The updated C02 density value to be pushed into the database
   */
  protected void handleSensorC02Reading(int sensorID,int newC02)
   {
    // Acquire the database mutex
    mutexDB.lock();

    try
     {
      // Attempt to push the sensor's updated C02 value into the database
      cloudMySQLConnector.pushSensorQuantity(ST_DB_SENSORS_TABLE_C02,
                                             ST_DB_SENSORS_COLUMN_C02,
                                             sensorID,newC02);

      // Log the successful C02 database update
      Log.info("Pushed sensor" + sensorID + " updated C02 "
               + "density " + "value (" + newC02 + ") into the database");
     }
    catch(SQLException sqlExcp)
     { Log.code(ERR_CLOUD_SENSOR_C02_UPDATE_FAILED,"(sensorID = " + sensorID +
                ", value = " + newC02 + ", reason = " + sqlExcp.getMessage() + ")"); }

    // Release the database mutex
    finally
     { mutexDB.unlock(); }
   }


  /**
   * Cloud MQTT Client Sensor temperature reading handler, pushing
   * into the database a sensor's updated temperature reading
   * @param sensorID The ID of the sensor the reading comes from
   * @param newTemp  The updated temperature value to be pushed into the database
   */
  protected void handleSensorTempReading(int sensorID,int newTemp)
   {
    // Acquire the database mutex
    mutexDB.lock();

    try
     {
      // Attempt to push the sensor's updated temperature value into the database
      cloudMySQLConnector.pushSensorQuantity(ST_DB_SENSORS_TABLE_TEMP,
                                             ST_DB_SENSORS_COLUMN_TEMP,
                                             sensorID,newTemp);

      // Log the successful temperature database update
      Log.info("Pushed sensor" + sensorID + " updated "
                + "temperature value (" + newTemp + ") into the database");
     }
    catch(SQLException sqlExcp)
     { Log.code(ERR_CLOUD_SENSOR_TEMP_UPDATE_FAILED,"(sensorID = " + sensorID +
                ", value = " + newTemp + ", reason = " + sqlExcp.getMessage() + ")"); }

    // Release the database mutex
    finally
     { mutexDB.unlock(); }
   }


  /* ============================== PACKAGE METHODS ============================== */

  /**
   * Cloud Module Sensors MQTT Handler constructor, initializing
   * the Cloud MQTT client handler, connecting with the MQTT
   * broker and starting the sensors' boostrap inactivity timer
   * @param cloudMySQLConnector The MySQL connector used for reading
   *                            and writing from the SafeTunnels database
   * @param sensorMap The <MAC,BaseSensor> map of sensors read from the
   *                  SafeTunnels database to receive MQTT publications from
   */
  CloudSensorsMQTTHandler(CloudMySQLConnector cloudMySQLConnector, HashMap<String,BaseSensor> sensorMap)
   {
    // Attempt to initialize the Cloud Module MQTT
    // Client Handler and connect with the MQTT broker
    super("CloudModule",sensorMap);

    // Set the MySQL connector used for reading
    // and writing from the SafeTunnels database
    this.cloudMySQLConnector = cloudMySQLConnector;

    // Initialize the DB access mutex
    mutexDB = new ReentrantLock();

    // Set that the boostrap inactivity timer has not run yet
    sensorsOfflineBootstrapTimerHasRun = false;

    // Initialize the sensors' boostrap inactivity timer which, once
    // triggered, pushes into database the assumed offline connection
    // state of all sensors publications have not been received from
    Timer sensorsOfflineUpdateTimer = new Timer();
    sensorsOfflineUpdateTimer.schedule(new TimerTask()
     {
      public void run()
       {
        try
         {
          // Acquire the database mutex
          mutexDB.lock();

          // Set that the timer has run
          sensorsOfflineBootstrapTimerHasRun = true;

          // For each sensor a publication has not been
          // received from (and so are not online), push
          // their assumed 'offline' status into the database
          sensorMap.forEach((MAC,sensor) ->
           {
            if(!sensor.connState)
             handleSensorDisconnect(sensor.ID);
           });
         }

        // Release the database mutex
        finally
         { mutexDB.unlock(); }
       }
     },MQTT_CLI_MAX_INACTIVITY * 1000); // In milliseconds
   }

 }