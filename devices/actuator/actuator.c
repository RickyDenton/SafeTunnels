/* SafeTunnels Actuator Application Definitions */

/* ================================== INCLUDES ================================== */

/* ------------------------------ Standard Headers ------------------------------ */
#include <stdio.h>
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


/* ============================== GLOBAL VARIABLES ============================== */

// Actuator CoAP Resources
extern coap_resource_t actuatorLight;
extern coap_resource_t actuatorFan;
extern coap_resource_t actuatorErrors;

// Simulation (button)
void simulateNewLightState();
void simulateNewFanRelSpeed();



// Whether the actuator node has external connectivity
static bool externalConn = false;

// Actuator connectivity check timer
static struct etimer actuatorConnCheckTimer;


/* =========================== FUNCTIONS DEFINITIONS =========================== */



/**
 * @brief Checks whether the actuator's external connectivity
 *        state has changed, updating and logging it in the case
 */
void checkExternalConnectivityChange()
 {
  // Log a message if the actuator's external connectivity state has changed
  if(isNodeOnline())
   {
    if(!externalConn)
     {
      externalConn = true;
      LOG_INFO("The actuator is now online and ready to serve CoAP requests\n");
     }
   }
  else
   {
    /*
     * NOTE: It appears that, once connection has established, isNodeOnline()
     *       always returns true even if the node is no longer connected with
     *       the RPL DODAG, making this check useless
     */
    if(externalConn)
     {
      externalConn = false;
      LOG_INFO("External connectivity lost, attempting to re-establish...\n");
     }
   }

  // Reset the connectivity check timer
  etimer_reset(&actuatorConnCheckTimer);
 }






PROCESS_THREAD(safetunnels_actuator_process, ev, data)
 {
  // Contiki-NG process start macro
  PROCESS_BEGIN()

  // Set the node's ID as its MAC address
  initNodeID();

  // Log that the actuator node has started and its MAC
  LOG_INFO("SafeTunnels actuator node started, MAC = %s\n", nodeID);

  // Activate the actuator's CoAP server resources
  coap_activate_resource(&actuatorLight, "light");
  coap_activate_resource(&actuatorFan, "fan");
  coap_activate_resource(&actuatorErrors, "actuatorErrors");

  // Log that the actuator's CoAP server resources have been activated
  // and that the node is now waiting for external connectivity
  LOG_DBG("Actuator CoAP server activated, waiting for external connectivity...\n");

  // Initialize the actuator connectivity check timer
  etimer_set(&actuatorConnCheckTimer, (clock_time_t)ACTUATOR_CONN_CHECK_TIMER_PERIOD);

  /* --------------------------- Actuator Process Main Loop --------------------------- */
  while(1)
   {
    // Yield the process's execution
    PROCESS_YIELD();

    // If the connectivity check timer has expired, checks whether the actuator's
    // external connectivity state has changed, updating and logging it in the case
    if(ev == PROCESS_EVENT_TIMER && data == &actuatorConnCheckTimer)
     checkExternalConnectivityChange();

    // Otherwise, if any of the actuator's buttons has been pressed,
    // change the light and fan resources state simulating an external
    // actor's intervention and notify all observers of the new values
    else
     if(ev == button_hal_press_event)
      {
       simulateNewLightState();
       simulateNewFanRelSpeed();
      }
   }

  /* ------- Outside the actuator main loop (execution should NEVER reach here) ------- */

  // Turn off both LEDs
  // TODO: Fatal blink both LEDs instead
  leds_off(LEDS_NUM_TO_MASK(LIGHT_LED) | LEDS_NUM_TO_MASK(FAN_LED));

  // Shut down the sensor process
  PROCESS_END()
 }