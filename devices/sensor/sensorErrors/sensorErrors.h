#ifndef SAFETUNNELS_SENSORERRORS_H
#define SAFETUNNELS_SENSORERRORS_H

// Error descriptions buffer size
#define ERR_DSCR_BUF_SIZE 200




enum sensorErrCode
 {
  // The sensor failed to publish a sampled quantity
  ERR_SENSOR_QUANTITY_PUB_FAILED,

  // The sensor failed to subscribe on the "SafeTunnels/avgFanRelSpeed" topic
  ERR_SENSOR_AVGFANRELSPEED_SUB_FAILED,

  // The sensor received an invalid "fanRelSpeed" value
  ERR_SENSOR_AVGFANRELSPEED_INVALID,

  // The sensor has connected with the MQTT broker when not in the 'MQTT_CLI_STATE_BROKER_CONNECTING' state
  ERR_SENSOR_MQTT_CONNECTED_IN_INVALID_STATE,




  // The sensor has disconnected from the MQTT broker
  // NOTE: this error is also published automatically by the broker as the sensor's "last will"
  ERR_SENSOR_MQTT_DISCONNECTED,

  // The sensor received a MQTT message on a topic it is not subscribed on
  ERR_SENSOR_MQTT_RECV_UNKNOWN_TOPIC,

  // The MQTT engine invoked a callback of unknown type
  ERR_SENSOR_MQTT_CLI_CALLBACK_UNKNOWN_TYPE,

  // An unknown event was passed to the sensor main loop
  ERR_SENSOR_MAIN_LOOP_UNKNOWN_EVENT,

  // Unknown MQTT client state in the sensor process main loop
  ERR_SENSOR_MAIN_LOOP_UNKNOWN_MQTT_CLI_STATE,

  // Exited from the sensor process main loop
  ERR_SENSOR_MAIN_LOOP_EXITED
 };


// WORKING GOOD
//#define LOG_PUB_ERROR(sensorErrCode, f_, ...) { snprintf(errDscr,ERR_DSCR_BUF_SIZE, (f_), __VA_ARGS__); logPublishError(sensorErrCode); }

// REQUIRES GCC?
//#define LOG_PUB_ERROR(sensorErrCode, f_, ...) { snprintf(errDscr,ERR_DSCR_BUF_SIZE, (f_), ##__VA_ARGS__); logPublishError(sensorErrCode); }


// Up to 4 snprintf() variadic arguments (a bit crude, but works)
#define LOG_PUB_CODE_ONLY(sensorErrCode) { errDscr[0] = '\0'; logPublishError(sensorErrCode); }
#define LOG_PUB_ERROR_DSCR(sensorErrCode,dscr) { snprintf(errDscr,ERR_DSCR_BUF_SIZE, dscr); logPublishError(sensorErrCode); }
#define LOG_PUB_ERROR_DSCR_1PARAM(sensorErrCode,dscr,param1) { snprintf(errDscr,ERR_DSCR_BUF_SIZE, dscr, param1); logPublishError(sensorErrCode); }
#define LOG_PUB_ERROR_DSCR_2PARAM(sensorErrCode,dscr,param1,param2) { snprintf(errDscr,ERR_DSCR_BUF_SIZE, dscr, param1, param2); logPublishError(sensorErrCode); }
#define LOG_PUB_ERROR_DSCR_3PARAM(sensorErrCode,dscr,param1,param2,param3) { snprintf(errDscr,ERR_DSCR_BUF_SIZE, dscr, param1, param2, param3); logPublishError(sensorErrCode); }
#define LOG_PUB_ERROR_DSCR_4PARAM(sensorErrCode,dscr,param1,param2,param3,param4) { snprintf(errDscr,ERR_DSCR_BUF_SIZE, dscr, param1, param2, param3, param4); logPublishError(sensorErrCode); }

#define GET_LOG_PUB_ERROR_MACRO(_1,_2,_3,_4,_5,_6,LOG_PUB_ERROR_MACRO,...) LOG_PUB_ERROR_MACRO
#define LOG_PUB_ERROR(...) GET_LOG_PUB_ERROR_MACRO(__VA_ARGS__,LOG_PUB_ERROR_DSCR_4PARAM,LOG_PUB_ERROR_DSCR_3PARAM,LOG_PUB_ERROR_DSCR_2PARAM,LOG_PUB_ERROR_DSCR_1PARAM,LOG_PUB_ERROR_DSCR,LOG_PUB_CODE_ONLY)(__VA_ARGS__)


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


char* MQTTCliStateToStr();
char* MQTTEngineAPIResultStr();
void logPublishError(enum sensorErrCode sensorErrCode);

#endif //SAFETUNNELS_SENSORERRORS_H
