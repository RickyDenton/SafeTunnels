/* SafeTunnels service sensor (C02 + temperature) */

/* ================================== INCLUDES ================================== */

/* ------------------------------- System Headers ------------------------------- */
#include "contiki.h"
#include "mqtt.h"
#include "sys/etimer.h"
#include "sys/ctimer.h"
#include "dev/button-hal.h"
#include "dev/leds.h"
#include "os/sys/log.h"
#include "random.h"

/* ------------------------------- Other Headers ------------------------------- */
#include "sensor.h"
#include "sensorErrors/sensorErrors.h"
#include "../common/devUtilities.h"
#include <string.h>
#include <strings.h>
#include <limits.h>

/* ==================== PROCESSES DEFINITIONS AND AUTOSTART ==================== */

PROCESS(safetunnels_sensor_process, "SafeTunnels Sensor Process");
AUTOSTART_PROCESSES(&safetunnels_sensor_process);

/* ============================== GLOBAL VARIABLES ============================== */

// MQTT Broker IPv6 Address
static const char mqtt_broker_ipv6_addr[] = MQTT_BROKER_IPV6_ADDR;

// MQTT connection
struct mqtt_connection mqttConn;

// MQTT Client State
uint8_t MQTTCliState = MQTT_CLI_STATE_INIT;

// Stores the result of a MQTT client engine API call
mqtt_status_t mqttCliEngineAPIRes;

// Buffers used to store MQTT topics and messages
char MQTTTopicBuf[MQTT_TOPIC_BUF_SIZE];
char MQTTMsgBuf[MQTT_MESSAGE_BUF_SIZE];

// Periodic timer to check the state of the MQTT client
static struct etimer mqttCliStatusTimer;

// Sensor Values
unsigned short C02Density = USHRT_MAX;   // C02 Density in parts per million (ppm)
unsigned short temp = USHRT_MAX;         // Temperature in celsius degrees

// Average Fan Relative Speed Value (correlated simulation purposes)
unsigned char avgFanRelSpeed = 200;

// Sensor Timers
static struct ctimer C02SamplingTimer;
static struct ctimer tempSamplingTimer;
struct ctimer MQTTBrokerCommLEDBlinkTimer;

// Quantities last MQTT updates in seconds from power
// on (used to prevent disconnection from the broker)
unsigned long C02LastMQTTUpdateTime = 0;
unsigned long tempLastMQTTUpdateTime = 0;

// Buffer used to store application errors descriptions
char errDscr[ERR_DSCR_BUF_SIZE];

/* =========================== FUNCTIONS DEFINITIONS =========================== */

/* ----------------------- MQTT Engine Callback Function ----------------------- */

