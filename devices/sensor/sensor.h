#ifndef SAFETUNNELS_SENSOR_H
#define SAFETUNNELS_SENSOR_H

/* SafeTunnels Sensor Default Parameters */


PROCESS_NAME(mqtt_client_process);

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
#define MQTT_CLI_STATE_ENGINE_OK    		     1
#define MQTT_CLI_STATE_NET_OK    	     2
#define MQTT_CLI_STATE_BROKER_CONNECTING      3
#define MQTT_CLI_STATE_BROKER_CONNECTED       4
#define MQTT_CLI_STATE_BROKER_SUBSCRIBED      5


// TCP Parameters
#define MQTT_MAX_TCP_SEGMENT_SIZE    32

// TODO: Check if necessary (mqtt_broker_ipv6_addr[CONFIG_IP_ADDR_STR_LEN])
// #define CONFIG_IP_ADDR_STR_LEN       64

// MQTT topics and messages buffer sizes
#define MQTT_TOPIC_BUF_SIZE 40
#define MQTT_MESSAGE_BUF_SIZE 200  // Must be large so as to also hold application error descriptions

// Application error descriptions buffer size
#define APP_ERR_DSCR_BUF_SIZE 150

// Initialize the timer periodically checking the MQTT client status

// MQTT client status timer periods
#define MQTT_CLI_STATUS_TIMER_PERIOD_BOOTSTRAP  (0.1 * CLOCK_SECOND)
#define MQTT_CLI_STATUS_TIMER_PERIOD_SUBSCRIBED CLOCK_SECOND)

// Main MQTT buffers size (to be increased if we start to publish more data) TODO: CHECK
#define APP_BUFFER_SIZE 512


// Sensors Sampling Period
#define C02_SENSOR_SAMPLING_PERIOD 8
#define TEMP_SENSOR_SAMPLING_PERIOD 5


#endif //SAFETUNNELS_SENSOR_H
