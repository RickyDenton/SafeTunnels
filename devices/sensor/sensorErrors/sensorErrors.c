
// TODO: Description, clean includes

/* ================================== INCLUDES ================================== */

/* ------------------------------- System Headers ------------------------------- */
#include "contiki.h"
#include "mqtt.h"
#include "sys/ctimer.h"
#include "os/sys/log.h"

/* ------------------------------- Other Headers ------------------------------- */
#include "sensor.h"
#include "sensorErrors/sensorErrors.h"
#include "../common/devUtilities.h"
#include <string.h>
#include <strings.h>


// Buffer used to store application errors descriptions
char errDscr[ERR_DSCR_BUF_SIZE];

/* ============================ FORWARD DECLARATIONS ============================ */

extern uint8_t MQTTCliState;
extern mqtt_status_t MQTTEngineAPIRes;
extern char MQTTTopicBuf[];
extern char MQTTMsgBuf[];
extern struct mqtt_connection mqttConn;
extern struct ctimer MQTTBrokerCommLEDBlinkTimer;





void blinkMQTTBrokerCommLED(__attribute__((unused)) void* ptr);



/* =========================== FUNCTIONS DEFINITIONS =========================== */

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
     sprintf(sensorErrCodeStr, "Failed to subscribe on the \"" TOPIC_AVG_FAN_REL_SPEED "\" topic");
     break;

    case ERR_SENSOR_RECV_INVALID_AVGFANRELSPEED:
     sprintf(sensorErrCodeStr, "Received an invalid \"avgFanRelSpeed\" value");
     break;

    // ---------------------- Invalid Application States ----------------------

    case ERR_SENSOR_MQTT_CONNECTED_NOT_CONNECTING:
     sprintf(sensorErrCodeStr, "Established connection with the MQTT broker when not in the \'MQTT_CLI_STATE_BROKER_CONNECTING\' state");
     break;

    case ERR_SENSOR_MQTT_ENGINE_UNKNOWN_CALLBACK_TYPE:
     sprintf(sensorErrCodeStr, "The MQTT engine invoked a callback of unknown type");
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
    snprintf(MQTTTopicBuf, MQTT_TOPIC_BUF_SIZE, TOPIC_SENSORS_ERRORS);

    // Prepare the error message to be published depending on whether
    // its additional description was stored in the "errDscr" buffer
    if(errDscr[0] != '\0')
     snprintf(MQTTMsgBuf, MQTT_MESSAGE_BUF_SIZE, "{"
                                                     " \"ID\": \"%s\","
                                                     " \"errCode\": %u,"
                                                     " \"errDscr\": \"%s\","
                                                     " \"MQTTCliState\": %u"
                                                     " }", nodeID, sensErrCode, errDscr, MQTTCliState);
    else
     snprintf(MQTTMsgBuf, MQTT_MESSAGE_BUF_SIZE, "{"
                                                     " \"ID\": \"%s\","
                                                     " \"errCode\": %u,"
                                                     " \"MQTTCliState\": %u"
                                                     " }", nodeID, sensErrCode, MQTTCliState);

    // Attempt to publish the error message
    MQTTEngineAPIRes = mqtt_publish(&mqttConn, NULL, MQTTTopicBuf, (uint8_t*)MQTTMsgBuf,
                                    strlen(MQTTMsgBuf), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);

    // If the error message has been successfully submitted to the broker
    if(MQTTEngineAPIRes == MQTT_STATUS_OK)
     {
      // Blink the communication LED
      ctimer_set(&MQTTBrokerCommLEDBlinkTimer, COMM_LED_BLINK_PERIOD, blinkMQTTBrokerCommLED, NULL);

      // Also log the error locally, informating that it was published
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