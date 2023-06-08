/* SafeTunnels Base Sensor Class */

package devices.sensor;

/* ================================== IMPORTS ================================== */

/* --------------------------- SafeTunnels Packages --------------------------- */
import devices.Device;
import static devices.Device.DevType.sensor;


/* ============================== CLASS DEFINITION ============================== */
public class BaseSensor implements Device
 {
  /* --------------------------- Sensors MQTT Topics --------------------------- */

  // Quantities reporting topics
  public final static String TOPIC_SENSORS_C02 = "SafeTunnels/C02";
  public final static String TOPIC_SENSORS_TEMP = "SafeTunnels/temp";

  // Errors reporting topic
  public final static String TOPIC_SENSORS_ERRORS = "SafeTunnels/sensorsErrors";

  // Average Fan Relative Speed subscription topic
  public final static String TOPIC_AVG_FAN_REL_SPEED = "SafeTunnels/avgFanRelSpeed";


  /* ----------------------- Sensors MQTT Client States ----------------------- */
  public enum SensorMQTTCliState
   {
    // The sensor must still initialize the MQTT engine
    MQTT_CLI_STATE_INIT,

    // The sensor has initialized the MQTT engine
    // and is waiting for the RPL DODAG to converge
    MQTT_CLI_STATE_ENGINE_OK,

    // The sensor is online and attempting
    // to connect with the MQTT broker
    MQTT_CLI_STATE_NET_OK,

    // The sensor is waiting for the connection
    // with the MQTT broker to be established
    MQTT_CLI_STATE_BROKER_CONNECTING,

    // The sensor is connected with the MQTT broker but is not
    // yet subscribed on the TOPIC_AVG_FAN_REL_SPEED topic
    MQTT_CLI_STATE_BROKER_CONNECTED,

    // The sensor is connected with the MQTT broker AND is subscribed
    // on the TOPIC_AVG_FAN_REL_SPEED topic (steady-state)
    MQTT_CLI_STATE_BROKER_SUBSCRIBED,

    // Unknown sensor MQTT client state (used if not specified in error messages)
    MQTT_CLI_STATE_UNKNOWN,
   }


  /* ============================ PUBLIC ATTRIBUTES ============================ */

  // The sensor's unique ID in the SafeTunnels database
  public final short ID;

  // The sensor's current connection state (false -> offline, true -> online)
  public boolean connState;


  /* ============================= PUBLIC METHODS ============================= */

  /**
   * BaseSensor constructor, initializing its attributes
   * @param ID The sensor's unique ID in the SafeTunnels database
   */
  public BaseSensor(short ID)
   {
    this.ID = ID;
    this.connState = false;
   }

  /**
   * @return The sensor's unique ID in the SafeTunnels database
   */
  public short getID()
   { return ID; }

  /**
   * @return The sensor's device type
   */
  public DevType getDevType()
   { return sensor; }
 }