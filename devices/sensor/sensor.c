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

// MQTT Broker IPv6 Address
static const char mqtt_broker_ipv6_addr[] = MQTT_BROKER_IPV6_ADDR;


// MQTT Client State
static uint8_t mqtt_cli_state = MQTT_CLI_STATE_INIT;

// Stores the result of a MQTT client engine API call
static unsigned int mqttCliEngineAPIRes;

// Buffers used to store MQTT topics and messages
static char MQTTTopicBuf[MQTT_TOPIC_BUF_SIZE];
static char MQTTMessageBuf[MQTT_MESSAGE_BUF_SIZE];

// Buffer used to store application errors descriptions
static char errDscr[APP_ERR_DSCR_BUF_SIZE];









// ClientIDs and Topics Buffer Sizes TODO CHECK!
static char pub_topic[BUFFER_SIZE];
static char sub_topic[BUFFER_SIZE];

static int value = 0;

//Periodic timer to check the state of the MQTT client
static struct etimer mqttCliStatusTimer;

// Main MQTT buffers (to be increased if we start to publish more data) TODO: CHECK
static char app_buffer[APP_BUFFER_SIZE];




// MQTT message pointer
static struct mqtt_message* msg_ptr = NULL;

// MQTT connection
static struct mqtt_connection mqttConn;

// MQTT status
mqtt_status_t status;




// Sensor Values
unsigned short C02Density = USHRT_MAX;   // C02 Density in parts per million (ppm)
unsigned short temp = USHRT_MAX;         // Temperature in celsius degrees

// Average Fan Speed Relative Value (correlated simulation purposes)
unsigned char fanSpeedRel = 200;

// Sensor Timers
static struct ctimer C02SamplingTimer;
static struct ctimer tempSamplingTimer;

// Quantities last MQTT updates in seconds from power
// on (used to prevent disconnection from the broker)
unsigned long C02LastMQTTUpdateTime = 0;
unsigned long tempLastMQTTUpdateTime = 0;



/* =========================== FUNCTIONS DEFINITIONS =========================== */



void logPublishError()
 {
  // If the node's MQTT client is NOT connected with the MQTT
  // broker, just log the error and that it couldn't be published
  if(mqtt_cli_state < MQTT_CLI_STATE_BROKER_CONNECTED)
   LOG_ERR("%s (the error couldn't be published as the MQTT client is not connected with the broker)", errDscr);

   // Otherwise, if the node's MQTT client IS connected with the MQTT broker
  else
   {
    // Attempt to publish the error on the "SafeTunnels/SensorsCtrlEvents" topic
    snprintf(MQTTTopicBuf, MQTT_TOPIC_BUF_SIZE, "SafeTunnels/SensorsCtrlEvents");
    snprintf(MQTTMessageBuf, MQTT_MESSAGE_BUF_SIZE, "{"
                                                    " \"ID\": \"%s\""
                                                    " \"error\": \"%s\""
                                                    " }", nodeID, errDscr);

    mqttCliEngineAPIRes = mqtt_publish(&mqttConn, NULL, MQTTTopicBuf, (uint8_t*)MQTTMessageBuf, strlen(MQTTMessageBuf), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);

    // Log the error and whether it was successfully published
    if(mqttCliEngineAPIRes == MQTT_STATUS_OK)
     LOG_ERR("%s (published)\n", errDscr);
    else
     LOG_ERR("%s (error that could also NOT published for error code \"%u\")\n", errDscr, mqttCliEngineAPIRes);
   }
 }



