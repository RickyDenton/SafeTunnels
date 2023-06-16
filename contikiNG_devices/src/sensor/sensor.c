/* SafeTunnels Sensor Application Definitions */

/* ================================== INCLUDES ================================== */

/* ------------------------------ Standard Headers ------------------------------ */
#include <string.h>
#include <strings.h>

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

/**
 * Primary MQTT Message Buffer
 * Used for:
 *  - Sending quantity updates
 *  - Sending errors
 */
static char priMQTTMsgTopicBuf[MQTT_MSG_TOPIC_BUF_SIZE];
static char priMQTTMsgContentsBuf[MQTT_MSG_CONTENTS_BUF_SIZE];

/**
 * Secondary MQTT Message Buffer
 * Used for:
 *  - Setting the sensor's last will on the MQTT broker
 *  - Subscribing on the TOPIC_AVG_FAN_REL_SPEED topic
 *  - Receiving TOPIC_AVG_FAN_REL_SPEED messages
 */
static char secMQTTMsgTopicBuf[MQTT_MSG_TOPIC_BUF_SIZE];
static char secMQTTMsgContentsBuf[MQTT_MSG_CONTENTS_BUF_SIZE];

// Stores the result of a MQTT engine API call
static mqtt_status_t MQTTEngineAPIRes;

// MQTT Broker IPv6 Address
static const char MQTTBrokerIPv6Addr[] = MQTT_BROKER_IPV6_ADDR;

// The structure used to manage the connection between the MQTT client and broker
static struct mqtt_connection mqttConn;


/* ------------------------- Sampling Global Variables ------------------------- */

// Quantities' values
unsigned short C02;   // In parts per million (ppm)
unsigned short temp;  // In celsius degrees (°C)

// Quantities' roads equilibrium points [0,100]
unsigned char C02RoadEqPoint;
unsigned char tempRoadEqPoint;

// Whether the quantities and their road
// equilibrium points have been initialized yet
bool quantitiesInitialized = false;

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

// The system's average relative fan speed value
// to be used for correlated sampling purposes
unsigned char avgFanRelSpeed = 0;


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
 *        a MQTT message (MQTTCommLEDTimer callback function)
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
    snprintf(priMQTTMsgTopicBuf, sizeof(priMQTTMsgTopicBuf), TOPIC_SENSORS_ERRORS);

    // Prepare the error message to be published depending on whether
    // its additional description was stored in the "errDscr" buffer
    if(errDscr[0] != '\0')
     snprintf(priMQTTMsgContentsBuf, sizeof(priMQTTMsgContentsBuf), "{"
                                                      " \"MAC\": \"%s\","
                                                      " \"errCode\": %u,"
                                                      " \"errDscr\": \"%s\","
                                                      " \"MQTTCliState\": %u"
                                                      " }", nodeMACAddr, sensErrCode, errDscr, MQTTCliState);
    else
     snprintf(priMQTTMsgContentsBuf, sizeof(priMQTTMsgContentsBuf), "{"
                                                      " \"MAC\": \"%s\","
                                                      " \"errCode\": %u,"
                                                      " \"MQTTCliState\": %u"
                                                      " }", nodeMACAddr, sensErrCode, MQTTCliState);

    // Attempt to publish the error message
    MQTTEngineAPIRes = mqtt_publish(&mqttConn, NULL, priMQTTMsgTopicBuf, (uint8_t*)priMQTTMsgContentsBuf,
                                    strlen(priMQTTMsgContentsBuf), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);

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

     // As the sampled quantities can no longer be published to the
     // broker, stop the sensor's sampling for energy savings purposes
     ctimer_stop(&C02SamplingTimer);
     ctimer_stop(&tempSamplingTimer);

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
     minRecvBufSize = MIN(sizeof(secMQTTMsgContentsBuf) - 1,recvMsg->payload_length);

     // Copy the received MQTT message topic and
     // contents into the local MQTT message buffers
     strcpy(secMQTTMsgTopicBuf, recvMsg->topic);
     memcpy(secMQTTMsgContentsBuf, recvMsg->payload_chunk, minRecvBufSize);

     // Strings safety
     secMQTTMsgTopicBuf[sizeof(secMQTTMsgTopicBuf) - 1] = '\0';
     secMQTTMsgContentsBuf[minRecvBufSize] = '\0';

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

/**
 * @brief Attempts to publish an updated sampled physical
 *        quantity value (C02 or temperature) to the MQTT broker
 * @param quantity The quantity to be published ("C02" or "temp")
 * @param quantityValue The quantity's updated value
 * @param quantityDiffer Whether the quantity's updated differs from its current value
 * @param publishInactivityTime The last time such quantity was published to possibly
 *                              avoid publishing a same value for energy savings purposes
 *                              while avoiding being disconnected from the MQTT broker
 * @return Whether the updated quantity value was submitted to the MQTT broker
 * @note The topics the sampled quantities are published on are dynamically built as:\n
 *       - CO2         -> "SafeTunnels/C02"\n
 *       - Temperature -> "SafeTunnels/temp"
 */
