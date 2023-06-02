#ifndef SAFETUNNELS_SENSORERRORS_H
#define SAFETUNNELS_SENSORERRORS_H

// Error descriptions buffer size
#define ERR_DSCR_BUF_SIZE 200



/* ========================= SENSOR ERRORS MANAGEMENT ========================= */

/* ---------------------------- Sensor Error Codes ---------------------------- */
enum sensorErrCode
 {
  // ------------------------- Connectivity Errors -------------------------

  // The sensor has disconnected from the MQTT broker
  //
  // NOTE: This error code is also published automatically by the MQTT
  //       broker on the TOPIC_SENSORS_ERROR as the sensor's "last will"
  //
  ERR_SENSOR_MQTT_DISCONNECTED,

  // The sensor failed to publish a sampled quantity
  // (C02 or temperature) to the MQTT broker
  ERR_SENSOR_PUB_QUANTITY_FAILED,

  // ----------------- Invalid MQTT Publications Reception -----------------

  // The sensor received a MQTT message on a topic it is not subscribed to
  ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC,

  // The sensor failed to subscribe on the TOPIC_AVG_FAN_REL_SPEED topic
  ERR_SENSOR_SUB_AVGFANRELSPEED_FAILED,

  // The sensor received the publishment of an
  // invalid "avgFanRelSpeed" value (not [0,100])
  ERR_SENSOR_RECV_INVALID_AVGFANRELSPEED,

  // ---------------------- Invalid Application States ----------------------

  // The sensor established a connection with the MQTT broker
  // when not in the 'MQTT_CLI_STATE_BROKER_CONNECTING' state
  ERR_SENSOR_MQTT_CONNECTED_NOT_CONNECTING,

  // The MQTT engine invoked a callback of unknown type
  ERR_SENSOR_MQTT_ENGINE_UNKNOWN_CALLBACK_TYPE,

  // Unknown MQTT client state in the sensor process main loop
  ERR_SENSOR_MAIN_LOOP_UNKNOWN_MQTT_CLI_STATE,

  // The sensor process exited from its main loop
  ERR_SENSOR_MAIN_LOOP_EXITED
 };


/* --------------------- LOG_PUB_ERROR_ Macros Management --------------------- */

/**
 * LOG_PUB_ERROR_ macros, writing the formatted additional description into the 'errDscr' buffer and invoking the "logPublish" error function with the passes sensorErrCode
 *  - 1 argument    -> reset "errDscr" and call logPublishError(sensorErrCode)
 *  - 2 arguments   -> snprintf in "errDscr" with format string only + call logPublishError(sensorErrCode)
 *  - 3-6 arguments -> snprintf in "errDscr" with format string + 1- parameters + call logPublishError(sensorErrCode)
 */
#define LOG_PUB_CODE_ONLY(sensorErrCode) { errDscr[0] = '\0'; logPublishError(sensorErrCode); }
#define LOG_PUB_ERROR_DSCR(sensorErrCode,dscr) { snprintf(errDscr,ERR_DSCR_BUF_SIZE, dscr); logPublishError(sensorErrCode); }
#define LOG_PUB_ERROR_DSCR_1PARAM(sensorErrCode,dscr,param1) { snprintf(errDscr,ERR_DSCR_BUF_SIZE, dscr, param1); logPublishError(sensorErrCode); }
#define LOG_PUB_ERROR_DSCR_2PARAM(sensorErrCode,dscr,param1,param2) { snprintf(errDscr,ERR_DSCR_BUF_SIZE, dscr, param1, param2); logPublishError(sensorErrCode); }
#define LOG_PUB_ERROR_DSCR_3PARAM(sensorErrCode,dscr,param1,param2,param3) { snprintf(errDscr,ERR_DSCR_BUF_SIZE, dscr, param1, param2, param3); logPublishError(sensorErrCode); }
#define LOG_PUB_ERROR_DSCR_4PARAM(sensorErrCode,dscr,param1,param2,param3,param4) { snprintf(errDscr,ERR_DSCR_BUF_SIZE, dscr, param1, param2, param3, param4); logPublishError(sensorErrCode); }

/**
 * Substitutes the appropriate LOG_PUB_ERROR_ depending on the
 * number of arguments passed to the LOG_PUB_ERROR variadic macro:
 *  - 1 argument    -> sensorErrCode only
 *  - 2 arguments   -> sensorErrCode + additional description (snprintf format string)
 *  - 3-6 arguments -> sensorErrCode + additional description (snprintf format string + 1-4 snprintf params)
 */
#define GET_LOG_PUB_ERROR_MACRO(_1,_2,_3,_4,_5,_6,LOG_PUB_ERROR_MACRO,...) LOG_PUB_ERROR_MACRO
#define LOG_PUB_ERROR(...) GET_LOG_PUB_ERROR_MACRO(__VA_ARGS__,LOG_PUB_ERROR_DSCR_4PARAM,LOG_PUB_ERROR_DSCR_3PARAM,LOG_PUB_ERROR_DSCR_2PARAM,LOG_PUB_ERROR_DSCR_1PARAM,LOG_PUB_ERROR_DSCR,LOG_PUB_CODE_ONLY)(__VA_ARGS__)


char* MQTTEngineAPIResultStr();
void logPublishError(enum sensorErrCode sensorErrCode);

#endif //SAFETUNNELS_SENSORERRORS_H
