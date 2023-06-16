/* SafeTunnels Actuator Application Definitions */

/* ================================== INCLUDES ================================== */

/* ------------------------------ Standard Headers ------------------------------ */
#include <strings.h>

/* ----------------------------- Contiki-NG Headers ----------------------------- */
#include "contiki.h"
#include "coap-engine.h"
#include "sys/etimer.h"
#include "dev/button-hal.h"
#include "dev/leds.h"
#include "os/sys/log.h"

/* ------------------------ SafeTunnels Service Headers ------------------------ */
#include "actuator.h"
#include "../common/devUtilities.h"


/* ================ CONTIKI-NG PROCESS DEFINITION AND AUTOSTART ================ */
PROCESS(safetunnels_actuator_process, "SafeTunnels Actuator Process");
AUTOSTART_PROCESSES(&safetunnels_actuator_process);

/* ============================ FORWARD DECLARATIONS ============================ */

// Actuator CoAP Resources
extern coap_resource_t actuatorLight;   // Light Resource
extern coap_resource_t actuatorFan;     // Fan Resource
extern coap_resource_t actuatorErrors;  // Actuator Errors Resource

// Light and Fan resources utility functions simulating an external actor's
// intervention changing their states, which are then notified to their observers
void simulateNewLightState();
void simulateNewFanRelSpeed();


/* ============================== GLOBAL VARIABLES ============================== */

// Connectivity check timer used to log when
// the actuator obtains external connectivity
static struct etimer connCheckTimer;

// Timer used to briefly blink the LIGHT_LED/Power LED at power on
static struct ctimer powerONLightLEDTimer;

// Timer used to briefly blink the FAN_LED/Conn
// LED when the actuator has external connectivity
static struct ctimer connFanLEDTimer;


/* =========================== FUNCTIONS DEFINITIONS =========================== */

/**
 * @brief Blinks the actuator's LIGHT_LED (green) at power
 *        on (powerONLightLEDTimer callback function)
 */
void blinkPowerOnLightLED(__attribute__((unused)) void* ptr)
 {
  // The remaining times the LIGHT_LED/Power LED must be toggled
  static unsigned char powerOnLightLEDToggleTimes = LEDS_NOTIFY_TOGGLE_TIMES;

  // Toggle the LIGHT_LED/Power LED
  leds_single_toggle(LIGHT_LED);

  // If the LIGHT_LED/Power LED must be further toggled, reset the powerONLightLEDTimer
  if(powerOnLightLEDToggleTimes-- > 0)
   ctimer_reset(&powerONLightLEDTimer);

  // Otherwise, if the LIGHT_LED/Power LED has finished blinking,
  // turn it off and restore the toggle times to their original value
  else
   {
    leds_single_off(LIGHT_LED);
    powerOnLightLEDToggleTimes = LEDS_NOTIFY_TOGGLE_TIMES;
   }
 }


/**
 * @brief Blinks the actuator's FAN_LED (yellow/blue) when the actuator
 *        has external connectivity (connFanLEDTimer callback function)
 */
void blinkConnFanLED(__attribute__((unused)) void* ptr)
 {
  // The remaining times the FAN_LED/Conn LED must be toggled
  static unsigned char connFanLEDToggleTimes = LEDS_NOTIFY_TOGGLE_TIMES;

  // Toggle the FAN_LED/Conn LED
  leds_single_toggle(FAN_LED);

  // If the FAN_LED/Conn LED must be further toggled, reset the connFanLEDTimer
  if(connFanLEDToggleTimes-- > 0)
   ctimer_reset(&connFanLEDTimer);

  // Otherwise, if the FAN_LED/Conn LED has finished blinking,
  // turn it off and restore the toggle times to their original value
  else
   {
    leds_single_off(FAN_LED);
    connFanLEDToggleTimes = LEDS_NOTIFY_TOGGLE_TIMES;
   }
 }


/**
 * @brief Checks and logs whether the actuator
 *        has external connectivity at startup
 */
void checkLogActuatorOnline()
 {
  // Counter used to suppress most of the LOG_DBG associated
  // with the actuator waiting for external network connectivity
  static unsigned char suppressNoConnLogs = 0;

  // If the actuator has not external connectivity yet
  if(!isNodeOnline())
   {
    // Log that the actuator is waiting for external
    // connectivity every 'suppressNoConnLogs' main loop cycles
    if(++suppressNoConnLogs % ACTUATOR_MAIN_LOOP_NO_CONN_LOG_PERIOD == 0)
     LOG_DBG("Waiting for external connectivity...\n");

    // Reinitialize the connectivity check timer
    etimer_reset(&connCheckTimer);
   }

  // Otherwise, if the actuator is now online,
  else
   {
    // Briefly blink the FAN_LED/Conn LED
    ctimer_set(&connFanLEDTimer, LEDS_NOTIFY_BLINK_TOGGLE_PERIOD, blinkConnFanLED, NULL);

    // Log that the actuator is now online
    LOG_INFO("The actuator is now online and ready to serve CoAP requests\n");

    /* The Connectivity check timer is NOT reinitialized here */
   }
 }


/**
 * @brief SafeTunnels actuator process main body
 * @param ev   The event passed by the Contiki-NG kernel to the process
 * @param data Additional information on the passed event
 */
PROCESS_THREAD(safetunnels_actuator_process, ev, data)
 {
  // Contiki-NG process start macro
  PROCESS_BEGIN()

  // Retrieve the node's MAC address
  getNodeMACAddr();

  // Briefly blink the LIGHT_LED/Power LED at power on
  ctimer_set(&powerONLightLEDTimer, LEDS_NOTIFY_BLINK_TOGGLE_PERIOD, blinkPowerOnLightLED, NULL);

  // Log that the actuator node has started and its MAC
  LOG_INFO("SafeTunnels actuator node started, MAC = %s\n", nodeMACAddr);

  // Activate the actuator's CoAP server resources
  coap_activate_resource(&actuatorLight, "light");
  coap_activate_resource(&actuatorFan, "fan");
  coap_activate_resource(&actuatorErrors, "actuatorErrors");

  // Log that the actuator's CoAP server resources have been activated
  // and that the node is now waiting for external connectivity
  LOG_DBG("Actuator CoAP server activated, waiting for external connectivity...\n");

  // Initialize the connectivity check timer
  etimer_set(&connCheckTimer, (clock_time_t)CONN_CHECK_TIMER_PERIOD);

  /* --------------------------- Actuator Process Main Loop --------------------------- */
  while(1)
   {
    // Yield the process's execution
    PROCESS_YIELD();

    // If the connectivity check timer has expired,
    // verify and log whether the actuator is now online
    if(ev == PROCESS_EVENT_TIMER && data == &connCheckTimer)
     checkLogActuatorOnline();

    // Otherwise, if any of the actuator's buttons has been pressed,
    // simulate an external actor's intervention changing the
    // fan and light resource states and notify their observers
    else
     if(ev == button_hal_press_event)
      {
       simulateNewLightState();
       simulateNewFanRelSpeed();
      }
   }

  /* ------- Outside the actuator main loop (execution should NEVER reach here) ------- */

  // Turn off ALL LEDs
  leds_off(LEDS_ALL);

  // Log that the actuator process has exited from the main loop
  LOG_ERR("Exited from the actuator process main loop\n");

  // Shut down the actuator process
  PROCESS_END()
 }