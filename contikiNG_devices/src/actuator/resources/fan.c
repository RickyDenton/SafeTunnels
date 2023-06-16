/* SafeTunnels Actuator Fan Resource Definitions */

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

// The fan CoAP resource GET, PUT and EVENT handlers
static void fan_GET_handler(__attribute__((unused)) coap_message_t* request,
                            coap_message_t* response,uint8_t* buffer,
                            __attribute__((unused)) uint16_t preferred_size,
                            __attribute__((unused)) int32_t* offset);
static void fan_PUT_handler(coap_message_t* request, coap_message_t* response,
                            uint8_t* buffer, __attribute__((unused)) uint16_t preferred_size,
                            __attribute__((unused)) int32_t* offset);
static void fanNotifyObservers();


/* ============================== GLOBAL VARIABLES ============================== */

// Actuator Fan CoAP Resource Definition
EVENT_RESOURCE(actuatorFan,           // Resource Name
               "title=\"SafeTunnels Actuator Fan (GET | PUT/POST "   // Title
                       "\"fanRelSpeed\" = <fanRelSpeed> [0-100]\"; "
               "rt=\"fanControl\"; "  // Resource Type
               "ct=50; "              // Content Format (50 -> application/json)
               "obs",                 // Observable resource
               fan_GET_handler,       // GET handler
               NULL,                  // POST handler
               fan_PUT_handler,       // PUT handler
               NULL,                  // DELETE handler
               fanNotifyObservers);   // EVENT handler


// The fan's current relative speed [0-100]
unsigned char fanRelSpeed = 0;

// Timer used to blink the FAN_LED with a period
// inversely proportional to its relative speed
static struct ctimer fanLEDBlinkTimer;

// Timer used to notify observers after a
// successful PUT request changed the fan state
static struct ctimer fanPUTObsNotifyTimer;


/* =========================== FUNCTIONS DEFINITIONS =========================== */

/* ----------------------------- Utility Functions ----------------------------- */

/**
 * @brief Toggles the FAN_LED so as to blink it
 *        (fanLEDBlinkTimer callback function)
 */
static void fanLEDBlink(__attribute__((unused)) void* ptr)
 {
  // Toggle the FAN_LED
  leds_single_toggle(FAN_LED);

  // Reset the fan blink timer
  ctimer_reset(&fanLEDBlinkTimer);
 }

/**
 * @brief Simulates an external actor's intervention changing the
 *        fan relative speed, notifying its observers afterwards
 */
void simulateNewFanRelSpeed()
 {
  // The new generated fan relative speed
  unsigned char newFanRelSpeed;

  // Randomly generate a new valid fan relative speed
  do
   newFanRelSpeed = random_rand() % 101;
  while (newFanRelSpeed == fanRelSpeed);

  // Adjust the FAN_LED blinking period
  // to the generated fan relative speed
  if(newFanRelSpeed == 0)
   {
    ctimer_stop(&fanLEDBlinkTimer);
    leds_single_off(FAN_LED);
   }
  else
   ctimer_set(&fanLEDBlinkTimer,
              FAN_LED_BLINK_PERIOD_MAX - (unsigned char)newFanRelSpeed * FAN_LED_BLINK_PERIOD_UNIT,
              fanLEDBlink, NULL);

  // Update the fan relative speed to its new value
  fanRelSpeed = newFanRelSpeed;

  // Log that a new fan relative speed has been simulated
  LOG_INFO("Simulated new fan relative speed: %u\n", fanRelSpeed);

  // Notify the observing clients (in this case immediately
  // without using the timer as no PUT request must be served)
  fanNotifyObservers();
 }


/* ----------------- CoAP Resource Requests Callback Functions ----------------- */

/**
 * @brief Fan GET request handler, returning the client (or all
 *        observers) the current fan relative speed in JSON format
 * @param response The CoAP response message to be returned to the client(s)
 * @param buffer   A buffer to be used for storing the response message's contents
 */
