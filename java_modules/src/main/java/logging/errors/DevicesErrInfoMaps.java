package logging.errors;

import devices.actuator.ActuatorError;
import devices.sensor.SensorError;

import java.util.Map;

import static java.util.Map.entry;


public class DevicesErrInfoMaps
 {
  static final Map<SensorError,ErrCodeInfo> sensorsErrorsInfoMap = Map.ofEntries
    (
     // ------------------------- Connectivity Errors -------------------------
     entry(SensorError.ERR_SENSOR_MQTT_DISCONNECTED,new ErrCodeInfo(ErrCodeSeverity.ERROR,"The sensor has disconnected from the MQTT broker")),
     entry(SensorError.ERR_SENSOR_PUB_QUANTITY_FAILED,new ErrCodeInfo(ErrCodeSeverity.ERROR,"Failed to publish a sampled quantity")),

     // ----------------- Invalid MQTT Publications Reception -----------------
     entry(SensorError.ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC,new ErrCodeInfo(ErrCodeSeverity.ERROR,"Received a MQTT message on a non-subscribed topic")),
     entry(SensorError.ERR_SENSOR_SUB_AVGFANRELSPEED_FAILED,new ErrCodeInfo(ErrCodeSeverity.ERROR,"Failed to subscribe on the \" TOPIC_AVG_FAN_REL_SPEED \" topic")),
     entry(SensorError.ERR_SENSOR_RECV_INVALID_AVGFANRELSPEED,new ErrCodeInfo(ErrCodeSeverity.ERROR,"Received an invalid \"avgFanRelSpeed\" value")),

     // ---------------------- Invalid Application States ----------------------

     entry(SensorError.ERR_SENSOR_MQTT_CONNECTED_NOT_CONNECTING,new ErrCodeInfo(ErrCodeSeverity.ERROR,"Established connection with the MQTT broker when not in the 'MQTT_CLI_STATE_BROKER_CONNECTING' state")),
     entry(SensorError.ERR_SENSOR_MQTT_ENGINE_UNKNOWN_CALLBACK_TYPE,new ErrCodeInfo(ErrCodeSeverity.ERROR,"Unknown event in the MQTT Engine callback function")),
     entry(SensorError.ERR_SENSOR_MAIN_LOOP_UNKNOWN_MQTT_CLI_STATE,new ErrCodeInfo(ErrCodeSeverity.ERROR,"Unknown MQTT client state in the sensor process main loop")),
     entry(SensorError.ERR_SENSOR_MAIN_LOOP_EXITED,new ErrCodeInfo(ErrCodeSeverity.ERROR,"Exited from the sensor process main loop"))
    );

  static final Map<ActuatorError,ErrCodeInfo> actuatorsErrorsInfoMap = Map.ofEntries
    (
     // No error (this entry should never be used)
     entry(ActuatorError.COAP_REQ_OK,new ErrCodeInfo(ErrCodeSeverity.ERROR,"No actuator error")),

     // ----------------- Light Resource CoAP Requests Application Error Codes -----------------
     entry(ActuatorError.ERR_LIGHT_PUT_NO_LIGHTSTATE,new ErrCodeInfo(ErrCodeSeverity.ERROR,"\"lightState\" variable missing from a light PUT request")),
     entry(ActuatorError.ERR_LIGHT_PUT_LIGHTSTATE_INVALID,new ErrCodeInfo(ErrCodeSeverity.ERROR,"Invalid \"lightState\" value received in a light PUT request")),

     // ------------------ Fan Resource CoAP Requests Application Error Codes ------------------
     entry(ActuatorError.ERR_FAN_PUT_NO_FANRELSPEED,new ErrCodeInfo(ErrCodeSeverity.ERROR,"\"fanRelSpeed\" variable missing from a fan PUT request")),
     entry(ActuatorError.ERR_FAN_PUT_FANRELSPEED_INVALID,new ErrCodeInfo(ErrCodeSeverity.ERROR,"Invalid \"fanRelSpeed\" value received in a fan PUT request"))
    );
 }