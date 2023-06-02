/* SafeTunnels Sensor Application Definitions */

/* ================================== INCLUDES ================================== */

/* ------------------------------ Standard Headers ------------------------------ */
#include <string.h>
#include <strings.h>
#include <limits.h>

/* ----------------------------- Contiki-NG Headers ----------------------------- */
#include "contiki.h"
#include "mqtt.h"
#include "sys/etimer.h"
#include "sys/ctimer.h"
#include "dev/button-hal.h"
#include "dev/leds.h"
#include "os/sys/log.h"
#include "random.h"

/* ------------------------ SafeTunnels Service Headers ------------------------ */
#include "sensor.h"
#include "../common/devUtilities.h"


/* ================ CONTIKI-NG PROCESS DEFINITION AND AUTOSTART ================ */
PROCESS(safetunnels_sensor_process, "SafeTunnels Sensor Process");
AUTOSTART_PROCESSES(&safetunnels_sensor_process);


/* ============================== GLOBAL VARIABLES ============================== */

/* -------------------- General Application Global Variables -------------------- */

// Sensor main loop timer
static struct etimer sensorMainLoopTimer;

// Sensor MQTT Client State
static uint8_t MQTTCliState = MQTT_CLI_STATE_INIT;

// Buffer used to store application errors descriptions
static char errDscr[ERR_DSCR_BUF_SIZE];

// The timer used to blink the MQTT_COMM_LED
// upon publishing a MQTT message
static struct ctimer MQTTCommLEDTimer;


/* --------------------------- MQTT Global Variables --------------------------- */

// MQTT messages topics and contents buffers
static char MQTTMsgTopicBuf[MQTT_MSG_TOPIC_BUF_SIZE];
static char MQTTMsgContentsBuf[MQTT_MSG_CONTENTS_BUF_SIZE];

// Stores the result of a MQTT engine API call
static mqtt_status_t MQTTEngineAPIRes;

// MQTT Broker IPv6 Address
static const char MQTTBrokerIPv6Addr[] = MQTT_BROKER_IPV6_ADDR;

// The structure used to manage the connection between the MQTT client and broker
static struct mqtt_connection mqttConn;


/* ------------------------- Sampling Global Variables ------------------------- */

// Current quantities values (USHRT_MAX -> not sampled yet)
unsigned short C02Density = USHRT_MAX;   // In parts per million (ppm)
unsigned short temp = USHRT_MAX;         // In celsius degrees (°C)

// Quantities sampling timers
static struct ctimer C02SamplingTimer;
static struct ctimer tempSamplingTimer;

// Set upon pressing any button to notify the quantities sampling
// function to report a maximum, "unsafe" value at the next sampling
static bool simulateMaxC02 = false;
static bool simulateMaxTemp = false;

// Seconds from power on the quantities were last published to the broker,
// which are used to refrain from publishing same sampled quantities for
// energy saving purposes while avoiding disconnecting from the broker
unsigned long C02LastMQTTUpdateTime = 0;
unsigned long tempLastMQTTUpdateTime = 0;

// The system's average relative fan speed value to be used for
// correlated sampling purposes (UCHAR_MAX -> not received yet)
unsigned char avgFanRelSpeed = UCHAR_MAX;


/* =========================== FUNCTIONS DEFINITIONS =========================== */

/* ----------------------------- Utility Functions ----------------------------- */

/**
 * @brief  Returns the stringyfied "MQTTCliState" enum
 * @return The stringyfied "MQTTCliState" enum
 */
