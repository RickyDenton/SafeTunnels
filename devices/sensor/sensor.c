/* SafeTunnels service sensor (C02 + temperature) */

/* ================================== INCLUDES ================================== */

/* ------------------------------- System Headers ------------------------------- */
#include "contiki.h"
#include "net/routing/routing.h"
#include "mqtt.h"
#include "net/ipv6/uip.h"
#include "net/ipv6/uip-icmp6.h"
#include "net/ipv6/sicslowpan.h"
#include "sys/etimer.h"
#include "sys/ctimer.h"
#include "lib/sensors.h"
#include "dev/button-hal.h"
#include "dev/leds.h"
#include "os/sys/log.h"
#include "random.h"

/* ------------------------------- Other Headers ------------------------------- */
#include "sensor.h"
#include "../common/devUtilities.h"
#include <string.h>
#include <strings.h>
#include <limits.h>

/* ==================== PROCESSES DEFINITIONS AND AUTOSTART ==================== */

PROCESS(safetunnels_sensor_process, "MQTT Client");
AUTOSTART_PROCESSES(&safetunnels_sensor_process);

/* ============================== GLOBAL VARIABLES ============================== */

// The node ID, or MAC address
static char nodeID[MAC_ADDRESS_SIZE];


// Pointer to the MQTT Broker IP address
static const char* broker_ip = MQTT_CLIENT_BROKER_IP_ADDR;

// Broker IP address
char broker_address[CONFIG_IP_ADDR_STR_LEN];


// MQTT Client State
static uint8_t mqtt_cli_state;

// ClientIDs and Topics Buffer Sizes TODO CHECK!
static char pub_topic[BUFFER_SIZE];
static char sub_topic[BUFFER_SIZE];

static int value = 0;

//Periodic timer to check the state of the MQTT client
static struct etimer periodic_timer;

// Main MQTT buffers (to be increased if we start to publish more data) TODO: CHECK
static char app_buffer[APP_BUFFER_SIZE];

// MQTT message pointer
static struct mqtt_message* msg_ptr = NULL;

// MQTT connection
static struct mqtt_connection mqttBrokerConn;

// MQTT status
mqtt_status_t status;




// Sensor Values
unsigned short C02Density = USHRT_MAX;   // C02 Density in parts per million (ppm)
unsigned short temp = USHRT_MAX;         // Temperature in celsius degrees

// Sensor Timers
static struct ctimer C02SamplingTimer;
static struct ctimer tempSamplingTimer;

// Quantities last MQTT updates in seconds from power
// on (used to prevent disconnection from the broker)
unsigned long C02LastMQTTUpdateTime = 0;
unsigned long tempLastMQTTUpdateTime = 0;



/* =========================== FUNCTIONS DEFINITIONS =========================== */

static void pub_handler(const char *topic, uint16_t topic_len, const uint8_t *chunk, uint16_t chunk_len)
{
 printf("Pub Handler: topic='%s' (len=%u), chunk_len=%u\n", topic, topic_len, chunk_len);

 if(strcmp(topic, "actuator") == 0)
  {
   printf("Received Actuator command\n");
	 printf("%s\n", chunk);
    // Do something :)
    return;
  }
}

static void mqtt_event(struct mqtt_connection* m, mqtt_event_t event, void *data)
{
 switch(event)
  {
   case MQTT_EVENT_CONNECTED:
    printf("Application has a MQTT connection\n");
    mqtt_cli_state = MQTT_CLI_STATE_CONNECTED;
    break;

   case MQTT_EVENT_DISCONNECTED:
    printf("MQTT Disconnect. Reason %u\n", *((mqtt_event_t *)data));
    mqtt_cli_state = MQTT_CLI_STATE_DISCONNECTED;
    process_poll(&safetunnels_sensor_process);
    break;

   case MQTT_EVENT_PUBLISH:
    msg_ptr = data;
    pub_handler(msg_ptr->topic, strlen(msg_ptr->topic),
                msg_ptr->payload_chunk, msg_ptr->payload_length);
    break;

   case MQTT_EVENT_SUBACK:
#if MQTT_311
    mqtt_suback_event_t *suback_event = (mqtt_suback_event_t *)data;

    if(suback_event->success)
     printf("Application is subscribed to topic successfully\n");
    else
     printf("Application failed to subscribe to topic (ret code %x)\n", suback_event->return_code);
#else
    printf("Application is subscribed to topic successfully\n");
#endif
    break;

   case MQTT_EVENT_UNSUBACK:
    printf("Application is unsubscribed to topic successfully\n");
    break;

   case MQTT_EVENT_PUBACK:
    printf("Publishing complete.\n");
    break;

   default:
    printf("Application got a unhandled MQTT event: %i\n", event);
    break;
  }
}




