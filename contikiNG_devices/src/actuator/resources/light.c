/* SafeTunnels Actuator Light Resource Definitions */

/* ================================== INCLUDES ================================== */

/* ------------------------------ Standard Headers ------------------------------ */
#include <string.h>

/* ----------------------------- Contiki-NG Headers ----------------------------- */
#include "contiki.h"
#include "coap-engine.h"
#include "os/sys/log.h"
#include "dev/leds.h"
#include "random.h"

/* ------------------------ SafeTunnels Service Headers ------------------------ */
#include "../actuator.h"
#include "actuatorErrors.h"


/* ============================ FORWARD DECLARATIONS ============================ */

// The light CoAP resource GET, PUT and EVENT handlers
static void light_GET_handler(__attribute__((unused)) coap_message_t* request,
                              coap_message_t* response, uint8_t* buffer,
                              __attribute__((unused)) uint16_t preferred_size,
                              __attribute__((unused)) int32_t* offset);
static void light_PUT_handler(coap_message_t* request, coap_message_t* response,
                              uint8_t* buffer, __attribute__((unused)) uint16_t preferred_size,
                              __attribute__((unused)) int32_t* offset);
static void lightNotifyObservers();


/* ============================== GLOBAL VARIABLES ============================== */

// Actuator Light CoAP resource definition
EVENT_RESOURCE(actuatorLight,           // Resource Name
               "title=\"SafeTunnels Actuator Light (GET | "    // Title
                       "PUT/POST \"lightState\" = \"LIGHT_ON\" | \"LIGHT_OFF\" | "
                       "\"LIGHT_BLINK_ALERT\" | \"LIGHT_BLINK_EMERGENCY\"; "
               "rt=\"lightControl\"; "  // Resource Type
               "ct=50; "                // Content Format (50 -> application/json)
               "obs",                   // Observable resource
               light_GET_handler,       // GET handler
               NULL,                    // POST handler
               light_PUT_handler,       // PUT handler
               NULL,                    // DELETE handler
               lightNotifyObservers);   // EVENT handler

// The light's current state as an enum and string
enum lightResState lightState = LIGHT_OFF;
char lightStateStr[22] = "LIGHT_OFF";

// Timer used to blink the LIGHT_LED into the
// LIGHT_BLINK_ALERT and LIGHT_BLINK_EMERGENCY states
struct ctimer lightLEDBlinkTimer;

// Timer used to notify observers after a
// successful PUT request changed the light state
static struct ctimer lightPUTObsNotifyTimer;


/* =========================== FUNCTIONS DEFINITIONS =========================== */

/* ----------------------------- Utility Functions ----------------------------- */

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


/**
 * @brief Simulates an external actor's intervention changing
 *        the light state, notifying its observers afterwards
 */
void simulateNewLightState()
 {
  // The new generated light state
  enum lightResState newLightState;

  // Randomly generate a new valid light state
  do
   newLightState = random_rand() % 4;
  while (newLightState == lightState ||
         newLightState == LIGHT_STATE_INVALID);  // Enums implementation-dependent mappings safety

  // Stringify the new light state into the
  // "lightStateStr" and drive the LIGHT_LED accordingly
  switch(newLightState)
   {
    case LIGHT_OFF:
     sprintf(lightStateStr,"LIGHT_OFF");

     // Stop the LIGHT_LED blinking timer
     // and turn OFF the LIGHT_LED
     ctimer_stop(&lightLEDBlinkTimer);
     leds_single_off(LIGHT_LED);
     break;

    case LIGHT_ON:
     sprintf(lightStateStr,"LIGHT_ON");

     // Stop the LIGHT_LED blinking timer
     // and turn ON the LIGHT_LED
     ctimer_stop(&lightLEDBlinkTimer);
     leds_single_on(LIGHT_LED);
     break;

    case LIGHT_BLINK_ALERT:
     sprintf(lightStateStr,"LIGHT_BLINK_ALERT");

     // Start the LIGHT_LED blinking timer
     // with the LIGHT_LED_ALERT_BLINK_PERIOD
     ctimer_set(&lightLEDBlinkTimer, LIGHT_LED_ALERT_BLINK_PERIOD,
               lightLEDBlink, NULL);
     break;

    case LIGHT_BLINK_EMERGENCY:
     sprintf(lightStateStr,"LIGHT_BLINK_EMERGENCY");

     // Start the LIGHT_LED blinking timer with
     // the LIGHT_LED_EMERGENCY_BLINK_PERIOD
     ctimer_set(&lightLEDBlinkTimer, LIGHT_LED_EMERGENCY_BLINK_PERIOD,
                lightLEDBlink, NULL);
     break;

    /* --- Invalid generated light state --- */
    default:
     LOG_ERR("INVALID new light state generated in "
             "simulateNewLightState() (%u)\n", newLightState);
     return;
   }

  // Update the light state to its new value
  lightState = newLightState;

  // Log that a new light state has been simulated
  LOG_INFO("Simulated new light state: \'%s\'\n", lightStateStr);

  // Notify the observing clients (in this case immediately
  // without using the timer as no PUT request must be served)
  lightNotifyObservers();
 }


/* ----------------- CoAP Resource Requests Callback Functions ----------------- */

/**
 * @brief Light GET request handler, returning the client (or all
 *        observers) the current light state in JSON format
 * @param response The CoAP response message to be returned to the client(s)
 * @param buffer   A buffer to be used for storing the response message's contents
 */