char* MQTTCliStateToStr()
 {
  // Stores the stringyfied "MQTTCliState" enum
  static char MQTTCliStateStr[35];

  // Stringify the "MQTTCliState" enum into "MQTTCliStateStr"
  switch(MQTTCliState)
   {
    case MQTT_CLI_STATE_INIT:
     sprintf(MQTTCliStateStr,"MQTT_CLI_STATE_INIT");
    break;

    case MQTT_CLI_STATE_ENGINE_OK:
     sprintf(MQTTCliStateStr,"MQTT_CLI_STATE_ENGINE_OK");
    break;

    case MQTT_CLI_STATE_NET_OK:
     sprintf(MQTTCliStateStr,"MQTT_CLI_STATE_NET_OK");
    break;

    case MQTT_CLI_STATE_BROKER_CONNECTING:
     sprintf(MQTTCliStateStr,"MQTT_CLI_STATE_BROKER_CONNECTING");
    break;

    case MQTT_CLI_STATE_BROKER_CONNECTED:
     sprintf(MQTTCliStateStr,"MQTT_CLI_STATE_BROKER_CONNECTED");
    break;

    case MQTT_CLI_STATE_BROKER_SUBSCRIBED:
     sprintf(MQTTCliStateStr,"MQTT_CLI_STATE_BROKER_SUBSCRIBED");
    break;

    default:
     sprintf(MQTTCliStateStr,"Unknown MQTTCliState (%u)",MQTTCliState);
    break;
   }

  // Return the stringyfied "MQTTCliState" enum
  return MQTTCliStateStr;
 }


/**
 * @brief  Returns the stringyfied "MQTTEngineAPIRes" enum (mqtt_status_t)
 * @return The stringyfied "MQTTEngineAPIRes" enum (mqtt_status_t)
 */
char* MQTTEngineAPIResultStr()
 {
  // Stores the stringyfied "MQTTEngineAPIRes"
  static char MQTTEngineAPIResStr[35];

  // Stringify the "MQTTEngineAPIRes" enum into "MQTTEngineAPIResStr"
  switch(MQTTEngineAPIRes)
   {
    case MQTT_STATUS_OK:
     sprintf(MQTTEngineAPIResStr, "MQTT_STATUS_OK");
    break;

    case MQTT_STATUS_OUT_QUEUE_FULL:
     sprintf(MQTTEngineAPIResStr, "MQTT_STATUS_OUT_QUEUE_FULL");
    break;

    case MQTT_STATUS_ERROR:
     sprintf(MQTTEngineAPIResStr, "MQTT_STATUS_ERROR");
    break;

    case MQTT_STATUS_NOT_CONNECTED_ERROR:
     sprintf(MQTTEngineAPIResStr, "MQTT_STATUS_NOT_CONNECTED_ERROR");
    break;

    case MQTT_STATUS_INVALID_ARGS_ERROR:
     sprintf(MQTTEngineAPIResStr, "MQTT_STATUS_INVALID_ARGS_ERROR");
    break;

    case MQTT_STATUS_DNS_ERROR:
     sprintf(MQTTEngineAPIResStr, "MQTT_STATUS_DNS_ERROR");
    break;

    default:
     sprintf(MQTTEngineAPIResStr, "Unknown MQTTEngineAPIRes (%u)",MQTTEngineAPIRes);
    break;
   }

  // Return the stringyfied "MQTTEngineAPIRes" enum
  return MQTTEngineAPIResStr;
 }


/**
 * @brief Returns a human-readable description of the sensorErrCode argument
 * @param sensErrCode The sensorErrCode enum to be returned the description
 * @return The human-readable description of the sensorErrCode argument
 */
