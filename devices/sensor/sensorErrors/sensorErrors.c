
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



/* ============================ FORWARD DECLARATIONS ============================ */

extern uint8_t MQTTCliState;
extern mqtt_status_t mqttCliEngineAPIRes;
extern char MQTTTopicBuf[];
extern char MQTTMsgBuf[];
extern char errDscr[];
extern struct mqtt_connection mqttConn;
extern struct ctimer MQTTBrokerCommLEDBlinkTimer;





void blinkMQTTBrokerCommLED(__attribute__((unused)) void* ptr);



char* MQTTCliStateToStr()
 {
  // Stores a MQTT client status as a string
  static char MQTTCliStateStr[36];

  // Stringify the "MQTTCliState" enum into "MQTTCliState"
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

    /* Unknown MQTT client state */
    default:
     sprintf(MQTTCliStateStr,"MQTT_CLI_STATE_UNKNOWN");
     break;
   }

  // Return the stringyfied MQTT client state
  return MQTTCliStateStr;
 }

char* MQTTEngineAPIResultStr()
 {
  // Stores a MQTT Engine API result as a string
  static char MQTTAPIStatusStr[32];

  // Stringify the "mqttCliEngineAPIRes" enum into "MQTTAPIStatusStr"
  switch(mqttCliEngineAPIRes)
   {
    /* Normal Execution */
    case MQTT_STATUS_OK:
     sprintf(MQTTAPIStatusStr, "MQTT_STATUS_OK");
    break;

    case MQTT_STATUS_OUT_QUEUE_FULL:
     sprintf(MQTTAPIStatusStr, "MQTT_STATUS_OUT_QUEUE_FULL");
    break;

    /* Errors */
    case MQTT_STATUS_ERROR:
     sprintf(MQTTAPIStatusStr, "MQTT_STATUS_ERROR");
    break;

    case MQTT_STATUS_NOT_CONNECTED_ERROR:
     sprintf(MQTTAPIStatusStr, "MQTT_STATUS_NOT_CONNECTED_ERROR");
    break;

    case MQTT_STATUS_INVALID_ARGS_ERROR:
     sprintf(MQTTAPIStatusStr, "MQTT_STATUS_INVALID_ARGS_ERROR");
    break;

    case MQTT_STATUS_DNS_ERROR:
     sprintf(MQTTAPIStatusStr, "MQTT_STATUS_DNS_ERROR");
    break;

    /* Unknown MQTT status */
    default:
     sprintf(MQTTAPIStatusStr, "MQTT_STATUS_UNKNOWN");
    break;
   }

  // Return the stringyfied MQTT Engine API result
  return MQTTAPIStatusStr;
 }


char* sensorErrCodeToStr(enum sensorErrCode sensErrCode)
 {
  // Stores a sensor error code as a string
  static char sensorErrCodeStr[105];

  // Stringify the "sensErrCode" enum into "sensorErrCodeStr"
  switch(sensErrCode)
   {
    case ERR_SENSOR_QUANTITY_PUB_FAILED:
     sprintf(sensorErrCodeStr, "Failed to publish a sampled quantity");
     break;

    case ERR_SENSOR_AVGFANRELSPEED_SUB_FAILED:
     sprintf(sensorErrCodeStr, "Failed to subscribe on the \"SafeTunnels/avgFanRelSpeed\" topic");
     break;

    case ERR_SENSOR_MQTT_RECV_UNKNOWN_TOPIC:
     sprintf(sensorErrCodeStr, "The sensor received a MQTT message on a topic it is not subscribed on");
     break;

    case ERR_SENSOR_AVGFANRELSPEED_INVALID:
     sprintf(sensorErrCodeStr, "The sensor received an invalid \"fanRelSpeed\" value");
     break;

    case ERR_SENSOR_MQTT_CONNECTED_IN_INVALID_STATE:
     sprintf(sensorErrCodeStr, "The sensor has connected with the MQTT broker when not in the \'MQTT_CLI_STATE_BROKER_CONNECTING\' state");
     break;

    case ERR_SENSOR_MQTT_DISCONNECTED:
     sprintf(sensorErrCodeStr, "The sensor has disconnected from the MQTT broker");
     break;

    case ERR_SENSOR_MQTT_CLI_CALLBACK_UNKNOWN_TYPE:
     sprintf(sensorErrCodeStr, "The MQTT engine invoked a callback of unknown type");
     break;

    case ERR_SENSOR_MAIN_LOOP_UNKNOWN_EVENT:
     sprintf(sensorErrCodeStr, "An unknown event was passed to the sensor main loop");
     break;

    case ERR_SENSOR_MAIN_LOOP_UNKNOWN_MQTT_CLI_STATE:
     sprintf(sensorErrCodeStr, "Unknown MQTT client state in the sensor process main loop");
     break;

    case ERR_SENSOR_MAIN_LOOP_EXITED:
     sprintf(sensorErrCodeStr, "Sensor process exited from its main loop");
     break;

    /* Unknown sensor error code */
    default:
     sprintf(sensorErrCodeStr, "UNKNOWN SENSOR ERROR");
    break;
   }

  // Return the stringyfied sensor error code
  return sensorErrCodeStr;
 }


void logPublishError(enum sensorErrCode sensErrCode)
 {
  // If the node's MQTT client is NOT connected with the MQTT
  // broker, just log the error and that it couldn't be published
  if(MQTTCliState < MQTT_CLI_STATE_BROKER_CONNECTED)
   LOG_ERR("%s %s (MQTTCliState = \'%s\') (the error couldn't be published as the MQTT client is not connected with the broker)\n",sensorErrCodeToStr(sensErrCode),errDscr,MQTTCliStateToStr());

   // Otherwise, If the node's MQTT client IS connected with the MQTT broker
  else
   {
    // Prepare the topic of the error to be published
    snprintf(MQTTTopicBuf, MQTT_TOPIC_BUF_SIZE, TOPIC_SENSORS_ERRORS);

    // Prepare the error message to be published depending on whether
    // an additional description was provided in the "errDscr" buffer
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

    // Attempt to publish the error on the MQTT broker
    mqttCliEngineAPIRes = mqtt_publish(&mqttConn, NULL, MQTTTopicBuf, (uint8_t*)MQTTMsgBuf, strlen(MQTTMsgBuf), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);

    // If the error has been submitted successfully
    if(mqttCliEngineAPIRes == MQTT_STATUS_OK)
     {
      // Blink the communication LED
      ctimer_set(&MQTTBrokerCommLEDBlinkTimer, COMM_LED_BLINK_PERIOD, blinkMQTTBrokerCommLED, NULL);

      // Log the error locally, informing that it has also been published
      LOG_ERR("%s %s (MQTTCliState = \'%s\') (submitted to the broker)\n",sensorErrCodeToStr(sensErrCode),errDscr,MQTTCliStateToStr());
     }

     // Otherwise just log the error locally, informing that it couldn't be published
    else
     LOG_ERR("%s %s (MQTTCliState = \'%s\') (failed to submit to the broker for error \'%s\')\n", sensorErrCodeToStr(sensErrCode), errDscr, MQTTCliStateToStr(), MQTTEngineAPIResultStr());
   }
 }