// MQTT Engine Client Callback function
static void MQTTEngineCallback(__attribute__((unused)) struct mqtt_connection* m, mqtt_event_t event, void* data)
 {
  // Pointer to a message received on a topic the MQTT client is
  // subscribed to (should be "SafeTunnels/avgFanRelSpeed" only)
  struct mqtt_message* recvMsg;

  // The minimum size between a message received on a topic the
  // MQTT client is subscribed to and the (MQTTMsgBuf-1)
  unsigned int minRecvBufSize;

  // Depending on the event passed by the MQTT engine
  switch(event)
   {
    /* ----------- The MQTT client has successfully connected with the MQTT broker ----------- */
    case MQTT_EVENT_CONNECTED:

     // This event can be received only with the MQTT client in the "MQTT_CLI_STATE_BROKER_CONNECTING" state
     if(MQTTCliState != MQTT_CLI_STATE_BROKER_CONNECTING)
      LOG_PUB_ERROR(ERR_SENSOR_MQTT_CONNECTED_IN_INVALID_STATE)

     // Update the MQTT client state
     MQTTCliState = MQTT_CLI_STATE_BROKER_CONNECTED;

     // Log that the MQTT client is now connected with the broker
     LOG_INFO("Successfully connected with the MQTT broker @%s\n",mqtt_broker_ipv6_addr);

     // The subscription attempt will be performed in the next sensor process loop
     break;

    /* ----------- The MQTT client has disconnected from the MQTT broker ----------- */
    case MQTT_EVENT_DISCONNECTED:

     // Log that the node has disconnected from the MQTT broker
     LOG_WARN("DISCONNECTED from the MQTT broker @%s (reason = %u), attempting to reconnect...\n",mqtt_broker_ipv6_addr,*((mqtt_event_t *)data));

     // Update the client state depending on whether it is still online
     if(!isNodeOnline())
      MQTTCliState = MQTT_CLI_STATE_ENGINE_OK;
     else
      MQTTCliState = MQTT_CLI_STATE_NET_OK;

     // Poll the sensor main process to wait for the network and reconnecting with the MQTT broker
     process_poll(&safetunnels_sensor_process);
     break;

    /* -------- The MQTT client successfully subscribed to a topic on the MQTT broker ------- */
    case MQTT_EVENT_SUBACK:

     // Turn on the MQTT broker communication LED
     // when subscribed to a topic on the broker
     leds_single_on(MQTT_BROKER_SUB_LED);

     // Update the MQTT client state
     MQTTCliState = MQTT_CLI_STATE_BROKER_SUBSCRIBED;

     // Log the successful topic subscription on the broker
     LOG_DBG("MQTT Client successfully subscribed to the " TOPIC_AVG_FAN_REL_SPEED " topic on the broker\n");
     break;

    /* -------- The MQTT client successfully subscribed to a topic on the MQTT broker ------- */
    case MQTT_EVENT_UNSUBACK:

     // Turn off the MQTT broker communication LED
     // when unsubscribed from a topic on the broker
     leds_single_off(MQTT_BROKER_SUB_LED);

     // Update the MQTT client state
     MQTTCliState = MQTT_CLI_STATE_BROKER_CONNECTED;

     // Log the error
     //
     // NOTE: Attempting to also publish it does not fit in the implemented pattern
     //
     LOG_DBG("MQTT Client unsubscribed from the " TOPIC_AVG_FAN_REL_SPEED ""
             " topic on the broker, attempting to re-subscribe...\n");

     // Poll the sensor process to attempt the re-subscription
     process_poll(&safetunnels_sensor_process);
     break;

    /* -------- A message on a topic the MQTT client is subscribed on has been received ------- */
    case MQTT_EVENT_PUBLISH:

     // The "data field" in this case represents a
     // received message the MQTT client is subscribed to
     recvMsg = data;

     // Compute the minimum size between a message received on a topic
     // the MQTT client is subscribed to and the (MQTTMsgBuf-1)
     minRecvBufSize = sizeof(MQTTMsgBuf) - 1 > recvMsg->payload_length ? recvMsg->payload_length : sizeof(MQTTMsgBuf) - 1;

     // Copy the message topic and contents into the MQTT local buffers
     strncpy(MQTTTopicBuf, recvMsg->topic, MQTT_TOPIC_BUF_SIZE);
     memcpy(MQTTMsgBuf, recvMsg->payload_chunk, minRecvBufSize);
     MQTTMsgBuf[minRecvBufSize] = '\0';

     // Poll the sensor process to parse the received MQTT message
     process_poll(&safetunnels_sensor_process);
     break;

    case MQTT_EVENT_PUBACK:
     // TODO: Check if necessary, in general called for EVERY publishment acknowledgement
     LOG_DBG("MQTT Client publication complete\n");
     break;

    default:
     LOG_PUB_ERROR(ERR_SENSOR_MQTT_CLI_CALLBACK_UNKNOWN_TYPE,"(%u)",event)
     break;
   }
 }



/* ------------------------- Sensor Process Functions ------------------------- */
 
void blinkMQTTBrokerCommLED(__attribute__((unused)) void* ptr)
 {
  static unsigned char blinkTimes = COMM_LED_BLINK_TIMES;

  // Toggle the MQTT Broker subscription LED
  leds_single_toggle(MQTT_BROKER_SUB_LED);

  // If the MQTT Broker communication LED should be further blinked, reset the timer
  if(blinkTimes-- > 0)
   ctimer_reset(&MQTTBrokerCommLEDBlinkTimer);

  // Otherwise, if the MQTT broker communication blinking has finished
  else
   {
    // Reset the "blinkTimes" variable to its default value
    blinkTimes = COMM_LED_BLINK_TIMES;

    // Turn the LED on or off depending on whether the
    // MQTT client is subscribed to a topic on the broker
    if(MQTTCliState == MQTT_CLI_STATE_BROKER_SUBSCRIBED)
     leds_single_on(MQTT_BROKER_SUB_LED);
    else
     leds_single_off(MQTT_BROKER_SUB_LED);
   }
 }

