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

// TODO: Check if necessary (mqtt_broker_ipv6_addr[CONFIG_IP_ADDR_STR_LEN])
// #define CONFIG_IP_ADDR_STR_LEN       64

// MQTT topics and messages buffer sizes
#define MQTT_TOPIC_BUF_SIZE 40
#define MQTT_MESSAGE_BUF_SIZE 350  // Must be large so as to also hold application error descriptions


// MQTT client status timer periods
#define SENSOR_MQTT_CLI_STATUS_LOOP_TIMER_PERIOD (1 * CLOCK_SECOND)
#define SENSOR_MQTT_CLI_RPL_WAITING_TIMES_MODULE 30

// TODO: shouldn-t be necessary
// #define MQTT_CLI_STATUS_TIMER_PERIOD_SUBSCRIBED CLOCK_SECOND)


// Sensors Sampling Period
#define C02_SENSOR_SAMPLING_PERIOD (20 * CLOCK_SECOND) // (8 * CLOCK_SECOND)
#define TEMP_SENSOR_SAMPLING_PERIOD (15 * CLOCK_SECOND) // (5 * CLOCK_SECOND)

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