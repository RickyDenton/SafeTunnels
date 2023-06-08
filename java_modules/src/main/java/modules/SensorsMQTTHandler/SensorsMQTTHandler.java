package modules.SensorsMQTTHandler;

// Paho imports
import devices.sensor.BaseSensor;
import devices.sensor.BaseSensorErrCode;
import errors.ErrCodeExcp;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import logging.Log;

import static devices.sensor.BaseSensor.*;
import static devices.sensor.BaseSensor.SensorMQTTCliState.MQTT_CLI_STATE_UNKNOWN;
import static modules.SensorsMQTTHandler.SensorsMQTTHandlerErrCode.*;

import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.*;

import java.util.HashMap;


public abstract class SensorsMQTTHandler implements MqttCallback
 {
  // MQTT Broker endpoint
  private final static String MQTT_BROKER_ENDPOINT = "tcp://127.0.0.1:1883";

  // MQTT Handler PAHO Client module
  protected MqttClient MQTTClient;

  // Sensors map
  protected final HashMap<String,BaseSensor> sensorMap;


  // Constructor
  public SensorsMQTTHandler(String mqttCliID,HashMap<String,BaseSensor> sensorMap)
   {
    // Initialize the sensorMap
    this.sensorMap = sensorMap;

    /*
     * Attempt to initialize the PAHO MQTT client module
     *
     * NOTE: Explicitly passing a MemoryPersistence object to the MqttClient()
     *       function suppresses the PAHO illegal reflective access warning
     */
    try
     { MQTTClient = new MqttClient(MQTT_BROKER_ENDPOINT,mqttCliID,new MemoryPersistence()); }
    catch(MqttException mqttExcp)
     { Log.code(ERR_MQTT_PAHO_INIT_FAILED,"(reason = " + mqttExcp.getMessage() + ")"); }

    // Set the PAHO callback as this object
    MQTTClient.setCallback(this);

    Log.dbg("SensorsMQTTHandler MQTT client module initialized");

    // Attempt to connect with the MQTT broker
    try
     { connectMQTTBroker(); }
    catch(MqttException mqttExcp)
     { Log.code(ERR_MQTT_BROKER_CONN_FAILED,"(reason = " + mqttExcp.getMessage() + ")"); }
   }


  public void connectMQTTBroker() throws MqttException
   {
    Log.dbg("Attempting to connect with the MQTT broker @" + MQTT_BROKER_ENDPOINT);

    // Attempt to connect with the MQTT broker
    MQTTClient.connect();

    Log.dbg("Connected with the MQTT broker @" + MQTT_BROKER_ENDPOINT + ", subscribing on topics");

    // Subscribe on topics
    MQTTClient.subscribe(TOPIC_SENSORS_C02);
    MQTTClient.subscribe(TOPIC_SENSORS_TEMP);
    MQTTClient.subscribe(TOPIC_SENSORS_ERRORS);

    Log.dbg("Successfully subscribed on the sensors' topics");
   }


  // Lost connection with the MQTT broker
  public void connectionLost(Throwable cause)
   {
    // Log that connection with the MQTT broker has been lost
    Log.code(ERR_MQTT_BROKER_DISCONNECTED,cause.toString());

    // Attempt to reconnect with the MQTT broker
    try
     { connectMQTTBroker(); }
    catch(MqttException mqttExcp)
     { Log.code(ERR_MQTT_BROKER_CONN_FAILED,"(reason = " + mqttExcp.getMessage() + ")"); }
   }


  // A published MQTT message has been ACKd by the MQTT broker
  public void deliveryComplete(IMqttDeliveryToken token)
   {}

  // Publish an updated "AvgFanRelSpeed" value to the MQTT broker
  public void publishAvgFanRelSpeed(int avgFanRelSpeed)
   {
    MqttMessage message = new MqttMessage(String.valueOf(avgFanRelSpeed).getBytes());

    try
     { MQTTClient.publish(TOPIC_AVG_FAN_REL_SPEED, message); }
    catch(MqttException mqttExcp)
     { Log.code(ERR_MQTT_AVGFANRELSPEED_PUBLISH_FAILED,"(reason = " + mqttExcp.getMessage() + ")"); }
   }


  private String getSensorMAC(JSONObject mqttMsgJSON,String mqttMsgStr) throws  ErrCodeExcp
   {
    String sensorMAC;

    // Check if the received MQTT message contains the sensor "MAC" attribute
    if(!mqttMsgJSON.has("MAC"))
     throw new ErrCodeExcp(ERR_MQTT_MSG_MAC_MISSING,"(\"" + mqttMsgStr + "\")");

    // Attempt to extract the sensor "MAC" attribute
    // as a String and ensure it to be non-null
    try
     { return mqttMsgJSON.getString("MAC"); }
    catch(JSONException jsonExcp)
     { throw new ErrCodeExcp(ERR_MQTT_MSG_MAC_NOT_NONNULL_STRING,"(\"" + mqttMsgStr + "\")"); }
   }


  private BaseSensorErrCode getSensorErrCode(JSONObject mqttMsgJSON,String mqttMsgStr) throws  ErrCodeExcp
   {
    // Check if the received MQTT message contains
    // the required sensor "errCode" attribute
    if(!mqttMsgJSON.has("errCode"))
     throw new ErrCodeExcp(ERR_MQTT_ERR_MSG_ERRCODE_MISSING,"(\"" + mqttMsgStr + "\")");

    // Attempt to interpret the sensor "errCode" attribute as an int
    try
     { return BaseSensorErrCode.values()[mqttMsgJSON.getInt("errCode")]; }
    catch(JSONException jsonExcp)
     { throw new ErrCodeExcp(ERR_MQTT_ERR_MSG_ERRCODE_NOT_INT,"(\"" + mqttMsgStr + "\")"); }
   }

  private SensorMQTTCliState getSensorMQTTCliState(JSONObject mqttMsgJSON,String mqttMsgStr) throws  ErrCodeExcp
   {
    // If the received MQTT message contains the optional sensor "MQTTCliState" attribute
    if(mqttMsgJSON.has("MQTTCliState"))
     {
      // Attempt to extract the sensor "MQTTCliState" attribute as a String
      try
       { return SensorMQTTCliState.values()[mqttMsgJSON.getInt("MQTTCliState")]; }
      catch(JSONException jsonExcp)
       { throw new ErrCodeExcp(ERR_MQTT_ERR_MSG_MQTTCLISTATE_NOT_INT,"(\"" + mqttMsgStr + "\")"); }
     }
    else
     return MQTT_CLI_STATE_UNKNOWN;
   }


  private String getSensorErrDscr(JSONObject mqttMsgJSON,String mqttMsgStr) throws  ErrCodeExcp
   {
    // If the received MQTT message contains the optional sensor "sensorErrDscr" attribute
    if(mqttMsgJSON.has("errDscr"))
     {
      // Attempt to extract the sensor "MAC" attribute as a String
      try
       { return mqttMsgJSON.getString("errDscr"); }
      catch(JSONException jsonExcp)
       { throw new ErrCodeExcp(ERR_MQTT_ERR_MSG_ERRDSCR_NOT_NONNULL_STRING,"(\"" + mqttMsgStr + "\")"); }
     }
    else
     return "";
   }


  private int getSensorC02Reading(JSONObject mqttMsgJSON,String mqttMsgStr) throws  ErrCodeExcp
   {
    // Check if the received MQTT message contains
    // the required sensor "C02" attribute
    if(!mqttMsgJSON.has("C02"))
     throw new ErrCodeExcp(ERR_MQTT_MSG_C02_MISSING,"(\"" + mqttMsgStr + "\")");

    // Attempt to interpret the "C02" attribute as an int
    try
     { return mqttMsgJSON.getInt("C02"); }
    catch(JSONException jsonExcp)
     { throw new ErrCodeExcp(ERR_MQTT_MSG_C02_NOT_INT,"(\"" + mqttMsgStr + "\")"); }
   }

  private int getSensorTempReading(JSONObject mqttMsgJSON,String mqttMsgStr) throws  ErrCodeExcp
   {
    // Check if the received MQTT message contains
    // the required sensor "temp" attribute
    if(!mqttMsgJSON.has("temp"))
     throw new ErrCodeExcp(ERR_MQTT_MSG_TEMP_MISSING,"(\"" + mqttMsgStr + "\")");

    // Attempt to interpret the "temp" attribute as an int
    try
     { return mqttMsgJSON.getInt("temp"); }
    catch(JSONException jsonExcp)
     { throw new ErrCodeExcp(ERR_MQTT_MSG_TEMP_NOT_INT,"(\"" + mqttMsgStr + "\")"); }
   }


  public void messageArrived(String topic, MqttMessage mqttMsg)
   {
    // MQTT message
    String mqttMsgStr;
    JSONObject mqttMsgJSON;

    // Sensors identifiers
    String sensorMAC;
    BaseSensor sensor;
    short sensorID;
    boolean sensorConnStatus;

    // Sensor Errors information
    BaseSensorErrCode sensorErrCode;
    SensorMQTTCliState sensorMQTTCliState;
    String sensorErrDscr;

    // Received quantity (C02 or temperature)
    int recvQuantity;

    try
     {
      // Stringify the received MQTT message
      mqttMsgStr = new String(mqttMsg.getPayload());

      // Attempt to interpret the stringifyed MQTT message as a JSON object
      try
       { mqttMsgJSON = new JSONObject(mqttMsgStr); }
      catch(JSONException jsonExcp)
       { throw new ErrCodeExcp(ERR_MQTT_MSG_NOT_JSON,"(\"" + mqttMsgStr + "\")"); }

      // Attempt to extract the required sensor MAC from the MQTT message
      sensorMAC = getSensorMAC(mqttMsgJSON,mqttMsgStr);

      // Retrieve the BaseSensor object associated
      // with the MAC and ensure it to be non-null
      sensor = sensorMap.get(sensorMAC);
      if(sensor == null)
       throw new ErrCodeExcp(ERR_MQTT_MSG_NO_SENSOR_SUCH_MAC,"(\"" + mqttMsgStr + "\")");

      // Retrieve the sensor's ID and connection status
      sensorID = sensor.ID;
      sensorConnStatus = sensor.connState;

      // Depending on the topic on which the message has been received
      switch(topic)
       {
        // A sensor error message has been received
        case TOPIC_SENSORS_ERRORS:
         // Attempt to extract the required "errCode" attribute from the MQTT error message
         sensorErrCode = getSensorErrCode(mqttMsgJSON,mqttMsgStr);

         // Attempt to extract the optional "MQTTCliState" attribute from the MQTT error message
         sensorMQTTCliState = getSensorMQTTCliState(mqttMsgJSON,mqttMsgStr);

         // Attempt to extract the optional "errDscr" attribute from the MQTT error message
         sensorErrDscr = getSensorErrDscr(mqttMsgJSON,mqttMsgStr);

         // If the sensor has disconnected
         if(sensorErrCode == BaseSensorErrCode.ERR_SENSOR_MQTT_DISCONNECTED)
          {
           // Update the sensor connection status
           sensor.connState = false;

           // Invoke the sensor disconnection abstract handler
           handleSensorDisconnect(sensorID);
          }

         // Otherwise, just log the reported sensor error
         else
          Log.code(sensorErrCode,sensorID,sensorErrDscr + " (MQTT_CLI_STATE = '" + sensorMQTTCliState + "')");
         break;

        // A sensor CO2 density reading has been received
        case TOPIC_SENSORS_C02:

         // Attempt to extract the required "C02" attribute from the MQTT message
         recvQuantity = getSensorC02Reading(mqttMsgJSON,mqttMsgStr);

         // If the sensor was offline
         if(!sensorConnStatus)
          {
           // Update the sensor connection status
           sensor.connState = true;

           // Invoke the sensor connection abstract handler
           handleSensorConnect(sensorID);
          }

         // Pass the information on the C02 sensor reading to the virtual handler
         handleSensorC02Reading(sensorID,recvQuantity);
         break;

        // A sensor temperature reading has been received
        case TOPIC_SENSORS_TEMP:

         // Attempt to extract the required "temp" attribute from the MQTT message
         recvQuantity = getSensorTempReading(mqttMsgJSON,mqttMsgStr);

         // If the sensor was offline
         if(!sensorConnStatus)
          {
           // Update the sensor connection status
           sensor.connState = true;

           // Invoke the sensor connection abstract handler
           handleSensorConnect(sensorID);
          }

         // Pass the information on the temperature sensor reading to the virtual handler
         handleSensorTempReading(sensorID,recvQuantity);
         break;

        default:
         throw new ErrCodeExcp(ERR_MQTT_MSG_UNKNOWN_TOPIC,"(topic = \"" + topic + "\", message = \"" + mqttMsgStr + "\"");
       }
     }
    catch(ErrCodeExcp errCodeExcp)
     { Log.excp(errCodeExcp);}
   }

  /* ---- Abstract methods ---- */

  // Handle sensor connection
  protected abstract void handleSensorConnect(int sensorID);

  // Handle sensor disconnection
  protected abstract void handleSensorDisconnect(int sensorID);

  // Handle C02 reading
  protected abstract void handleSensorC02Reading(int sensorID,int newC02);

  // Handle temperature reading
  protected abstract void handleSensorTempReading(int sensorID,int newTemp);
 }