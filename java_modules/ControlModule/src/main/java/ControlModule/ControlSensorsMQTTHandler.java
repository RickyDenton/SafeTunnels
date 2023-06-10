/* SafeTunnels Control Module Sensors MQTT Handler */

package ControlModule;

/* ================================== IMPORTS ================================== */

/* -------------------------- Java Standard Libraries -------------------------- */

import DevicesManagers.ControlSensorManager;
import devices.sensor.BaseSensor;
import modules.SensorsMQTTHandler.SensorsMQTTHandler;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;


/* ============================== CLASS DEFINITION ============================== */
final class ControlSensorsMQTTHandler extends SensorsMQTTHandler
 {
  /* ============================ PRIVATE ATTRIBUTES ============================ */

  // The estimated maximum sensor MQTT inactivity in seconds for tuning the
  // sensors' boostrap inactivity timer which, once triggered, ascertains
  // the sensors publications have not been received from as offline
  private final static int MQTT_CLI_MAX_INACTIVITY = 50;

  // Whether the sensor offline bootstrap timer has run
  private boolean sensorsOfflineBootstrapTimerHasRun;

  HashMap<Integer,ControlSensorManager> sensorIDMap;


  /* ============================= PROTECTED METHODS ============================= */

  /**
   * Cloud MQTT Client Sensor Connection handler, pushing into the database the 'online'
   * connection state of a previously offline sensor that data has been received from
   * @param sensorID The ID of the sensor to push the 'online state into the database
   */


  protected void handleSensorConnect(int sensorID)
   { sensorIDMap.get(sensorID).updateSensorConnection(); }


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

    sensorIDMap.get(sensorID).updateSensorDisconnection();

   }


  /**
   * Cloud MQTT Client Sensor C02 reading handler, pushing
   * into the database a sensor's updated C02 density reading
   * @param sensorID The ID of the sensor the reading comes from
   * @param newC02   The updated C02 density value to be pushed into the database
   */


  protected void handleSensorC02Reading(int sensorID,int newC02)
   { sensorIDMap.get(sensorID).updateC02(newC02); }


  /**
   * Cloud MQTT Client Sensor temperature reading handler, pushing
   * into the database a sensor's updated temperature reading
   * @param sensorID The ID of the sensor the reading comes from
   * @param newTemp  The updated temperature value to be pushed into the database
   */


  protected void handleSensorTempReading(int sensorID,int newTemp)
   { sensorIDMap.get(sensorID).updateTemp(newTemp); }


  /* ============================== PACKAGE METHODS ============================== */

  /**
   * Cloud Module Sensors MQTT Handler constructor, initializing
   * the Cloud MQTT client handler, connecting with the MQTT
   * broker and starting the sensors' boostrap inactivity timer
   * @param sensorMacMap The MySQL connector used for reading
   *                            and writing from the SafeTunnels database
   * @param sensorIDMap The <MAC,BaseSensor> map of sensors read from the
   *                  SafeTunnels database to receive MQTT publications from
   */


  ControlSensorsMQTTHandler(HashMap<String,BaseSensor> sensorMacMap,HashMap<Integer,ControlSensorManager> sensorIDMap)
   {
    // Attempt to initialize the Cloud Module MQTT
    // Client Handler and connect with the MQTT broker
    super("CloudModule",sensorMacMap);

    // Set the MySQL connector used for reading
    // and writing from the SafeTunnels database
    this.sensorIDMap = sensorIDMap;

    // Set that the boostrap inactivity timer has not run yet
    sensorsOfflineBootstrapTimerHasRun = false;

    // Initialize the sensors' boostrap inactivity timer which, once triggered,
    // ascertains the sensors publications have not been received from as offline
    Timer sensorsOfflineUpdateTimer = new Timer();
    sensorsOfflineUpdateTimer.schedule(new TimerTask()
     {
      public void run()
       {
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
     },MQTT_CLI_MAX_INACTIVITY * 1000); // In milliseconds
   }

 }