#include "contiki.h"
#include "coap-engine.h"
#include "../actuator.h"
#include <string.h>
#include <stdio.h>
#include "os/sys/log.h"
#include "dev/leds.h"

//extern char CoAPResBuf[];

// The fan current state
unsigned char fanRelSpeed = 0;

// Last fan observers' notification time
unsigned long fanLastObsNotifyTime = 0;


// The timer used to blink the FAN_LED
// depending on the current "fanRelSpeed"
static struct ctimer fanLEDBlinkTimer;

// Timer used to notify observers after a PUT or POST changed the fan state
static struct ctimer fanPUTPOSTObsNotifyTimer;


// Forward Declarations
static void fan_GET_handler(coap_message_t* request, coap_message_t* response,
                            uint8_t* buffer, uint16_t preferred_size, int32_t* offset);
static void fan_POST_PUT_handler(coap_message_t* request, coap_message_t* response,
                                 uint8_t* buffer, uint16_t preferred_size, int32_t* offset);
static void fanNotifyObservers();


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


// Fan LED blink timer callback
static void fanLEDBlink(__attribute__((unused)) void* ptr)
 {
  // Toggle the FAN_LED
  leds_single_toggle(FAN_LED);

  // Reset the light blink timer
  ctimer_reset(&fanLEDBlinkTimer);
 }


static void fan_GET_handler(coap_message_t* request, coap_message_t* response,
                            uint8_t* buffer, uint16_t preferred_size, int32_t* offset)
 {
  uint8_t resLength;

  sprintf((char*)buffer, "{ \"fanRelSpeed\": %u }",fanRelSpeed);
  resLength = strlen((char*)buffer);

  // Prepare the response
  coap_set_header_content_format(response, APPLICATION_JSON);
  coap_set_header_etag(response, &resLength, 1);
  coap_set_payload(response, buffer, resLength);

  // Probably default and thus unnecessary (not used in any CoAP GET handler example)
  // coap_set_status_code(response, CONTENT_2_05);
 }


static void fan_POST_PUT_handler(coap_message_t* request, coap_message_t* response,
                                 uint8_t* buffer, uint16_t preferred_size, int32_t* offset)
 {
  //size_t newLightStateLen = 0;
  const char*newFanRelSpeedStr = NULL;
  unsigned long newFanRelSpeed;
  bool newFanRelSpeedValid;

  newFanRelSpeedValid = false;

  if(coap_get_post_variable(request, "fanRelSpeed", &newFanRelSpeedStr) > 0)
   {
    // Interpret the value of the "fanRelSpeed" variable as an unsigned
    // (long) integer representing the new "fanRelSpeed" value
    newFanRelSpeed = strtoul(newFanRelSpeedStr, NULL, 10);

    // Ensure the received "newFanRelSpeed" value to be valid (<= 100)
    if(newFanRelSpeed <= 100)
     newFanRelSpeedValid = true;
    else
     LOG_INFO("Received INVALID new fan relative speed: %lu\n", newFanRelSpeed);
   }
  else
   LOG_ERR("MISSING \'fanRelSpeed\' variable in fan PUT\n");

  if(newFanRelSpeedValid)
   {
    // Update the fan relative speed
    fanRelSpeed = newFanRelSpeed;


    LOG_INFO("Received valid new fan relative speed: %u\n", fanRelSpeed);

    // Vary the FAN_LED blinking frequency
    if(fanRelSpeed == 0)
     ctimer_stop(&fanLEDBlinkTimer);
    else
     ctimer_set(&fanLEDBlinkTimer, FAN_LED_BLINK_PERIOD_MAX - fanRelSpeed * FAN_LED_BLINK_PERIOD_UNIT,
                fanLEDBlink, NULL);

    // Notify observing clients
    // TODO: Only if the status has changed
    ctimer_set(&fanPUTPOSTObsNotifyTimer, 0, fanNotifyObservers, NULL);
   }
  else
   {
    coap_set_status_code(response, BAD_REQUEST_4_00);

    // TODO: Send the error to the Control Module and possibly in the response's payload

   }
 }


// Notify all Observers
static void fanNotifyObservers()
 {
  // Notify all observers
  coap_notify_observers(&actuatorFan);

  // Update the last fan observers notification time
  fanLastObsNotifyTime = clock_seconds();
 }





