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


// The timer used to blink the FAN_LED
// depending on the current "fanRelSpeed"
static struct ctimer fanLEDBlinkTimer;


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
 }


static void fan_PUT_handler(coap_message_t* request, coap_message_t* response,
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
   }
  else
   {
    coap_set_status_code(response, BAD_REQUEST_4_00);

    // TODO: Send the error to the Control Module and possibly in the response's payload

   }
 }


// Actuator Fan Resource Definition
RESOURCE(actuatorFan,
         "title=\"Fan\"; GET; PUT \"fanRelSpeed\" = <fanRelSpeed> 0-100; rt=\"JSON\"",
         fan_GET_handler,
         NULL,
         fan_PUT_handler,
         NULL);