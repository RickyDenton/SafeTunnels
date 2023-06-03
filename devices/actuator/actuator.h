#ifndef SAFETUNNELS_ACTUATOR_H
#define SAFETUNNELS_ACTUATOR_H

/* SafeTunnels Actuator Application Declarations */

/* ========================== APPLICATION PARAMETERS ========================== */

/* ---------------------- General Application Parameters ---------------------- */

// Contiki-NG logging parameters
#define LOG_MODULE "actuator"
#define LOG_LEVEL  LOG_LEVEL_DBG

// ---------------------- LEDs Definitions and Management ----------------------

/*
 * Light LED
 *  - OFF -> No warning
 *  - ON  -> Warning
 *  - BLINKING
 *    - SLOW -> Alert
 *    - FAST -> Emergency
 */
#define LIGHT_LED LEDS_GREEN

/*
 * Fan LED
 *  - OFF      -> Fan OFF
 *  - BLINKING -> Fan ON with relative speed inversely
 *                proportional to the blinking period
 */
#define FAN_LED LEDS_YELLOW


#define LIGHT_LED_ALERT_BLINK_PERIOD     (1 * CLOCK_SECOND)
#define LIGHT_LED_EMERGENCY_BLINK_PERIOD (0.3 * CLOCK_SECOND)


// The maximum FAN_LED blinking period (which would be for fanRelSpeed = 0)
#define FAN_LED_BLINK_PERIOD_MAX (6 * CLOCK_SECOND)

// The minimum FAN_LED blinking period associated with fanRelSpeed = 100
#define FAN_LED_BLINK_PERIOD_MIN (0.2 * CLOCK_SECOND)

// How much the FAN_LED blinking period is reduced for every fan speed unit
#define FAN_LED_BLINK_PERIOD_UNIT ((FAN_LED_BLINK_PERIOD_MAX - FAN_LED_BLINK_PERIOD_MIN) / 100)


/* ----------------------------- CoAP Parameters ----------------------------- */

// Control Module CoAP server endpoint
#define CONTROL_MODULE_IPV6_ADDR        "fd00::1"
#define CONTROL_MODULE_COAP_SERVER_PORT "5683"
#define CONTROL_MODULE_COAP_SERVER_ENDPOINT \
 "coap://[" CONTROL_MODULE_IPV6_ADDR "]:" CONTROL_MODULE_COAP_SERVER_PORT


/* ============================== TYPE DEFINITIONS ============================== */

/* --------------------------- Light Resource States --------------------------- */
enum lightResState
 {
  // The light is OFF (no warning)
  LIGHT_OFF,

  // The light is ON (warning)
  LIGHT_ON,

  // The light is blinking slowly (alert)
  LIGHT_BLINK_ALERT,

  // The light is blinking fast (emergency)
  LIGHT_BLINK_EMERGENCY
 };

#endif //SAFETUNNELS_ACTUATOR_H
