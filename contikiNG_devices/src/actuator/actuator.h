#ifndef SAFETUNNELS_ACTUATOR_H
#define SAFETUNNELS_ACTUATOR_H

/* SafeTunnels Actuator Application Declarations */

/* ========================== APPLICATION PARAMETERS ========================== */

/* ---------------------- General Application Parameters ---------------------- */

// Whether the application is deployed on physical
// sensors or on Cooja motes (comment in the latter case)
#define DEPLOY_PHY

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
#ifdef DEPLOY_PHY
 #define LIGHT_LED LEDS_YELLOW  // This is the second green LED in nRF52840 dongles
#else
 #define LIGHT_LED LEDS_GREEN   // This is green in Cooja motes
#endif

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
#ifdef DEPLOY_PHY
 #define FAN_LED LEDS_LED4    // This is the first blue LED in nRF52840 dongles
#else
 #define FAN_LED LEDS_YELLOW  // This is yellow in Cooja motes
#endif

// --------------------- FAN_LED Blinking Period Parameters ---------------------

// The maximum FAN_LED blinking period associated with fanRelSpeed = 0
// (even if the LED is kept OFF for such relative speed)
#define FAN_LED_BLINK_PERIOD_MAX (6 * CLOCK_SECOND)

// The minimum FAN_LED blinking period associated with a fanRelSpeed = 100
#define FAN_LED_BLINK_PERIOD_MIN (0.1 * CLOCK_SECOND)

// How much the FAN_LED blinking period is decreased from its
// maximum blinking period for each fan relative speed unit
#define FAN_LED_BLINK_PERIOD_UNIT ((FAN_LED_BLINK_PERIOD_MAX - FAN_LED_BLINK_PERIOD_MIN) / 100)

// --------------------- Notification LEDs Blink Parameters ---------------------

// The period and how many times the Light_LED/Power LED is blinked at power ON
// and the FAN_LED/Conn LED is blinked when the actuator has external connectivity
#define LEDS_NOTIFY_TOGGLE_TIMES 3
#define LEDS_NOTIFY_BLINK_TOGGLE_PERIOD (0.1 * CLOCK_SECOND)


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
  LIGHT_OFF = 0,

  // The light is ON (WARNING)
  LIGHT_ON = 1,

  // The light is blinking slowly (ALERT)
  LIGHT_BLINK_ALERT = 2,

  // The light is blinking fast (EMERGENCY)
  LIGHT_BLINK_EMERGENCY = 3,

  // Invalid light state used for validating a received new light state
  LIGHT_STATE_INVALID = 4
 };


#endif //SAFETUNNELS_ACTUATOR_H