bool publishMQTTSensorUpdate(char* quantity, unsigned int quantityValue,
                             bool quantityDiffer, unsigned long publishInactivityTime)
 {
  // If the sensor's MQTT client is NOT connected with the MQTT broker,
  // the updated sampled quantity cannot be published in any case
  if(MQTTCliState < MQTT_CLI_STATE_BROKER_CONNECTED)
   {
    // Log that the updated quantity value could not be published
    // because the sensor is disconnected from the broker
    LOG_WARN("The updated %s value (%u) could not be published (not "
             "connected with the broker)\n", quantity, quantityValue);

    // Return that the updated sampled quantity has not been published
    return false;
   }

  // Otherwise, if the sensor's MQTT client IS connected with the MQTT broker
  else
   {
    // If the quantity's updated differs from its current value OR such quantity
    // has not been published for more than the MQTT_CLIENT_MAX_INACTIVITY time,
    // publish the updated quantity to the broker
    if(quantityDiffer || (publishInactivityTime > (unsigned long)MQTT_CLI_MAX_INACTIVITY))
     {
      // Prepare MQTT message topic ("SafeTunnels/C02" || "SafeTunnels/temp")
      snprintf(priMQTTMsgTopicBuf, sizeof(priMQTTMsgTopicBuf), "SafeTunnels/%s", quantity);

      // Prepare MQTT message contents
      snprintf(priMQTTMsgContentsBuf,
               sizeof(priMQTTMsgContentsBuf),
               "{ "
               "\"MAC\": \"%s\", "                      // Node ID/MAC Address
               "\"%s\": %u "                           // "quantity" : value
               "}", nodeMACAddr, quantity, quantityValue);

      // Attempt to submit the MQTT message to the broker
      MQTTEngineAPIRes = mqtt_publish(&mqttConn, NULL, priMQTTMsgTopicBuf, (uint8_t*)priMQTTMsgContentsBuf,
                                      strlen(priMQTTMsgContentsBuf), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);

      // If the MQTT message was submitted to the MQTT broker
      if(MQTTEngineAPIRes == MQTT_STATUS_OK)
       {
        // Log that the MQTT message has been submitted to the MQTT broker
        LOG_INFO("Submitted updated %s value (%u) to the MQTT broker\n", quantity, quantityValue);

        // Blink the communication LED
        ctimer_set(&MQTTCommLEDTimer, MQTT_COMM_LED_TOGGLE_PERIOD, blinkMQTTBrokerCommLED, NULL);

        // Return that the updated sampled quantity has been published
        return true;
       }

      // Otherwise, if the MQTT message WASN'T submitted to the MQTT broker
      else
       {
        // If the MQTT publication failed because the MQTT engine outbound
        // queue is full (MQTT_STATUS_OUT_QUEUE_FULL), just log the error
        // without publishing it as it would result in the same error
        if(MQTTEngineAPIRes == MQTT_STATUS_OUT_QUEUE_FULL)
         LOG_ERR("FAILED to publish the updated %s value (%u) because the MQTT Engine outbound "
                 "queue is full (MQTT_STATUS_OUT_QUEUE_FULL) \n", quantity, quantityValue);

        // Otherwise, log and attempt to publish the error
        else
         LOG_PUB_ERROR(ERR_SENSOR_PUB_QUANTITY_FAILED, "(%s = %u, error = \'%s\')",
                       quantity, quantityValue, MQTTEngineAPIResultStr())

        // Return that the updated sampled quantity has not been published
        return false;
       }
     }

    // Otherwise, if the updated is the same as the current quantity value
    // and it was last published within the MQTT_CLIENT_MAX_INACTIVITY time,
    // refrain from publishing a same value for energy saving purposes
    else
     {
      // Log that the same sampled value was not published to the broker
      LOG_INFO("A same sampled %s value (%u) was NOT published the MQTT "
               "broker for energy savings purposes\n", quantity, quantityValue);

      // Return that the updated sampled quantity has not been published
      return false;
     }
   }
 }


/**
 * @brief Generates a new value for a quantity's road equilibrium point
 * @param quantityRoadEqPoint A pointer to the quantity's
 *                            road equilibrium point
 */
