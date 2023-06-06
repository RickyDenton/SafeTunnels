package modules.SensorsMQTTHandler;

import errors.ErrCodeInfo;
import errors.ModuleErrCode;

import java.util.EnumMap;
import java.util.Map;

import static errors.ErrCodeSeverity.ERROR;


public enum SensorsMQTTHandlerErrCode implements ModuleErrCode
 {
  /* =========================== Enumeration Values Definition =========================== */

  /* -------------------------- General MQTT Broker Interaction -------------------------- */

  // Disconnected from the MQTT broker
  ERR_MQTT_BROKER_DISCONNECTED,

  // Failed to reconnect with the MQTT broker
  ERR_MQTT_BROKER_RECONN_FAILED,

  // An error occurred in publishing an average fan relative speed value
  ERR_MQTT_AVGFANRELSPEED_PUBLISH_FAILED,

  /* -------------------- Received MQTT Message General Errors Codes -------------------- */

  // A received MQTT message could not be interpreted in JSON format
  ERR_MQTT_MSG_NOT_JSON,

  // A received MQTT message does not contain the sensor "MAC" attribute
  ERR_MQTT_MSG_MAC_MISSING,

  // The "MAC" attribute in a received MQTT message could not be interpreted as a String
  ERR_MQTT_MSG_MAC_NOT_STRING,

  // The "MAC" attribute in a received MQTT message is null
  ERR_MQTT_MSG_MAC_NULL_STRING,

  // A MQTT message of unknown topic was received
  ERR_MQTT_MSG_UNKNOWN_TOPIC,

  /* --------------------- Received MQTT Error Message Errors Codes --------------------- */

  // A received MQTT error message does not contain the sensor "errCode" attribute
  ERR_MQTT_ERR_MSG_ERRCODE_MISSING,

  // The "errCode" attribute in a received MQTT error message could not be interpreted as an int
  ERR_MQTT_ERR_MSG_ERRCODE_NOT_INT,

  // A received MQTT error message does not contain the sensor "MQTTCliState" attribute
  ERR_MQTT_ERR_MSG_MQTTCLISTATE_MISSING,

  // The "MQTTCliState" attribute in a received MQTT error message could not be interpreted as an int
  ERR_MQTT_ERR_MSG_MQTTCLISTATE_NOT_INT,

  // The "errDscr" attribute in a received MQTT error message could not be interpreted as a String
  ERR_MQTT_ERR_MSG_ERRDSCR_NOT_STRING,

  /* ---------------- Received MQTT Quantity Reading Message Errors Codes --------------- */

  // A received MQTT C02 reading does not contain the "C02" attribute
  ERR_MQTT_MSG_C02_MISSING,

  // The "C02" attribute in a MQTT C02 reading could not be interpreted as an int
  ERR_MQTT_MSG_C02_NOT_INT,

  // A received MQTT temperature reading does not contain the "temp" attribute
  ERR_MQTT_MSG_TEMP_MISSING,

  // The "temp" attribute in a MQTT temperature reading could not be interpreted as an int
  ERR_MQTT_MSG_TEMP_NOT_INT;

  /* ===================== SensorsMQTTHandlerErrCode ErrCodeInfo Map ===================== */

  private static final EnumMap<SensorsMQTTHandlerErrCode,ErrCodeInfo> sensorsErrorsInfoMap = new EnumMap<>(Map.ofEntries
   (
    /* ------------------------ General MQTT Broker Interaction ------------------------ */
    Map.entry(ERR_MQTT_BROKER_DISCONNECTED,new ErrCodeInfo(ERROR,"The MQTT Handler has disconnected from the MQTT broker, attempting reconnection...")),
     Map.entry(ERR_MQTT_BROKER_RECONN_FAILED,new ErrCodeInfo(ERROR,"MQTT Broker reconnection attempt failed, please restart the application to restore MQTT connectivity")),
    Map.entry(ERR_MQTT_AVGFANRELSPEED_PUBLISH_FAILED,new ErrCodeInfo(ERROR,"An error occurred in publishing an average fan relative speed value")),

    /* ------------------ Received MQTT Message General Errors Codes ------------------ */
    Map.entry(ERR_MQTT_MSG_NOT_JSON,new ErrCodeInfo(ERROR,"A received MQTT message could not be interpreted in JSON format")),
    Map.entry(ERR_MQTT_MSG_MAC_MISSING,new ErrCodeInfo(ERROR,"A received MQTT message does not contain the sensor \"MAC\" attribute")),
    Map.entry(ERR_MQTT_MSG_MAC_NOT_STRING,new ErrCodeInfo(ERROR,"The \"MAC\" attribute in a received MQTT message could not be interpreted as a String")),
    Map.entry(ERR_MQTT_MSG_MAC_NULL_STRING,new ErrCodeInfo(ERROR,"The \"MAC\" attribute in a received MQTT message is null")),
    Map.entry(ERR_MQTT_MSG_UNKNOWN_TOPIC,new ErrCodeInfo(ERROR,"A MQTT message of unknown topic was received")),

    /* ------------------- Received MQTT Error Message Errors Codes ------------------- */
    Map.entry(ERR_MQTT_ERR_MSG_ERRCODE_MISSING,new ErrCodeInfo(ERROR,"A received MQTT error message does not contain the sensor \"errCode\" attribute")),
    Map.entry(ERR_MQTT_ERR_MSG_ERRCODE_NOT_INT,new ErrCodeInfo(ERROR,"The \"errCode\" attribute in a received MQTT error message could not be interpreted as an int")),
    Map.entry(ERR_MQTT_ERR_MSG_MQTTCLISTATE_MISSING,new ErrCodeInfo(ERROR,"A received MQTT error message does not contain the sensor \"MQTTCliState\" attribute")),
    Map.entry(ERR_MQTT_ERR_MSG_MQTTCLISTATE_NOT_INT,new ErrCodeInfo(ERROR,"The \"MQTTCliState\" attribute in a received MQTT error message could not be interpreted as an int")),
    Map.entry(ERR_MQTT_ERR_MSG_ERRDSCR_NOT_STRING,new ErrCodeInfo(ERROR,"The \"errDscr\" attribute in a received MQTT error message could not be interpreted as a String")),

    /* -------------- Received MQTT Quantity Reading Message Errors Codes ------------- */
    Map.entry(ERR_MQTT_MSG_C02_MISSING,new ErrCodeInfo(ERROR,"A received MQTT C02 reading does not contain the \"C02\" attribute")),
    Map.entry(ERR_MQTT_MSG_C02_NOT_INT,new ErrCodeInfo(ERROR,"The \"C02\" attribute in a MQTT C02 reading could not be interpreted as an int")),
    Map.entry(ERR_MQTT_MSG_TEMP_MISSING,new ErrCodeInfo(ERROR,"A received MQTT temperature reading does not contain the \"temp\" attribute")),
    Map.entry(ERR_MQTT_MSG_TEMP_NOT_INT,new ErrCodeInfo(ERROR,"The \"temp\" attribute in a MQTT temperature reading could not be interpreted as an int"))
   ));

  /* ================================ Enumeration Methods  ================================ */

  public ErrCodeInfo getErrCodeInfo()
   { return sensorsErrorsInfoMap.get(this); }

  public String getModuleName()
   { return "SensorsMQTTHandler"; }
 }