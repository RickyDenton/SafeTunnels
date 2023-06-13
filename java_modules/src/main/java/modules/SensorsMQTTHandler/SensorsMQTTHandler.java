package modules.SensorsMQTTHandler;

// Paho imports

import devices.sensor.BaseSensor;
import devices.sensor.BaseSensorErrCode;
import errors.ErrCodeExcp;
import logging.Log;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static devices.sensor.BaseSensor.*;
import static devices.sensor.BaseSensor.SensorMQTTCliState.MQTT_CLI_STATE_UNKNOWN;
import static modules.SensorsMQTTHandler.SensorsMQTTHandlerErrCode.*;


public class SensorsMQTTHandler implements MqttCallback
 {
  // MQTT Broker endpoint
  private final static String MQTT_BROKER_ENDPOINT = "tcp://127.0.0.1:1883";

  // MQTT Handler PAHO Client module
  protected MqttClient MQTTClient;

  // Sensors list
  ArrayList<? extends BaseSensor> sensorsList;

  // The estimated maximum sensor MQTT inactivity in seconds
  // for tuning the sensors' boostrap inactivity timer
  // which, once triggered, properly updates all sensors
  // publications have not been received from as offline
  private final static int MQTT_CLI_MAX_INACTIVITY = 50;


  // Whether the sensor offline bootstrap timer has run
  public boolean sensorsOfflineBootstrapTimerHasRun;

  // Constructor
  public SensorsMQTTHandler(String mqttCliID,ArrayList<? extends BaseSensor> sensorsList)
   {
    // Initialize the sensorList reference
    this.sensorsList = sensorsList;

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

    Log.dbg("Sensors MQTT client module initialized");

    // Attempt to connect with the MQTT broker
    try
     { connectMQTTBroker(); }
    catch(MqttException mqttExcp)
     { Log.code(ERR_MQTT_BROKER_CONN_FAILED,"(reason = " + mqttExcp.getMessage() + ")"); }

    // Initialize the sensors' boostrap inactivity timer
    // which, once triggered, properly updates all sensors
    // publications have not been received from as offline
    Timer sensorsOfflineUpdateTimer = new Timer();
    sensorsOfflineUpdateTimer.schedule(new TimerTask()
     {
      public void run()
       {
        sensorsList.forEach((sensor) ->
         {
          if(!sensor.getConnState())
           sensor.setConnStateOffline();
         });

        // Set that the timer has run
        sensorsOfflineBootstrapTimerHasRun = true;
       }
     },MQTT_CLI_MAX_INACTIVITY * 1000); // In milliseconds
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

  // Attempts to publish an updated "AvgFanRelSpeed" value to the MQTT broker
  public void publishAvgFanRelSpeed(int newAvgFanRelSpeed)
   {
    // Ensure the avgFanRelSpeed value to be valid
    if(newAvgFanRelSpeed < 0 || newAvgFanRelSpeed > 100)
     {
      Log.code(ERR_MQTT_AVGFANRELSPEED_VALUE_INVALID,"(" + newAvgFanRelSpeed + ")");
      return;
     }

    // Create the message to be published
    MqttMessage newAvgFanRelSpeedMQTTMsg = new MqttMessage(String.valueOf(newAvgFanRelSpeed).getBytes());

    // Attempt to publish the message on the "TOPIC_AVG_FAN_REL_SPEED" topic
    try
     { MQTTClient.publish(TOPIC_AVG_FAN_REL_SPEED, newAvgFanRelSpeedMQTTMsg); }
    catch(MqttException mqttExcp)
     { Log.code(ERR_MQTT_AVGFANRELSPEED_PUBLISH_FAILED,"(reason = " + mqttExcp.getMessage() + ")"); }
   }


  private String getSensorMAC(JSONObject mqttMsgJSON,String mqttMsgStr) throws  ErrCodeExcp
   {
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

    try
     {
      // The MQTT message "errCode" mapped into a BaseSensorErrCode
      BaseSensorErrCode sensorErrCode;

      // Attempt to interpret the MQTT message's "errCode" attribute as an integer and map it into
      // a BaseSensorErrCode, throwing an exception if no BaseSensorErrCode of such index exists
      try
       { sensorErrCode = BaseSensorErrCode.values[mqttMsgJSON.getInt("errCode")]; }
      catch(ArrayIndexOutOfBoundsException outOfBoundsException)
       { throw new ErrCodeExcp(ERR_MQTT_ERR_MSG_ERRCODE_UNKNOWN,"(\"" + mqttMsgStr + "\")"); }

      // Return the valid sensor error code
      return sensorErrCode;
     }
    catch(JSONException jsonExcp)
     { throw new ErrCodeExcp(ERR_MQTT_ERR_MSG_ERRCODE_NOT_INT,"(\"" + mqttMsgStr + "\")"); }
   }

  private SensorMQTTCliState getSensorMQTTCliState(JSONObject mqttMsgJSON,String mqttMsgStr) throws  ErrCodeExcp
   {
    // If the received MQTT message contains the optional sensor "MQTTCliState" attribute
    if(mqttMsgJSON.has("MQTTCliState"))
     {
      try
       {
        // The MQTT message "MQTTCliState" mapped into a SensorMQTTCliState
        SensorMQTTCliState sensorMQTTCliState;

        // Attempt to interpret the MQTT message's "MQTTCliState" attribute as an integer and map it into
        // a SensorMQTTCliState, throwing an exception if no SensorMQTTCliState of such index exists
        try
         { sensorMQTTCliState = SensorMQTTCliState.values()[mqttMsgJSON.getInt("MQTTCliState")]; }
        catch(ArrayIndexOutOfBoundsException outOfBoundsException)
         { throw new ErrCodeExcp(ERR_MQTT_ERR_MSG_MQTTCLISTATE_UNKNOWN,"(\"" + mqttMsgStr + "\")"); }

        // Return the valid sensor's MQTT client state
        return sensorMQTTCliState;
       }
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

    // Otherwise, return null
    else
     return null;
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

      // Retrieve the BaseSensor object associated with the MAC
      sensor = sensorsList.stream()
               .filter(sensorMac -> sensorMAC.equals(sensorMac.MAC))
               .findFirst()
               .orElse(null);

      // Ensure that a BaseSensor object was found
      if(sensor == null)
       throw new ErrCodeExcp(ERR_MQTT_MSG_NO_SENSOR_SUCH_MAC,"(\"" + mqttMsgStr + "\")");

      // Retrieve the sensor's ID and connection status
      sensorID = sensor.ID;
      
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
         
         // If the sensor has disconnected,
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

         // Otherwise, just log the reported sensor error depending
         // on whether an additional description was provided
         else
          if(sensorErrDscr == null)
           Log.code(sensorErrCode,sensorID,"(MQTT_CLI_STATE = '" + sensorMQTTCliState + "')");
          else
           Log.code(sensorErrCode,sensorID,sensorErrDscr + " (MQTT_CLI_STATE = '" + sensorMQTTCliState + "')");
         break;

        // A sensor CO2 density reading has been received
        case TOPIC_SENSORS_C02:

         // Attempt to extract the required "C02" attribute from the MQTT message
         recvQuantity = getSensorC02Reading(mqttMsgJSON,mqttMsgStr);
         
         // Call the sensor's setC02 abstract handler
         sensor.setC02(recvQuantity);
         break;

        // A sensor temperature reading has been received
        case TOPIC_SENSORS_TEMP:

         // Attempt to extract the required "temp" attribute from the MQTT message
         recvQuantity = getSensorTempReading(mqttMsgJSON,mqttMsgStr);

         // Call the sensor's temperature abstract handler
         sensor.setTemp(recvQuantity);
         break;

        default:
         throw new ErrCodeExcp(ERR_MQTT_MSG_UNKNOWN_TOPIC,"(topic = \"" + topic + "\", message = \"" + mqttMsgStr + "\"");
       }
     }
    catch(ErrCodeExcp errCodeExcp)
     { Log.excp(errCodeExcp);}
   }
 }