void genNewQuantityRoadEqPoint(unsigned char* quantityRoadEqPoint)
 {
  // Generate a random unsigned short to be used for
  // updating the quantity's road equilibrium point
  unsigned short quantityRoadEqPointRand = random_rand();

  // The random value first byte MSB determines whether the quantity's road
  // equilibrium point should be updated (1) or not (0), and if it should:
  if(GETUBYTE(quantityRoadEqPointRand, 0) & (1 << 7))
   {
    // Determine the sign of the quantity's road equilibrium point
    // change, i.e. whether it should decrement or increment, as the
    // second bit of the first random byte (0 decrement, 1 increment)
    bool quantityRoadEqPointGenSign = GETUBYTE(quantityRoadEqPointRand, 0) & (1 << 6);

    // Determine the magnitude of the quantity's road equilibrium point change as the
    // second random byte modulo ROAD_EQ_POINT_MAX_CHANGE (+ 1, as it cannot be 0)
    unsigned char quantityRoadEqPointGenModulo = (GETUBYTE(quantityRoadEqPointRand, 1)
                                                 % ROAD_EQ_POINT_MAX_CHANGE) + 1;

    // Change the quantity's road equilibrium point
    // bounding it within its minimum and maximum values
    if(quantityRoadEqPointGenSign)
     *quantityRoadEqPoint = UINT_BOUND_SUM(*quantityRoadEqPoint,
                                           quantityRoadEqPointGenModulo, ROAD_EQ_POINT_MAX);
    else
     *quantityRoadEqPoint = UINT_BOUND_SUB(*quantityRoadEqPoint,
                                           quantityRoadEqPointGenModulo, ROAD_EQ_POINT_MIN);
   }
 }



/**
 * @brief Generates a new value for a sensor's quantity (C02 or temperature)
 * @param quantityCurrValue      The quantity's current value
 * @param quantityRoadEqPoint    The quantity's road equilibrium point
 * @param quantityCurrPoint      The quantity's current point
 * @param quantityBaseMaxChange  The quantity's base maximum change
 * @param quantityMinValue       The quantity's minimum value
 * @param quantityMaxValue       The quantity's maximum value
 * @return                       The new quantity value
 */
unsigned int genNewQuantityValue(unsigned int quantityCurrValue,  unsigned char* quantityRoadEqPoint,
                                 unsigned char quantityCurrPoint, unsigned int quantityBaseMaxChange,
                                 unsigned int quantityMinValue,   unsigned int quantityMaxValue)
 {
  // Initialize the probabilities for the quantity to increment,
  // decrement and stay the same to their base values
  unsigned char probQuantitySame = PROB_QUANTITY_SAME;
  unsigned char probQuantityDecrement = PROB_QUANTITY_DECREMENT;
  unsigned char probQuantityIncrement = PROB_QUANTITY_INCREMENT;

  /*
   *  A boolean used to bias the modulo of the quantity's
   *  increment or decrement depending on the difference
   *  between its current and operating state equilibrium point
   *    - quantityCurrPoint < quantityOpEnvEqPoint -> false (favor decrement)
   *    - quantityCurrPoint > quantityOpEnvEqPoint -> true (favor increment)
   */
  bool quantityGenBias;

  // Initialize the maximum amount the quantity can change to its base value
  unsigned int quantityMaxChange = quantityBaseMaxChange;

  // The amount the quantity has changed
  // from its current value, if any
  unsigned int quantityAmountChange;

  // The (absolute) difference between the quantity
  // operating environment equilibrium and current point
  unsigned char diffFromEqPoint = 0;

  // Randomly update the quantity's road equilibrium point
  genNewQuantityRoadEqPoint(quantityRoadEqPoint);

  // Compute the average fan relative speed contribution
  // to the quantity operating environment equilibrium point
  unsigned char fanRelSpeedEqContrib = (unsigned char)(avgFanRelSpeed * AVG_FAN_REL_SPEED_EQ_PERC);

  // Compute the quantity operating environment equilibrium point (road + fan)
  unsigned char quantityOpEnvEqPoint = UINT_BOUND_SUB(*quantityRoadEqPoint, fanRelSpeedEqContrib, 0);

  // If the quantity current is greater than
  // its operating environment equilibrium point
  if(quantityCurrPoint > quantityOpEnvEqPoint)
   {
    // Bias the quantity new value generation towards the decrement
    quantityGenBias = false;

    // Compute the (absolute) difference between the quantity
    // operating environment equilibrium and current point
    diffFromEqPoint = quantityCurrPoint - quantityOpEnvEqPoint;

    // Alter the probabilities for the quantity to increment,
    // decrement and stay the same biasing them towards decrement
    probQuantitySame = UINT_BOUND_SUB(probQuantitySame, diffFromEqPoint, 5);
    probQuantityDecrement = UINT_BOUND_SUM(probQuantityDecrement, 2 * diffFromEqPoint, 95);
    probQuantityIncrement = UINT_BOUND_SUB(probQuantityIncrement, diffFromEqPoint, 0);
   }

  // If the quantity current is smaller than
  // its operating environment equilibrium point
  if(quantityCurrPoint < quantityOpEnvEqPoint)
   {
    // Bias the quantity new value generation towards the increment
    quantityGenBias = true;

    // Compute the (absolute) difference between the quantity
    // operating environment equilibrium and current point
    diffFromEqPoint = quantityOpEnvEqPoint - quantityCurrPoint;

    // Alter the probabilities for the quantity to increment,
    // decrement and stay the same biasing them towards increment
    probQuantitySame = UINT_BOUND_SUB(probQuantitySame, diffFromEqPoint, 5);
    probQuantityDecrement = UINT_BOUND_SUB(probQuantityDecrement, diffFromEqPoint, 0);
    probQuantityIncrement = UINT_BOUND_SUM(probQuantityIncrement, 2 * diffFromEqPoint, 95);
   }

  // A random number modulo 100+1 is computed to determine
  // whether the quantity should increment, decrement or stay the
  // same according to the previously computed probabilities
  unsigned char quantityChangeType = random_rand() % 101;

  // If the quantity should stay the same, simply return its plain value
  if(quantityChangeType <= probQuantitySame)
   return quantityCurrValue;

  // Otherwise, if the value should decrement
  if(quantityChangeType <= probQuantitySame + probQuantityDecrement)
   {
    // If the quantity generation is biased towards decrement, increase the maximum
    // amount the quantity can change by a constant depending on the difference
    // between the quantity operating environment equilibrium and current point
    if(!quantityGenBias)
     quantityMaxChange = (unsigned int)((double)quantityMaxChange * (1 + (double)diffFromEqPoint / 100 * 4));

    // Randomly generate the amount the quantity decreases
    // modulo quantityMaxChange (+ 1, as it cannot be 0)
    quantityAmountChange = (random_rand() % quantityMaxChange) + 1;

    // Return the updated quantity bounded within its minimum value
    return UINT_BOUND_SUB(quantityCurrValue,quantityAmountChange,quantityMinValue);
   }

  // Otherwise, if the quantity should increment
  else
   {
    // If the quantity generation is biased towards increment, increase the maximum
    // amount the quantity can change by a constant depending on the difference
    // between the quantity operating environment equilibrium and current point
    if(quantityGenBias)
     quantityMaxChange = (unsigned int)((double)quantityMaxChange * (1 + (double)diffFromEqPoint / 100 * 4));

    // Randomly generate the amount the quantity increases
    // modulo quantityMaxChange (+ 1, as it cannot be 0)
    quantityAmountChange = (random_rand() % quantityMaxChange) + 1;

    // Return the updated quantity bounded within its maximum value
    return UINT_BOUND_SUM(quantityCurrValue,quantityAmountChange,quantityMaxValue);
   }
 }