char* sensorErrCodeToStr(enum sensorErrCode sensErrCode)
 {
  // Stores a sensor error code description string
  static char sensorErrCodeStr[105];

  // Write the human-readable description of the
  // passed sensErrCode into "sensorErrCodeStr"
  switch(sensErrCode)
   {
    // ------------------------- Connectivity Errors -------------------------

    case ERR_SENSOR_MQTT_DISCONNECTED:
     sprintf(sensorErrCodeStr, "The sensor has disconnected from the MQTT broker");
    break;

    case ERR_SENSOR_PUB_QUANTITY_FAILED:
     sprintf(sensorErrCodeStr, "Failed to publish a sampled quantity");
    break;

    // ----------------- Invalid MQTT Publications Reception -----------------

    case ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC:
     sprintf(sensorErrCodeStr, "Received a MQTT message on a non-subscribed topic");
    break;

    case ERR_SENSOR_SUB_AVGFANRELSPEED_FAILED:
     sprintf(sensorErrCodeStr, "Failed to subscribe on the "
                               "\"" TOPIC_AVG_FAN_REL_SPEED "\" topic");
    break;

    case ERR_SENSOR_RECV_INVALID_AVGFANRELSPEED:
     sprintf(sensorErrCodeStr, "Received an invalid \"avgFanRelSpeed\" value");
    break;

    // ---------------------- Invalid Application States ----------------------

    case ERR_SENSOR_MQTT_CONNECTED_NOT_CONNECTING:
     sprintf(sensorErrCodeStr, "Established connection with the MQTT broker when not "
                               "in the \'MQTT_CLI_STATE_BROKER_CONNECTING\' state");
    break;

    case ERR_SENSOR_MQTT_ENGINE_UNKNOWN_CALLBACK_TYPE:
     sprintf(sensorErrCodeStr, "Unknown event in the MQTT Engine callback function");
    break;

    case ERR_SENSOR_MAIN_LOOP_UNKNOWN_MQTT_CLI_STATE:
     sprintf(sensorErrCodeStr, "Unknown MQTT client state in the sensor process main loop");
    break;

    case ERR_SENSOR_MAIN_LOOP_EXITED:
     sprintf(sensorErrCodeStr, "Exited from the sensor process main loop");
    break;

    // ---------------------- Unknown sensor error code ----------------------
    default:
     sprintf(sensorErrCodeStr, "Unknown sensor error code (%u)",sensErrCode);
    break;
   }

  // Return the sensorErrCode's human-readable description
  return sensorErrCodeStr;
 }


/**
 * @brief Blinks the sensor's MQTT_COMM_LED upon publishing
 *        a MQTT message (MQTTCommLEDTimer callback)
 */
void blinkMQTTBrokerCommLED(__attribute__((unused)) void* ptr)
 {
  // The remaining times the MQTT_COMM_LED must be toggled
  static unsigned char MQTTCommLEDToggleTimes = MQTT_COMM_LED_TOGGLE_TIMES;

  // Toggle the MQTT_COMM_LED
  leds_single_toggle(MQTT_COMM_LED);

  // If the MQTT_COMM_LED must be further toggled, reset the MQTTCommLEDTimer
  if(MQTTCommLEDToggleTimes-- > 0)
   ctimer_reset(&MQTTCommLEDTimer);

  // Otherwise, if the MQTT_COMM_LED has finished blinking
  else
   {
    // Reset the number of times the LED must be toggled to its default value
    MQTTCommLEDToggleTimes = MQTT_COMM_LED_TOGGLE_TIMES;

    // Turn the MQTT_COMM_LED ON or OFF depending on whether the
    // MQTT client is subscribed on a topic on the MQTT broker
    if(MQTTCliState == MQTT_CLI_STATE_BROKER_SUBSCRIBED)
     leds_single_on(MQTT_COMM_LED);
    else
     leds_single_off(MQTT_COMM_LED);
   }
 }


/**
 * @brief Logs and attempts to publish on the TOPIC_SENSORS_ERRORS topic
 *        information on an error occurred in the sensor, including:
 *           - The node's ID/MAC address
 *           - The sensor error code passed as argument
 *           - If present, an additional error description
 *             stored in the "errDscr" buffer
 *           - The node's MQTTCliState
 * @param sensErrCode The code of the sensor error that has occurred
 */
