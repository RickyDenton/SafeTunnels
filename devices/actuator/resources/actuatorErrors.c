/* SafeTunnels Actuator Errors Resource Definitions */

/* ================================== INCLUDES ================================== */

/* ------------------------------ Standard Headers ------------------------------ */
#include <string.h>

/* ----------------------------- Contiki-NG Headers ----------------------------- */
#include "contiki.h"
#include "coap-engine.h"
#include "net/ipv6/uiplib.h"
#include "os/sys/log.h"

/* ------------------------ SafeTunnels Service Headers ------------------------ */
#include "actuatorErrors.h"
#include "../actuator.h"


/* ============================ FORWARD DECLARATIONS ============================ */

// The actuator errors CoAP resource GET and EVENT handlers
static void actuatorErrors_GET_handler(__attribute__((unused)) coap_message_t* request,
                                       coap_message_t* response, uint8_t* buffer,
                                       __attribute__((unused)) uint16_t preferred_size,
                                       __attribute__((unused)) int32_t* offset);
static void actuatorErrorsNotifyObservers();


/* ============================== GLOBAL VARIABLES ============================== */

// Actuator Errors Resource Definition
EVENT_RESOURCE(actuatorErrors,                 // Resource Name
               "title=\"SafeTunnels Actuator Error "    // Title
               "Reporting Resource (GET, obs); "
               "rt=\"errReporting\"; "         // Resource Type
               "ct=50; "                       // Content Format (50 -> application/json)
               "obs",                          // Observable resource
               actuatorErrors_GET_handler,     // GET handler
               NULL,                           // POST handler
               NULL,                           // PUT handler
               NULL,                           // DELETE handler
               actuatorErrorsNotifyObservers); // EVENT handler


// Timer used to notify the resource observers of errors occurred
// in clients' CoAP requests immediately after they have been served
struct ctimer coapReqErrNotifyObsTimer;

/* ------------------------- Pending Error Information ------------------------- */

/*
 * These variables store information on a pending error that has
 * occurred in a client CoAP request, which will be notified to
 *  all observers immediately after such request has been served
 */

// The CoAP request application error code that has occurred
static enum coapReqAppErrCode pendingCoapReqErrCode = COAP_REQ_OK;

// The human-readable description of the CoAP
// request application error that has occurred
static char pendingCoapReqErrCodeStr[100] = "";

// An additional optional human-readable description of
// the CoAP request application error that has occurred
char pendingCoapReqErrDscr[COAP_REQ_ERR_DSCR_BUF_SIZE];

// The IP address of the client whose
// CoAP request caused an error
static char pendingCoapReqErrCliIP[70];


/* =========================== FUNCTIONS DEFINITIONS =========================== */

/* ------------------------ Errors Management Functions ------------------------ */

/**
 * @brief Prints the human-readable description of a CoAP request
 *        application error that has occurred as of the 'pendingCoapReqErrCode'
 *        variable into the 'pendingCoapReqErrCodeStr' buffer
 */
void coapReqAppErrCodeToStr()
 {
  switch(pendingCoapReqErrCode)
   {
    // No error (the function should never be invoked in this case)
    case COAP_REQ_OK:
     LOG_WARN("coapReqAppErrCodeToStr() invoked with pendingCoapReqErrCode = COAP_REQ_OK\n");
     sprintf(pendingCoapReqErrCodeStr, "OK");
     break;

    // --------- Light Resource CoAP Requests Application Error Codes ---------

    // The "lightState" variable is missing from a light PUT request
    case ERR_LIGHT_PUT_NO_LIGHTSTATE:
     sprintf(pendingCoapReqErrCodeStr, "\"lightState\" variable missing "
                                       "from a light PUT request");
     break;

    // An invalid "lightState" value was received in a light PUT request
    case ERR_LIGHT_PUT_LIGHTSTATE_INVALID:
     sprintf(pendingCoapReqErrCodeStr, "Invalid \"lightState\" value "
                                       "received in a light PUT request");
     break;

    // ---------- Fan Resource CoAP Requests Application Error Codes ----------

    // The "fanRelSpeed" variable is missing from a fan PUT request
    case ERR_FAN_PUT_NO_FANRELSPEED:
     sprintf(pendingCoapReqErrCodeStr, "\"fanRelSpeed\" variable missing "
                                       "from a fan PUT request");
     break;

    // An invalid "fanRelSpeed" value was received in a fan PUT request
    case ERR_FAN_PUT_FANRELSPEED_INVALID:
     sprintf(pendingCoapReqErrCodeStr, "Invalid \"fanRelSpeed\" value "
                                       "received in a fan PUT request");
     break;
   }
 }