/**
 * @brief Periodic C02 density sampling function
 *        (C02SamplingTimer callback function)
 */
static void C02Sampling(__attribute__((unused)) void* ptr)
 {
  // Stores the updated sampled C02 density value
  unsigned int newC02;

  // If the CO2 density must be simulated to its maximum
  // "unsafe" value (by having pressed the sensor's button)
  if(simulateMaxC02)
   {
    // Set the updated sampled C02 to its maximum, "unsafe" value
    newC02 = C02_VALUE_MAX;

    // Reset the variable used for simulating
    // the CO2 to its maximum "unsafe" value
    simulateMaxC02 = false;
   }

  // Otherwise, if the CO2 density should be sampled normally
  else
   {
    // Compute the C02 current point, or percentage
    // with respect to its maximum value
    unsigned char C02CurrPoint = C02 * 100 / C02_VALUE_MAX;

    // Randomly generate a new value for the C02
    newC02 = genNewQuantityValue(C02,&C02RoadEqPoint,C02CurrPoint,
                                 C02_BASE_MAX_CHANGE,C02_VALUE_MIN,C02_VALUE_MAX);
   }

  // Check and attempt to publish the updated C02 value on the MQTT
  // broker and, if successful, update its last publication time
 if(publishMQTTSensorUpdate("C02", newC02, newC02 != C02,
                            clock_seconds() - C02LastMQTTUpdateTime))
   C02LastMQTTUpdateTime = clock_seconds();

  // Set the C02 density to its updated value
  C02 = newC02;

  // Restart the C02 sampling timer depending on
  // the sampling mode used for the two quantities
#ifdef QUANTITIES_SHARED_SAMPLING_PERIOD
  ctimer_set(&C02SamplingTimer, QUANTITIES_SHARED_SAMPLING_PERIOD, C02Sampling, NULL);
#else
  ctimer_set(&C02SamplingTimer, C02_SAMPLING_PERIOD, C02Sampling, NULL);
#endif
 }


/**
 * @brief Periodic temperature sampling function
 *        (C02SamplingTimer callback function)
 */
