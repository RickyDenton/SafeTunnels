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
#include <string.h>
#include <strings.h>

/* ==================== PROCESSES DEFINITIONS AND AUTOSTART ==================== */

PROCESS(safetunnels_sensor_process, "MQTT Client");
AUTOSTART_PROCESSES(&safetunnels_sensor_process);

/* ============================== GLOBAL VARIABLES ============================== */

// Pointer to the MQTT Broker IP address
static const char* broker_ip = MQTT_CLIENT_BROKER_IP_ADDR;

// Broker IP address
char broker_address[CONFIG_IP_ADDR_STR_LEN];


// MQTT Client State
static uint8_t mqtt_cli_state;

// ClientIDs and Topics Buffer Sizes TODO CHECK!
static char mqtt_client_id[MAC_ADDRESS_SIZE];
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
unsigned short C02Density;   // C02 Density in parts per million (ppm)
unsigned short temperature;  // Temperature in celsius degrees



// Sensor Timers
static struct ctimer C02Timer;
static struct ctimer tempTimer;


// Last MQTT Updates (to prevent disconnection from the broker)
unsigned long C02LastMQTTUpdateTime = 0;
unsigned long tempLastMQTTUpdateTime = 0;


static char C02Topic[] = "SafeTunnels/C02";
static char tempTopic[] = "SafeTunnels/temp";


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

static void mqtt_event(struct mqtt_connection *m, mqtt_event_t event, void *data)
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

static bool have_connectivity(void)
{
 if(uip_ds6_get_global(ADDR_PREFERRED) == NULL || uip_ds6_defrt_choose() == NULL)
  return false;
 return true;
}


// C02 Timer
static void C02TimerCallback(__attribute__((unused)) void* ptr)
 {
  unsigned int newC02Density;

  char newC02DensityJSON[15];
  unsigned int mqttPublishResult;

  /* ---- TODO: Generate the C02 value depending on the "fanSpeedRel" value ---- */

  newC02Density = random_rand();
  LOG_DBG("Randomly generated new C02 density: %u\n",newC02Density);

  /* --------------------------------------------------------------------------- */

  // If the node's MQTT client is NOT connected to the MQTT
  // broker, warn that the C02 update has not been published
  if(mqtt_cli_state < MQTT_CLI_STATE_CONNECTED)
   LOG_WARN("Cannot publish the updated C02 density (%u) as the node's MQTT client is disconnected from the broker\n", newC02Density);

  // Otherwise, if the node's MQTT client IS connected to the broker
  else
   {
    // If the newly generated C02 density differs from the current one OR a
    // new C02 density has not been published for the MQTT_CLIENT_MAX_INACTIVITY
    // time, publish the new C02 density to the broker
    if((newC02Density != C02Density) || (clock_seconds() - C02LastMQTTUpdateTime > (unsigned long)MQTT_CLI_MAX_INACTIVITY))
     {
      // Prepare the message to be published
      sprintf(newC02DensityJSON, "{\"C02\": %u}", newC02Density);

      // Publish the message
      mqttPublishResult = mqtt_publish(&mqttBrokerConn, NULL, C02Topic, (uint8_t*)newC02DensityJSON, strlen(newC02DensityJSON), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);

      // If the publishment was successful
      if(mqttPublishResult == MQTT_STATUS_OK)
       {
        // Update the time the last message was published
        C02LastMQTTUpdateTime = clock_seconds();

        // Log the successful publishment
        LOG_INFO("Successfully published to MQTT broker new C02 density: %u\n", newC02Density);
       }
      else
       {
        LOG_ERR("FAILED to publish to the MQTT broker the new C02 density (%u, error: %u)\n", newC02Density, mqttPublishResult);

        // TODO: Possibly attempt to send a publish in the "SafeTunnels/errors" topic
       }
     }
   }

  // In any case, update the new C02 Density value
  C02Density = newC02Density;

  // Reset the C02 Sampling timer
  ctimer_reset(&C02Timer);
 }

// Temp Timer
static void tempTimerCallback(void* ptr)
 {
  static unsigned int nTimes10 = 0;

  printf("[tempTimer]: Elapsed %u seconds\n",++nTimes10*10);

  ctimer_reset(&tempTimer);
 }

PROCESS_THREAD(safetunnels_sensor_process, ev, data)
{
 PROCESS_BEGIN();

 // Turn on the green LED at power on
 leds_single_on(LEDS_GREEN);

 // Print that the sensor node has started
 LOG_INFO("SafeTunnels sensor node started, MAC = ");
 LOG_INFO_LLADDR(&linkaddr_node_addr);
 LOG_INFO_("\n");

 // Start the nodes' sensors sampling (as they do not depend on the node's connection status)
 ctimer_set(&C02Timer,C02_SENSOR_SAMPLING_PERIOD * CLOCK_SECOND,C02TimerCallback,NULL);
 ctimer_set(&tempTimer,TEMP_SENSOR_SAMPLING_PERIOD * CLOCK_SECOND,tempTimerCallback,NULL);

 // Set the node's MQTT ID as its MAC address
 snprintf(mqtt_client_id, MAC_ADDRESS_SIZE, "%02x%02x%02x%02x%02x%02x",
          linkaddr_node_addr.u8[0], linkaddr_node_addr.u8[1],
          linkaddr_node_addr.u8[2], linkaddr_node_addr.u8[5],
          linkaddr_node_addr.u8[6], linkaddr_node_addr.u8[7]);

 // Broker registration
 mqtt_register(&mqttBrokerConn, &safetunnels_sensor_process, mqtt_client_id, mqtt_event, MAX_TCP_SEGMENT_SIZE);

 mqtt_cli_state = MQTT_CLI_STATE_INIT;
				    
 // Initialize periodic timer to check the status
 etimer_set(&periodic_timer, STATE_MACHINE_PERIODIC);

 /* Main loop */
 while(1)
  {
   PROCESS_YIELD();

   if((ev == PROCESS_EVENT_TIMER && data == &periodic_timer) || ev == PROCESS_EVENT_POLL)
    {
     if(mqtt_cli_state == MQTT_CLI_STATE_INIT && have_connectivity())
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