/**
 * @brief Prepares the CoAP response error message to be returned
 *        to the client whose PUT request raised an error
 * @param respBuffer The buffer to be used for storing the response message's
 *                   contents (the PUT handler's "buffer" parameter)
 * @param response   The CoAP response error message to be returned to the
 *                   client(s) (the PUT handler's "response" parameter)
 */
void prepareCliPUTErrResp(uint8_t* respBuffer, coap_message_t* response)
 {
  // CoAP response length
  uint8_t respLength;

  // Initialize the PUT CoAP response body as the human-readable
  // description of the CoAP request application error that has occurred
  strcpy((char*)respBuffer, pendingCoapReqErrCodeStr);

  // If an additional human-readable description of the CoAP
  // request application error that has occurred was provided,
  // append it after the error's base description
  if(pendingCoapReqErrDscr[0] != '\0')
   {
    strcat((char*)respBuffer, " ");
    strcat((char*)respBuffer, pendingCoapReqErrDscr);
   }

  // Prepare the metadata of the CoAP error
  // response to be returned to the client
  respLength = strlen((char*)respBuffer);
  coap_set_header_content_format(response, TEXT_PLAIN);
  coap_set_header_etag(response, &respLength, 1);
  coap_set_payload(response, respBuffer, respLength);

  // Being all handled CoAP request application errors relative to
  // a missing or malformed option/variable in the PUT's request, set
  // the CoAP response status code appropriately so as to inform the
  // client that it should not repeat the request without modifying it
  coap_set_status_code(response, BAD_OPTION_4_02);
 }


/**
 * @brief Error handler invoked by the other resources' PUT methods
 *        should a CoAP application error occur in handling a request
 * @param coapErrCode The CoAP application error code that has occurred
 * @param cliIPAddr   The IP address of the client
 *                    whose request caused the error
 * @param respBuffer  The buffer to be used for storing the response message's
 *                    contents (the PUT handler's "buffer" parameter)
 * @param response    The CoAP response error message to be returned to the
 *                    client(s) (the PUT handler's "response" parameter)
 */
void reportCoAPAppReqError(enum coapReqAppErrCode coapErrCode,
                           const uip_ipaddr_t* cliIPAddr,
                           uint8_t* respBuffer, coap_message_t* response)
 {
  // Ensure that a valid CoAP application error code was passed
  if((coapErrCode != ERR_LIGHT_PUT_NO_LIGHTSTATE)      &&
     (coapErrCode != ERR_LIGHT_PUT_LIGHTSTATE_INVALID) &&
     (coapErrCode != ERR_FAN_PUT_NO_FANRELSPEED)       &&
     (coapErrCode != ERR_FAN_PUT_FANRELSPEED_INVALID))
   {
    LOG_ERR("INVALID CoAP request application error code passed to "
            "reportCoAPAppReqError(), the error will NOT be reported\n");

    // Inform the client that an internal
    // server error has occurred and return
    coap_set_status_code(response, INTERNAL_SERVER_ERROR_5_00);
    return;
   }

  // Set the pending CoAP request application error code and write its
  // human-readable description into the 'pendingCoapReqErrDscr' buffer
  pendingCoapReqErrCode = coapErrCode;
  coapReqAppErrCodeToStr();

  /*
   * NOTE: An additional error description, if any, has
   *       already been stored in the 'pendingCoapReqErrDscr'
   *       buffer by the 'REPORT_COAP_REQ_ERR_' macro
   */

  // Extract the IP address of the client from the CoAP request that raised
  // the error and write it into the 'pendingCoapReqErrCliIP' buffer
  uiplib_ipaddr_snprint(pendingCoapReqErrCliIP, sizeof(pendingCoapReqErrCliIP), cliIPAddr);

  // Log the error locally depending on whether
  // its additional description was provided
  if(pendingCoapReqErrDscr[0] != '\0')
   LOG_ERR("%s %s (clientIP = %s)\n", pendingCoapReqErrCodeStr,
           pendingCoapReqErrDscr, pendingCoapReqErrCliIP);
  else
   LOG_ERR("%s (clientIP = %s)\n", pendingCoapReqErrCodeStr, pendingCoapReqErrCliIP);

  // Prepare the CoAP response error message to be returned to the client
  prepareCliPUTErrResp(respBuffer, response);

  // Initialize the error notification timer so as to notify observers
  // immediately after the client CoAP request has been served
  ctimer_set(&coapReqErrNotifyObsTimer, 0, actuatorErrorsNotifyObservers, NULL);
 }


