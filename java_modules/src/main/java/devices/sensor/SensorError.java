package devices.sensor;

public enum SensorError
 {
  // ---------------------------------- Connectivity Errors ----------------------------------

  // The sensor has disconnected from the MQTT broker
  //
  // NOTE: This error code is published automatically by the MQTT broker on the TOPIC_SENSORS_ERROR as the sensor's "last will"
  //
  ERR_SENSOR_MQTT_DISCONNECTED  { @Override public String toString() { return "NO_ERROR"; } },

  // The sensor failed to publish a sampled quantity (C02 or temperature) to the MQTT broker
  ERR_SENSOR_PUB_QUANTITY_FAILED       { @Override public String toString() { return "\"lightState\" variable missing from a light PUT request"; } },

  // -------------------------- Invalid MQTT Publications Reception --------------------------

  // The sensor received a MQTT message on a topic it is not subscribed to
  ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC  { @Override public String toString() { return "Invalid \"lightState\" value received in a light PUT request"; } },

  // The sensor failed to subscribe on the TOPIC_AVG_FAN_REL_SPEED topic
  ERR_SENSOR_SUB_AVGFANRELSPEED_FAILED { @Override public String toString() { return "\"fanRelSpeed\" variable missing from a fan PUT request"; } },

  // The sensor received the publication of an invalid "avgFanRelSpeed" value (not [0,100])
  ERR_SENSOR_RECV_INVALID_AVGFANRELSPEED   { @Override public String toString() { return "Invalid \"fanRelSpeed\" value received in a fan PUT request"; } },

  // ------------------------------ Invalid Application States ------------------------------

  // The sensor established a connection with the MQTT broker when not in the 'MQTT_CLI_STATE_BROKER_CONNECTING' state
  ERR_SENSOR_MQTT_CONNECTED_NOT_CONNECTING  { @Override public String toString() { return "Invalid \"lightState\" value received in a light PUT request"; } },

  // The MQTT engine invoked a callback of unknown type
  ERR_SENSOR_MQTT_ENGINE_UNKNOWN_CALLBACK_TYPE { @Override public String toString() { return "\"fanRelSpeed\" variable missing from a fan PUT request"; } },

  // Unknown MQTT client state in the sensor process main loop
  ERR_SENSOR_MAIN_LOOP_UNKNOWN_MQTT_CLI_STATE   { @Override public String toString() { return "Invalid \"fanRelSpeed\" value received in a fan PUT request"; } },

  // The sensor process has exited from its main loop
  ERR_SENSOR_MAIN_LOOP_EXITED   { @Override public String toString() { return "Invalid \"fanRelSpeed\" value received in a fan PUT request"; } }
 }