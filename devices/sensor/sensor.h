#ifndef SAFETUNNELS_SENSOR_H
#define SAFETUNNELS_SENSOR_H

/* SafeTunnels Sensor Default Parameters */

/* ------------------------------ MQTT Parameters ------------------------------ */
#define MQTT_CLIENT_BROKER_IP_ADDR "fd00::1"              // MQTT Broker IP address
#define DEFAULT_BROKER_PORT         1883                  // MQTT Broker port



#define DEFAULT_PUBLISH_INTERVAL    (30 * CLOCK_SECOND)   // Default publish interval


// Logging
#define LOG_MODULE "sensor"
#define LOG_LEVEL  LOG_LEVEL_DBG

// MQTT Client States
#define MQTT_CLI_STATE_INIT    		  0
#define MQTT_CLI_STATE_NET_OK    	  1
#define MQTT_CLI_STATE_CONNECTING      2
#define MQTT_CLI_STATE_CONNECTED       3
#define MQTT_CLI_STATE_SUBSCRIBED      4
#define MQTT_CLI_STATE_DISCONNECTED    5

// TCP Parameters
#define MAX_TCP_SEGMENT_SIZE    32
#define CONFIG_IP_ADDR_STR_LEN   64

// Buffer Size
#define BUFFER_SIZE 64

// Periodic timer to check the state of the MQTT client
#define STATE_MACHINE_PERIODIC     (CLOCK_SECOND >> 1)

// Main MQTT buffers size (to be increased if we start to publish more data) TODO: CHECK
#define APP_BUFFER_SIZE 512

#endif //SAFETUNNELS_SENSOR_H
