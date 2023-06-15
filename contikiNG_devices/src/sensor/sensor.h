#ifndef SAFETUNNELS_SENSOR_H
#define SAFETUNNELS_SENSOR_H

/* SafeTunnels Sensor Application Declarations */

/* ========================== APPLICATION PARAMETERS ========================== */

/* ---------------------- General Application Parameters ---------------------- */

// Contiki-NG logging parameters
#define LOG_MODULE "sensor"
#define LOG_LEVEL  LOG_LEVEL_DBG

// Sensor errors descriptions buffer size
#define ERR_DSCR_BUF_SIZE 200

// Sensor main loop timer period
#define SENSOR_MAIN_LOOP_PERIOD (1 * CLOCK_SECOND)

// How many main loop cycles the sensor logs
// that it is waiting for external connectivity
#define SENSOR_MAIN_LOOP_NO_CONN_LOG_PERIOD 30

/* ----------------------------- LEDs Management ----------------------------- */

/*
 * Power LED
 * =========
 *  - OFF -> Device OFF
 *  - ON  -> Device ON
 */
#define POWER_LED LEDS_GREEN

/*
 * MQTT communication LED
 * ======================
 *  - OFF      -> The MQTT client is NOT subscribed to a topic on the MQTT broker
 *  - ON       -> The MQTT client IS subscribed to a topic on the MQTT broker
 *  - BLINKING -> The MQTT client has published a message to the broker
 */
#define MQTT_COMM_LED LEDS_YELLOW

// The period and how many times the MQTT_COMM_LED is
// toggled upon publishing a MQTT message so as to blink it
#define MQTT_COMM_LED_TOGGLE_PERIOD (0.1 * CLOCK_SECOND)
#define MQTT_COMM_LED_TOGGLE_TIMES 3


/* ----------------------------- MQTT Parameters ----------------------------- */

// MQTT messages topics and contents buffers sizes
#define MQTT_MSG_TOPIC_BUF_SIZE    40
#define MQTT_MSG_CONTENTS_BUF_SIZE 350


// --------------------------- MQTT Topics Definitions ---------------------------

/*
 * NOTE: The topics the sampled quantities are
 *       published on are dynamically built as:
 *
 *         CO2         -> "SafeTunnels/C02"
 *         Temperature -> "SafeTunnels/temp"
 */

// The topic the sensor subscribes to so as to receive the updated average
// relative fan speed in the system to be used for correlating sampling purposes
#define TOPIC_AVG_FAN_REL_SPEED "SafeTunnels/avgFanRelSpeed"

// The topic the sensor publishes application errors to
#define TOPIC_SENSORS_ERRORS    "SafeTunnels/sensorsErrors"

// ---------------------- MQTT Broker Connection Parameters ----------------------

// MQTT Broker IPv6 address
#define MQTT_BROKER_IPV6_ADDR "fd00::1"

// MQTT Broker port
#define MQTT_BROKER_DEFAULT_PORT 1883

// Maximum TCP segment size
#define MQTT_MAX_TCP_SEGMENT_SIZE 32

// ----------------- MQTT Broker Connection Keepalive Parameters -----------------

// Expected maximum MQTT client publish period in seconds
#define SENSOR_MAX_MQTT_PUBLISH_PERIOD 15

// The time in seconds from when its last publication was received
// after which the MQTT broker should consider the sensor down
// (and so publish its "last will" disconnection message)
#define MQTT_BROKER_KEEPALIVE_TIMEOUT (4 * SENSOR_MAX_MQTT_PUBLISH_PERIOD)

// The maximum time in seconds the node can refrain from
// publishing a same sampled quantity for energy savings
// purposes while avoiding disconnecting from the broker
#define MQTT_CLI_MAX_INACTIVITY (2.5 * SENSOR_MAX_MQTT_PUBLISH_PERIOD)


/* ---------------------- Sampling Management Parameters ---------------------- */

