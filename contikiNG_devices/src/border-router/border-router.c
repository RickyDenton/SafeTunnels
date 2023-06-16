/* SafeTunnels Border Router Definitions */

/* ================================== INCLUDES ================================== */

/* ----------------------------- Contiki-NG Headers ----------------------------- */
#include "contiki.h"
#include "sys/log.h"
#include "dev/leds.h"


/* ============================= DEFINES AND MACROS ============================= */

// Whether the application is deployed on physical
// sensors or on Cooja motes (comment in the latter case)
#define DEPLOY_PHY

// Log level
#define LOG_MODULE "RPL BR"
#define LOG_LEVEL LOG_LEVEL_INFO

/*
 * Power LED
 * =========
 *  - OFF -> Device OFF
 *  - ON  -> Device ON
 */
#ifdef DEPLOY_PHY
 #define POWER_LED LEDS_YELLOW  // This is the second green LED in nRF52840 dongles
#else
 #define POWER_LED LEDS_GREEN   // This is green in Cooja
#endif

/* ================ CONTIKI-NG PROCESS DEFINITION AND AUTOSTART ================ */
PROCESS(contiki_ng_br, "Contiki-NG Border Router");
AUTOSTART_PROCESSES(&contiki_ng_br);

PROCESS_THREAD(contiki_ng_br, ev, data)
{
  PROCESS_BEGIN()

  // Turn ON the POWER_LED at power on
  leds_single_on(POWER_LED);

#if BORDER_ROUTER_CONF_WEBSERVER
  PROCESS_NAME(webserver_nogui_process);
  process_start(&webserver_nogui_process, NULL);
#endif /* BORDER_ROUTER_CONF_WEBSERVER */

  LOG_INFO("Contiki-NG Border Router started\n");

  PROCESS_END()
}