bool publishMQTTUpdate(char* quantity, unsigned int quantityValue, bool quantityDiffer, unsigned long publishInactivityTime)
 {
  // The topic of the MQTT message to be published
  // ("SafeTunnels/C02" || "SafeTunnels/temp")
  char MQTTPublishTopic[18];

  // The JSON MQTT message to be published
  char MQTTPublishMessageJSON[60];

  // The MQTT publishing result
  unsigned int mqttPublishResult;

  // If the node's MQTT client is NOT connected to the MQTT
  // broker, the updated sampled quantity cannot be published
  if(mqtt_cli_state < MQTT_CLI_STATE_CONNECTED)
   {
    // Log that the sampled quantity updated could not be published
    LOG_WARN("The updated %s quantity (%u) could not be published as the node's MQTT client is currently disconnected from the broker", quantity, quantityValue);

    // Return that the sampled quantity update has not been published
    return false;
   }

   // Otherwise, if the node's MQTT client IS connected to the broker
  else
   {
    // If the newly generated quantity differs from its current value OR
    // such quantity has not published for a MQTT_CLIENT_MAX_INACTIVITY time,
    // publish the quantity to the broker
    if(quantityDiffer || (publishInactivityTime > (unsigned long)MQTT_CLI_MAX_INACTIVITY))
     {
      // Prepare the topic of the message to be published
      sprintf(MQTTPublishTopic, "SafeTunnels/%s", quantity);

      // Prepare the message to be published
      sprintf(MQTTPublishMessageJSON, "{"
                                      " \"ID\": \"%s\""
                                      " \"%s\": \"%u\""
                                      " }", nodeID, quantity, quantityValue);

      // Attempt to publish the message on the topic
      mqttPublishResult = mqtt_publish(&mqttBrokerConn, NULL, MQTTPublishTopic, (uint8_t*)MQTTPublishMessageJSON, strlen(MQTTPublishMessageJSON), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);

      // If the MQTT publishment was successful
      if(mqttPublishResult == MQTT_STATUS_OK)
       {
        // Log that the MQTT publishment was successful
        LOG_INFO("Published updated %s (%u) to the MQTT broker\n", quantity, quantityValue);

        // Return that the MQTT publishment was successful
        return true;
       }

       // Otherwise, if the MQTT publishment was NOT successful
      else
       {
        // Log that the MQTT publishment was NOT successful
        LOG_ERR("FAILED to publish the updated %s (%u) to the MQTT broker (error = %u)\n", quantity, quantityValue, mqttPublishResult);

        // TODO: Possibly attempt to send a publish in the "SafeTunnels/errors" topic

        // Return that the MQTT publishment was NOT successful
        return false;
       }
     }

     // Otherwise, if a same sampled value was not published to the broker
    else
     {
      // Log that the same sampled value was not published to the broker
      LOG_INFO("A same sampled %s value (%u) was NOT published the MQTT broker\n", quantity, quantityValue);

      // Return that quantity has not been published to the broker
      return false;
     }
   }
 }


// C02 Sampling Function (timer)
static void C02Sampling(__attribute__((unused)) void* ptr)
 {
  unsigned int newC02Density;

  /* ---- TODO: Generate the C02 value depending on the "fanSpeedRel" value ---- */

  newC02Density = random_rand();
  LOG_DBG("New randomly generated C02 density: %u\n",newC02Density);

  /* --------------------------------------------------------------------------- */

  // Check and attempt to publish the updated C02 value, and, if
  // the publishment was successful, update the last publishment time
  if(publishMQTTUpdate("C02", newC02Density, newC02Density != C02Density, clock_seconds() - C02LastMQTTUpdateTime))
   C02LastMQTTUpdateTime = clock_seconds();

  // In any case, update the new C02 Density value
  C02Density = newC02Density;

  // Reset the C02 sampling timer
  ctimer_reset(&C02SamplingTimer);
 }

