#ifndef SAFETUNNELS_SENSOR_H
#define SAFETUNNELS_SENSOR_H

/* SafeTunnels Sensor Default Parameters */


PROCESS_NAME(safetunnels_sensor_process);

/* ------------------------------ MQTT Parameters ------------------------------ */
#define MQTT_BROKER_IPV6_ADDR "fd00::1"              // MQTT Broker IP address
#define MQTT_BROKER_DEFAULT_PORT         1883                  // MQTT Broker port



#define MQTT_CLI_MAX_PUBLISH_PERIOD   10                                    // Maximum MQTT client publish period
#define MQTT_BROKER_KEEPALIVE_TIMEOUT (4 * MQTT_CLI_MAX_PUBLISH_PERIOD)     // MQTT Broker Keepalive timeout
#define MQTT_CLI_MAX_INACTIVITY       (2.5 * MQTT_CLI_MAX_PUBLISH_PERIOD)   // MQTT Client maximum inactivity (to prevent disconnecting from the broker)

// Logging
#define LOG_MODULE "sensor"
#define LOG_LEVEL  LOG_LEVEL_DBG

// MQTT Client States
#define MQTT_CLI_STATE_INIT    0
#define MQTT_CLI_STATE_ENGINE_OK 1
#define MQTT_CLI_STATE_NET_OK 2
#define MQTT_CLI_STATE_BROKER_CONNECTING 3
#define MQTT_CLI_STATE_BROKER_CONNECTED  4
#define MQTT_CLI_STATE_BROKER_SUBSCRIBED 5


// TCP Parameters
#define MQTT_MAX_TCP_SEGMENT_SIZE    32

// TODO: Check if necessary (mqtt_broker_ipv6_addr[CONFIG_IP_ADDR_STR_LEN])
// #define CONFIG_IP_ADDR_STR_LEN       64

// MQTT topics and messages buffer sizes
#define MQTT_TOPIC_BUF_SIZE 40
#define MQTT_MESSAGE_BUF_SIZE 350  // Must be large so as to also hold application error descriptions

// Application error descriptions buffer size
#define APP_ERR_DSCR_BUF_SIZE 200

// MQTT client status timer periods
#define MQTT_CLI_STATUS_TIMER_PERIOD_BOOTSTRAP  (0.1 * CLOCK_SECOND)
#define MQTT_CLI_STATUS_TIMER_PERIOD_RPL  (5 * CLOCK_SECOND)

// TODO: shouldn-t be necessary
// #define MQTT_CLI_STATUS_TIMER_PERIOD_SUBSCRIBED CLOCK_SECOND)


// Sensors Sampling Period
#define C02_SENSOR_SAMPLING_PERIOD (20 * CLOCK_SECOND) // (8 * CLOCK_SECOND)
#define TEMP_SENSOR_SAMPLING_PERIOD (15 * CLOCK_SECOND) // (5 * CLOCK_SECOND)

// Communication LED blinking period and number
#define COMM_LED_BLINK_PERIOD (0.1 * CLOCK_SECOND)
#define COMM_LED_BLINK_TIMES 5


// Device LEDs
#define POWER_LED            LEDS_GREEN
#define MQTT_BROKER_COMM_LED LEDS_YELLOW

// Sampled quantities thresholds TODO
#define C02_VALUE_MIN 10
#define C02_VALUE_MAX 200
#define TEMP_VALUE_MIN 10
#define TEMP_VALUE_MAX 50





enum sensorErrCode // : unsigned char
 {

  // The sensor failed to publish a sampled quantity
  ERR_SENSOR_QUANTITY_PUB_FAILED,

  // The sensor failed to subscribe on the "SafeTunnels/avgFanRelSpeed" topic
  ERR_SENSOR_FANRELSPEED_SUB_FAILED,

  // The sensor received a MQTT message when not in the MQTT_CLI_STATE_BROKER_SUBSCRIBED state
  ERR_SENSOR_RECV_WHEN_NOT_SUB,

  // The sensor received a MQTT message on a topic it is not subscribed on
  ERR_SENSOR_RECEIVED_UNKNOWN_TOPIC,

  // The sensor received an invalid "fanRelSpeed" value
  ERR_SENSOR_FANRELSPEED_INVALID,

  //The MQTT client unsubscribed from an unknown topic
  ERR_SENSOR_MQTT_UNSUB_TOPIC,

  // The MQTT engine invoked a callback of unknown type
  ERR_SENSOR_MQTT_CLI_CALLBACK_UNKNOWN_TYPE,

  // An unknown event was passed to the sensor main loop
  ERR_SENSOR_MAIN_LOOP_UNKNOWN_EVENT,

  // Unknown MQTT client state in the sensor process main loop
  ERR_SENSOR_MAIN_LOOP_UNKNOWN_MQTT_CLI_STATE,

  // Exited from the sensor process main loop
  ERR_SENSOR_MAIN_LOOP_EXITED
 };



