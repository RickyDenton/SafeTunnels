/* Cloud Module SafeTunnels Sensor Manager */

package CloudModule.DevicesManagers.SensorManager;

/* ================================== IMPORTS ================================== */

/* --------------------------- SafeTunnels Resources --------------------------- */
import devices.sensor.BaseSensor;
import static devices.sensor.BaseSensor.SensorQuantity.C02;
import static devices.sensor.BaseSensor.SensorQuantity.TEMP;
import CloudModule.CloudMySQLConnector.CloudMySQLConnector;


/* ============================== CLASS DEFINITION ============================== */
final public class CloudSensorManager extends BaseSensor
 {
  /* ============================ PRIVATE ATTRIBUTES ============================ */

  // A reference to the Cloud MySQL connector to
  // be used for pushing the updated sensor state
  private final CloudMySQLConnector cloudMySQLConnector;


  /* ============================ PROTECTED METHODS ============================ */

  /**
   *  Sensor Connection Handler, invoked by the "setC02" and "setTemp" handlers
   *  upon receiving an updated quantity value with the sensor is offline
   */
  @Override
  public void setConnStateOnline()
   {
    // Set that the sensor is now online
    connState = true;

    // Attempt to push the 'ONLINE' sensor connState into the database
    cloudMySQLConnector.pushSensorConnState(ID, true);
   }


  /* ============================== PUBLIC METHODS ============================== */

  /**
   * CloudSensorManager constructor, initializing the BaseSensor
   * attributes and the manager's Cloud MySQL Connector reference
   * @param MAC The sensor's (unique) MAC
   * @param ID The sensor's unique ID in the SafeTunnels Database
   * @param cloudMySQLConnector A reference to the Cloud MySQL Connector
   */
  public CloudSensorManager(String MAC, short ID,CloudMySQLConnector cloudMySQLConnector)
   {
    super(MAC,ID);
    this.cloudMySQLConnector = cloudMySQLConnector;
   }


  /**
   *  Sensor Disconnection Handler, invoked by the Cloud MQTT Client Handler upon
   *  detecting that the sensor is offline (which may due either to its sensors'
   *  bootstrap inactivity timer or upon receiving the sensor's
   *  ERR_SENSOR_MQTT_DISCONNECTED last will message published by the broker
   */
  @Override
  public void setConnStateOffline()
   {
    // Set that the sensor is now offline
    connState = false;

    // Attempt to push the 'OFFLINE' sensor connState into the database
    cloudMySQLConnector.pushSensorConnState(ID, false);
   }


  /**
   * Sensor C02 Update Handler, invoked by the Cloud MQTT Client Handler
   * upon receiving an updated C02 value published by the sensor
   * @param newC02 The updated C02 value published by the sensor
   */
  @Override
  public void setC02(int newC02)
   {
    // If the sensor was offline, set it online
    if(!connState)
     setConnStateOnline();

    // Attempt to push the updated sensor C02 value into the database
    cloudMySQLConnector.pushSensorQuantityValue(ID,C02,newC02);
   }


  /**
   * Sensor C02 Update Handler, invoked by the Cloud MQTT Client Handler
   * upon receiving an updated temperature value published by the sensor
   * @param newTemp The temperature value published by the sensor
   */
  @Override
  public void setTemp(int newTemp)
   {
    // If the sensor was offline, attempt to set it online
    if(!connState)
     setConnStateOnline();

    // Attempt to push the updated sensor temperature value into the database
    cloudMySQLConnector.pushSensorQuantityValue(ID,TEMP,newTemp);
   }
 }