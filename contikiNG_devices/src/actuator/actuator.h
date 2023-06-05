#ifndef SAFETUNNELS_ACTUATOR_H
#define SAFETUNNELS_ACTUATOR_H

/* SafeTunnels Actuator Application Declarations */

/* ========================== APPLICATION PARAMETERS ========================== */

/* ---------------------- General Application Parameters ---------------------- */

// Contiki-NG logging parameters
#define LOG_MODULE "actuator"
#define LOG_LEVEL  LOG_LEVEL_DBG

// Connectivity Check timer period
#define CONN_CHECK_TIMER_PERIOD (2 * CLOCK_SECOND)

// How many main loop cycles the actuator logs
// that it is waiting for external connectivity
#define ACTUATOR_MAIN_LOOP_NO_CONN_LOG_PERIOD 15

/* ----------------------------- LEDs Management ----------------------------- */

/*
 * Light LED
 * =========
 *  - OFF -> NOMINAL
 *  - ON  -> WARNING
 *  - BLINKING
 *    - SLOW -> ALERT
 *    - FAST -> EMERGENCY
 */
#define LIGHT_LED LEDS_GREEN

// LIGHT_LED ALERT and EMERGENCY blinking periods
#define LIGHT_LED_ALERT_BLINK_PERIOD     (1 * CLOCK_SECOND)
#define LIGHT_LED_EMERGENCY_BLINK_PERIOD (0.3 * CLOCK_SECOND)

/*
 * Fan LED
 * =======
 *  - OFF      -> Fan OFF
 *  - BLINKING -> Fan ON with relative speed inversely
 *                proportional to the blinking period
 */
#define FAN_LED LEDS_YELLOW

// --------------------- FAN_LED Blinking Period Parameters ---------------------

// The maximum FAN_LED blinking period associated with fanRelSpeed = 0
// (even if the LED is kept OFF for such relative speed)
#define FAN_LED_BLINK_PERIOD_MAX (6 * CLOCK_SECOND)

// The minimum FAN_LED blinking period associated with a fanRelSpeed = 100
#define FAN_LED_BLINK_PERIOD_MIN (0.2 * CLOCK_SECOND)

// How much the FAN_LED blinking period is decreased from its
// maximum blinking period for each fan relative speed unit
#define FAN_LED_BLINK_PERIOD_UNIT ((FAN_LED_BLINK_PERIOD_MAX - FAN_LED_BLINK_PERIOD_MIN) / 100)

/* -------------------------- CoAP Client Parameters -------------------------- */

/*
 * UNUSED (CoAP client functionalities have been discarded from the actuator)

// Control Module CoAP server endpoint
#define CONTROL_MODULE_IPV6_ADDR        "fd00::1"
#define CONTROL_MODULE_COAP_SERVER_PORT "5683"
#define CONTROL_MODULE_COAP_SERVER_ENDPOINT \
        "coap://[" CONTROL_MODULE_IPV6_ADDR "]:" CONTROL_MODULE_COAP_SERVER_PORT
*/


/* ============================== TYPE DEFINITIONS ============================== */

/* --------------------------- Light Resource States --------------------------- */
enum lightResState
 {
  // The light is OFF (NOMINAL)
  LIGHT_OFF,

  // The light is ON (WARNING)
  LIGHT_ON,

  // The light is blinking slowly (ALERT)
  LIGHT_BLINK_ALERT,

  // The light is blinking fast (EMERGENCY)
  LIGHT_BLINK_EMERGENCY,

  // Invalid light state used for validating a received new light state
  LIGHT_STATE_INVALID
 };


#endif //SAFETUNNELS_ACTUATOR_H