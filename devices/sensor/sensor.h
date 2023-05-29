#ifndef SAFETUNNELS_SENSOR_H
#define SAFETUNNELS_SENSOR_H

/* SafeTunnels Sensor Default Parameters */


PROCESS_NAME(mqtt_client_process);

/* ------------------------------ MQTT Parameters ------------------------------ */
#define MQTT_CLIENT_BROKER_IP_ADDR "fd00::1"              // MQTT Broker IP address
#define DEFAULT_BROKER_PORT         1883                  // MQTT Broker port



#define MQTT_CLI_MAX_PUBLISH_PERIOD   10                                    // Maximum MQTT client publish period
#define MQTT_BROKER_KEEPALIVE_TIMEOUT (4 * MQTT_CLI_MAX_PUBLISH_PERIOD)     // MQTT Broker Keepalive timeout
#define MQTT_CLI_MAX_INACTIVITY       (2.5 * MQTT_CLI_MAX_PUBLISH_PERIOD)   // MQTT Client maximum inactivity (to prevent disconnecting from the broker)

// Logging
#define LOG_MODULE "sensor"
#define LOG_LEVEL  LOG_LEVEL_DBG

// MQTT Client States
#define MQTT_CLI_STATE_DISCONNECTED    0
#define MQTT_CLI_STATE_INIT    		     1
#define MQTT_CLI_STATE_NET_OK    	     2
#define MQTT_CLI_STATE_CONNECTING      3
#define MQTT_CLI_STATE_CONNECTED       4
#define MQTT_CLI_STATE_SUBSCRIBED      5


// TCP Parameters
#define MAX_TCP_SEGMENT_SIZE    32
#define CONFIG_IP_ADDR_STR_LEN   64

// Buffer Size
#define BUFFER_SIZE 64

// Periodic timer to check the state of the MQTT client
#define STATE_MACHINE_PERIODIC     (CLOCK_SECOND >> 1)

// Main MQTT buffers size (to be increased if we start to publish more data) TODO: CHECK
#define APP_BUFFER_SIZE 512


// Sensors Sampling Period
#define C02_SENSOR_SAMPLING_PERIOD 8
#define TEMP_SENSOR_SAMPLING_PERIOD 5



#endif //SAFETUNNELS_SENSOR_H
