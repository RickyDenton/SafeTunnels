/* SafeTunnels Actuator Application Definitions */

/* ================================== INCLUDES ================================== */

/* ------------------------------ Standard Headers ------------------------------ */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <limits.h>

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


// CoAP Resources
extern coap_resource_t actuatorLight;
extern coap_resource_t actuatorFan;

char CoAPResBuf[200];


// Timer
//static struct etimer e_timer;


/* =========================== FUNCTIONS DEFINITIONS =========================== */

PROCESS_THREAD(safetunnels_actuator_process, ev, data)
 {
  // Contiki-NG process start macro
  PROCESS_BEGIN()

  // Set the node's ID as its MAC address
  initNodeID();

  // Log that the actuator node has started and its MAC
  LOG_INFO("SafeTunnels actuator node started, MAC = %s\n",nodeID);

  // Activate the actuator's CoAP resources
  coap_activate_resource(&actuatorLight, "light");
  coap_activate_resource(&actuatorFan, "fan");

  LOG_DBG("waiting indefinitely...\n");

  while(1)
   { PROCESS_WAIT_EVENT(); }


  /*
   * From the observing resource example

  etimer_set(&e_timer, CLOCK_SECOND * 4);

  printf("Loop\n");

  while(1) {
       PROCESS_WAIT_EVENT();

       if(ev == PROCESS_EVENT_TIMER && data == &e_timer){
         printf("Event triggered\n");

         coapObsRes.trigger();

         etimer_set(&e_timer, CLOCK_SECOND * 4);
        }
      }
  */

  PROCESS_END();
 }