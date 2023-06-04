#include "contiki.h"
#include "coap-engine.h"
#include "../actuator.h"
#include <string.h>
#include <stdio.h>
#include <limits.h>
#include "os/sys/log.h"
#include "dev/leds.h"
#include "actuatorErrors.h"
#include "random.h"

// --------------------- Forward Declarations ----------------------------


static void fan_GET_handler(__attribute__((unused)) coap_message_t* request, coap_message_t* response,
                            uint8_t* buffer, __attribute__((unused)) uint16_t preferred_size, __attribute__((unused)) int32_t* offset);
static void fan_POST_PUT_handler(coap_message_t* request, coap_message_t* response,
                                 uint8_t* buffer, __attribute__((unused)) uint16_t preferred_size, __attribute__((unused)) int32_t* offset);
static void fanNotifyObservers();



// The fan current relative speed
unsigned char fanRelSpeed = 0;

// The timer used to blink the FAN_LED
// depending on the current "fanRelSpeed"
static struct ctimer fanLEDBlinkTimer;

// Timer used to notify observers after a PUT or POST changed the fan state
static struct ctimer fanPUTPOSTObsNotifyTimer;



// Actuator Fan Resource Definition
EVENT_RESOURCE(actuatorFan,
               "title=\"SafeTunnels Actuator Fan (GET | PUT/POST "
                      "\"fanRelSpeed\" = <fanRelSpeed> [0-100]\"; "
               "rt=\"fanControl\"; "
               "ct=50; "              // ct = 50 -> application/json
               "obs",                 // Observable resource
               fan_GET_handler,
               fan_POST_PUT_handler,
               fan_POST_PUT_handler,
               NULL,
               fanNotifyObservers);


/**
 * @brief Toggles the FAN_LED so as to blink it
 *        (fanLEDBlinkTimer callback function)
 */
static void fanLEDBlink(__attribute__((unused)) void* ptr)
 {
  // Toggle the FAN_LED
  leds_single_toggle(FAN_LED);

  // Reset the light blink timer
  ctimer_reset(&fanLEDBlinkTimer);
 }


static void fan_GET_handler(__attribute__((unused)) coap_message_t* request,
                            coap_message_t* response, uint8_t* buffer,
                            __attribute__((unused)) uint16_t preferred_size,
                            __attribute__((unused)) int32_t* offset)
 {
  // Length of the GET response
  uint8_t respLength;

  // Set the GET CoAP response body as the
  // current fan relative speed in JSON format
  sprintf((char*)buffer, "{ \"fanRelSpeed\": %u }",fanRelSpeed);


  // Prepare the metadata of the CoAP response to be returned to the client
  respLength = strlen((char*)buffer);
  coap_set_header_content_format(response, APPLICATION_JSON);
  coap_set_header_etag(response, &respLength, 1);
  coap_set_payload(response, buffer, respLength);

  // Probably default and thus unnecessary (not used in any CoAP GET handler example)
  // coap_set_status_code(response, CONTENT_2_05);

  // Log that the fan relative speed has been returned
  LOG_DBG("Fan relative speed returned (%u)\n",fanRelSpeed);
 }


static void fan_POST_PUT_handler(coap_message_t* request, coap_message_t* response,
                                 uint8_t* buffer, __attribute__((unused)) uint16_t preferred_size,
                                 __attribute__((unused)) int32_t* offset)
 {
  // Used to parse the content of the POST/PUT request
  const char* newFanRelSpeedStr = NULL;
  unsigned long newFanRelSpeed = ULONG_MAX;

  // Ensure the "fanRelSpeed" variable to be present in the POST/PUT request
  if(coap_get_post_variable(request, "fanRelSpeed", &newFanRelSpeedStr) <= 0)
   REPORT_COAP_REQ_ERR(ERR_FAN_POST_PUT_NO_FANRELSPEED)
  else
   {
    // Interpret the value of the "fanRelSpeed" variable as
    // the unsigned (long) integer new "fanRelSpeed" value
    newFanRelSpeed = strtoul(newFanRelSpeedStr, NULL, 10);

    // If the new "fanRelSpeed" value is invalid, report the error
    if(newFanRelSpeed > 100)
     REPORT_COAP_REQ_ERR(ERR_FAN_POST_PUT_FANRELSPEED_INVALID, "(%lu)", newFanRelSpeed)

    // Otherwise, if the new "fanRelSpeed" value is valid
    else
     {
      // Adjust the FAN_LED blinking period to the new fan relative speed
      if(newFanRelSpeed == 0)
       ctimer_stop(&fanLEDBlinkTimer);
      else
       ctimer_set(&fanLEDBlinkTimer,
                  FAN_LED_BLINK_PERIOD_MAX - (unsigned char)newFanRelSpeed * FAN_LED_BLINK_PERIOD_UNIT,
                  fanLEDBlink, NULL);

      // If the fan relative speed has changed
      if(newFanRelSpeed != fanRelSpeed)
       {
        // Log that a new valid fan relative speed has been received
        LOG_INFO("Received valid new fan relative speed: %lu\n", newFanRelSpeed);

        // Report in the response message status code
        // that the fan's relative speed has changed
        coap_set_status_code(response, CHANGED_2_04);

        // Initialize a timer to notify the observing clients
        // immediately after this request has been served
        ctimer_set(&fanPUTPOSTObsNotifyTimer, 0, fanNotifyObservers, NULL);
       }

      // Otherwise, if the light state has NOT changed
      else
       {
        // Log that the same valid fan relative speed has been received
        LOG_INFO("Received same valid fan relative speed: %lu\n", newFanRelSpeed);

        // Report in the response message status code
        // that the fan relative speed has NOT changed
        coap_set_status_code(response, VALID_2_03);
       }

      // Update the fan relative speed
      fanRelSpeed = newFanRelSpeed;
     }
   }
 }


/**
 * @brief Notifies all observers subscribed on the fan resource
 *        (fanPUTPOSTObsNotifyTimer callback function)
 */
void fanNotifyObservers()
 { coap_notify_observers(&actuatorFan); }



void simulateNewFanRelSpeed()
 {
  unsigned char newFanRelSpeed;

  // Randomly select a new valid fan relative speed
  do
   newFanRelSpeed = random_rand() % 101;
  while (newFanRelSpeed == fanRelSpeed);

  // Adjust the FAN_LED blinking period to the new fan relative speed
  if(newFanRelSpeed == 0)
   ctimer_stop(&fanLEDBlinkTimer);
  else
   ctimer_set(&fanLEDBlinkTimer,
              FAN_LED_BLINK_PERIOD_MAX - (unsigned char)newFanRelSpeed * FAN_LED_BLINK_PERIOD_UNIT,
              fanLEDBlink, NULL);

  // Update the fan relative speed
  fanRelSpeed = newFanRelSpeed;

  // Log that a new fan relative speed has been simulated
  LOG_INFO("Simulated new fan relative speed %u\n", fanRelSpeed);

  // Notify the observing clients (in this case, immediately)
  fanNotifyObservers();
 }