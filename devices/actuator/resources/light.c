#include "contiki.h"
#include "coap-engine.h"
#include "../actuator.h"
#include <string.h>
#include <stdio.h>
#include "os/sys/log.h"
#include "dev/leds.h"
#include "actuatorErrors.h"
#include "random.h"

// Forward Declarations
static void light_GET_handler(__attribute__((unused)) coap_message_t* request, coap_message_t* response,
                              uint8_t* buffer, __attribute__((unused)) uint16_t preferred_size, __attribute__((unused)) int32_t* offset);
static void light_POST_PUT_handler(coap_message_t* request, coap_message_t* response,
                                   uint8_t* buffer, __attribute__((unused)) uint16_t preferred_size, __attribute__((unused)) int32_t* offset);
static void lightNotifyObservers();



// The light's current state
enum lightResState lightState = LIGHT_OFF;
char lightStateStr[22] = "LIGHT_OFF";

// The timer used to blink the LIGHT_LED into the
// LIGHT_BLINK_ALERT and LIGHT_BLINK_EMERGENCY states
struct ctimer lightLEDBlinkTimer;

// Timer used to notify observers after a PUT or POST changed the light state
static struct ctimer lightPUTPOSTObsNotifyTimer;



// Actuator Light Resource Definition
EVENT_RESOURCE(actuatorLight,
               "title=\"SafeTunnels Actuator Light (GET | PUT/POST \"lightState\" = \"LIGHT_ON\" | "
               "\"LIGHT_OFF\" | \"LIGHT_BLINK_ALERT\" | \"LIGHT_BLINK_EMERGENCY\"; "
               "rt=\"lightControl\"; "
               "ct=50; "              // ct = 50 -> application/json
               "obs",                 // Observable resource
               light_GET_handler,
               light_POST_PUT_handler,
               light_POST_PUT_handler,
               NULL,
               lightNotifyObservers);



/**
 * @brief Toggles the LIGHT_LED so as to blink it
 *        (lightLEDBlinkTimer callback function)
 */
static void lightLEDBlink(__attribute__((unused)) void* ptr)
 {
  // Toggle the LIGHT_LED
  leds_single_toggle(LIGHT_LED);

  // Reset the light blink timer
  ctimer_reset(&lightLEDBlinkTimer);
 }


static void light_GET_handler(__attribute__((unused)) coap_message_t* request,
                              coap_message_t* response, uint8_t* buffer,
                              __attribute__((unused)) uint16_t preferred_size,
                              __attribute__((unused)) int32_t* offset)
 {
  // Length of the GET response
  uint8_t respLength;

  // Set the GET CoAP response body as the current light state in JSON format
  sprintf((char*)buffer, "{ \"lightState\": \"%s\" }",lightStateStr);

  // Prepare the metadata of the CoAP response to be returned to the client
  respLength = strlen((char*)buffer);
  coap_set_header_content_format(response, APPLICATION_JSON);
  coap_set_header_etag(response, &respLength, 1);
  coap_set_payload(response, buffer, respLength);

  // Probably default and thus unnecessary (not used in any CoAP GET handler example)
  // coap_set_status_code(response, CONTENT_2_05);

  // Log that the light state has been returned
  LOG_DBG("Light state returned (\'%s\')\n",lightStateStr);
 }


