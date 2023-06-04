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


/* =========================== FUNCTIONS DEFINITIONS =========================== */

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

  // Otherwise, if the actuator is now online, log the event
  // without reinitializing the connectivity check timer
  else
   LOG_INFO("The actuator is now online and ready to serve CoAP requests\n");
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