void logPublishError(enum sensorErrCode sensErrCode)
 {
  // If the node's MQTT client is NOT connected with the MQTT broker,
  // just log the error informing that it couldn't be published
  if(MQTTCliState < MQTT_CLI_STATE_BROKER_CONNECTED)
   LOG_ERR("%s %s (MQTTCliState = \'%s\') (not published as disconnected from the MQTT broker)\n",
           sensorErrCodeToStr(sensErrCode),errDscr,MQTTCliStateToStr());

   // Otherwise, If the node's MQTT client IS connected with the MQTT broker
  else
   {
    // Prepare the topic of the error message to be published
    snprintf(MQTTMsgTopicBuf, MQTT_MSG_TOPIC_BUF_SIZE, TOPIC_SENSORS_ERRORS);

    // Prepare the error message to be published depending on whether
    // its additional description was stored in the "errDscr" buffer
    if(errDscr[0] != '\0')
     snprintf(MQTTMsgContentsBuf, MQTT_MSG_CONTENTS_BUF_SIZE, "{"
                                                      " \"ID\": \"%s\","
                                                      " \"errCode\": %u,"
                                                      " \"errDscr\": \"%s\","
                                                      " \"MQTTCliState\": %u"
                                                      " }", nodeID, sensErrCode, errDscr, MQTTCliState);
    else
     snprintf(MQTTMsgContentsBuf, MQTT_MSG_CONTENTS_BUF_SIZE, "{"
                                                      " \"ID\": \"%s\","
                                                      " \"errCode\": %u,"
                                                      " \"MQTTCliState\": %u"
                                                      " }", nodeID, sensErrCode, MQTTCliState);

    // Attempt to publish the error message
    MQTTEngineAPIRes = mqtt_publish(&mqttConn, NULL, MQTTMsgTopicBuf, (uint8_t*)MQTTMsgContentsBuf,
                                    strlen(MQTTMsgContentsBuf), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);

    // If the error message has been successfully submitted to the broker
    if(MQTTEngineAPIRes == MQTT_STATUS_OK)
     {
      // Blink the communication LED
      ctimer_set(&MQTTCommLEDTimer, MQTT_COMM_LED_TOGGLE_PERIOD, blinkMQTTBrokerCommLED, NULL);

      // Also log the error locally, informing that it was published
      LOG_ERR("%s %s (MQTTCliState = \'%s\') (submitted to the broker)\n",
              sensorErrCodeToStr(sensErrCode),errDscr,MQTTCliStateToStr());
     }

     // Otherwise, if the error message could not be submitted to the
     // broker, just log it locally informing that it couldn't be published
    else
     LOG_ERR("%s %s (MQTTCliState = \'%s\') (failed to submit to the MQTT broker for error \'%s\')\n",
             sensorErrCodeToStr(sensErrCode), errDscr, MQTTCliStateToStr(), MQTTEngineAPIResultStr());
   }
 }


/* ----------------------- MQTT Engine Callback Function ----------------------- */

/**
 * @brief The callback function invoked by the MQTT
 *        Engine to notify events to the MQTT client
 * @param m      The MQTT connection between the client and broker (unused)
 * @param event  The event passed by the MQTT Engine
 * @param data   Additional information associated with the passed event
 */
