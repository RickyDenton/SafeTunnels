package devices.sensor;

import devices.Device;
import devices.DeviceErrCode;
import logging.errors.ErrCodeInfo;

import java.util.EnumMap;
import java.util.Map;

import static logging.errors.ErrCodeSeverity.WARNING;


public class BaseSensor implements Device
 {
  public final String deviceID;

  enum SensorErrCode implements DeviceErrCode
   {
    // ---------------------------------- Connectivity Errors ----------------------------------

    // The sensor has disconnected from the MQTT broker
    //
    // NOTE: This error code is published automatically by the MQTT broker on the TOPIC_SENSORS_ERROR as the sensor's "last will"
    //
    ERR_SENSOR_MQTT_DISCONNECTED,

    // The sensor failed to publish a sampled quantity (C02 or temperature) to the MQTT broker
    ERR_SENSOR_PUB_QUANTITY_FAILED,

    // -------------------------- Invalid MQTT Publications Reception --------------------------

    // The sensor received a MQTT message on a topic it is not subscribed to
    ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC,

    // The sensor failed to subscribe on the TOPIC_AVG_FAN_REL_SPEED topic
    ERR_SENSOR_SUB_AVGFANRELSPEED_FAILED,

    // The sensor received the publication of an invalid "avgFanRelSpeed" value (not [0,100])
    ERR_SENSOR_RECV_INVALID_AVGFANRELSPEED,

    // ------------------------------ Invalid Application States ------------------------------

    // The sensor established a connection with the MQTT broker when not in the 'MQTT_CLI_STATE_BROKER_CONNECTING' state
    ERR_SENSOR_MQTT_CONNECTED_NOT_CONNECTING,

    // The MQTT engine invoked a callback of unknown type
    ERR_SENSOR_MQTT_ENGINE_UNKNOWN_CALLBACK_TYPE,

    // Unknown MQTT client state in the sensor process main loop
    ERR_SENSOR_MAIN_LOOP_UNKNOWN_MQTT_CLI_STATE,

    // The sensor process has exited from its main loop
    ERR_SENSOR_MAIN_LOOP_EXITED;

    private static final EnumMap<SensorErrCode,ErrCodeInfo> sensorsErrorsInfoMap = new EnumMap<>(Map.ofEntries(
      // ---------------------------------- Connectivity Errors ----------------------------------
      Map.entry(ERR_SENSOR_MQTT_DISCONNECTED,new ErrCodeInfo(WARNING,"The sensor has disconnected from the MQTT broker")),
      Map.entry(ERR_SENSOR_PUB_QUANTITY_FAILED,new ErrCodeInfo(WARNING,"Failed to publish a sampled quantity")),

      // -------------------------- Invalid MQTT Publications Reception --------------------------
      Map.entry(ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC,new ErrCodeInfo(WARNING,"Received a MQTT message on a non-subscribed topic")),
      Map.entry(ERR_SENSOR_SUB_AVGFANRELSPEED_FAILED,new ErrCodeInfo(WARNING,"Failed to subscribe on the \"SafeTunnels/avgFanRelSpeed\" topic")),
      Map.entry(ERR_SENSOR_RECV_INVALID_AVGFANRELSPEED,new ErrCodeInfo(WARNING,"Received an invalid \"avgFanRelSpeed\" value")),

      // ------------------------------ Invalid Application States ------------------------------
      Map.entry(ERR_SENSOR_MQTT_CONNECTED_NOT_CONNECTING,new ErrCodeInfo(WARNING,
        "Established connection with the MQTT broker when not in the 'MQTT_CLI_STATE_BROKER_CONNECTING' state")),
      Map.entry(ERR_SENSOR_MQTT_ENGINE_UNKNOWN_CALLBACK_TYPE,new ErrCodeInfo(WARNING,"Unknown event in the MQTT Engine callback function")),
      Map.entry(ERR_SENSOR_MAIN_LOOP_UNKNOWN_MQTT_CLI_STATE,new ErrCodeInfo(WARNING,"Unknown MQTT client state in the sensor process main loop")),
      Map.entry(ERR_SENSOR_MAIN_LOOP_EXITED,new ErrCodeInfo(WARNING,"Exited from the sensor process main loop"))));

    public ErrCodeInfo getErrCodeInfo()
     {return sensorsErrorsInfoMap.get(this);}
   }

  public BaseSensor(String deviceID)
   {this.deviceID = deviceID;}
 }