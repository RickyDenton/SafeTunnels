package modules.SensorsMQTTHandler;

import errors.ErrCodeInfo;
import errors.ModuleErrCode;

import java.util.EnumMap;
import java.util.Map;

import static errors.ErrCodeSeverity.ERROR;
import static errors.ErrCodeSeverity.FATAL;


public enum SensorsMQTTHandlerErrCode implements ModuleErrCode
 {
  /* =========================== Enumeration Values Definition =========================== */

  /* ---------------------------- MQTT Initialization Errors ---------------------------- */

  // Failed to initialize the PAHO MQTT client
  ERR_MQTT_PAHO_INIT_FAILED,

  // Failed to connect with the MQTT broker
  ERR_MQTT_BROKER_CONN_FAILED,

  // Disconnected from the MQTT broker
  ERR_MQTT_BROKER_DISCONNECTED,

  /* -------------------- Received MQTT Message General Errors Codes -------------------- */

  // A received MQTT message could not be interpreted in JSON format
  ERR_MQTT_MSG_NOT_JSON,

  // A received MQTT message lacks the sensor "MAC" attribute
  ERR_MQTT_MSG_MAC_MISSING,

  // The "MAC" attribute in a received MQTT message could not be interpreted as a non-null string
  ERR_MQTT_MSG_MAC_NOT_NONNULL_STRING,

  // The "MAC" attribute in a received MQTT message is associated with no sensor stored in the database
  ERR_MQTT_MSG_NO_SENSOR_SUCH_MAC,

  // A MQTT message of unknown topic was received
  ERR_MQTT_MSG_UNKNOWN_TOPIC,

  /* --------------------- Received MQTT Error Message Errors Codes --------------------- */

  // A received MQTT error message lacks the sensor "errCode" attribute
  ERR_MQTT_ERR_MSG_ERRCODE_MISSING,

  // The "errCode" attribute in a received MQTT error message could not be interpreted as an integer
  ERR_MQTT_ERR_MSG_ERRCODE_NOT_INT,

  // A received MQTT error message does not contain the sensor "MQTTCliState" attribute
  ERR_MQTT_ERR_MSG_MQTTCLISTATE_MISSING,

  // The "MQTTCliState" attribute in a received MQTT error message could not be interpreted as an integer
  ERR_MQTT_ERR_MSG_MQTTCLISTATE_NOT_INT,

  // The "MQTTCliState" attribute in a received MQTT error message could not be mapped to a valid sensor MQTT client state
  ERR_MQTT_ERR_MSG_MQTTCLISTATE_UNKNOWN,

  // The "errDscr" attribute specified in a received MQTT error message could not be interpreted as a non-null String
  ERR_MQTT_ERR_MSG_ERRDSCR_NOT_NONNULL_STRING,

  /* ---------------- Received MQTT Quantity Reading Message Errors Codes --------------- */

  // A received MQTT C02 reading does not contain the "C02" attribute
  ERR_MQTT_MSG_C02_MISSING,

  // The "C02" attribute in a MQTT C02 reading could not be interpreted as an integer
  ERR_MQTT_MSG_C02_NOT_INT,

  // A received MQTT temperature reading does not contain the "temp" attribute
  ERR_MQTT_MSG_TEMP_MISSING,

  // The "temp" attribute in a MQTT temperature reading could not be interpreted as an integer
  ERR_MQTT_MSG_TEMP_NOT_INT,

  /* ------------------------------ MQTT Publishing Errors ------------------------------ */

  // Attempting to publish an invalid average fan relative speed
  ERR_MQTT_AVGFANRELSPEED_VALUE_INVALID,

  // An error occurred in publishing an average fan relative speed value
  ERR_MQTT_AVGFANRELSPEED_PUBLISH_FAILED;


  /* ===================== SensorsMQTTHandlerErrCode ErrCodeInfo Map ===================== */

  private static final EnumMap<SensorsMQTTHandlerErrCode,ErrCodeInfo> sensorsErrorsInfoMap = new EnumMap<>(Map.ofEntries
   (
    /* -------------------------- MQTT Initialization Errors -------------------------- */
    Map.entry(ERR_MQTT_PAHO_INIT_FAILED,new ErrCodeInfo(FATAL,"Failed to initialize the PAHO MQTT client")),
    Map.entry(ERR_MQTT_BROKER_CONN_FAILED,new ErrCodeInfo(FATAL,"Failed to connect with the MQTT broker")),
    Map.entry(ERR_MQTT_BROKER_DISCONNECTED,new ErrCodeInfo(ERROR,"The MQTT Handler has disconnected from the MQTT broker, attempting reconnection...")),

    /* ------------------ Received MQTT Message General Errors Codes ------------------ */
    Map.entry(ERR_MQTT_MSG_NOT_JSON,new ErrCodeInfo(ERROR,"A received MQTT message could not be interpreted in JSON format")),
    Map.entry(ERR_MQTT_MSG_MAC_MISSING,new ErrCodeInfo(ERROR,"A received MQTT message lacks the sensor \"MAC\" attribute")),
    Map.entry(ERR_MQTT_MSG_MAC_NOT_NONNULL_STRING,new ErrCodeInfo(ERROR,"The \"MAC\" attribute in a received MQTT message could not be interpreted as a non-null string")),
    Map.entry(ERR_MQTT_MSG_NO_SENSOR_SUCH_MAC,new ErrCodeInfo(ERROR,"The \"MAC\" attribute in a received MQTT message is associated with no sensor stored in the database")),
    Map.entry(ERR_MQTT_MSG_UNKNOWN_TOPIC,new ErrCodeInfo(ERROR,"A MQTT message of unknown topic was received")),

    /* ------------------- Received MQTT Error Message Errors Codes ------------------- */
    Map.entry(ERR_MQTT_ERR_MSG_ERRCODE_MISSING,new ErrCodeInfo(ERROR,"A received MQTT error message lacks the sensor \"errCode\" attribute")),
    Map.entry(ERR_MQTT_ERR_MSG_ERRCODE_NOT_INT,new ErrCodeInfo(ERROR,"The \"errCode\" attribute in a received MQTT error message could not be interpreted as an integer")),
    Map.entry(ERR_MQTT_ERR_MSG_MQTTCLISTATE_MISSING,new ErrCodeInfo(ERROR,"A received MQTT error message does not contain the sensor \"MQTTCliState\" attribute")),
    Map.entry(ERR_MQTT_ERR_MSG_MQTTCLISTATE_NOT_INT,new ErrCodeInfo(ERROR,"The \"MQTTCliState\" attribute in a received MQTT error message could not be interpreted as an integer")),
    Map.entry(ERR_MQTT_ERR_MSG_MQTTCLISTATE_UNKNOWN,new ErrCodeInfo(ERROR,"The \"MQTTCliState\" attribute in a received MQTT error message could not be mapped to a valid sensor MQTT client state")),
    Map.entry(ERR_MQTT_ERR_MSG_ERRDSCR_NOT_NONNULL_STRING,new ErrCodeInfo(ERROR,"The \"errDscr\" attribute specified in a received MQTT error message could not be interpreted as a non-null String")),

    /* -------------- Received MQTT Quantity Reading Message Errors Codes ------------- */
    Map.entry(ERR_MQTT_MSG_C02_MISSING,new ErrCodeInfo(ERROR,"A received MQTT C02 reading does not contain the \"C02\" attribute")),
    Map.entry(ERR_MQTT_MSG_C02_NOT_INT,new ErrCodeInfo(ERROR,"The \"C02\" attribute in a MQTT C02 reading could not be interpreted as an integer")),
    Map.entry(ERR_MQTT_MSG_TEMP_MISSING,new ErrCodeInfo(ERROR,"A received MQTT temperature reading does not contain the \"temp\" attribute")),
    Map.entry(ERR_MQTT_MSG_TEMP_NOT_INT,new ErrCodeInfo(ERROR,"The \"temp\" attribute in a MQTT temperature reading could not be interpreted as an integer")),

    /* ------------------------------ MQTT Publishing Errors ------------------------------ */
     Map.entry(ERR_MQTT_AVGFANRELSPEED_VALUE_INVALID,new ErrCodeInfo(ERROR,"Attempting to publish an invalid average fan relative speed")),
     Map.entry(ERR_MQTT_AVGFANRELSPEED_PUBLISH_FAILED,new ErrCodeInfo(ERROR,"An error occurred in publishing an average fan relative speed value"))
   ));

  /* ================================ Enumeration Methods  ================================ */

  public ErrCodeInfo getErrCodeInfo()
   { return sensorsErrorsInfoMap.get(this); }

  public String getModuleName()
   { return "SensorsMQTTHandler"; }
 }