static void tempSampling(__attribute__((unused)) void* ptr)
 {
  // Stores the updated sampled temperature value
  unsigned int newTemp;

  // If the temperature must be simulated to its maximum
  // "unsafe" value (by having pressed the sensor's button)
  if(simulateMaxTemp)
   {
    // Set the updated sampled temperature
    // to its maximum, "unsafe" value
    newTemp = TEMP_VALUE_MAX;

    // Reset the variable used for simulating the
    // temperature to its maximum "unsafe" value
    simulateMaxTemp = false;
   }

  // Otherwise, if the temperature should be sampled normally
  else
   {
    // Compute the temperature current point, or
    // percentage with respect to its maximum value
    unsigned char tempCurrPoint = temp * 100 / TEMP_VALUE_MAX;

    // Randomly generate a new value for the C02
    newTemp = genNewQuantityValue(temp,&tempRoadEqPoint,tempCurrPoint,
                                  TEMP_BASE_MAX_CHANGE,TEMP_VALUE_MIN,TEMP_VALUE_MAX);
   }

  // Check and attempt to publish the updated temperature on the MQTT
  // broker and, if successful, update its last publication time
  if(publishMQTTSensorUpdate("temp", newTemp, newTemp != temp,
                             clock_seconds() - tempLastMQTTUpdateTime))
   tempLastMQTTUpdateTime = clock_seconds();

  // Set the temperature to its updated value
  temp = newTemp;

  // Restart the temperature sampling timer depending on the sampling mode used for the two quantities
#ifdef QUANTITIES_SHARED_SAMPLING_PERIOD
  ctimer_set(&tempSamplingTimer, QUANTITIES_SHARED_SAMPLING_PERIOD, tempSampling, NULL);
#else
  ctimer_set(&tempSamplingTimer, TEMP_SAMPLING_PERIOD, tempSampling, NULL);
#endif
 }


/* ---------------------------- Sensor Process Body ---------------------------- */

/**
 * @brief MQTT_CLI_STATE_INIT callback function, executed
 *        at power on to initialize the MQTT engine
 */
void sensor_MQTT_CLI_STATE_INIT_Callback()
 {
  // Attempt to initialize the MQTT Engine
  MQTTEngineAPIRes = mqtt_register(&mqttConn, &safetunnels_sensor_process,
                                   nodeMACAddr, MQTTEngineCallback, MQTT_MAX_TCP_SEGMENT_SIZE);

  // If the MQTT Engine has been successfully initialized
  if(MQTTEngineAPIRes == MQTT_STATUS_OK)
   {
    // Update the MQTT client state
    MQTTCliState = MQTT_CLI_STATE_ENGINE_OK;

    // Log the successful MQTT Engine initialization and that
    // the node will now wait for external network connectivity
    LOG_DBG("MQTT Engine successfully initialized\n");
    LOG_DBG("Waiting for the external network connectivity...\n");
   }

  // Otherwise, if the MQTT Engine initialization failed, log the
  // error (and try again at the next process main loop activation)
  else
   LOG_ERR("FAILED to initialize the MQTT Engine (error = %s)", MQTTEngineAPIResultStr());

  /*
   * Reinitialize the sensor main loop timer, whose expiration will trigger:
   *  - If the MQTT engine initialization failed,
   *    again the MQTT_CLI_STATE_INIT callback
   *  - If the MQTT engine initialization succeeded,
   *    the MQTT_CLI_STATE_ENGINE_OK callback
   */
  etimer_restart(&sensorMainLoopTimer);
 }


/**
 * @brief MQTT_CLI_STATE_ENGINE_OK callback function,
 *        executed after the MQTT Engine initialization
 *        checking for external network connectivity
 */
void sensor_MQTT_CLI_STATE_ENGINE_OK_Callback()
 {
  // Counter used to suppress most of the LOG_DBG associated
  // with the sensor waiting for external network connectivity
  static unsigned char suppressNetworkConnLogs = 0;

  // If the sensor now has external connectivity
  if(isNodeOnline())
   {
    // Reset the counter used for suppressing
    // external network connectivity logs
    suppressNetworkConnLogs = 0;

    // Log that the sensor is now online
    LOG_DBG("The sensor is now online\n");

    // Update the MQTT client state
    MQTTCliState = MQTT_CLI_STATE_NET_OK;
   }

  // Otherwise, if the node has not external connectivity, log
  // it every 'suppressNetworkConnLogs' sensor loops cycles
  else
   if(++suppressNetworkConnLogs % SENSOR_MAIN_LOOP_NO_CONN_LOG_PERIOD == 0)
    LOG_DBG("Waiting for external connectivity...\n");

  /*
   * Reinitialize the sensor main loop timer, whose expiration will trigger:
   *  - If the sensor has not external connectivity,
   *    again the MQTT_CLI_STATE_ENGINE_OK callback
   *  - If the sensor has external connectivity,
   *    the MQTT_CLI_STATE_NET_OK callback
   */
  etimer_restart(&sensorMainLoopTimer);
 }


/**
 * @brief MQTT_CLI_STATE_NET_OK callback function, executed
 *        when the sensor has external network connectivity
 *        to attempt to connect with the MQTT broker
 */
