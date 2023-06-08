/* SafeTunnels Cloud Module Main Class */

package CloudModule;

/* ================================== IMPORTS ================================== */

/* -------------------------- Java Standard Libraries -------------------------- */
import java.util.HashMap;

/* --------------------------- SafeTunnels Packages --------------------------- */
import devices.sensor.BaseSensor;
import errors.ErrCodeSeverity;
import logging.Log;


/* ============================== CLASS DEFINITION ============================== */
final class CloudModule
 {
  /* ============================ PRIVATE ATTRIBUTES ============================ */

  // The SafeTunnels MySQL database connector used by the Cloud Module
  final CloudMySQLConnector cloudMySQLConnector;

  // The <MAC,BaseSensor> map of sensors read from the
  // SafeTunnels database to receive MQTT publications from
  final HashMap<String,BaseSensor> sensorMap;

  // The Cloud Module MQTT Client Handler
  final CloudSensorsMQTTHandler cloudSensorsMQTTHandler;

  /* ============================== PRIVATE METHODS ============================== */

  /**
   * Cloud Module Constructor, instantiating its components
   */
  private CloudModule()
   {
    // Attempt to connect with the SafeTunnels MySQL database
    cloudMySQLConnector = new CloudMySQLConnector();

    // Attempt to retrieve the <MAC,BaseSensor> map of sensors from the database
    sensorMap = cloudMySQLConnector.getDBSensorsMap();

    // If LOG_LEVEL = DEBUG, log the map of sensors retrieved from the database
    // (which is ascertained to be non-null by the getDBSensorsMap() method)
    if(Log.LOG_LEVEL == ErrCodeSeverity.DEBUG)
     {
      Log.dbg(sensorMap.size() + " sensors were retrieved from the database <MAC,sensorID>:");
      sensorMap.forEach((MAC,sensor) -> Log.dbg("|- <" + MAC + "," + sensor.ID + ">"));
     }

    // Attempt to instantiate the MQTT Client Handler and connect with the MQTT broker
    cloudSensorsMQTTHandler = new CloudSensorsMQTTHandler(cloudMySQLConnector,sensorMap);

    // Log that the Cloud Module initialization was successful
    Log.info("Cloud Module successfully initialized");
   }

  /**
   * Cloud Module application entry point
   */
  public static void main(String[] args)
   { new CloudModule(); }
 }