/**
 * SENSOR SAMPLING MODES
 * ---------------------
 * The sensor can sample the physical quantities (C02 and temperature) by using:
 *
 *  1) A shared period for both quantities
 *  2) Different, independent periods for the two quantities
 *
 * Where using different periods for sampling the two quantities causes, due to
 * the Contiki-NG MQTT engine limitation of allowing a single outbound message to
 *  be published at a time (as the "uint8_t out_queue_full" variable defined in
 * the "mqtt.h" file, line 544 is in fact used as a boolean), the publication of
 * one of the two quantities to most likely fail with a MQTT_STATUS_OUT_QUEUE_FULL
 * error at times around integer multiples of their individual periods
 *
 *              t '= n * (CO2_SAMPLING_PERIOD * TEMP_SAMPLING_PERIOD)
 *                 => MQTT_STATUS_OUT_QUEUE_FULL
 *
 * A problem that can be avoided by uniformly distributing the two quantities'
 * sampling instants within a shared sampling period
 */

// Sampling mode 1) (shared period for both quantities)
#define QUANTITIES_SHARED_SAMPLING_PERIOD (12 * CLOCK_SECOND)

// Sampling mode 2) (different, independent periods for the two quantities)
#ifndef QUANTITIES_SHARED_SAMPLING_PERIOD
 #define C02_SAMPLING_PERIOD (16 * CLOCK_SECOND)
 #define TEMP_SAMPLING_PERIOD (10 * CLOCK_SECOND)
#endif

// Initial Sampling delay from when the sensor is connected with the MQTT broker
#define INIT_SAMPLING_DELAY (5 * CLOCK_SECOND)

// ---------- Sampled Quantities Thresholds and Evolution Parameters ----------

// Utility macro extracting byte of index 'i' from a variable
#define GETBYTE(var,index) ((char*)(&(var)))[index]

// Utility macros bounding two unsigned integers sum and
// subtraction between a maximum and minimum value respectively
#define UINT_BOUND_SUM(a,b,max) (((a) > ((max) - (b))) ? (max) : ((a) + (b)))
#define UINT_BOUND_SUB(a,b,min) (((a) < ((min) + (b))) ? (min) : ((a) - (b)))

// Utility macro bounding a value between its minimum and maximum value
#define BOUND(value,min,max) MIN(MAX(value,min),max)

// Sampled quantities minimum and maximum values
#define C02_VALUE_MIN 295
#define C02_VALUE_MAX 12370
#define TEMP_VALUE_MIN 7
#define TEMP_VALUE_MAX 51

// Sampled quantities base maximum
// changes at every update
#define C02_BASE_MAX_CHANGE  250
#define TEMP_BASE_MAX_CHANGE 1

// The minimum and maximum quantities'
// road equilibrium points
#define ROAD_EQ_POINT_MAX 100
#define ROAD_EQ_POINT_MIN 60

// How much a quantity's road equilibrium
// point may vary in an update
#define ROAD_EQ_POINT_MAX_CHANGE 2

// The percentage of the average fan relative speed
// affecting a quantity's cumulative equilibrium point
#define AVG_FAN_REL_SPEED_EQ_PERC 0.6


/* ============================== TYPE DEFINITIONS ============================== */

/* ------------------------- Sensor MQTT Client States ------------------------- */
enum MQTTCliState
 {
  // The sensor must still initialize the MQTT engine
  MQTT_CLI_STATE_INIT = 0,

  // The sensor has initialized the MQTT engine
  // and is waiting for the RPL DODAG to converge
  MQTT_CLI_STATE_ENGINE_OK = 1,

  // The sensor is online and attempting
  // to connect with the MQTT broker
  MQTT_CLI_STATE_NET_OK = 2,

  // The sensor is waiting for the connection
  // with the MQTT broker to be established
  MQTT_CLI_STATE_BROKER_CONNECTING = 3,

  // The sensor is connected with the MQTT broker but is not
  // yet subscribed on the TOPIC_AVG_FAN_REL_SPEED topic
  MQTT_CLI_STATE_BROKER_CONNECTED = 4,

  // The sensor is connected with the MQTT broker AND is subscribed
  // on the TOPIC_AVG_FAN_REL_SPEED topic (steady-state)
  MQTT_CLI_STATE_BROKER_SUBSCRIBED = 5
 };


