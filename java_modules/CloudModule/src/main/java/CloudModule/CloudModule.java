/* SafeTunnels Cloud Module Main Class */

package CloudModule;

/* ================================== IMPORTS ================================== */

/* -------------------------- Java Standard Libraries -------------------------- */
import java.util.Collections;
import java.util.ArrayList;

/* --------------------------- SafeTunnels Packages --------------------------- */
import errors.ErrCodeSeverity;
import logging.Log;
import modules.MySQLConnector.DevMACIDPair;
import modules.SensorsMQTTHandler.SensorsMQTTHandler;


/* ============================== CLASS DEFINITION ============================== */
final class CloudModule
 {
  /* ============================ PRIVATE ATTRIBUTES ============================ */

  // The SafeTunnels MySQL database connector used by the Cloud Module
  final CloudMySQLConnector cloudMySQLConnector;

  // The Cloud Module's sensors MQTT Client Handler
  final SensorsMQTTHandler sensorsMQTTHandler;

  /* ============================== PRIVATE METHODS ============================== */

  /**
   * Cloud Module Constructor, instantiating its components
   */
  private CloudModule()
   {
    // Attempt to connect with the SafeTunnels MySQL database
    cloudMySQLConnector = new CloudMySQLConnector();

    // Attempt to retrieve the <MAC,sensorID> list of sensors stored in the database
    ArrayList<DevMACIDPair> sensorsList = cloudMySQLConnector.getDBSensorsList();

    // If LOG_LEVEL = DEBUG, log the list of sensors retrieved from the database
    // (which is ascertained to be non-null by the getDBSensorsList() method)
    if(Log.LOG_LEVEL == ErrCodeSeverity.DEBUG)
     {
      Log.dbg(sensorsList.size() + " sensors were retrieved from the database <sensorID,MAC>:");
      sensorsList.forEach((sensor) -> Log.dbg("|- <" + sensor.ID + "," + sensor.MAC + ">"));
     }

    // Initialize and populate the ArrayList of
    // CloudSensorManagers to be passed to the SensorMQTTHandler
    ArrayList<CloudSensorManager> sensorsManagersList = new ArrayList<>();
    sensorsList.forEach(sensor -> sensorsManagersList.add
                       (new CloudSensorManager(sensor.MAC,sensor.ID,cloudMySQLConnector)));

    // Sort the ArrayList by increasing sensorID
    Collections.sort(sensorsManagersList);

    // Attempt to instantiate the Cloud MQTT Client Handler
    sensorsMQTTHandler = new SensorsMQTTHandler("CloudModule",sensorsManagersList);

    // Log that the Cloud Module has been successfully initialized
    Log.info("Cloud Module successfully initialized");
   }


  /**
   * Cloud Module application entry point
   */
  public static void main(String[] args)
   { new CloudModule(); }
 }