/* ----------------- CoAP Resource Requests Callback Functions ----------------- */

/**
 * @brief Actuator Errors GET request handler, which:\n
 *          - If there is no pending CoAP application error to be notified
 *            to observers (pendingCoapReqErrCode == COAP_REQ_OK), it is
 *            relative to a client requesting to (supposedly) observe the
 *            resource, which is returned an empty confirmation message\n
 *          - If there is a pending CoAP application error to be notified
 *            to observers (pendingCoapReqErrCode != COAP_REQ_OK) it is
 *            invoked as a callback by the CoAP engine coap_notify_observers()
 *            function to generate the message to be sent to all observers,
 *            which is prepared in JSON format in the "response" variable
 * @param response The CoAP response message to be returned to the client(s)
 * @param buffer   A buffer to be used for storing the response message's contents
 */
static void actuatorErrors_GET_handler(__attribute__((unused)) coap_message_t* request,
                                       coap_message_t* response, uint8_t* buffer,
                                       __attribute__((unused)) uint16_t preferred_size,
                                       __attribute__((unused)) int32_t* offset)
 {
  // CoAP response length
  uint8_t resLength;

  // If there is no pending CoAP application error to be notified to observers,
  // a client is (supposedly) requesting to observe the actuator error
  if(pendingCoapReqErrCode == COAP_REQ_OK)
   {
    // Write for logging purposes the IP of the client that has (supposedly) requested
    // to observe actuator errors into the 'pendingCoapReqErrCliIP' buffer
    uiplib_ipaddr_snprint(pendingCoapReqErrCliIP, sizeof(pendingCoapReqErrCliIP),
                          &request->src_ep->ipaddr);

    // Log the IP of the client that has (supposedly) requested to observe actuator errors
    LOG_DBG("Client @%s (supposedly) requested observing actuator errors\n", pendingCoapReqErrCliIP);

    // Reset the "cliIPAddr" buffer (safety purposes)
    pendingCoapReqErrCliIP[0] = '\0';

    // Return the observing client an (empty) confirmation response
    coap_set_status_code(response, VALID_2_03);
   }

  // Otherwise, if there is a pending CoAP application errors
  // to be notified to observers and so this method was invoked
  // as a callback by the CoAP engine coap_notify_observers()
  // function to generate the message to be sent to all observers
  else
   {
    // Prepare the contents of the CoAP message to be sent to
    // observers depending on whether an additional error
    // description is stored in the "pendingCoapReqErrDscr" buffer
    if(pendingCoapReqErrDscr[0] != '\0')
     sprintf((char*)buffer, "{"
                            "\"errCode\": %u, "
                            "\"errDscr\": \"%s\", "
                            "\"clientIP\": \"%s\" "
                            "}", pendingCoapReqErrCode, pendingCoapReqErrDscr, pendingCoapReqErrCliIP);
    else
     sprintf((char*)buffer, "{"
                            "\"errCode\": %u, "
                            "\"clientIP\": \"%s\" "
                            "}", pendingCoapReqErrCode, pendingCoapReqErrCliIP);

    // Prepare the metadata of the CoAP message to be sent to observers
    resLength = strlen((char*)buffer);
    coap_set_header_content_format(response, APPLICATION_JSON);
    coap_set_header_etag(response, &resLength, 1);
    coap_set_payload(response, buffer, resLength);

    // Setting the CoAP response status code to "2.05" CONTENT is probably
    // performed by default (not present in any CoAP GET handler example)
    //
    // coap_set_status_code(response, CONTENT_2_05);
   }
 }


/**
 * @brief Notifies all actuator errors resource observers of a pending
 *        CoAP request application error and resets its information
 *        state variables (lightPUTObsNotifyTimer callback function)
 */
void actuatorErrorsNotifyObservers()
 {
  // Notify the observers of the pending CoAP request application error
  coap_notify_observers(&actuatorErrors);

  // Reset the error information state variables
  pendingCoapReqErrCode = COAP_REQ_OK;
  pendingCoapReqErrCodeStr[0] = '\0';
  pendingCoapReqErrDscr[0] = '\0';
  pendingCoapReqErrCliIP[0] = '\0';
 }