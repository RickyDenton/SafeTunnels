#ifndef SAFETUNNELS_SENSOR_H
#define SAFETUNNELS_SENSOR_H

/* SafeTunnels Sensor Default Parameters */

PROCESS_NAME(safetunnels_sensor_process);

PROCESS_NAME(mqtt_process);

/* ------------------------------ MQTT Parameters ------------------------------ */
#define MQTT_BROKER_IPV6_ADDR "fd00::1"              // MQTT Broker IP address
#define MQTT_BROKER_DEFAULT_PORT         1883                  // MQTT Broker port



#define MQTT_CLI_MAX_PUBLISH_PERIOD   10                                    // Maximum MQTT client publish period
#define MQTT_BROKER_KEEPALIVE_TIMEOUT (4 * MQTT_CLI_MAX_PUBLISH_PERIOD)     // MQTT Broker Keepalive timeout
#define MQTT_CLI_MAX_INACTIVITY       (2.5 * MQTT_CLI_MAX_PUBLISH_PERIOD)   // MQTT Client maximum inactivity (to prevent disconnecting from the broker)

// Logging
#define LOG_MODULE "sensor"
#define LOG_LEVEL  LOG_LEVEL_DBG


// TCP Parameters
#define MQTT_MAX_TCP_SEGMENT_SIZE    32


// MQTT topics and messages buffer sizes
#define MQTT_TOPIC_BUF_SIZE 40
#define MQTT_MESSAGE_BUF_SIZE 350  // Must be large so as to also hold application error descriptions

// MQTT client status timer periods
#define SENSOR_MAIN_LOOP_PERIOD (1 * CLOCK_SECOND)
#define SENSOR_MQTT_CLI_RPL_WAITING_TIMES_MODULE 30


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
 * the "mqtt.h" file, line 544 is in fact used as a boolean), the publishment of
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




// Communication LED blinking period and number
#define COMM_LED_BLINK_PERIOD (0.1 * CLOCK_SECOND)
#define COMM_LED_BLINK_TIMES 3


// Device LEDs
#define POWER_LED            LEDS_GREEN
#define MQTT_BROKER_SUB_LED  LEDS_YELLOW

// Sampled quantities thresholds TODO
#define C02_VALUE_MIN 10
#define C02_VALUE_MAX 200
#define TEMP_VALUE_MIN 10
#define TEMP_VALUE_MAX 50

// Utility Topics
#define TOPIC_AVG_FAN_REL_SPEED "SafeTunnels/avgFanRelSpeed"
#define TOPIC_SENSORS_ERRORS "SafeTunnels/sensorsErrors"



enum MQTTCliState
 {
  MQTT_CLI_STATE_INIT,
  MQTT_CLI_STATE_ENGINE_OK,
  MQTT_CLI_STATE_NET_OK,
  MQTT_CLI_STATE_BROKER_CONNECTING,
  MQTT_CLI_STATE_BROKER_CONNECTED,
  MQTT_CLI_STATE_BROKER_SUBSCRIBED
 };



#endif //SAFETUNNELS_SENSOR_H