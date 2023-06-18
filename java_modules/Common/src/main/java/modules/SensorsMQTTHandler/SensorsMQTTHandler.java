/* SafeTunnels Sensors MQTT Client Handler, used both by the Cloud and Control Module */

package modules.SensorsMQTTHandler;

/* ================================== IMPORTS ================================== */

/* --------------------------- SafeTunnels Resources --------------------------- */

/* --------------------- Java Standard Libraries Resources --------------------- */
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/* ----------------------- Maven Dependencies Resources ----------------------- */

// Paho MQTT Client
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

// JSON parser
import org.json.JSONException;
import org.json.JSONObject;

/* --------------------------- SafeTunnels Resources --------------------------- */
import logging.Log;
import errors.ErrCodeExcp;
import devices.sensor.BaseSensor;
import devices.sensor.BaseSensorErrCode;
import static devices.sensor.BaseSensor.*;
import static devices.sensor.BaseSensor.SensorMQTTCliState.MQTT_CLI_STATE_UNKNOWN;
import static modules.SensorsMQTTHandler.SensorsMQTTHandlerErrCode.*;


/* ============================== CLASS DEFINITION ============================== */
public class SensorsMQTTHandler implements MqttCallback
 {
  /* ================================ ATTRIBUTES ================================ */

  // MQTT Broker endpoint
  private final static String MQTT_BROKER_ENDPOINT = "tcp://127.0.0.1:1883";

  // MQTT PAHO Client Handler
  protected MqttClient MQTTClient;

  // The list of sensors in the application
  ArrayList<? extends BaseSensor> sensorsList;

  // The estimated maximum sensor MQTT inactivity in milliseconds
  // for tuning the sensors' boostrap inactivity timer
  // which, once triggered, properly updates all sensors
  // publications have not been received from as offline
  private final static int MQTT_CLI_MAX_INACTIVITY = 50 * 1000;

  // Whether the sensor offline bootstrap timer has run
  public boolean sensorsOfflineBootstrapTimerHasRun;


  /* ============================== PRIVATE METHODS ============================== */

  /**
   * Attempts to connect the PAHO MQTT client module with the local
   * MQTT broker and to subscribe on the sensor's MQTT topics
   * @throws MqttException Failed to connect the PAHO MQTT client
   *                       module with the local MQTT broker
   */
  private void connectSubscribeMQTTBroker() throws MqttException
   {
    // Attempt to connect the PAHO MQTT client
    // module with the local MQTT broker
    MQTTClient.connect();

    // Log the successful connection
    Log.dbg("MQTT Client connected with the MQTT broker @"
            + MQTT_BROKER_ENDPOINT + ", subscribing on topics");

    // Attempt to subscribe the PAHO MQTT client
    // module on the sensor's MQTT topics
    MQTTClient.subscribe(TOPIC_SENSORS_C02);
    MQTTClient.subscribe(TOPIC_SENSORS_TEMP);
    MQTTClient.subscribe(TOPIC_SENSORS_ERRORS);

    // Log the successful topics' subscriptions
    Log.dbg("MQTT Client successfully subscribed on the sensors' topics");
   }


  /* ------------------ Received MQTT Message Parsing Methods ------------------ */

  /**
   * Retrieves the "MAC" attribute from a MQTT message
   * received by a sensor interpreted as a JSON object
   * @param mqttMsgJSON The received MQTT message interpreted as a JSON object
   * @param mqttMsgStr  The received MQTT message interpreted as a String
   * @return The "MAC" attribute in the MQTT message
   * @throws ErrCodeExcp The "MAC" attribute in the MQTT message
   *                     is missing or is not a non-null String
   */
  private String getSensorMAC(JSONObject mqttMsgJSON,String mqttMsgStr) throws  ErrCodeExcp
   {
    // Ascertain the received MQTT message to contain the
    // required "MAC" attribute, throwing an exception otherwise
    if(!mqttMsgJSON.has("MAC"))
     throw new ErrCodeExcp(ERR_MQTT_MSG_MAC_MISSING,"(\"" + mqttMsgStr + "\")");

    // Attempt to extract the "MAC" attribute as a String and
    // ensure it to be non-null, throwing an exception otherwise
    try
     { return mqttMsgJSON.getString("MAC"); }
    catch(JSONException jsonExcp)
     { throw new ErrCodeExcp(ERR_MQTT_MSG_MAC_NOT_NONNULL_STRING,
                     "(\"" + mqttMsgStr + "\")"); }
   }


  /**
   * Retrieves the "BaseSensorErrCode" attribute from a MQTT error
   * message received by a sensor interpreted as a JSON object
   * @param mqttMsgJSON The received MQTT error message interpreted as a JSON object
   * @param mqttMsgStr  The received MQTT error message interpreted as a String
   * @return The "BaseSensorErrCode" attribute in the MQTT error message
   * @throws ErrCodeExcp The "BaseSensorErrCode" attribute is missing or its value
   *                     could not be interpreted as a valid BaseSensorErrCode
   */
  private BaseSensorErrCode getSensorErrCode(JSONObject mqttMsgJSON,String mqttMsgStr) throws  ErrCodeExcp
   {
    // Ascertain the received MQTT message to contain the required
    // "errCode" attribute, throwing an exception otherwise
    if(!mqttMsgJSON.has("errCode"))
     throw new ErrCodeExcp(ERR_MQTT_ERR_MSG_ERRCODE_MISSING,
                    "(\"" + mqttMsgStr + "\")");

    try
     {
      // The MQTT error message "errCode" mapped into a BaseSensorErrCode
      BaseSensorErrCode sensorErrCode;

      // Attempt to interpret the MQTT error message's "errCode" attribute
      // as an integer and map it into a BaseSensorErrCode, throwing
      // an exception if no BaseSensorErrCode of such index exists
      try
       { sensorErrCode = BaseSensorErrCode.values[mqttMsgJSON.getInt("errCode")]; }
      catch(ArrayIndexOutOfBoundsException outOfBoundsException)
       { throw new ErrCodeExcp(ERR_MQTT_ERR_MSG_ERRCODE_UNKNOWN,
                       "(\"" + mqttMsgStr + "\")"); }

      // Return the valid SensorErrCode
      return sensorErrCode;
     }

    // If the MQTT error message's "errCode" attribute could
    // not be interpreted as an integer, throw an exception
    catch(JSONException jsonExcp)
     { throw new ErrCodeExcp(ERR_MQTT_ERR_MSG_ERRCODE_NOT_INT,
                     "(\"" + mqttMsgStr + "\")"); }
   }


  /**
   * Retrieves the "MQTTCliState" attribute from a MQTT error
   * message received by a sensor interpreted as a JSON object
   * @param mqttMsgJSON The received MQTT error message interpreted as a JSON object
   * @param mqttMsgStr  The received MQTT error message interpreted as a String
   * @return The "MQTTCliState" attribute in the MQTT error message
   * @throws ErrCodeExcp The "MQTTCliState" value could not be
   *                     interpreted as a valid SensorMQTTCliState
   */
  private SensorMQTTCliState getSensorMQTTCliState(JSONObject mqttMsgJSON,String mqttMsgStr) throws  ErrCodeExcp
   {
    // Check whether the received MQTT error message contains
    // the optional "MQTTCliState" attribute and, if it does
    if(mqttMsgJSON.has("MQTTCliState"))
     {
      try
       {
        // The MQTT error message "MQTTCliState"
        // mapped into a SensorMQTTCliState
        SensorMQTTCliState sensorMQTTCliState;

        // Attempt to interpret the MQTT error message's "MQTTCliState"
        // attribute as an integer and map it into a SensorMQTTCliState,
        // throwing an exception if no SensorMQTTCliState of such index exists
        try
         { sensorMQTTCliState = SensorMQTTCliState.values()[mqttMsgJSON.getInt("MQTTCliState")]; }
        catch(ArrayIndexOutOfBoundsException outOfBoundsException)
         { throw new ErrCodeExcp(ERR_MQTT_ERR_MSG_MQTTCLISTATE_UNKNOWN,
                         "(\"" + mqttMsgStr + "\")"); }

        // Return the valid sensor's MQTT client state
        return sensorMQTTCliState;
       }

      // If the MQTT error message's "MQTTCliState" attribute could
      // not be interpreted as an integer, throw an exception
      catch(JSONException jsonExcp)
       { throw new ErrCodeExcp(ERR_MQTT_ERR_MSG_MQTTCLISTATE_NOT_INT,
                       "(\"" + mqttMsgStr + "\")"); }
     }

    // Otherwise, if the "MQTTCliState" attribute is missing (which should occur for
    // MQTT broker "last will" messages only), return a default unknown MQTTCliState
    else
     return MQTT_CLI_STATE_UNKNOWN;
   }


  /**
   * Retrieves the "errDscr" attribute from a MQTT error
   * message received by a sensor interpreted as a JSON object
   * @param mqttMsgJSON The received MQTT error message interpreted as a JSON object
   * @param mqttMsgStr  The received MQTT error message interpreted as a String
   * @return If present, the "errDscr" attribute value, or null otherwise
   * @throws ErrCodeExcp The "errDscr" attribute value could not be interpreted as a non-null String
   */
  private String getSensorErrDscr(JSONObject mqttMsgJSON,String mqttMsgStr) throws  ErrCodeExcp
   {
    // Check whether the received MQTT error message contains
    // the optional "errDscr" attribute and, if it does
    if(mqttMsgJSON.has("errDscr"))
     {
      // Attempt to extract the "errDscr" attribute as a String and
      // ensure it to be non-null, throwing an exception otherwise
      try
       { return mqttMsgJSON.getString("errDscr"); }
      catch(JSONException jsonExcp)
       { throw new ErrCodeExcp(ERR_MQTT_ERR_MSG_ERRDSCR_NOT_NONNULL_STRING,
                        "(\"" + mqttMsgStr + "\")"); }
     }

    // Otherwise, if the "errDscr" attribute is missing, return null
    else
     return null;
   }


  /**
   * Retrieves the "C02" attribute from a MQTT message
   * received by a sensor interpreted as a JSON object
   * @param mqttMsgJSON The received MQTT message interpreted as a JSON object
   * @param mqttMsgStr  The received MQTT message interpreted as a String
   * @return The "C02" attribute value in the MQTT message
   * @throws ErrCodeExcp The "C02" attribute is missing or its value
   *                     could not be interpreted as an integer
   */
  private int getSensorC02Reading(JSONObject mqttMsgJSON,String mqttMsgStr) throws  ErrCodeExcp
   {
    // Ascertain the received MQTT message to contain the
    // required "C02" attribute, throwing an exception otherwise
    if(!mqttMsgJSON.has("C02"))
     throw new ErrCodeExcp(ERR_MQTT_MSG_C02_MISSING,
                   "(\"" + mqttMsgStr + "\")");

    // Attempt to extract the "C02" attribute as
    // an integer, throwing an exception otherwise
    try
     { return mqttMsgJSON.getInt("C02"); }
    catch(JSONException jsonExcp)
     { throw new ErrCodeExcp(ERR_MQTT_MSG_C02_NOT_INT,
                     "(\"" + mqttMsgStr + "\")"); }
   }


  /**
   * Retrieves the "temp" attribute from a MQTT message
   * received by a sensor interpreted as a JSON object
   * @param mqttMsgJSON The received MQTT message interpreted as a JSON object
   * @param mqttMsgStr  The received MQTT message interpreted as a String
   * @return The "temp" attribute value in the MQTT message
   * @throws ErrCodeExcp The "temp" attribute is missing or its value
   *                     could not be interpreted as an integer
   */
  private int getSensorTempReading(JSONObject mqttMsgJSON,String mqttMsgStr) throws  ErrCodeExcp
   {
    // Ascertain the received MQTT message to contain the
    // required "temp" attribute, throwing an exception otherwise
    if(!mqttMsgJSON.has("temp"))
     throw new ErrCodeExcp(ERR_MQTT_MSG_TEMP_MISSING,
                   "(\"" + mqttMsgStr + "\")");

    // Attempt to extract the "temp" attribute as
    // an integer, throwing an exception otherwise
    try
     { return mqttMsgJSON.getInt("temp"); }
    catch(JSONException jsonExcp)
     { throw new ErrCodeExcp(ERR_MQTT_MSG_TEMP_NOT_INT,
                     "(\"" + mqttMsgStr + "\")"); }
   }


  /* ============================= PUBLIC METHODS ============================= */

  /**
   * SensorsMQTTHandler constructor, initializing the PAHO MQTT Client
   * module and attempting to connect with the local MQTT broker
   * @param mqttCliID   The MQTT Client ID to be used for
   *                    registering on the local MQTT broker
   * @param sensorsList The list of sensors to be managed by the handler
   */
  public SensorsMQTTHandler(String mqttCliID,ArrayList<? extends BaseSensor> sensorsList)
   {
    // Initialize the list of sensors to be managed by the handler
    this.sensorsList = sensorsList;

    /*
     * Attempt to initialize the PAHO MQTT client module,
     *
     * NOTE: Explicitly passing a MemoryPersistence object to the MqttClient()
     *       function suppresses the PAHO illegal reflective access warning
     */
    try
     {
      MQTTClient = new MqttClient(MQTT_BROKER_ENDPOINT,
                                   mqttCliID,new MemoryPersistence());
     }

    // Failing to initialize the PAHO MQTT client module is a FATAL error
    catch(MqttException mqttExcp)
     { Log.code(ERR_MQTT_PAHO_INIT_FAILED,
        "(reason = " + mqttExcp.getMessage() + ")"); }

    // Set the PAHO callback as this object
    MQTTClient.setCallback(this);

    // Log that the Sensor MQTT client handler has been initialized
    Log.dbg("Sensors MQTT client handler initialized");

    // Attempt to connect with the local MQTT broker
    try
     { connectSubscribeMQTTBroker(); }

    // Failing to connect with the local MQTT broker is a FATAL error
    catch(MqttException mqttExcp)
     { Log.code(ERR_MQTT_BROKER_CONN_FAILED,
         "(reason = " + mqttExcp.getMessage() + ")"); }

    // Initialize the sensors' boostrap inactivity timer
    // which, once triggered, properly updates all sensors
    // publications have not been received from as offline
    Timer sensorsOfflineUpdateTimer = new Timer();
    sensorsOfflineUpdateTimer.schedule(new TimerTask()
     {
      public void run()
       {
        // For every managed sensor, if it's still
        // offline, call its setConnStateOffline() method
        sensorsList.forEach((sensor) ->
         {
          if(!sensor.getConnState())
           sensor.setConnStateOffline();
         });

        // Set that the sensorsOfflineUpdateTimer has run
        sensorsOfflineBootstrapTimerHasRun = true;
       }
     },MQTT_CLI_MAX_INACTIVITY);
   }


  /**
   * Attempts to publish an updated "avgFanRelSpeed" value to the local MQTT broker
   * @param newAvgFanRelSpeed The new "avgFanRelSpeed" value to be published
   */
  public void publishAvgFanRelSpeed(int newAvgFanRelSpeed)
   {
    // Ensure the new "avgFanRelSpeed" value to
    // be valid, logging the error otherwise
    if(newAvgFanRelSpeed < 0 || newAvgFanRelSpeed > 100)
     {
      Log.code(ERR_MQTT_AVGFANRELSPEED_VALUE_INVALID,
        "(" + newAvgFanRelSpeed + ")");
      return;
     }

    // Build the MQTT message to be published
    MqttMessage newAvgFanRelSpeedMQTTMsg = new MqttMessage(String.valueOf(newAvgFanRelSpeed).getBytes());

    // Attempt to publish the MQTT message on the "TOPIC_AVG_FAN_REL_SPEED"
    // topic on the MQTT broker, logging the error otherwise
    try
     { MQTTClient.publish(TOPIC_AVG_FAN_REL_SPEED, newAvgFanRelSpeedMQTTMsg); }
    catch(MqttException mqttExcp)
     { Log.code(ERR_MQTT_AVGFANRELSPEED_PUBLISH_FAILED,
       "(reason = " + mqttExcp.getMessage() + ")"); }
   }


  /* -------------------- PAHO MQTT Client Callback Methods -------------------- */

  /**
   * Callback method invoked by the PAHO MQTT client MQTT broker whenever
   * the MQTT broker acknowledges one of its publications (unused)
   * @param token The token associated with the publication that was acknowledged
   */
  public void deliveryComplete(IMqttDeliveryToken token)
   {}

  /**
   * Callback method invoked by the PAHO MQTT client
   * should it lose connection with the local MQTT broker
   * @param cause The reason because the connection was lost
   */
  public void connectionLost(Throwable cause)
   {
    // Log that the connection with the MQTT broker has been lost
    Log.code(ERR_MQTT_BROKER_DISCONNECTED,cause.toString());

    // As an error recovery mechanism, attempt
    // to re-connect with the MQTT broker
    try
     { connectSubscribeMQTTBroker(); }

    // Failing to reconnect with the MQTT broker is a FATAL error
    catch(MqttException mqttExcp)
     { Log.code(ERR_MQTT_BROKER_CONN_FAILED,
        "(reason = " + mqttExcp.getMessage() + ")"); }
   }


  /**
   * Callback method invoked by the PAHO MQTT client whenever a
   * MQTT message on a topic it is subscribed to is received,
   * i.e. a MQTT message from a sensor has been received
   * @param topic   The received MQTT message topic
   * @param mqttMsg The received MQTT message contents
   */
  public void messageArrived(String topic, MqttMessage mqttMsg)
   {
    // The MQTT message interpreted as a
    // JSON object and as a String
    JSONObject mqttMsgJSON;
    String mqttMsgStr;

    // The publisher sensor's attributes
    String sensorMAC;
    BaseSensor sensor;
    short sensorID;

    // Information on a received MQTT error message
    BaseSensorErrCode sensorErrCode;
    SensorMQTTCliState sensorMQTTCliState;
    String sensorErrDscr;

    // The sensor's published quantity (C02 or temperature)
    int recvQuantity;

    try
     {
      // Stringify the received MQTT message
      mqttMsgStr = new String(mqttMsg.getPayload());

      // Attempt to interpret the stringifyed MQTT message
      // as a JSON object, throwing an exception otherwise
      try
       { mqttMsgJSON = new JSONObject(mqttMsgStr); }
      catch(JSONException jsonExcp)
       { throw new ErrCodeExcp(ERR_MQTT_MSG_NOT_JSON,"(\"" + mqttMsgStr + "\")"); }

      // Attempt to extract the required sensor MAC from the MQTT message
      sensorMAC = getSensorMAC(mqttMsgJSON,mqttMsgStr);

      // Retrieve the BaseSensor object associated with the such MAC
      sensor = sensorsList.stream()
               .filter(sensorMac -> sensorMAC.equals(sensorMac.MAC))
               .findFirst()
               .orElse(null);

      // Ensure that a BaseSensor object was
      // found, throwing an exception otherwise
      if(sensor == null)
       throw new ErrCodeExcp(ERR_MQTT_MSG_NO_SENSOR_SUCH_MAC,
                     "(\"" + mqttMsgStr + "\")");

      // Retrieve the sensor's ID and connection status
      sensorID = sensor.ID;

      // Depending on the topic on which the message has been received
      switch(topic)
       {
        /* -------------------------- Sensor Error Message -------------------------- */
        case TOPIC_SENSORS_ERRORS:

         // Attempt to extract the required "errCode"
         // attribute from the MQTT error message
         sensorErrCode = getSensorErrCode(mqttMsgJSON,mqttMsgStr);

         // Attempt to extract the optional "MQTTCliState"
         // attribute from the MQTT error message
         sensorMQTTCliState = getSensorMQTTCliState(mqttMsgJSON,mqttMsgStr);

         // Attempt to extract the optional "errDscr"
         // attribute from the MQTT error message
         sensorErrDscr = getSensorErrDscr(mqttMsgJSON,mqttMsgStr);

         // If the sensor has disconnected
         if(sensorErrCode == BaseSensorErrCode.ERR_SENSOR_MQTT_DISCONNECTED)
          {
           /*
            * If the sensors' bootstrap inactivity timer has not run
            * yet, the method was called upon receiving a sensor's last
            * will disconnection message of a past execution that was
            * retained by the MQTT broker, and that can be ignored
            */
           if(!sensorsOfflineBootstrapTimerHasRun)
            return;

           // Otherwise call the sensor's abstract disconnection handler
           sensor.setConnStateOffline();
          }

         // Otherwise, if the sensor has not disconnected, just log its reported
         // error depending on whether an additional description was provided
         else
          if(sensorErrDscr == null)
           Log.code(sensorErrCode,sensorID,"(MQTT_CLI_STATE = '"
                    + sensorMQTTCliState + "')");
          else
           Log.code(sensorErrCode,sensorID,sensorErrDscr
                    + " (MQTT_CLI_STATE = '" + sensorMQTTCliState + "')");
          break;

        /* --------------------------- Sensor C02 Reading --------------------------- */
        case TOPIC_SENSORS_C02:

         // Attempt to extract the required "C02" attribute from the MQTT message
         recvQuantity = getSensorC02Reading(mqttMsgJSON,mqttMsgStr);

         // Call the sensor's abstract handler for updating its C02 value
         sensor.setC02(recvQuantity);
         break;

        /* ----------------------- Sensor Temperature Reading ----------------------- */
        case TOPIC_SENSORS_TEMP:

         // Attempt to extract the required "temp" attribute from the MQTT message
         recvQuantity = getSensorTempReading(mqttMsgJSON,mqttMsgStr);

         // Call the sensor's abstract handler for updating its temperature value
         sensor.setTemp(recvQuantity);
         break;

        /* ----------------------- Unknown MQTT Message Topic ----------------------- */

        // Throw an exception logging the received MQTT message and its unknown topic
        default:
         throw new ErrCodeExcp(ERR_MQTT_MSG_UNKNOWN_TOPIC,"(topic = \""
                               + topic + "\", message = \"" + mqttMsgStr + "\"");
       }
     }

    // If an error has occurred in parsing
    // the received MQTT message, log it
    catch(ErrCodeExcp errCodeExcp)
     { Log.excp(errCodeExcp);}
   }
 }