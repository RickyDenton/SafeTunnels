#include "contiki.h"
#include "coap-engine.h"
#include "../actuator.h"
#include <string.h>
#include <stdio.h>
#include <net/ipv6/uiplib.h>
#include "os/sys/log.h"
#include "actuatorErrors.h"


// Forward Declarations
static void actuatorErrors_GET_handler(__attribute__((unused)) coap_message_t* request, coap_message_t* response,
                                       uint8_t* buffer, __attribute__((unused)) uint16_t preferred_size, __attribute__((unused)) int32_t* offset);
static void actuatorErrorsNotifyObservers();



// The last CoAP request application error code that has occurred
enum coapReqAppErrCode coapReqErrCode = COAP_REQ_OK;

// Stores the human-readable description of the last CoAP
// request application error code that has occurred
char coapReqErrCodeStr[150] = "";

// Stores an additional human-readable description of the last
// CoAP request application error code that has occurred
char coapReqErrDscr[COAP_REQ_ERR_DSCR_SIZE];

// Stores the IP address of the last CoAP
// client whose CoAP request raised an error
char coapReqErrCliIP[70];

// Timer used to notify "actuatorErrors" resource
// observers of errors occurred in a CoAP requests
struct ctimer coapReqErrNotifyObsTimer;



// Actuator Fan Resource Definition
EVENT_RESOURCE(actuatorErrors,
               "title=\"SafeTunnels Actuator Error "
                       "Reporting Resource (GET, obs); "
               "rt=\"errReporting\"; "
               "ct=50; "              // ct = 50 -> application/json
               "obs",                 // Observable resource
               actuatorErrors_GET_handler,
               NULL,
               NULL,
               NULL,
               actuatorErrorsNotifyObservers);


/**
 * @brief Prints the human-readable description of the last CoAP
 *        request application error code that has occurred
 *        ("coapReqErrCode" global variable) into the "errDscr" buffer
 */
void coapReqAppErrCodeToStr()
 {
  switch(coapReqErrCode)
   {
    case COAP_REQ_OK:
     sprintf(coapReqErrCodeStr, "OK");
     break;

    // --------- Light Resource CoAP Requests Application Error Codes ---------

    case ERR_LIGHT_POST_PUT_NO_LIGHTSTATE:
     sprintf(coapReqErrCodeStr, "\"lightState\" variable missing "
                                "from a light POST or PUT request");
     break;

    case ERR_LIGHT_POST_PUT_LIGHTSTATE_INVALID:
     sprintf(coapReqErrCodeStr, "Invalid \"lightState\" value "
                                "passed in a light POST or PUT request");
     break;

    // ---------- Fan Resource CoAP Requests Application Error Codes ----------

    case ERR_FAN_POST_PUT_NO_FANRELSPEED:
     sprintf(coapReqErrCodeStr, "\"fanRelSpeed\" variable missing "
                                "from a fan POST or PUT request");
     break;

    case ERR_FAN_POST_PUT_FANRELSPEED_INVALID:
     sprintf(coapReqErrCodeStr, "Invalid \"fanRelSpeed\" value "
                                "passed in a fan POST or PUT request");
     break;

    // ----------- Unknown last CoAP request application error code -----------

    default:
     sprintf(coapReqErrCodeStr, "Unknown CoAP request application "
                                "error code (%u)", coapReqErrCode);
     break;
   }
 }


void prepareCliErrResp(uint8_t* respBuffer,coap_message_t* response)
 {
  uint8_t respLength;

  // Prepare the contents of the CoAP error response to be returned
  // to the client depending on whether an additional error
  // description is stored in the "coapReqErrDscr" buffer
  strcpy((char*)respBuffer,coapReqErrCodeStr);
  if(coapReqErrDscr[0] != '\0')
   {
    strcat((char*)respBuffer, " ");
    strcat((char*)respBuffer, coapReqErrDscr);
   }

  // Prepare the metadata of the CoAP error response to be returned by the client
  respLength = strlen((char*)respBuffer);
  coap_set_header_content_format(response, TEXT_PLAIN);
  coap_set_header_etag(response, &respLength, 1);
  coap_set_payload(response, respBuffer, respLength);

  // All the current coapReqAppErrCode (but COAP_REQ_OK) are
  // associated with the "4.02 Bad Option" response code
  coap_set_status_code(response, BAD_OPTION_4_02);
 }