void sensor_MQTT_CLI_STATE_NET_OK_Callback()
 {
  // Prepare the "last will" message to be automatically
  // published by the MQTT broker on the TOPIC_SENSORS_ERRORS
  // topic should the MQTT client disconnect from it
  snprintf(secMQTTMsgTopicBuf, sizeof(secMQTTMsgTopicBuf), TOPIC_SENSORS_ERRORS);
  snprintf(secMQTTMsgContentsBuf,
           sizeof(secMQTTMsgContentsBuf),
           "{ "
           "\"MAC\": \"%s\", "    // Node ID/MAC Address)
           "\"errCode\": %u "    // ERR_SENSOR_MQTT_DISCONNECTED
           "}", nodeMACAddr, ERR_SENSOR_MQTT_DISCONNECTED);

  // Set the MQTT client "last will" message
  mqtt_set_last_will(&mqttConn, (char*)secMQTTMsgTopicBuf,
                     secMQTTMsgContentsBuf, MQTT_QOS_LEVEL_0);

  // Attempt to submit a MQTT broker connection request
  MQTTEngineAPIRes = mqtt_connect(&mqttConn, (char*)MQTTBrokerIPv6Addr, MQTT_BROKER_DEFAULT_PORT,
                                  MQTT_BROKER_KEEPALIVE_TIMEOUT, MQTT_CLEAN_SESSION_ON);

  // If the MQTT broker connection has been successfully submitted
  if(MQTTEngineAPIRes == MQTT_STATUS_OK)
   {
    // Log that the MQTT client is attempting to connect with the MQTT broker
    LOG_DBG("Attempting to connect with the MQTT broker @%s...\n", MQTTBrokerIPv6Addr);

    // Update the MQTT client state
    MQTTCliState = MQTT_CLI_STATE_BROKER_CONNECTING;
   }

  // Otherwise, if the MQTT broker connection submission failed, log the error
  else
   LOG_ERR("FAILED to attempt to connect with the MQTT broker"
           "(error = \'%s\')", MQTTEngineAPIResultStr());

  /*
   * Reinitialize the sensor main loop timer, whose expiration will trigger:
   *  - If the MQTT broker connection submission failed,
   *    again the MQTT_CLI_STATE_NET_OK callback
   *  - If the MQTT broker connection submission succeeded but the
   *    connection has not yet been established, the timer's
   *    reinitialization (MQTT_CLI_STATE_BROKER_CONNECTING, no callback)
   *  - If the MQTT engine successfully established a connection with the MQTT
   *    broker (MQTT_EVENT_CONNECTED), the MQTT_CLI_STATE_BROKER_CONNECTED callback
   */
  etimer_restart(&sensorMainLoopTimer);
 }


/**
 * @brief MQTT_CLI_STATE_BROKER_CONNECTED callback function,
 *        executed when the MQTT engine has established a
 *        connection with the MQTT broker, after which the node:
 *          1) If necessary, initializes the quantities'
 *             values and equilibrium points
 *          2) Starts the C02 density and temperature sampling
 *          3) Attempts to subscribe on the TOPIC_AVG_FAN_REL_SPEED topic
 */
