/* SafeTunnels Cloud Module Main Class */

package CloudModule;

/* ================================== IMPORTS ================================== */

/* --------------------- Java Standard Libraries Resources --------------------- */
import java.util.Collections;
import java.util.ArrayList;

/* --------------------------- SafeTunnels Resources --------------------------- */
import logging.Log;
import errors.ErrCodeSeverity;
import modules.MySQLConnector.DevMACIDPair;
import static devices.BaseDevice.DevType.sensor;
import CloudModule.CloudMySQLConnector.CloudMySQLConnector;
import CloudModule.DevicesManagers.CloudSensorManager;
import modules.SensorsMQTTHandler.SensorsMQTTHandler;


/* ============================== CLASS DEFINITION ============================== */
final class CloudModule
 {
  /* ============================ PRIVATE ATTRIBUTES ============================ */

  // The SafeTunnels MySQL database connector used by the Cloud Module
  final CloudMySQLConnector cloudMySQLConnector;

  // The Cloud Module's sensors MQTT Client Handler
  final SensorsMQTTHandler cloudMQTTHandler;

  /* ============================== PRIVATE METHODS ============================== */

  /**
   * Cloud Module Constructor, instantiating its components
   */
  private CloudModule()
   {
    // Attempt to connect with the SafeTunnels MySQL database
    cloudMySQLConnector = new CloudMySQLConnector();

    // Attempt to retrieve the <MAC,sensorID> list of sensors stored in the database
    ArrayList<DevMACIDPair> sensorsList = cloudMySQLConnector.getDBDevicesList(sensor);

    // If LOG_LEVEL = DEBUG, log the list of sensors retrieved from the database
    // (which is ascertained to be non-null by the getDBDevicesList() method)
    if(Log.LOG_LEVEL == ErrCodeSeverity.DEBUG)
     {
      Log.dbg(sensorsList.size() + " sensors were retrieved from the database <sensorID,MAC>:");
      sensorsList.forEach((sensor) -> Log.dbg("|- <" + sensor.ID + "," + sensor.MAC + ">"));
     }

    // Initialize and populate the ArrayList of
    // CloudSensorManagers to be passed to the SensorMQTTHandler
    ArrayList<CloudSensorManager> cloudSensorsManagersList = new ArrayList<>();
    sensorsList.forEach(sensor -> cloudSensorsManagersList.add
                       (new CloudSensorManager(sensor.MAC,sensor.ID,cloudMySQLConnector)));

    // Sort the CloudSensorManagers by increasing sensorID
    Collections.sort(cloudSensorsManagersList);

    // Attempt to instantiate the Cloud Module MQTT Client Handler
    cloudMQTTHandler = new SensorsMQTTHandler("CloudModule",cloudSensorsManagersList);

    // Log that the Cloud Module has been successfully initialized
    Log.info("Cloud Module successfully initialized");
   }


  /**
   * Cloud Module application entry point
   */
  public static void main(String[] args)
   { new CloudModule(); }
 }