static void light_POST_PUT_handler(coap_message_t* request, coap_message_t* response,
                                   uint8_t* buffer, __attribute__((unused)) uint16_t preferred_size,
                                   __attribute__((unused)) int32_t* offset)
{
 // Used to parse the content of the POST/PUT request
 const char* newLightStateStr = NULL;
 enum lightResState newLightState = LIGHT_STATE_INVALID;

 // Ensure the "lightState" variable to be present in the POST/PUT request
 if(coap_get_post_variable(request, "lightState", &newLightStateStr) <= 0)
  REPORT_COAP_REQ_ERR(ERR_LIGHT_POST_PUT_NO_LIGHTSTATE)
 else
  {
   // Check whether a valid "lightState" value has been
   // passed, modifying the LIGHT_LED appropriately
   if(strcmp(newLightStateStr, "LIGHT_OFF") == 0)
    {
     // Stop the LIGHT_LED blinking timer
     ctimer_stop(&lightLEDBlinkTimer);

     // Turn OFF the LIGHT_LED
     leds_single_off(LIGHT_LED);

     newLightState = LIGHT_OFF;
    }
   else
    if(strcmp(newLightStateStr, "LIGHT_ON") == 0)
     {
      // Stop the LIGHT_LED blinking timer
      ctimer_stop(&lightLEDBlinkTimer);

      // Turn ON the LIGHT_LED
      leds_single_on(LIGHT_LED);

      newLightState = LIGHT_ON;
     }
    else
     if(strcmp(newLightStateStr, "LIGHT_BLINK_ALERT") == 0)
      {
       // Start the LIGHT_LED blinking timer
       ctimer_set(&lightLEDBlinkTimer, LIGHT_LED_ALERT_BLINK_PERIOD, lightLEDBlink, NULL);

       newLightState = LIGHT_BLINK_ALERT;
      }
     else
      if(strcmp(newLightStateStr, "LIGHT_BLINK_EMERGENCY") == 0)
       {
        // Start the LIGHT_LED blinking timer
        ctimer_set(&lightLEDBlinkTimer, LIGHT_LED_EMERGENCY_BLINK_PERIOD, lightLEDBlink, NULL);

        newLightState = LIGHT_BLINK_EMERGENCY;
       }

      // If the light state value is invalid, report the error
      else
       REPORT_COAP_REQ_ERR(ERR_LIGHT_POST_PUT_LIGHTSTATE_INVALID,"(\"%s\")",newLightStateStr)
  }

 // If a valid light state value was passed
 if(newLightState != LIGHT_STATE_INVALID)
  {
   // Update the "lightStateStr" directly from
   // the CoAP message buffer (optimization)
   strcpy(lightStateStr, newLightStateStr);

   // If the light state has changed
   if(newLightState != lightState)
    {
     // Log that a new valid light state has been received
     LOG_INFO("Received valid new light state \'%s\'\n", lightStateStr);

     // Report in the response message status
     // code that the light's state has changed
     coap_set_status_code(response, CHANGED_2_04);

     // Initialize a timer to notify the observing clients
     // immediately after this request has been served
     ctimer_set(&lightPUTPOSTObsNotifyTimer, 0, lightNotifyObservers, NULL);
    }

   // Otherwise, if the light state has NOT changed
   else
    {
     // Log that the same valid light state has been received
     LOG_INFO("Received same valid light state \'%s\'\n", lightStateStr);

     // Report in the response message status
     // code that the light state has NOT changed
     coap_set_status_code(response, VALID_2_03);
    }

   // Update the light state
   lightState = newLightState;
  }
}


/**
 * @brief Notifies all observers subscribed on the light resource
 *        (lightPUTPOSTObsNotifyTimer callback function)
 */
void lightNotifyObservers()
 { coap_notify_observers(&actuatorLight); }


void simulateNewLightState()
 {
  enum lightResState newLightState;

  // Randomly select a new valid light state
  do
   newLightState = random_rand() % 4;
  while (newLightState == LIGHT_STATE_INVALID || newLightState == lightState);

  // Stringify the new light state into the
  // "lightStateStr" and drive the LIGHT_LED accordingly
  switch(newLightState)
   {
    case LIGHT_OFF:
     sprintf(lightStateStr,"LIGHT_OFF");
     ctimer_stop(&lightLEDBlinkTimer);
     leds_single_off(LIGHT_LED);
     break;

    case LIGHT_ON:
     sprintf(lightStateStr,"LIGHT_ON");
     ctimer_stop(&lightLEDBlinkTimer);
     leds_single_on(LIGHT_LED);
     break;

    case LIGHT_BLINK_ALERT:
     sprintf(lightStateStr,"LIGHT_BLINK_ALERT");
     ctimer_set(&lightLEDBlinkTimer, LIGHT_LED_ALERT_BLINK_PERIOD,
                lightLEDBlink, NULL);
     break;

    case LIGHT_BLINK_EMERGENCY:
     sprintf(lightStateStr,"LIGHT_BLINK_EMERGENCY");
     ctimer_set(&lightLEDBlinkTimer, LIGHT_LED_EMERGENCY_BLINK_PERIOD, lightLEDBlink, NULL);
     break;

    default:
     LOG_ERR("Generated an INVALID new light state in simulateNewLightState() (%u)\n", newLightState);
     return;
   }

  // Update the light state
  lightState = newLightState;

  // Log that a new light state has been simulated
  LOG_INFO("Simulated new light state \'%s\'\n", lightStateStr);

  // Notify the observing clients (in this case, immediately)
  lightNotifyObservers();
 }