// Temperature sampling function (timer)
static void tempSampling(__attribute__((unused)) void* ptr)
 {
  unsigned int newTemp;

  /* ---- TODO: Generate the temperature value depending on the "fanSpeedRel" value ---- */

  newTemp = random_rand();
  LOG_DBG("New randomly generated temperature: %u\n",newTemp);

  /* ----------------------------------------------------------------------------------- */

  // Check and attempt to publish the updated temperature value, and, if
  // the publishment was successful, update the last publishment time
  if(publishMQTTUpdate("temp", newTemp, newTemp != temp, clock_seconds() - tempLastMQTTUpdateTime))
   tempLastMQTTUpdateTime = clock_seconds();

  // In any case, update the new C02 Density value
  temp = newTemp;

  // Reset the temperature sampling timer
  ctimer_reset(&tempSamplingTimer);
 }


PROCESS_THREAD(safetunnels_sensor_process, ev, data)
{
 PROCESS_BEGIN();

 // Turn on the green LED at power on
 leds_single_on(LEDS_GREEN);

 // Set the node's ID as its MAC address
 writeNodeMAC(nodeID);

 // Log that the sensor node has started along with its MAC
 LOG_INFO("SafeTunnels sensor node started, MAC = %s\n",nodeID);

 // Start the nodes' sensors sampling (as they do not depend on the node's connection status)
 ctimer_set(&C02SamplingTimer, C02_SENSOR_SAMPLING_PERIOD * CLOCK_SECOND, C02Sampling, NULL);
 ctimer_set(&tempSamplingTimer, TEMP_SENSOR_SAMPLING_PERIOD * CLOCK_SECOND, tempSampling, NULL);

 /* ------------ MQTT Initialization ------------ */

 // Broker registration
 mqtt_register(&mqttBrokerConn, &safetunnels_sensor_process, nodeID, mqtt_event, MAX_TCP_SEGMENT_SIZE);

 mqtt_cli_state = MQTT_CLI_STATE_INIT;
				    
 // Initialize periodic timer to check the status
 etimer_set(&periodic_timer, STATE_MACHINE_PERIODIC);

 /* Main loop */
 while(1)
  {
   PROCESS_YIELD();

   if((ev == PROCESS_EVENT_TIMER && data == &periodic_timer) || ev == PROCESS_EVENT_POLL)
    {
     if(mqtt_cli_state == MQTT_CLI_STATE_INIT && isNodeConnected())
      mqtt_cli_state = MQTT_CLI_STATE_NET_OK;

     if(mqtt_cli_state == MQTT_CLI_STATE_NET_OK)
      {
       // Connect to MQTT server
       printf("Connecting!\n");
			  
       memcpy(broker_address, broker_ip, strlen(broker_ip));
			  
       mqtt_connect(&mqttBrokerConn, broker_address, DEFAULT_BROKER_PORT, (DEFAULT_PUBLISH_INTERVAL * 3) / CLOCK_SECOND, MQTT_CLEAN_SESSION_ON);
       mqtt_cli_state = MQTT_CLI_STATE_CONNECTING;
		  }
		  
		  if(mqtt_cli_state == MQTT_CLI_STATE_CONNECTED)
       {
        // Subscribe to a topic
			  strcpy(sub_topic,"actuator");

			  status = mqtt_subscribe(&mqttBrokerConn, NULL, sub_topic, MQTT_QOS_LEVEL_0);

			  printf("Subscribing!\n");
			  if(status == MQTT_STATUS_OUT_QUEUE_FULL)
         {
				  LOG_ERR("Tried to subscribe but command queue was full!\n");
				  PROCESS_EXIT();
			   }
        mqtt_cli_state = MQTT_CLI_STATE_SUBSCRIBED;
		   }

		 if(mqtt_cli_state == MQTT_CLI_STATE_SUBSCRIBED)
      {
			 // Publish something
       sprintf(pub_topic, "%s", "status");

			 sprintf(app_buffer, "report %d", value);

			 value++;

			 mqtt_publish(&mqttBrokerConn, NULL, pub_topic, (uint8_t *)app_buffer, strlen(app_buffer), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);

		  }
     else
      if(mqtt_cli_state == MQTT_CLI_STATE_DISCONNECTED )
       {
		    LOG_ERR("Disconnected form MQTT broker\n");
		    // Recover from error
		   }
		
		 etimer_set(&periodic_timer, STATE_MACHINE_PERIODIC);
   }
  }
 PROCESS_END();
}