bool publishMQTTSensorUpdate(char* quantity, unsigned int quantityValue, bool quantityDiffer, unsigned long publishInactivityTime)
 {
  // If the node's MQTT client is NOT connected to the MQTT
  // broker, the updated sampled quantity cannot be published
  if(MQTTCliState < MQTT_CLI_STATE_BROKER_CONNECTED)
   {
    // Log that the sampled quantity updated could not be published
    LOG_WARN("The updated %s value (%u) could not be published (not "
             "connected with the broker)\n", quantity, quantityValue);

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
      snprintf(MQTTTopicBuf, sizeof(MQTTTopicBuf), "SafeTunnels/%s", quantity);

      // Prepare the message to be published
      snprintf(MQTTMsgBuf,
               sizeof(MQTTMsgBuf),
               "{"
               " \"ID\": \"%s\","
               " \"%s\": \"%u\""
               " }", nodeID, quantity, quantityValue);

      // Attempt to publish the message on the topic
      mqttCliEngineAPIRes = mqtt_publish(&mqttConn, NULL, MQTTTopicBuf, (uint8_t*)MQTTMsgBuf, strlen(MQTTMsgBuf), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);

      // If the MQTT publishment was successful
      if(mqttCliEngineAPIRes == MQTT_STATUS_OK)
       {
        // Log that the MQTT publishment was successful
        LOG_INFO("Published updated %s value (%u) to the MQTT broker\n", quantity, quantityValue);

        // Blink the communication LED
        ctimer_set(&MQTTBrokerCommLEDBlinkTimer, COMM_LED_BLINK_PERIOD, blinkMQTTBrokerCommLED, NULL);

        // Return that the MQTT publishment was successful
        return true;
       }

       // Otherwise, if the MQTT publishment was NOT successful
      else
       {
        // If the MQTT publishment failed for a MQTT_STATUS_OUT_QUEUE_FULL error,
        // just log it, as attempting to publishing it would result in the same error
        if(mqttCliEngineAPIRes == MQTT_STATUS_OUT_QUEUE_FULL)
         LOG_ERR("Failed to publish updated %s value (%u) because the MQTT outbound queue is full (MQTT_STATUS_OUT_QUEUE_FULL) \n", quantity, quantityValue);

        // Otherwise, log the error and attempt to publish it
        else
         LOG_PUB_ERROR(ERR_SENSOR_QUANTITY_PUB_FAILED, "(%s = %u, error = \'%s\', MQTTCliState = \'%s\')", quantity, quantityValue, MQTTEngineAPIResultStr(),MQTTCliStateToStr())

        // Return that the MQTT publishment was NOT successful
        return false;
       }
     }

    // Otherwise, if a same sampled value was not published to the broker
    else
     {
      // Log that the same sampled value was not published to the broker
      LOG_INFO("A same sampled %s value (%u) was NOT published the MQTT broker for energy savings purposes\n", quantity, quantityValue);

      // Return that quantity has not been published to the broker
      return false;
     }
   }
 }