// Associates each SafeCloud session error code with its severity level and human-readable description
static const char* sensorErrCodesDscr[] =
 {
   // ERR_SENSOR_QUANTITY_PUB_FAILED
   "The sensor failed to publish a sampled quantity",

   // ERR_SENSOR_FANRELSPEED_SUB_FAILED
   "The sensor failed to subscribe on the \"SafeTunnels/avgFanRelSpeed\" topic",

   // ERR_SENSOR_RECV_WHEN_NOT_SUB
   "The sensor received a MQTT message when not in the MQTT_CLI_STATE_BROKER_SUBSCRIBED state",

   // ERR_SENSOR_RECEIVED_UNKNOWN_TOPIC
   "The sensor received a MQTT message on a topic it is not subscribed on",

   // ERR_SENSOR_FANRELSPEED_INVALID
   "The sensor received an invalid \"fanRelSpeed\" value",

   // ERR_SENSOR_MQTT_UNSUB_TOPIC
   "The MQTT client unsubscribed from an unknown topic",

   // ERR_SENSOR_MQTT_CLI_CALLBACK_UNKNOWN_TYPE
   "The MQTT engine invoked a callback of unknown type",

   // ERR_SENSOR_MAIN_LOOP_UNKNOWN_EVENT
   "An unknown event was passed to the sensor main loop",

   // ERR_SENSOR_MAIN_LOOP_UNKNOWN_MQTT_CLI_STATE
   "Unknown MQTT client state in the sensor process main loop",

   // ERR_SENSOR_MAIN_LOOP_EXITED
   "Sensor process exited from its main loop"
 };

// WORKING GOOD
//#define LOG_PUB_ERROR(sensorErrCode, f_, ...) { snprintf(errDscr,APP_ERR_DSCR_BUF_SIZE, (f_), __VA_ARGS__); logPublishError(sensorErrCode); }

// REQUIRES GCC?
//#define LOG_PUB_ERROR(sensorErrCode, f_, ...) { snprintf(errDscr,APP_ERR_DSCR_BUF_SIZE, (f_), ##__VA_ARGS__); logPublishError(sensorErrCode); }


// Up to 3 snprintf() variadic arguments (a bit crude, but works)
#define LOG_PUB_CODE_ONLY(sensorErrCode) { errDscr[0] = '\0'; logPublishError(sensorErrCode); }
#define LOG_PUB_ERROR_DSCR(sensorErrCode,dscr) { snprintf(errDscr,APP_ERR_DSCR_BUF_SIZE, (dscr)); logPublishError(sensorErrCode); }
#define LOG_PUB_ERROR_DSCR_1PARAM(sensorErrCode,dscr,param1) { snprintf(errDscr,APP_ERR_DSCR_BUF_SIZE, (dscr), param1); logPublishError(sensorErrCode); }
#define LOG_PUB_ERROR_DSCR_2PARAM(sensorErrCode,dscr,param1,param2) { snprintf(errDscr,APP_ERR_DSCR_BUF_SIZE, (dscr), param1, param2); logPublishError(sensorErrCode); }
#define LOG_PUB_ERROR_DSCR_3PARAM(sensorErrCode,dscr,param1,param2,param3) { snprintf(errDscr,APP_ERR_DSCR_BUF_SIZE, (dscr), param1, param2, param3); logPublishError(sensorErrCode); }

#define GET_LOG_PUB_ERROR_MACRO(_1,_2,_3,_4,_5,LOG_PUB_ERROR_MACRO,...) LOG_PUB_ERROR_MACRO
#define LOG_PUB_ERROR(...) GET_LOG_PUB_ERROR_MACRO(__VA_ARGS__,LOG_PUB_ERROR_DSCR_3PARAM,LOG_PUB_ERROR_DSCR_2PARAM,LOG_PUB_ERROR_DSCR_1PARAM,LOG_PUB_ERROR_DSCR,LOG_PUB_CODE_ONLY)(__VA_ARGS__)


/**
 * LOG_EXEC_CODE_ macros, calling the handleExecErrCode()
 * function with the arguments passed to the LOG_EXEC_CODE macro:
 *  - 1 argument   -> execErrCode only
 *  - 2 arguments  -> execErrCode + additional description
 *  - 3 arguments  -> execErrCode + additional description + error reason
 *  - (DEBUG_MODE) -> The source file name and line number at which the exception is thrown
 */
#ifdef DEBUG_MODE
#define LOG_EXEC_CODE_ONLY(execErrCode) handleExecErrCode(execErrCode,nullptr,nullptr,new std::string(__FILE__),__LINE__-1)
 #define LOG_EXEC_CODE_DSCR(execErrCode,dscr) handleExecErrCode(execErrCode,new std::string(dscr),nullptr,new std::string(__FILE__),__LINE__-1)
 #define LOG_EXEC_CODE_DSCR_REASON(execErrCode,dscr,reason) handleExecErrCode(execErrCode,new std::string(dscr),new std::string(reason),new std::string(__FILE__),__LINE__-1)
#else
#define LOG_EXEC_CODE_ONLY(execErrCode) handleExecErrCode(execErrCode,nullptr,nullptr)
 #define LOG_EXEC_CODE_DSCR(execErrCode,dscr) handleExecErrCode(execErrCode,new std::string(dscr),nullptr)
 #define LOG_EXEC_CODE_DSCR_REASON(execErrCode,dscr,reason) handleExecErrCode(execErrCode,new std::string(dscr),new std::string(reason))
#endif

/**
 * Substitutes the appropriate LOG_EXEC_CODE_ depending on the
 * number of arguments passed to the LOG_EXEC_CODE variadic macro:
 *  - 1 argument  -> execErrCode only
 *  - 2 arguments -> execErrCode + additional description
 *  - 3 arguments -> execErrCode + additional description + error reason
 */
#define GET_LOG_EXEC_CODE_MACRO(_1,_2,_3,LOG_EXEC_CODE_MACRO,...) LOG_EXEC_CODE_MACRO
#define LOG_EXEC_CODE(...) GET_LOG_EXEC_CODE_MACRO(__VA_ARGS__,LOG_EXEC_CODE_DSCR_REASON,LOG_EXEC_CODE_DSCR,LOG_EXEC_CODE_ONLY)(__VA_ARGS__)

#endif //SAFETUNNELS_SENSOR_H