/* ---------------------------- Sensor Error Codes ---------------------------- */
enum sensorErrCode
 {
  // ------------------------- Connectivity Errors -------------------------

  // The sensor has disconnected from the MQTT broker
  //
  // NOTE: This error code is also published automatically by the MQTT
  //       broker on the TOPIC_SENSORS_ERROR as the sensor's "last will"
  //
  ERR_SENSOR_MQTT_DISCONNECTED = 0,

  // The sensor failed to publish a sampled quantity
  // (C02 or temperature) to the MQTT broker
  ERR_SENSOR_PUB_QUANTITY_FAILED = 1,

  // ----------------- Invalid MQTT Publications Reception -----------------

  // The sensor received a MQTT message on a topic it is not subscribed to
  ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC = 2,

  // The sensor failed to subscribe on the TOPIC_AVG_FAN_REL_SPEED topic
  ERR_SENSOR_SUB_AVGFANRELSPEED_FAILED = 3,

  // The sensor received the publication of an
  // invalid "avgFanRelSpeed" value (not [0,100])
  ERR_SENSOR_RECV_INVALID_AVGFANRELSPEED = 4,

  // ---------------------- Invalid Application States ----------------------

  // The sensor established a connection with the MQTT broker
  // when not in the 'MQTT_CLI_STATE_BROKER_CONNECTING' state
  ERR_SENSOR_MQTT_CONNECTED_NOT_CONNECTING = 5,

  // The MQTT engine invoked a callback of unknown type
  ERR_SENSOR_MQTT_ENGINE_UNKNOWN_CALLBACK_TYPE = 6,

  // Unknown MQTT client state in the sensor process main loop
  ERR_SENSOR_MAIN_LOOP_UNKNOWN_MQTT_CLI_STATE = 7,

  // The sensor process has exited from its main loop
  ERR_SENSOR_MAIN_LOOP_EXITED = 8
 };


/* ================ SENSOR ERRORS LOGGING AND PUBLISHING MACROS ================ */

/**
 * LOG_PUB_ERROR_ macros, writing the formatted additional description into the 'errDscr'
 * buffer and invoking the "logPublish" error function with the passed 'sensorErrCode'
 *   - 1 argument    -> reset "errDscr" and call logPublishError()
 *   - 2 arguments   -> snprintf in "errDscr" with format string only +
 *                      call logPublishError()
 *   - 3-6 arguments -> snprintf in "errDscr" with format string + 1-4 parameters
 *                      call logPublishError()
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
 *   - 1 argument    -> sensorErrCode only
 *   - 2 arguments   -> sensorErrCode +
 *                      additional description (snprintf format string)
 *   - 3-6 arguments -> sensorErrCode +
 *                      additional description (snprintf format string + 1-4 snprintf params)
 */
#define GET_LOG_PUB_ERROR_MACRO(_1,_2,_3,_4,_5,_6,LOG_PUB_ERROR_MACRO,...) LOG_PUB_ERROR_MACRO
#define LOG_PUB_ERROR(...) GET_LOG_PUB_ERROR_MACRO(__VA_ARGS__,LOG_PUB_ERROR_DSCR_4PARAM,LOG_PUB_ERROR_DSCR_3PARAM,LOG_PUB_ERROR_DSCR_2PARAM,LOG_PUB_ERROR_DSCR_1PARAM,LOG_PUB_ERROR_DSCR,LOG_PUB_CODE_ONLY)(__VA_ARGS__)

#endif //SAFETUNNELS_SENSOR_H