void reportCoAPReqError(enum coapReqAppErrCode coapErrCode,const uip_ipaddr_t* cliIPAddr,uint8_t* respBuffer,coap_message_t* response)
 {
  // Ensure that a valid error code was passed
  if(coapErrCode == COAP_REQ_OK)
   {
    LOG_ERR("\'COAP_REQ_OK\' error passed in reportCoAPReqError(), the error will NOT be reported\n");
    coap_set_status_code(response, INTERNAL_SERVER_ERROR_5_00);
    return;
   }

  // Set Err Code
  coapReqErrCode = coapErrCode;

  // Convert to string
  coapReqAppErrCodeToStr();

  // Write Client IP
  uiplib_ipaddr_snprint(coapReqErrCliIP, sizeof(coapReqErrCliIP), cliIPAddr);

  // Log
  if(coapReqErrDscr[0] != '\0')
   LOG_ERR("%s %s (clientIP = %s)\n",coapReqErrCodeStr,coapReqErrDscr,coapReqErrCliIP);
  else
   LOG_ERR("%s (clientIP = %s)\n",coapReqErrCodeStr,coapReqErrCliIP);

  // Prepare Client Response
  prepareCliErrResp(respBuffer,response);

  // Set Timer
  ctimer_set(&coapReqErrNotifyObsTimer, 0, actuatorErrorsNotifyObservers, NULL);
 }





static void actuatorErrors_GET_handler(__attribute__((unused)) coap_message_t* request,
                                       coap_message_t* response, uint8_t* buffer,
                                       __attribute__((unused)) uint16_t preferred_size,
                                       __attribute__((unused)) int32_t* offset)
 {
  uint8_t resLength;

  // If there is no error pending, a client is attempting to observe the actuator errors
  if(coapReqErrCode == COAP_REQ_OK)
   {
    // Write the observing client IP into the "cliIPAddr" buffer for logging purposes
    uiplib_ipaddr_snprint(coapReqErrCliIP, sizeof(coapReqErrCliIP), &request->src_ep->ipaddr);

    // Log that the client is now (probably) observing the actuator errors
    LOG_DBG("Client @%s requested observing actuator errors\n", coapReqErrCliIP);

    // Reset the "cliIPAddr" buffer for safety purposes
    coapReqErrCliIP[0] = '\0';

    // Return the observing client an (empty) confirmation response
    coap_set_status_code(response, VALID_2_03);
    sprintf((char*)buffer, "{ \"errCode\": %u }", coapReqErrCode);
   }

   // Otherwise, if this GET invocation is associated
   // with a pending error to be notified to observers
  else
   {
    // Prepare the contents of the message to be returned to observers depending on
    // whether an additional error description is stored in the "coapReqErrDscr" buffer
    if(coapReqErrDscr[0] != '\0')
     sprintf((char*)buffer, "{"
                            "\"errCode\": %u, "
                            "\"errDscr\": \"%s\", "
                            "\"clientIP\": \"%s\" "
                            "}", coapReqErrCode, coapReqErrDscr, coapReqErrCliIP);
    else
     sprintf((char*)buffer, "{"
                            "\"errCode\": %u, "
                            "\"clientIP\": \"%s\" "
                            "}", coapReqErrCode, coapReqErrCliIP);

    // Prepare the metadata of the message to be returned to observers
    resLength = strlen((char*)buffer);
    coap_set_header_content_format(response, APPLICATION_JSON);
    coap_set_header_etag(response, &resLength, 1);
    coap_set_payload(response, buffer, resLength);

    // Probably default and thus unnecessary (not used in any CoAP GET handler example)
    // coap_set_status_code(response, CONTENT_2_05);
   }
 }


// Notify all Observers
void actuatorErrorsNotifyObservers()
 {
  coap_notify_observers(&actuatorErrors);

  // Reset last CoAP request application error code information
  coapReqErrCode = COAP_REQ_OK;
  coapReqErrCodeStr[0] = '\0';
  coapReqErrDscr[0] = '\0';
  coapReqErrCliIP[0] = '\0';
 }