static void MQTTEngineCallback(__attribute__((unused)) struct mqtt_connection* m,
                               mqtt_event_t event, void* data)
 {
  // Used to parse the contents of a received MQTT message
  struct mqtt_message* recvMsg;

  // The minimum between the size of a received MQTT message
  // and the size of the client MQTT message contents buffer -1
  unsigned int minRecvBufSize;

  // Depending on the event passed by the MQTT engine
  switch(event)
   {
    /* ----- The MQTT Engine has established a connection with the MQTT broker ----- */
    case MQTT_EVENT_CONNECTED:

     // Ensure the event to have been received with the MQTT
     // client in the "MQTT_CLI_STATE_BROKER_CONNECTING"
     // state, logging and publishing the error otherwise
     if(MQTTCliState != MQTT_CLI_STATE_BROKER_CONNECTING)
      LOG_PUB_ERROR(ERR_SENSOR_MQTT_CONNECTED_NOT_CONNECTING)

     // Update the MQTT client state
     MQTTCliState = MQTT_CLI_STATE_BROKER_CONNECTED;

     // Log that the MQTT client is now connected with the MQTT broker
     LOG_INFO("Connected with the MQTT broker @%s\n", MQTTBrokerIPv6Addr);

     // Execution will proceed in the next sensor main loop
     // (sensor_MQTT_CLI_STATE_BROKER_CONNECTED_Callback() function)
     break;

    /* ----------- The MQTT Engine has disconnected from the MQTT broker ----------- */
    case MQTT_EVENT_DISCONNECTED:

     // Log that the MQTT client has disconnected from the MQTT broker
     LOG_WARN("DISCONNECTED from the MQTT broker @%s (reason = %u), attempting"
              " to reconnect...\n", MQTTBrokerIPv6Addr, *((mqtt_event_t *)data));

     // Update the MQTT client state depending on whether it is still online
     if(!isNodeOnline())
      MQTTCliState = MQTT_CLI_STATE_ENGINE_OK;
     else
      MQTTCliState = MQTT_CLI_STATE_NET_OK;

     // Poll the sensor main loop to recovery from the disconnection
     // (sensor_MQTT_CLI_STATE_ENGINE_OK_Callback() or sensor_MQTT_CLI_STATE_NET_OK_Callback()
     // depending on the updated MQTT client state)
     process_poll(&safetunnels_sensor_process);
     break;

    /* ------- The MQTT Engine has subscribed to a topic on the MQTT broker ------- */
    case MQTT_EVENT_SUBACK:

     // Turn ON the MQTT_COMM_LED when subscribed to a topic on the MQTT broker
     leds_single_on(MQTT_COMM_LED);

     // Update the MQTT client state
     MQTTCliState = MQTT_CLI_STATE_BROKER_SUBSCRIBED;

     // Log that the node has subscribed to the TOPIC_AVG_FAN_REL_SPEED
     // on the MQTT broker (as it is the only topic it can subscribe on)
     LOG_DBG("Subscribed to the " TOPIC_AVG_FAN_REL_SPEED " topic on the MQTT broker\n");
     break;

    /* ----- The MQTT Engine has unsubscribed from a topic on the MQTT broker ----- */
    case MQTT_EVENT_UNSUBACK:

     // Turn OFF the MQTT_COMM_LED when unsubscribed from a topic on the MQTT broker
     leds_single_off(MQTT_COMM_LED);

     // Update the MQTT client state
     MQTTCliState = MQTT_CLI_STATE_BROKER_CONNECTED;

     // Log the unexpected unsubscription from the only
     // TOPIC_AVG_FAN_REL_SPEED topic the sensor can be subscribed to
     //
     // NOTE: The error is not published as it would conflict
     //       with the subsequent re-subscription attempt
     //
     LOG_ERR("UNSUBSCRIBED from the " TOPIC_AVG_FAN_REL_SPEED ""
             " topic, attempting to re-subscribe...\n");

     // Poll the sensor main loop to attempt the re-subscription
     // (sensor_MQTT_CLI_STATE_BROKER_CONNECTED_Callback() function)
     process_poll(&safetunnels_sensor_process);
     break;

    /* ------ Received a message on a topic the MQTT client is subscribed to ------ */
    case MQTT_EVENT_PUBLISH:

     // Interpret the additional information associated
     // with the event as a "mqtt_message" struct
     recvMsg = data;

     // Compute the minimum between the size of a received MQTT message
     // and the size of the client MQTT message contents buffer -1
     minRecvBufSize = sizeof(MQTTMsgContentsBuf) - 1 > recvMsg->payload_length ?
                      recvMsg->payload_length : sizeof(MQTTMsgContentsBuf) - 1;

     // Copy the received MQTT message topic and
     // contents into the local MQTT message buffers
     strcpy(MQTTMsgTopicBuf, recvMsg->topic);
     memcpy(MQTTMsgContentsBuf, recvMsg->payload_chunk, minRecvBufSize);

     // Strings safety
     MQTTMsgTopicBuf[sizeof(MQTTMsgTopicBuf)-1] = '\0';
     MQTTMsgContentsBuf[minRecvBufSize] = '\0';

     // Poll the sensor main loop to parse the received MQTT message
     // (sensor_MQTT_CLI_STATE_BROKER_SUBSCRIBED_Callback() function)
     process_poll(&safetunnels_sensor_process);
     break;

    /* --------- Received the acknowledgment of a published MQTT message --------- */

    /*
     * NOTE: This event is in fact NEVER passed by the MQTT engine (probably
     *       publication acknowledgments are optional and NOT enabled)
     */
    case MQTT_EVENT_PUBACK:
     LOG_DBG("Received MQTT publication acknowledgment\n");
     break;

    /* ------------------------ Unknown MQTT Engine event ------------------------ */
    default:
     LOG_PUB_ERROR(ERR_SENSOR_MQTT_ENGINE_UNKNOWN_CALLBACK_TYPE, "(%u)", event)
     break;
   }
 }


/* ----------------------- Quantities Sampling Functions ----------------------- */