static void light_GET_handler(__attribute__((unused)) coap_message_t* request,
                              coap_message_t* response, uint8_t* buffer,
                              __attribute__((unused)) uint16_t preferred_size,
                              __attribute__((unused)) int32_t* offset)
 {
  // CoAP response length
  uint8_t respLength;

  // Set the GET CoAP response body as the current light state in JSON format
  sprintf((char*)buffer, "{ \"lightState\": \"%s\" }",lightStateStr);

  // Prepare the metadata of the CoAP response to be returned to the client(s)
  respLength = strlen((char*)buffer);
  coap_set_header_content_format(response, APPLICATION_JSON);
  coap_set_header_etag(response, &respLength, 1);
  coap_set_payload(response, buffer, respLength);

  // Setting the CoAP response status code to "2.05" CONTENT is probably
  // performed by default (not present in any CoAP GET handler example)
  //
  // coap_set_status_code(response, CONTENT_2_05);

  // Log that the light state has been returned to a client
  LOG_DBG("Light state returned (\'%s\')\n",lightStateStr);
 }


/**
 * @brief Light PUT request handler, allowing clients to change its light state
 * @param request  The client's CoAP request message
 * @param response The CoAP response to be returned to the client(s)
 * @param buffer   A buffer to be used for storing the response message's contents
 */
static void light_PUT_handler(coap_message_t* request, coap_message_t* response,
                              uint8_t* buffer, __attribute__((unused)) uint16_t preferred_size,
                              __attribute__((unused)) int32_t* offset)
 {
  // Store the light state received in the request as an enum and as a string
  enum lightResState newLightState = LIGHT_STATE_INVALID;
  const char* newLightStateStr = NULL;

  // Ensure the "lightState" variable to be present in the PUT request body, logging
  // the error and reporting it to the client and to errors observers otherwise
  if(coap_get_post_variable(request, "lightState", &newLightStateStr) <= 0)
   REPORT_COAP_REQ_ERR(ERR_LIGHT_PUT_NO_LIGHTSTATE)
  else
   {
    // Determine the light state passed by the
    // client and drive the LED_LIGHT accordingly
    if(strcmp(newLightStateStr, "LIGHT_OFF") == 0)
     {
      newLightState = LIGHT_OFF;

      // Stop the LIGHT_LED blinking timer
      // and turn OFF the LIGHT_LED
      ctimer_stop(&lightLEDBlinkTimer);
      leds_single_off(LIGHT_LED);
     }
    else
     if(strcmp(newLightStateStr, "LIGHT_ON") == 0)
      {
       newLightState = LIGHT_ON;

       // Stop the LIGHT_LED blinking timer
       // and turn ON the LIGHT_LED
       ctimer_stop(&lightLEDBlinkTimer);
       leds_single_on(LIGHT_LED);
      }
     else
      if(strcmp(newLightStateStr, "LIGHT_BLINK_ALERT") == 0)
       {
        newLightState = LIGHT_BLINK_ALERT;

        // Start the LIGHT_LED blinking timer
        // with the LIGHT_LED_ALERT_BLINK_PERIOD
        ctimer_set(&lightLEDBlinkTimer, LIGHT_LED_ALERT_BLINK_PERIOD,
                   lightLEDBlink, NULL);
       }
      else
       if(strcmp(newLightStateStr, "LIGHT_BLINK_EMERGENCY") == 0)
        {
         newLightState = LIGHT_BLINK_EMERGENCY;

         // Start the LIGHT_LED blinking timer
         // with the LIGHT_LED_EMERGENCY_BLINK_PERIOD
         ctimer_set(&lightLEDBlinkTimer, LIGHT_LED_EMERGENCY_BLINK_PERIOD,
                    lightLEDBlink, NULL);
        }

       // If the light state passed by the client is invalid, log the
       // error and report it to the client and to error observers
       else
        REPORT_COAP_REQ_ERR(ERR_LIGHT_PUT_LIGHTSTATE_INVALID,
                            "(\"%s\")", newLightStateStr)
   }

  // If a valid light state was received
  if(newLightState != LIGHT_STATE_INVALID)
   {
    // Update the "lightStateStr" directly
    // from the CoAP message buffer (optimization)
    strcpy(lightStateStr, newLightStateStr);

    // If the received light state differs from its current value
    if(newLightState != lightState)
     {
      // Log that a new valid light state has been received
      LOG_INFO("Received new light state: \'%s\'\n", lightStateStr);

      // Report in the CoAP response status code
      // that the resource state has changed
      coap_set_status_code(response, CHANGED_2_04);

      // Initialize the lightPUTObsNotifyTimer so as to
      // notify observing clients of the updated resource
      // state immediately after this request has been served
      ctimer_set(&lightPUTObsNotifyTimer, 0, lightNotifyObservers, NULL);

      // Update the light state to its new value
      lightState = newLightState;
     }

    // Otherwise, if the received light state
    // is the same of its current value
    else
     {
      // Log that the same valid light state has been received
      LOG_INFO("Received same light state: \'%s\'\n", lightStateStr);

      // Report in the CoAP response status code
      // that the resource state has NOT changed
      coap_set_status_code(response, VALID_2_03);
     }
   }
 }


/**
 * @brief Notifies all light resource observers of its updated
 *        state (lightPUTObsNotifyTimer callback function)
 */
void lightNotifyObservers()
 { coap_notify_observers(&actuatorLight); }