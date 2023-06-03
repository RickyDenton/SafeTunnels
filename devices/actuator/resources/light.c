#include "contiki.h"
#include "coap-engine.h"
#include "../actuator.h"
#include <string.h>
#include <stdio.h>
#include "os/sys/log.h"
#include "dev/leds.h"

//extern char CoAPResBuf[];

// The light's current state
// enum lightResState lightState = LIGHT_OFF;
char lightStateStr[22] = "LIGHT_OFF";

// Last light observers' notification time
unsigned long lightLastObsNotifyTime = 0;


// The timer used to blink the LIGHT_LED into the
// LIGHT_BLINK_ALERT and LIGHT_BLINK_EMERGENCY states
static struct ctimer lightLEDBlinkTimer;

// Timer used to notify observers after a PUT or POST changed the light state
static struct ctimer lightPUTPOSTObsNotifyTimer;



// Forward Declarations
static void light_GET_handler(coap_message_t* request, coap_message_t* response,
                              uint8_t* buffer, uint16_t preferred_size, int32_t* offset);
static void light_POST_PUT_handler(coap_message_t* request, coap_message_t* response,
                                   uint8_t* buffer, uint16_t preferred_size, int32_t* offset);
static void lightNotifyObservers();


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



// Light LED blink timer callback
static void lightLEDBlink(__attribute__((unused)) void* ptr)
 {
  // Toggle the LIGHT_LED
  leds_single_toggle(LIGHT_LED);

  // Reset the light blink timer
  ctimer_reset(&lightLEDBlinkTimer);
 }


static void light_GET_handler(coap_message_t* request, coap_message_t* response,
                              uint8_t* buffer, uint16_t preferred_size, int32_t* offset)
 {
  uint8_t resLength;

  sprintf((char*)buffer, "{ \"lightState\": \"%s\" }",lightStateStr);
  resLength = strlen((char*)buffer);

  // Prepare the response
  coap_set_header_content_format(response, APPLICATION_JSON);
  coap_set_header_etag(response, &resLength, 1);
  coap_set_payload(response, buffer, resLength);

  // Probably default and thus unnecessary (not used in any CoAP GET handler example)
  // coap_set_status_code(response, CONTENT_2_05);
 }


static void light_POST_PUT_handler(coap_message_t* request, coap_message_t* response,
                                   uint8_t* buffer, uint16_t preferred_size, int32_t* offset)
{
 //size_t newLightStateLen = 0;
 bool newLightStateValid;
 const char* newLightStateStr = NULL;

 newLightStateValid = false;


 if(coap_get_post_variable(request, "lightState", &newLightStateStr) > 0)
  {
   /*
   if((strcmp(newLightStateStr, "LIGHT_OFF") == 0)             ||
      (strcmp(newLightStateStr, "LIGHT_ON") == 0)              ||
      (strcmp(newLightStateStr, "LIGHT_BLINK_ALERT") == 0)     ||
      (strcmp(newLightStateStr, "LIGHT_BLINK_EMERGENCY") == 0))
    newLightStateValid = true;
   else
    LOG_ERR("Received invalid new light state \'%s\'\n", newLightStateStr);
   */
   if(strcmp(newLightStateStr, "LIGHT_OFF") == 0)
    {
     // Stop the LED blinking timer
     ctimer_stop(&lightLEDBlinkTimer);

     // Turn OFF the LIGHT_LED
     leds_single_off(LIGHT_LED);

     newLightStateValid = true;
    }
   else
    if(strcmp(newLightStateStr, "LIGHT_ON") == 0)
     {
      // Stop the LED blinking timer
      ctimer_stop(&lightLEDBlinkTimer);

      // Turn ON the LIGHT_LED
      leds_single_on(LIGHT_LED);

      newLightStateValid = true;
     }
    else
     if(strcmp(newLightStateStr, "LIGHT_BLINK_ALERT") == 0)
      {
       // Start the blinking timer
       ctimer_set(&lightLEDBlinkTimer, LIGHT_LED_ALERT_BLINK_PERIOD, lightLEDBlink, NULL);
       newLightStateValid = true;
      }
     else
      if(strcmp(newLightStateStr, "LIGHT_BLINK_EMERGENCY") == 0)
       {
        // Start the blinking timer
        ctimer_set(&lightLEDBlinkTimer, LIGHT_LED_EMERGENCY_BLINK_PERIOD, lightLEDBlink, NULL);

        newLightStateValid = true;
       }
      else
       {
        // Log unknown light state
        LOG_ERR("Received INVALID new light state \'%s\'\n", newLightStateStr);
       }
   }
 else
  LOG_ERR("MISSING \'lightState\' variable in light PUT\n");

 if(newLightStateValid)
  {
   // Update the lightStrateStr directly from the CoAP message buffer
   strcpy(lightStateStr, newLightStateStr);

   // Notify observing clients
   // TODO: Only if the status has changed
   ctimer_set(&lightPUTPOSTObsNotifyTimer, 0, lightNotifyObservers, NULL);

   LOG_INFO("Received valid new light state \'%s\'\n", lightStateStr);

   coap_set_status_code(response, CHANGED_2_04);
  }
 else
  {
   coap_set_status_code(response, BAD_REQUEST_4_00);

   // TODO: Send the error to the Control Module and possibly in the response's payload

  }
}


// Notify all Observers
static void lightNotifyObservers()
 {
  // Notify all observers
  coap_notify_observers(&actuatorLight);

  // Update the last fan observers notification time
  lightLastObsNotifyTime = clock_seconds();
 }