// TODO: Continue from here

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
      snprintf(MQTTMsgTopicBuf, sizeof(MQTTMsgTopicBuf), "SafeTunnels/%s", quantity);

      // Prepare the message to be published
      snprintf(MQTTMsgContentsBuf,
               sizeof(MQTTMsgContentsBuf),
               "{"
               " \"ID\": \"%s\","
               " \"%s\": \"%u\""
               " }", nodeID, quantity, quantityValue);

      // Attempt to publish the message on the topic
      MQTTEngineAPIRes = mqtt_publish(&mqttConn, NULL, MQTTMsgTopicBuf, (uint8_t*)MQTTMsgContentsBuf, strlen(MQTTMsgContentsBuf), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);

      // If the MQTT publishment was successful
      if(MQTTEngineAPIRes == MQTT_STATUS_OK)
       {
        // Log that the MQTT publishment was successful
        LOG_INFO("Submitted updated %s value (%u) to the MQTT broker\n", quantity, quantityValue);

        // Blink the communication LED
        ctimer_set(&MQTTCommLEDTimer, MQTT_COMM_LED_TOGGLE_PERIOD, blinkMQTTBrokerCommLED, NULL);

        // Return that the MQTT publishment was successful
        return true;
       }

       // Otherwise, if the MQTT publishment was NOT successful
      else
       {
        // If the MQTT publishment failed for a MQTT_STATUS_OUT_QUEUE_FULL error,
        // just log it, as attempting to publishing it would result in the same error
        if(MQTTEngineAPIRes == MQTT_STATUS_OUT_QUEUE_FULL)
         LOG_ERR("Failed to publish updated %s value (%u) because the MQTT outbound queue is full (MQTT_STATUS_OUT_QUEUE_FULL) \n", quantity, quantityValue);

        // Otherwise, log the error and attempt to publish it
        else
         LOG_PUB_ERROR(ERR_SENSOR_PUB_QUANTITY_FAILED, "(%s = %u, error = \'%s\')", quantity, quantityValue, MQTTEngineAPIResultStr())

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
static void C02Sampling(__attribute__((unused)) void* ptr)
 {
  unsigned int newC02Density;

  // Check if the CO2 must be simulated to its maximum, unsafe value
  if(simulateMaxC02)
   {
    newC02Density = C02_VALUE_MAX;
    simulateMaxC02 = false;
   }

  // Otherwise, sample the C02 normally
  else
   {
    /* ---- TODO: Generate the C02 value depending on the "avgFanRelSpeed" value ---- */

    // TODO:  Test
    // newC02Density = random_rand();
    newC02Density = 30;

    // LOG_DBG("New randomly generated C02 density: %u\n",newC02Density);

    /* ------------------------------------------------------------------------------ */
   }

  // Check and attempt to publish the updated C02 value, and, if
  // the publishment was successful, update the last publishment time
  if(publishMQTTSensorUpdate("C02", newC02Density, newC02Density != C02Density, clock_seconds() - C02LastMQTTUpdateTime))
   C02LastMQTTUpdateTime = clock_seconds();

  // In any case, update the new C02 Density value
  C02Density = newC02Density;

  // Restart the C02 sampling timer depending on the sampling mode used for the two quantities
#ifdef QUANTITIES_SHARED_SAMPLING_PERIOD
  ctimer_set(&C02SamplingTimer, QUANTITIES_SHARED_SAMPLING_PERIOD, C02Sampling, NULL);
#else
  ctimer_reset(&C02SamplingTimer);
#endif
 }


// Temperature periodic sampling function (timer)
static void tempSampling(__attribute__((unused)) void* ptr)
 {
  unsigned int newTemp;

  // Check if the temperature must be simulated to its maximum, unsafe value
  if(simulateMaxTemp)
   {
    newTemp = TEMP_VALUE_MAX;
    simulateMaxTemp = false;
   }

   // Otherwise, sample the temperature normally
  else
   {
    /* --- TODO: Generate the temperature value depending on the "avgFanRelSpeed" value --- */

    // TODO: Test
    // newTemp = random_rand();
    newTemp = 20;

    // LOG_DBG("New randomly generated temperature: %u\n",newTemp);

    /* ------------------------------------------------------------------------------------ */
   }

  // Check and attempt to publish the updated temperature value, and, if
  // the publishment was successful, update the last publishment time
  if(publishMQTTSensorUpdate("temp", newTemp, newTemp != temp, clock_seconds() - tempLastMQTTUpdateTime))
   tempLastMQTTUpdateTime = clock_seconds();

  // In any case, update the new C02 Density value
  temp = newTemp;

  // Restart the temperature sampling timer depending on the sampling mode used for the two quantities
#ifdef QUANTITIES_SHARED_SAMPLING_PERIOD
  ctimer_set(&tempSamplingTimer, QUANTITIES_SHARED_SAMPLING_PERIOD, tempSampling, NULL);
#else
  ctimer_reset(&tempSamplingTimer);
#endif
 }



void sensor_MQTT_CLI_STATE_INIT_Callback()
 {
  // Attempt to initialize the MQTT client engine
  MQTTEngineAPIRes = mqtt_register(&mqttConn, &safetunnels_sensor_process, nodeID, MQTTEngineCallback, MQTT_MAX_TCP_SEGMENT_SIZE);

  // If the MQTT client engine initialization was successful
  if(MQTTEngineAPIRes == MQTT_STATUS_OK)
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
   LOG_ERR("FAILED to initialize the MQTT Client engine (error = %u)", MQTTEngineAPIRes);

  // Reinitialize the MQTT client status timer with the bootstrap period
  etimer_restart(&sensorMainLoopTimer);
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
   // the LLN, log it every SENSOR_MAIN_LOOP_RPL_LOG_DEFER_TIMES attempts
  else
   if(++RPLWaitingTimes % SENSOR_MAIN_LOOP_RPL_LOG_DEFER_TIMES == 0)
    LOG_DBG("Waiting for the RPL DODAG to converge...\n");
   
  // Reinitialize the MQTT client status timer with the bootstrap period
  etimer_restart(&sensorMainLoopTimer);
 }


void sensor_MQTT_CLI_STATE_NET_OK_Callback()
 {
  // Prepare the MQTT client "last will" topic and message to be published
  // automatically by the broker should the node disconnect from it
  snprintf(MQTTMsgTopicBuf, sizeof(MQTTMsgTopicBuf), "SafeTunnels/sensorsErrors");
  snprintf(MQTTMsgContentsBuf,
           sizeof(MQTTMsgContentsBuf),
           "{"
           " \"ID\": \"%s\","
           " \"errCode\": %u"
           " }", nodeID, ERR_SENSOR_MQTT_DISCONNECTED);

  // Set the MQTT client "last will" message
  mqtt_set_last_will(&mqttConn, (char*)MQTTMsgTopicBuf, MQTTMsgContentsBuf, MQTT_QOS_LEVEL_0);

  // Attempt to connect with the MQTT broker
  MQTTEngineAPIRes = mqtt_connect(&mqttConn, (char*)MQTTBrokerIPv6Addr, MQTT_BROKER_DEFAULT_PORT, MQTT_BROKER_KEEPALIVE_TIMEOUT, MQTT_CLEAN_SESSION_ON);

  // If the MQTT broker connection has been successfully submitted
  if(MQTTEngineAPIRes == MQTT_STATUS_OK)
   {
    // Log that the MQTT client is attempting to connect with the MQTT broker
    LOG_DBG("Attempting to connect with the MQTT broker @%s...\n", MQTTBrokerIPv6Addr);

    // Update the MQTT client state
    MQTTCliState = MQTT_CLI_STATE_BROKER_CONNECTING;

    // TODO: Test (activated because the process_poll was deactivated in the MQTT engine handler);
    etimer_restart(&sensorMainLoopTimer);
   }

   // Otherwise, if the MQTT broker connection has NOT been successfully submitted
  else
   {
    // Log the error
    LOG_ERR("FAILED to attempt to connect with the MQTT broker (error = \'%s\')", MQTTEngineAPIResultStr());

    // Try again at the next client status timer activation
    etimer_restart(&sensorMainLoopTimer);
   }
 }


void sensor_MQTT_CLI_STATE_BROKER_CONNECTED_Callback()
 {
  // Attempt to subscribe to the TOPIC_AVG_FAN_REL_SPEED topic
  snprintf(MQTTMsgTopicBuf, sizeof(MQTTMsgTopicBuf), TOPIC_AVG_FAN_REL_SPEED);
  MQTTEngineAPIRes = mqtt_subscribe(&mqttConn, NULL, MQTTMsgTopicBuf, MQTT_QOS_LEVEL_0);

  // If the topic subscription submission was successful
  if(MQTTEngineAPIRes == MQTT_STATUS_OK)
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
    LOG_PUB_ERROR(ERR_SENSOR_SUB_AVGFANRELSPEED_FAILED, "(error = \'%s\')",
                  MQTTEngineAPIResultStr())

    // Reinitialize the timer to try again at the next cycle
    etimer_restart(&sensorMainLoopTimer);
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
  if(strncmp(MQTTMsgTopicBuf, TOPIC_AVG_FAN_REL_SPEED, sizeof(MQTTMsgTopicBuf)) != 0)
   LOG_PUB_ERROR(ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC, "(topic = %s, msg = %.100s)", MQTTMsgTopicBuf, MQTTMsgContentsBuf)
  else
   {
    // Interpret the message's contents as an unsigned (long) integer
    recvFanSpeedRelative = strtoul(MQTTMsgContentsBuf, NULL, 10);

    // Ensure the received average fan relative speed value to
    // be valid, logging and publishing the error otherwise
    if(recvFanSpeedRelative > 100)
     LOG_PUB_ERROR(ERR_SENSOR_RECV_INVALID_AVGFANRELSPEED, "(%lu > 100)", recvFanSpeedRelative)
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

 /*
  * As they do not depend on its connection status, start the sensor's
  * physical quantities sampling depending on the sampling mode uses
  * (see 'SENSOR SAMPLING MODES' noted in sensor.h, line TODO), uniformly
  * distributing their sampling instants if using a shared sampling period
  */
#ifdef QUANTITIES_SHARED_SAMPLING_PERIOD
 ctimer_set(&C02SamplingTimer, 0, C02Sampling, NULL);
 ctimer_set(&tempSamplingTimer, QUANTITIES_SHARED_SAMPLING_PERIOD >> 1, tempSampling, NULL);
#else
 ctimer_set(&C02SamplingTimer, C02_SAMPLING_PERIOD, C02Sampling, NULL);
 ctimer_set(&tempSamplingTimer, TEMP_SAMPLING_PERIOD, tempSampling, NULL);
#endif

 // Initialize the sensor process main loop timer
 etimer_set(&sensorMainLoopTimer, (clock_time_t)SENSOR_MAIN_LOOP_PERIOD);

 /* Sensor Process Main Loop */
 while(1)
  {
   // Yield the process's execution
   PROCESS_YIELD();

   // If the MQTT client status timer has expired, or the process has been explicitly polled
   if((ev == PROCESS_EVENT_TIMER && data == &sensorMainLoopTimer) || ev == PROCESS_EVENT_POLL)
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
        etimer_restart(&sensorMainLoopTimer);
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
      // Notify the quantities' sampling functions to report
      // a maximum, unsafe value at their next sampling
      simulateMaxC02 = true;
      simulateMaxTemp = true;

      // Log that the C02 and temperature values have been simulated to their maximum, unsafe values
      LOG_INFO("Sampled quantities simulated to their maximum, unsafe values\n");
     }

    /*
     * NOTE: Checking for unknown events passed by the process has been
     *       disabled due to the OS repeatedly passing event '150'
     *       (which also occurs in the "mqtt_client.c" course example)
     *

    else
     LOG_ERR("An unknown event was passed to the sensor main loop (%u)\n",ev);
    */
  }


 /* ------------ Execution should NEVER reach here ------------ */

 // Turn off both LEDs
 leds_off(LEDS_NUM_TO_MASK(POWER_LED) | LEDS_NUM_TO_MASK(MQTT_COMM_LED));

 // Attempt to log and publish the error
 LOG_PUB_ERROR(ERR_SENSOR_MAIN_LOOP_EXITED,"(last event= %u)",ev)

 PROCESS_END()
}