// C02 periodic sampling function (timer)
static void C02PeriodicSampling(__attribute__((unused)) void* ptr)
 {
  unsigned int newC02Density;

  /* ---- TODO: Generate the C02 value depending on the "fanSpeedRel" value ---- */

  newC02Density = random_rand();
  // LOG_DBG("New randomly generated C02 density: %u\n",newC02Density);

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


// Temperature periodic sampling function (timer)
static void tempPeriodicSampling(__attribute__((unused)) void* ptr)
 {
  unsigned int newTemp;

  /* ---- TODO: Generate the temperature value depending on the "fanSpeedRel" value ---- */

  newTemp = random_rand();
  // LOG_DBG("New randomly generated temperature: %u\n",newTemp);

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



void sensor_MQTT_CLI_STATE_INIT_Callback()
 {
  // Attempt to initialize the MQTT client engine
  mqttCliEngineAPIRes = mqtt_register(&mqttConn, &safetunnels_sensor_process, nodeID, MQTTEngineCallback, MQTT_MAX_TCP_SEGMENT_SIZE);

  // If the MQTT client engine initialization was successful
  if(mqttCliEngineAPIRes == MQTT_STATUS_OK)
   {
    // Update the MQTT client state
    MQTTCliState = MQTT_CLI_STATE_ENGINE_OK;

    // Log that now the successful MQTT client engine initialization
    // and that the node will now wait for the RPL DODAG to converge
    LOG_DBG("MQTT Client engine successfully initialized\n");
    LOG_DBG("Waiting for the RPL DODAG to converge...\n");
   }

   // Otherwise, if the MQTT client engine initialization was NOT successful,
   // log the error (and try again at the next client status timer activation)
  else
   LOG_ERR("FAILED to initialize the MQTT Client engine (error = %u)", mqttCliEngineAPIRes);

  // Reinitialize the MQTT client status timer with the bootstrap period
  etimer_set(&mqttCliStatusTimer, SENSOR_MQTT_CLI_STATUS_LOOP_TIMER_PERIOD);
 }

void sensor_MQTT_CLI_STATE_ENGINE_OK_Callback()
 {
  // A byte used to suppress most of the sensor logs
  // relative to waiting the RPL DODAG to converge
  static unsigned char RPLWaitingTimes = 0;

  // If the node can communicate with hosts external to the LLN
  if(isNodeOnline())
   {
    // Reset the RPLWaitingTimes;
    RPLWaitingTimes = 0;

    // Log that the node is online
    LOG_DBG("The node is now online\n");

    // Update the MQTT client state
    MQTTCliState = MQTT_CLI_STATE_NET_OK;
   }

   // Otherwise, if the node CANNOT (yet) communicate with hosts external to
   // the LLN, log it every SENSOR_MQTT_CLI_RPL_WAITING_TIMES_MODULE attempts
  else
   if(++RPLWaitingTimes % SENSOR_MQTT_CLI_RPL_WAITING_TIMES_MODULE == 0)
    LOG_DBG("Waiting for the RPL DODAG to converge...\n");

  // Reinitialize the MQTT client status timer with the bootstrap period
  etimer_set(&mqttCliStatusTimer, SENSOR_MQTT_CLI_STATUS_LOOP_TIMER_PERIOD);
 }


void sensor_MQTT_CLI_STATE_NET_OK_Callback()
 {
  // Prepare the MQTT client "last will" topic and message to be published
  // automatically by the broker should the node disconnect from it
  snprintf(MQTTTopicBuf, sizeof(MQTTTopicBuf), "SafeTunnels/sensorsErrors");
  snprintf(MQTTMsgBuf,
           sizeof(MQTTMsgBuf),
           "{"
           " \"ID\": \"%s\","
           " \"errCode\": %u"
           " }", nodeID, ERR_SENSOR_MQTT_DISCONNECTED);

  // Set the MQTT client "last will" message
  mqtt_set_last_will(&mqttConn, (char*)MQTTTopicBuf, MQTTMsgBuf, MQTT_QOS_LEVEL_0);

  // Attempt to connect with the MQTT broker
  mqttCliEngineAPIRes = mqtt_connect(&mqttConn, (char*)mqtt_broker_ipv6_addr, MQTT_BROKER_DEFAULT_PORT, MQTT_BROKER_KEEPALIVE_TIMEOUT, MQTT_CLEAN_SESSION_ON);

  // If the MQTT broker connection has been successfully submitted
  if(mqttCliEngineAPIRes == MQTT_STATUS_OK)
   {
    // Log that the MQTT client is attempting to connect with the MQTT broker
    LOG_DBG("Attempting to connect with the MQTT broker @%s...\n",mqtt_broker_ipv6_addr);

    // Update the MQTT client state
    MQTTCliState = MQTT_CLI_STATE_BROKER_CONNECTING;

    // TODO: Test (activated because the process_poll was deactivated in the MQTT engine handler);
    etimer_set(&mqttCliStatusTimer, SENSOR_MQTT_CLI_STATUS_LOOP_TIMER_PERIOD);
   }

   // Otherwise, if the MQTT broker connection has NOT been successfully submitted
  else
   {
    // Log the error
    LOG_ERR("FAILED to attempt to connect with the MQTT broker (error = \'%s\')", MQTTEngineAPIResultStr());

    // Try again at the next client status timer activation
    etimer_set(&mqttCliStatusTimer, SENSOR_MQTT_CLI_STATUS_LOOP_TIMER_PERIOD);
   }
 }


void sensor_MQTT_CLI_STATE_BROKER_CONNECTED_Callback()
 {
  // Attempt to subscribe to the TOPIC_AVG_FAN_REL_SPEED topic
  snprintf(MQTTTopicBuf, sizeof(MQTTTopicBuf), TOPIC_AVG_FAN_REL_SPEED);
  mqttCliEngineAPIRes = mqtt_subscribe(&mqttConn, NULL, MQTTTopicBuf, MQTT_QOS_LEVEL_0);

  // If the topic subscription submission was successful
  if(mqttCliEngineAPIRes == MQTT_STATUS_OK)
   {
    // Log the successful submission
    LOG_DBG("Submitted subscription to the " TOPIC_AVG_FAN_REL_SPEED " topic\n");

    // Note that the MQTT client timer is NOT reinitialized in this case (we wait
    // for the MQTTEngineCallback MQTT_EVENT_SUBACK or MQTT_EVENT_UNSUBACK events)
   }

   // Otherwise, if the topic subscription submission was unsuccessful
  else
   {
    // Log and attempt to publish the error
    LOG_PUB_ERROR(ERR_SENSOR_AVGFANRELSPEED_SUB_FAILED, "(error = \'%s\', MQTTCliState = \'%s\')",
                  MQTTEngineAPIResultStr(), MQTTCliStateToStr())

    // Reinitialize the timer to try again at the next cycle
    etimer_set(&mqttCliStatusTimer, SENSOR_MQTT_CLI_STATUS_LOOP_TIMER_PERIOD);
   }
 }


// Called by the MQTT Engine when receiving a message on a topic the sensor is subscribed to
void sensor_MQTT_CLI_STATE_BROKER_SUBSCRIBED_Callback()
 {
  // The candidate "avgFanRelSpeed" value received
  // on the TOPIC_AVG_FAN_REL_SPEED topic, if any
  unsigned long recvFanSpeedRelative;

  // Ensure the received MQTT message topic to consist of the only TOPIC_AVG_FAN_REL_SPEED
  // topic a sensor can be subscribed to, logging and publishing the error otherwise
  if(strncmp(MQTTTopicBuf, TOPIC_AVG_FAN_REL_SPEED, sizeof(MQTTTopicBuf)) != 0)
   LOG_PUB_ERROR(ERR_SENSOR_MQTT_RECV_UNKNOWN_TOPIC, "(topic = %s, msg = %.100s)", MQTTTopicBuf, MQTTMsgBuf)
  else
   {
    // Interpret the message's contents as an unsigned (long) integer
    recvFanSpeedRelative = strtoul(MQTTMsgBuf, NULL, 10);

    // Ensure the received average fan relative speed value to
    // be valid, logging and publishing the error otherwise
    if(recvFanSpeedRelative > 100)
     LOG_PUB_ERROR(ERR_SENSOR_AVGFANRELSPEED_INVALID, "(%lu > 100)", recvFanSpeedRelative)
    else
     {
      // Update the "avgFanRelSpeed" to the received value
      avgFanRelSpeed = recvFanSpeedRelative;

      // Log the received average fan relative speed value
      LOG_INFO("Received new average fan speed value: %u\n", avgFanRelSpeed);

      // Note that the MQTT client timer is NOT reinitialized in this case (the
      // 'MQTT_CLI_STATE_BROKER_SUBSCRIBED' state is permanent at the steady state)
     }
   }
 }


/* SafeTunnels Sensor Process Body */
PROCESS_THREAD(safetunnels_sensor_process, ev, data)
{
 // Contiki-NG process start
 PROCESS_BEGIN()

 // Turn the on the POWER_LED
 leds_single_on(POWER_LED);

 // Set the node's ID as its MAC address
 initNodeID();

 // Log that the sensor node has started along with its MAC
 LOG_INFO("SafeTunnels sensor node started, MAC = %s\n",nodeID);

 // Start the nodes' sensors sampling (as they do not depend on the node's connection status)
 ctimer_set(&C02SamplingTimer, C02_SENSOR_SAMPLING_PERIOD, C02PeriodicSampling, NULL);
 ctimer_set(&tempSamplingTimer, TEMP_SENSOR_SAMPLING_PERIOD, tempPeriodicSampling, NULL);

 /* ------------ MQTT Initialization ------------ */

 // Initialize the MQTT client status timer with the bootstrap period
 etimer_set(&mqttCliStatusTimer, (clock_time_t)SENSOR_MQTT_CLI_STATUS_LOOP_TIMER_PERIOD);

 /* Sensor Process Main Loop */
 while(1)
  {
   // Yield the process's execution
   PROCESS_YIELD();

   // If the MQTT client status timer has expired, or the process has been explicitly polled
   if((ev == PROCESS_EVENT_TIMER && data == &mqttCliStatusTimer) || ev == PROCESS_EVENT_POLL)
    {
     // Depending on the MQTT client current state
     switch(MQTTCliState)
      {
       /* ---------- The MQTT client engine must yet be initialized ---------- */
       case MQTT_CLI_STATE_INIT:
        sensor_MQTT_CLI_STATE_INIT_Callback();
        break;

       /*
        * The MQTT client engine has been initialized, and we must wait for the
        * RPL DODAG to converge before attempting to connect with the MQTT broker
        * */
       case MQTT_CLI_STATE_ENGINE_OK:
        sensor_MQTT_CLI_STATE_ENGINE_OK_Callback();
        break;

       /* ---------- The node is online and must attempt to connect with the broker ---------- */
       case MQTT_CLI_STATE_NET_OK:
        sensor_MQTT_CLI_STATE_NET_OK_Callback();
        break;

       /* ----- The node is currently attempting to connect with the broker (should never be called) ----- */
       case MQTT_CLI_STATE_BROKER_CONNECTING:
        // TODO: Test (activated because the process_poll was deactivated in the MQTT engine handler);
        etimer_set(&mqttCliStatusTimer, SENSOR_MQTT_CLI_STATUS_LOOP_TIMER_PERIOD);
        //LOG_WARN("Process main loop in the MQTT_CLI_STATE_BROKER_CONNECTING state (should never happen)\n");
        break;

       case MQTT_CLI_STATE_BROKER_CONNECTED:
        sensor_MQTT_CLI_STATE_BROKER_CONNECTED_Callback();
        break;

       // Called by the MQTT Engine when receiving a message on a topic the sensor is subscribed to
       case MQTT_CLI_STATE_BROKER_SUBSCRIBED:
        sensor_MQTT_CLI_STATE_BROKER_SUBSCRIBED_Callback();
        break;

       default:
        LOG_PUB_ERROR(ERR_SENSOR_MAIN_LOOP_UNKNOWN_MQTT_CLI_STATE, "(MQTTCliState = %u)", MQTTCliState)
        break;
      }
    }

   // Otherwise, if any button has been pressed
   else
    if(ev == button_hal_press_event)
     {
      // Log that the C02 and Temperature values have been simulated to their maximum values
      LOG_INFO("Sampled quantities set to their maximum values (C02Density = %u, temp = %u)\n",C02_VALUE_MAX,TEMP_VALUE_MAX);

      // Set the C02 and Temperature quantities to their maximum "unsafe"
      // values for the purposes of simulating the service's feedback mechanism
      C02Density = C02_VALUE_MAX;
      if(publishMQTTSensorUpdate("C02", C02Density, true, ULONG_MAX))
       C02LastMQTTUpdateTime = clock_seconds();

      temp = TEMP_VALUE_MAX;
       if(publishMQTTSensorUpdate("temp", temp, true, ULONG_MAX))
        tempLastMQTTUpdateTime = clock_seconds();
     }

    // Unknown event
    else
     ;
     // TODO: Always passes event 150, check if relevant
     //LOG_ERR("An unknown event was passed to the sensor main loop (%u)\n",ev);
     //LOG_PUB_ERROR(ERR_SENSOR_MAIN_LOOP_UNKNOWN_EVENT,"(event = %u)",ev)
  }


 /* ------------ Execution should NEVER reach here ------------ */

 // Turn off both LEDs
 leds_off(LEDS_NUM_TO_MASK(POWER_LED) | LEDS_NUM_TO_MASK(MQTT_BROKER_SUB_LED));

 // Attempt to log and publish the error
 LOG_PUB_ERROR(ERR_SENSOR_MAIN_LOOP_EXITED,"(last event= %u)",ev)

 PROCESS_END()
}