bool publishMQTTSensorUpdate(char* quantity, unsigned int quantityValue, bool quantityDiffer, unsigned long publishInactivityTime)
 {
  // If the node's MQTT client is NOT connected to the MQTT
  // broker, the updated sampled quantity cannot be published
  if(mqtt_cli_state < MQTT_CLI_STATE_BROKER_CONNECTED)
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
      // Prepare the topic of the message to be published  ("SafeTunnels/C02" || "SafeTunnels/temp")
      snprintf(MQTTTopicBuf, MQTT_TOPIC_BUF_SIZE, "SafeTunnels/%s", quantity);

      // Prepare the message to be published
      snprintf(MQTTMessageBuf,
               MQTT_MESSAGE_BUF_SIZE,
               "{"
               " \"ID\": \"%s\""
               " \"%s\": \"%u\""
               " }", nodeID, quantity, quantityValue);

      // Attempt to publish the message on the topic
      mqttCliEngineAPIRes = mqtt_publish(&mqttConn, NULL, MQTTTopicBuf, (uint8_t*)MQTTMessageBuf, strlen(MQTTMessageBuf), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);

      // If the MQTT publishment was successful
      if(mqttCliEngineAPIRes == MQTT_STATUS_OK)
       {
        // Log that the MQTT publishment was successful
        LOG_INFO("Published updated %s (%u) to the MQTT broker\n", quantity, quantityValue);

        // Return that the MQTT publishment was successful
        return true;
       }

       // Otherwise, if the MQTT publishment was NOT successful
      else
       {
        // Log the error and attempt to publish it
        snprintf(errDscr, APP_ERR_DSCR_BUF_SIZE, "FAILED to publish the updated %s (%u) to the MQTT broker (error = %u)", quantity, quantityValue, mqttCliEngineAPIRes);
        logPublishError();

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









// MQTT Engine Client Callback function
static void mqttEngineCliCallback(struct mqtt_connection* m, mqtt_event_t event, void* data)
{
 // Pointer to a message received on a topic the MQTT client is
 // subscribed to (should be "SafeTunnels/avgFanRelSpeed" only)
 struct mqtt_message* recvMsg;
 unsigned long recvFanSpeedRelative;

 // Depending on the event passed by the MQTT engine
 switch(event)
  {
   /* ----------- The MQTT client has successfully connected with the MQTT broker ----------- */
   case MQTT_EVENT_CONNECTED:

    // Update the MQTT client state
    mqtt_cli_state = MQTT_CLI_STATE_BROKER_CONNECTED;

    // Turn on the yellow LED when connected with the broker
    leds_single_on(LEDS_YELLOW);

    // Log that the MQTT client is now connected with the broker
    LOG_INFO("Successfully connected with the MQTT broker @%s\n",mqtt_broker_ipv6_addr);

    // If a sampled CO2 density or temperature value are available, immediately
    // publish them on the broker, updating their last updated times if successful
    if(C02Density < USHRT_MAX)
     if(publishMQTTSensorUpdate("C02", C02Density, true, ULONG_MAX))
      C02LastMQTTUpdateTime = clock_seconds();

    if(temp < USHRT_MAX)
     if(publishMQTTSensorUpdate("temp", temp, true, ULONG_MAX))
      tempLastMQTTUpdateTime = clock_seconds();

    // Attempt to subscribe to the "SafeTunnels/avgFanRelSpeed" topic
    snprintf(MQTTTopicBuf, MQTT_TOPIC_BUF_SIZE, "SafeTunnels/avgFanRelSpeed");
    mqttCliEngineAPIRes = status = mqtt_subscribe(&mqttConn, NULL, MQTTTopicBuf, MQTT_QOS_LEVEL_0);

    // If the topic subscription was successful
    if(mqttCliEngineAPIRes == MQTT_STATUS_OK)
     {
      // Update the MQTT client state
      mqtt_cli_state = MQTT_CLI_STATE_BROKER_SUBSCRIBED;

      // Log that the sensor has successfully subscribed to the "SafeTunnels/avgFamRelSpeed" topic
      LOG_DBG("Sensor successfully subscribed to \"SafeTunnels/avgFanRelSpeed\" topic\n");
     }

    // Otherwise, if the topic subscription was unsuccessful
    else
     {
      // Log the error and attempt to publish it
      snprintf(errDscr, APP_ERR_DSCR_BUF_SIZE, "FAILED to subscribe on the \"SafeTunnels/avgFanRelSpeed\" topic (error = %u)", mqttCliEngineAPIRes);
      logPublishError();
     }

      /*
       {
      // Attempt to publish the error on the "SafeTunnels/SensorsCtrlEvents" topic
      snprintf(MQTTTopicBuf, MQTT_TOPIC_BUF_SIZE, "SafeTunnels/SensorsCtrlEvents");
      snprintf(MQTTMessageBuf, MQTT_MESSAGE_BUF_SIZE, "{"
                                                          " \"ID\": \"%s\""
                                                          " \"event\": \"offline\""
                                                          " }", nodeID);

      mqttCliEngineAPIRes = mqtt_publish(&mqttConn, NULL, MQTTTopicBuf, (uint8_t*)MQTTMessageBuf, strlen(MQTTMessageBuf), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);

      // Log the error and whether it was successfully published
      if(mqttCliEngineAPIRes == MQTT_STATUS_OK)
       LOG_ERR("FAILED to subscribe on the \"SafeTunnels/avgFanRelSpeed\" topic (published)\n");
      else
       LOG_ERR("FAILED to subscribe on the \"SafeTunnels/avgFanRelSpeed\" topic AND to publish the error\n");
     }
       */
    break;

   /* ----------- The MQTT client has disconnected from the MQTT broker ----------- */
   case MQTT_EVENT_DISCONNECTED:

    // Turn off the yellow LED when disconnected from the broker
    leds_single_off(LEDS_YELLOW);

    // Log that the node has disconnected from the MQTT broker
    LOG_WARN("DISCONNECTED from the MQTT broker @%s (reason: %u), attempting to reconnect...\n",mqtt_broker_ipv6_addr,*(unsigned int*)data);

    // Update the client state depending on whether it is still online
    if(isNodeOnline())
     mqtt_cli_state = MQTT_CLI_STATE_NET_OK;
    else
     MQTT_CLI_STATE_ENGINE_OK;

    // Poll the sensor main process to attempt reconnecting with the MQTT broker
    process_poll(&safetunnels_sensor_process);
    break;


   /* -------- A message on a topic the MQTT client is subscribed on has been received ------- */
   case MQTT_EVENT_PUBLISH:

    // The data passed to the callback function in this case consists
    // of a message received on a topic the node is subscribed to
    recvMsg = data;

    // Ensure the message to have been received in a consistent
    // MQTT client state, logging and publishing the error otherwise
    if(mqtt_cli_state != MQTT_CLI_STATE_BROKER_SUBSCRIBED)
     {
       snprintf(errDscr, APP_ERR_DSCR_BUF_SIZE, "Received message with the MQTT client not in the MQTT_CLI_STATE_BROKER_SUBSCRIBED"
                                                " state (MQTT_CLI_STATE = %u, topic = %s, msg = %s)",mqtt_cli_state,recvMsg->topic,recvMsg->payload_chunk);
       logPublishError();
     }

    // Ensure the message to have been received on the only
    // "SafeTunnels/avgFanRelSpeed" topic a sensor can be subscribed to
    else
     if(strcmp(msg_ptr->topic, "SafeTunnels/avgFanRelSpeed") != 0)
      {
       snprintf(errDscr, APP_ERR_DSCR_BUF_SIZE, "Received message on a topic the MQTT client is not subscribed on"
                                                " (topic = %s, msg = %s)",recvMsg->topic,recvMsg->payload_chunk);
       logPublishError();
      }

     // Otherwise, if a message on the expected "SafeTunnels/avgFanRelSpeed" topic was received
     // with the MQTT client in the expected "MQTT_CLI_STATE_BROKER_SUBSCRIBED" state
     else
      {
       // Interpret the received message as an unsigned (long) integer
       recvFanSpeedRelative = strtoul((char*)recvMsg->payload_chunk, NULL, 10);

       // If the received average fan relative speed value is invalid (> 100)
       if(recvFanSpeedRelative > 100)
        {
         // Log the error and attempt to publish it
         snprintf(errDscr, APP_ERR_DSCR_BUF_SIZE, "Received a \"fanSpeedRelative\" of invalid value (%lu > 100)", recvFanSpeedRelative);
         logPublishError();
        }

       // Otherwise, if the received average fan relative speed value is valid (<= 100)
       else
        {
         // Set the sensor's average fan relative speed value
         fanSpeedRel = recvFanSpeedRelative;

         // Log the received average fan relative speed value
         LOG_INFO("Received new average fan speed value: %u",fanSpeedRel);
        }
    break;


// TODO: Continue from here, also divide the switch cases in helper functions (here and in the main process)

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
  if(publishMQTTSensorUpdate("C02", newC02Density, newC02Density != C02Density, clock_seconds() - C02LastMQTTUpdateTime))
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
  if(publishMQTTSensorUpdate("temp", newTemp, newTemp != temp, clock_seconds() - tempLastMQTTUpdateTime))
   tempLastMQTTUpdateTime = clock_seconds();

  // In any case, update the new C02 Density value
  temp = newTemp;

  // Reset the temperature sampling timer
  ctimer_reset(&tempSamplingTimer);
 }

/* SafeTunnels Sensor Process Body */
PROCESS_THREAD(safetunnels_sensor_process, ev, data)
{
 // Contiki-NG process start
 PROCESS_BEGIN()

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

 // Initialize the MQTT client status timer with the bootstrap period
 etimer_set(&mqttCliStatusTimer, (clock_time_t)MQTT_CLI_STATUS_TIMER_PERIOD_BOOTSTRAP);

 /* Process Main Loop */
 while(1)
  {
   // Yield the process's execution
   PROCESS_YIELD();

   // If the MQTT client status timer has expired, or the process has been explicitly polled
   if((ev == PROCESS_EVENT_TIMER && data == &mqttCliStatusTimer) || ev == PROCESS_EVENT_POLL)
    {
     // Depending on the MQTT client current state
     switch(mqtt_cli_state)
      {
       /* ---------- The MQTT client engine must yet be initialized ---------- */
       case MQTT_CLI_STATE_INIT:

        // Attempt to initialize the MQTT client engine
        mqttCliEngineAPIRes = mqtt_register(&mqttConn, &safetunnels_sensor_process, nodeID, mqttEngineCliCallback, MQTT_MAX_TCP_SEGMENT_SIZE);

       // If the MQTT client engine initialization was successful
       if(mqttCliEngineAPIRes == MQTT_STATUS_OK)
        {
         // Log the successful MQTT client engine initialization
         LOG_DBG("MQTT Client engine successfully initialized\n");

         // Update the MQTT client state
         mqtt_cli_state = MQTT_CLI_STATE_ENGINE_OK;
        }

        // Otherwise, if the MQTT client engine initialization was NOT successful,
        // log the error (and try again at the next client status timer activation)
       else
        LOG_ERR("FAILED to initialize the MQTT Client engine (error = %u)", mqttCliEngineAPIRes);

       // Reinitialize the MQTT client status timer with the bootstrap period
       etimer_set(&mqttCliStatusTimer, MQTT_CLI_STATUS_TIMER_PERIOD_BOOTSTRAP);
       break;


       /*
        * The MQTT client engine has been initialized, and we must wait for the
        * RPL DODAG to converge before attempting to connect with the MQTT broker
        * */
       case MQTT_CLI_STATE_ENGINE_OK:

        // If the node can communicate with hosts external to the LLN
        if(isNodeOnline())
         {
          // Log that the node is online
          LOG_DBG("The node is now online\n");

          // Update the MQTT client state
          mqtt_cli_state = MQTT_CLI_STATE_NET_OK;
         }

         // Otherwise, if the node CANNOT (yet) communicate with hosts external to
         // the LLN, log it (and try again at the next client status timer activation)
        else
         LOG_DBG("Waiting for the RPL DODAG to converge...\n");

       // Reinitialize the MQTT client status timer with the bootstrap period
       etimer_set(&mqttCliStatusTimer, MQTT_CLI_STATUS_TIMER_PERIOD_BOOTSTRAP);
       break;


       /* ---------- The node is online and must attempt to connect with the broker ---------- */
       case MQTT_CLI_STATE_NET_OK:

        // Prepare the MQTT client "last will" topic and message to be published
        // automatically by the broker should the node disconnect from it
        snprintf(MQTTTopicBuf, MQTT_TOPIC_BUF_SIZE, "SafeTunnels/SensorsCtrlEvents");
        snprintf(MQTTMessageBuf,
                 MQTT_MESSAGE_BUF_SIZE,
                 "{"
                 " \"ID\": \"%s\""
                 " \"event\": \"offline\""
                 " }", nodeID);

        // Set the MQTT client "last will" message
        mqtt_set_last_will(&mqttConn, (char*)MQTTTopicBuf, MQTTMessageBuf, MQTT_QOS_LEVEL_0);

        // Attempt to connect with the MQTT broker
        // TODO: It was changed from MQTT_CLEAN_SESSION_ON -> MQTT_CLEAN_SESSION_OFF, check if it causes problems
        mqttCliEngineAPIRes = mqtt_connect(&mqttConn, (char*)mqtt_broker_ipv6_addr, MQTT_BROKER_DEFAULT_PORT, MQTT_BROKER_KEEPALIVE_TIMEOUT, MQTT_CLEAN_SESSION_OFF);

        // If the MQTT broker connection has been successfully submitted
        if(mqttCliEngineAPIRes == MQTT_STATUS_OK)
         {
          // Log that the MQTT client is attempting to connect with the MQTT broker
          LOG_DBG("Attempting to connect with the MQTT broker\n");

          // Update the MQTT client state
          mqtt_cli_state = MQTT_CLI_STATE_BROKER_CONNECTING;
         }

        // Otherwise, if the MQTT broker connection has NOT been successfully submitted
        // log the error (and try again at the next client status timer activation)
        else
         LOG_ERR("FAILED to attempt to connect with the MQTT broker (error = %u)\n", mqttCliEngineAPIRes);

        // TODO: Reinitializing the timer is probably NOT required here (further events are handled by the mqttEngineCliCallback())
        // etimer_set(&mqttCliStatusTimer, MQTT_CLI_STATUS_TIMER_PERIOD_BOOTSTRAP);
        break;

       /* ---------- Other MQTT Client states (should never happen)  ---------- */

       case MQTT_CLI_STATE_BROKER_CONNECTING:
        LOG_WARN("Process main loop in the MQTT_CLI_STATE_BROKER_CONNECTING state (should never happen)\n");
        break;

       case MQTT_CLI_STATE_BROKER_CONNECTED:
        LOG_WARN("Process main loop in the MQTT_CLI_STATE_BROKER_CONNECTED state (should never happen)\n");
        break;

       case MQTT_CLI_STATE_BROKER_SUBSCRIBED:
        LOG_WARN("Process main loop in the MQTT_CLI_STATE_BROKER_SUBSCRIBED state (should never happen)\n");
        break;

       default:
        LOG_ERR("Unknown MQTT client state in the process main loop (%u)\n",mqtt_cli_state);
        break;
/*
		  if(mqtt_cli_state == MQTT_CLI_STATE_BROKER_CONNECTED)
       {
        // Subscribe to a topic
			  strcpy(sub_topic,"actuator");

			  status = mqtt_subscribe(&mqttConn, NULL, sub_topic, MQTT_QOS_LEVEL_0);

			  printf("Subscribing!\n");
			  if(status == MQTT_STATUS_OUT_QUEUE_FULL)
         {
				  LOG_ERR("Tried to subscribe but command queue was full!\n");
				  PROCESS_EXIT();
			   }
        mqtt_cli_state = MQTT_CLI_STATE_BROKER_SUBSCRIBED;
		   }

		 if(mqtt_cli_state == MQTT_CLI_STATE_BROKER_SUBSCRIBED)
      {
			 // Publish something
       sprintf(pub_topic, "%s", "status");

			 sprintf(app_buffer, "report %d", value);

			 value++;

			 mqtt_publish(&mqttConn, NULL, pub_topic, (uint8_t *)app_buffer, strlen(app_buffer), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);

		  }
     else
      if(mqtt_cli_state == MQTT_CLI_STATE_INIT )
       {
		    LOG_ERR("Disconnected form MQTT broker\n");
		    // Recover from error
		   }
		
		 etimer_set(&mqttCliStatusTimer, MQTT_CLI_STATUS_TIMER_PERIOD);

      */
      }
    }

   // TODO: Buttons!
  }

 // Execution should never reach here
 // TODO: Send publish a message on the SafeTunnels/errors topic?
 PROCESS_END()
}