void sensor_MQTT_CLI_STATE_BROKER_CONNECTED_Callback()
 {
  // If the sensor's quantities' values and road
  // equilibrium points have not yet been initialized
  if(!quantitiesInitialized)
   {
    // Randomly generate the C02 value and road equilibrium point
    C02 = (random_rand() % (C02_VALUE_MAX - C02_VALUE_MIN + 1)) + C02_VALUE_MIN;
    C02RoadEqPoint = (random_rand() % (ROAD_EQ_POINT_MAX -
                                       ROAD_EQ_POINT_MIN + 1) ) + ROAD_EQ_POINT_MIN;

    // Randomly generate the temperature value and road equilibrium point
    temp = (random_rand() % (TEMP_VALUE_MAX - TEMP_VALUE_MIN + 1)) + TEMP_VALUE_MIN;
    tempRoadEqPoint = (random_rand() % (ROAD_EQ_POINT_MAX -
                                        ROAD_EQ_POINT_MIN + 1)) +  ROAD_EQ_POINT_MIN;

    // Set that the quantities' values and road
    // equilibrium points have been initialized
    quantitiesInitialized = true;
   }

  /*
   * Start after an initial delay the sensor's physical quantities sampling
   * timers depending on the sampling mode used (see 'SENSOR SAMPLING MODES'
   * note in "sensor.h", line 101 for more details), uniformly distributing
   * their sampling instants within the  shared sampling period if applicable
   */
#ifdef QUANTITIES_SHARED_SAMPLING_PERIOD
  ctimer_set(&C02SamplingTimer, INIT_SAMPLING_DELAY, C02Sampling, NULL);
  ctimer_set(&tempSamplingTimer, INIT_SAMPLING_DELAY + (QUANTITIES_SHARED_SAMPLING_PERIOD >> 1), tempSampling, NULL);
#else
  ctimer_set(&C02SamplingTimer, C02_SAMPLING_PERIOD, C02Sampling, NULL);
  ctimer_set(&tempSamplingTimer, TEMP_SAMPLING_PERIOD, tempSampling, NULL);
#endif

  // Attempt to submit a subscription on the
  // TOPIC_AVG_FAN_REL_SPEED topic on the MQTT broker
  snprintf(secMQTTMsgTopicBuf, sizeof(secMQTTMsgTopicBuf), TOPIC_AVG_FAN_REL_SPEED);
  MQTTEngineAPIRes = mqtt_subscribe(&mqttConn, NULL, secMQTTMsgTopicBuf, MQTT_QOS_LEVEL_0);

  // If the topic subscription submission was successful
  if(MQTTEngineAPIRes == MQTT_STATUS_OK)
   {
    // Log the successful topic subscription submission
    LOG_DBG("Submitted subscription to the " TOPIC_AVG_FAN_REL_SPEED " topic\n");

    /*
     * The sensor main loop timer is NOT reinitialized, with the sensor process
     * that will be explicitly rescheduled by the MQTT engine upon receiving
     * any between of the following events:
     *  - MQTT_EVENT_UNSUBACK     -> MQTT_CLI_STATE_BROKER_CONNECTED callback
     *  - MQTT_EVENT_PUBLISH      -> MQTT_CLI_STATE_BROKER_SUBSCRIBED callback
     *  - MQTT_EVENT_DISCONNECTED -> MQTT_CLI_STATE_ENGINE_OK or
     *                               MQTT_CLI_STATE_NET_OK callbacks
     */
   }

  // Otherwise, if the topic subscription submission failed
  else
   {
    // Log and attempt to publish the error
    LOG_PUB_ERROR(ERR_SENSOR_SUB_AVGFANRELSPEED_FAILED, "(error = \'%s\')",
                  MQTTEngineAPIResultStr())

    /*
     * Reinitialize the sensor main loop timer, whose expiration will
     * trigger again the MQTT_CLI_STATE_BROKER_CONNECTED callback so as
     * to reattempt to subscribe on the TOPIC_AVG_FAN_REL_SPEED topic
     */
    etimer_restart(&sensorMainLoopTimer);
   }
 }


/**
 * @brief MQTT_CLI_STATE_BROKER_SUBSCRIBED callback function, triggered by the
 *        MQTT engine whenever a MQTT message on a topic the sensor is subscribed
 *        to (supposedly TOPIC_AVG_FAN_REL_SPEED only) has been received
 */
void sensor_MQTT_CLI_STATE_BROKER_SUBSCRIBED_Callback()
 {
  // The candidate new "avgFanRelSpeed" value
  // received on the TOPIC_AVG_FAN_REL_SPEED topic
  unsigned long newAvgFanRelSpeed;

  // Ensure the topic of the received MQTT message to be
  // TOPIC_AVG_FAN_REL_SPEED, logging and publishing the error otherwise
  if(strncmp(secMQTTMsgTopicBuf, TOPIC_AVG_FAN_REL_SPEED, sizeof(secMQTTMsgTopicBuf)) != 0)
   LOG_PUB_ERROR(ERR_SENSOR_MQTT_RECV_NOT_SUB_TOPIC,
                 "(topic = %s)", secMQTTMsgTopicBuf)
  else
   {
    // Interpret the MQTT message contents as an unsigned (long)
    // integer representing the new "avgFanRelSpeed" value
    newAvgFanRelSpeed = strtoul(secMQTTMsgContentsBuf, NULL, 10);

    // Ensure the received "avgFanRelSpeed" value to be valid
    // (<= 100), logging and publishing the error otherwise
    if(newAvgFanRelSpeed > 100)
     LOG_PUB_ERROR(ERR_SENSOR_RECV_INVALID_AVGFANRELSPEED,
                   "(%lu > 100)", newAvgFanRelSpeed)
    else
     {
      // Update the sensor's "avgFanRelSpeed" to the new value
      avgFanRelSpeed = newAvgFanRelSpeed;

      // Log the new average fan relative speed value
      LOG_INFO("Received new average fan relative speed value: %u\n", avgFanRelSpeed);

      /*
       * The sensor main loop timer is NOT reinitialized, with the sensor process
       * that will be explicitly rescheduled by the MQTT engine upon receiving
       * any between of the following events:
       *  - MQTT_EVENT_UNSUBACK     -> MQTT_CLI_STATE_BROKER_CONNECTED callback
       *  - MQTT_EVENT_PUBLISH      -> MQTT_CLI_STATE_BROKER_SUBSCRIBED callback
       *  - MQTT_EVENT_DISCONNECTED -> MQTT_CLI_STATE_ENGINE_OK or
       *                               MQTT_CLI_STATE_NET_OK callbacks
       */
     }
   }
 }