static void fan_GET_handler(__attribute__((unused)) coap_message_t* request,
                            coap_message_t* response, uint8_t* buffer,
                            __attribute__((unused)) uint16_t preferred_size,
                            __attribute__((unused)) int32_t* offset)
 {
  // CoAP response length
  uint8_t respLength;

  // Set the GET CoAP response body as the
  // current fan relative speed in JSON format
  sprintf((char*)buffer, "{ \"fanRelSpeed\": %u }",fanRelSpeed);

  // Prepare the metadata of the CoAP response to be returned to the client(s)
  respLength = strlen((char*)buffer);
  coap_set_header_content_format(response, APPLICATION_JSON);
  coap_set_header_etag(response, &respLength, 1);
  coap_set_payload(response, buffer, respLength);

  // Setting the CoAP response status code to "2.05" CONTENT is probably
  // performed by default (not present in any CoAP GET handler example)
  //
  // coap_set_status_code(response, CONTENT_2_05);

  // Log that the fan relative speed has been returned to a client
  LOG_DBG("Fan relative speed returned (%u)\n",fanRelSpeed);
 }


/**
 * @brief Fan PUT request handler, allowing clients to change its relative speed
 * @param request  The client's CoAP request message
 * @param response The CoAP response message to be returned to the client(s)
 * @param buffer   A buffer to be used for storing the response message's contents
 */
static void fan_PUT_handler(coap_message_t* request, coap_message_t* response,
                            uint8_t* buffer, __attribute__((unused)) uint16_t preferred_size,
                            __attribute__((unused)) int32_t* offset)
 {
  // Store the fan relative speed received in the request
  // as an unsigned (long) integer and as a string
  unsigned long newFanRelSpeed;
  const char* newFanRelSpeedStr = NULL;

  // Ensure the "fanRelSpeed" variable to be present in the PUT request body, logging
  // the error and reporting it to the client and to errors observers otherwise
  if(coap_get_post_variable(request, "fanRelSpeed", &newFanRelSpeedStr) <= 0)
   REPORT_COAP_REQ_ERR(ERR_FAN_PUT_NO_FANRELSPEED)
  else
   {
    // Interpret the "fanRelSpeed" variable value as an unsigned
    // (long) integer representing the new fan relative speed value
    newFanRelSpeed = strtoul(newFanRelSpeedStr, NULL, 10);

    // If the received fan relative speed is invalid (> 100), log
    // the error and report it to the client and to error observers
    if(newFanRelSpeed > 100)
     REPORT_COAP_REQ_ERR(ERR_FAN_PUT_FANRELSPEED_INVALID, "(%lu)", newFanRelSpeed)

    // Otherwise, if the received fan relative speed is valid (<= 100)
    else
     {
      // If the received fan relative speed differs from its current value
      if(newFanRelSpeed != fanRelSpeed)
       {
        // Adjust the FAN_LED blinking period to the new fan relative speed
        if(newFanRelSpeed == 0)
         {
          ctimer_stop(&fanLEDBlinkTimer);
          leds_single_off(FAN_LED);
         }
        else
         ctimer_set(&fanLEDBlinkTimer, FAN_LED_BLINK_PERIOD_MAX - (unsigned char)newFanRelSpeed * FAN_LED_BLINK_PERIOD_UNIT, fanLEDBlink, NULL);

        // Log that a new valid fan relative speed has been received
        LOG_INFO("Received new fan relative speed: %lu\n", newFanRelSpeed);

        // Report in the CoAP response status code
        // that the resource state has changed
        coap_set_status_code(response, CHANGED_2_04);

        // Initialize the fanPUTObsNotifyTimer so as to
        // notify observing clients of the updated resource
        // state immediately after this request has been served
        ctimer_set(&fanPUTObsNotifyTimer, 0, fanNotifyObservers, NULL);

        // Update the fan relative speed to its new value
        fanRelSpeed = newFanRelSpeed;
       }

      // Otherwise, if the received fan relative
      // speed is the same of its current value
      else
       {
        // Log that the same valid fan relative speed has been received
        LOG_INFO("Received same fan relative speed: %lu\n", newFanRelSpeed);

        // Report in the CoAP response status code
        // that the resource state has NOT changed
        coap_set_status_code(response, VALID_2_03);
       }
     }
   }
 }


/**
 * @brief Notifies all fan resource observers of its updated
 *        state (fanPUTObsNotifyTimer callback function)
 */
void fanNotifyObservers()
 { coap_notify_observers(&actuatorFan); }