/**
 * @brief SafeTunnels sensor process main body
 * @param ev   The event passed by the Contiki-NG kernel to the process
 * @param data Additional information on the passed event
 */
PROCESS_THREAD(safetunnels_sensor_process, ev, data)
{
 // Contiki-NG process start macro
 PROCESS_BEGIN()

 // Turn ON the POWER_LED at power on
 leds_single_on(POWER_LED);

 // Retrieve the node's MAC address
 getNodeMACAddr();

 // Log that the sensor node has started and its MAC
 LOG_INFO("SafeTunnels sensor node started, MAC = %s\n", nodeMACAddr);

 // Initialize the sensor main loop timer, whose expiration will
 // trigger the initial MQTT_CLI_STATE_INIT callback function
 etimer_set(&sensorMainLoopTimer, (clock_time_t)SENSOR_MAIN_LOOP_PERIOD);

 /* ---------------------------- Sensor Process Main Loop ---------------------------- */
 while(1)
  {
   // Yield the process's execution
   PROCESS_YIELD();

   // If the sensor main loop timer has expired OR the process
   // has been explicitly polled by a MQTT engine callback function
   if((ev == PROCESS_EVENT_TIMER && data == &sensorMainLoopTimer) ||
       ev == PROCESS_EVENT_POLL)
    {
     // Invoke the callback function associated with
     // the sensor's MQTT client current state
     switch(MQTTCliState)
      {
       /* ------------------- The MQTT Engine must be initialized ------------------- */
       case MQTT_CLI_STATE_INIT:

        // Attempt to initialize the MQTT engine
        sensor_MQTT_CLI_STATE_INIT_Callback();
        break;

       /* ------------------- The MQTT Engine has been initialized ------------------- */
       case MQTT_CLI_STATE_ENGINE_OK:

        // Wait for external connectivity
        sensor_MQTT_CLI_STATE_ENGINE_OK_Callback();
        break;

       /* ------------------- The sensor has external connectivity ------------------- */
       case MQTT_CLI_STATE_NET_OK:

        // Attempt to submit a connection request to the MQTT broker
        sensor_MQTT_CLI_STATE_NET_OK_Callback();
        break;

       /* ---- The sensor is waiting for the MQTT broker to accept the connection ---- */
       case MQTT_CLI_STATE_BROKER_CONNECTING:

        // No callback function in this case (just restart the sensor main loop timer)
        etimer_restart(&sensorMainLoopTimer);
        break;

       /* --------------- The sensor is connected with the MQTT broker --------------- */
       case MQTT_CLI_STATE_BROKER_CONNECTED:

        // Attempt to subscribe to the TOPIC_AVG_FAN_REL_SPEED topic on the MQTT broker
        sensor_MQTT_CLI_STATE_BROKER_CONNECTED_Callback();
        break;

       /* ----- The sensor is subscribed on the 'TOPIC_AVG_FAN_REL_SPEED' topic ----- */
       case MQTT_CLI_STATE_BROKER_SUBSCRIBED:

        // Parse a MQTT message received on a topic the sensor is subscribed
        // to (triggered by the MQTT Engine MQTT_EVENT_PUBLISH event callback)
        sensor_MQTT_CLI_STATE_BROKER_SUBSCRIBED_Callback();
        break;

       /* --------------------- Unknown sensor MQTT client state --------------------- */
       default:

        // Log and attempt to publish the unknown sensor MQTT client state
        LOG_PUB_ERROR(ERR_SENSOR_MAIN_LOOP_UNKNOWN_MQTT_CLI_STATE,
                      "(MQTTCliState = %u)", MQTTCliState)
        break;
      }
    }

   // Otherwise, if any of the sensor's buttons has been pressed
   else
    if(ev == button_hal_press_event)
     {
      // Set the quantities' sampling functions to report
      // a maximum, "unsafe" value at their next invocation
      simulateMaxC02 = true;
      simulateMaxTemp = true;

      // Log that the sampled quantities have been
      // simulated to their maximum, "unsafe" values
      LOG_INFO("Sampled quantities simulated to their maximum, unsafe values\n");
     }

    /*
     * NOTE: Checking for unknown events in the process's main loop has been disabled
     *       due to the kernel continuously passing event '150' (this also occurs in
     *       the course "mqtt_client.c" example and has not been further investigated)

    else
     LOG_ERR("An unknown event was passed to the sensor main loop (%u)\n",ev);
    */
  }

 /* -------- Outside the sensor main loop (execution should NEVER reach here) -------- */

 // Turn off ALL LEDs
 leds_off(LEDS_ALL);

 // Attempt to log and publish that the sensor has exited from the main loop
 LOG_PUB_ERROR(ERR_SENSOR_MAIN_LOOP_EXITED,"(last event= %u)",ev)

 // Shut down the sensor process
 PROCESS_END()
}