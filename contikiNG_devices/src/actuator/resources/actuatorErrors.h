#ifndef SAFETUNNELS_ACTUATORERRORS_H
#define SAFETUNNELS_ACTUATORERRORS_H

/* SafeTunnels Actuator Errors Resource Declarations */

// Size of the buffer storing an additional optional human-readable
// description of a CoAP request application error that has occurred
#define COAP_REQ_ERR_DSCR_BUF_SIZE 100

/* ============================== TYPE DEFINITIONS ============================== */

/* ------------------- CoAP Requests Application Error Codes ------------------- */
enum coapReqAppErrCode
 {
  // No error
  COAP_REQ_OK = 0,

  // --------- Light Resource CoAP Requests Application Error Codes ---------

  // The "lightState" variable is missing from a light PUT request
  ERR_LIGHT_PUT_NO_LIGHTSTATE = 1,

  // An invalid "lightState" value was received in a light PUT request
  ERR_LIGHT_PUT_LIGHTSTATE_INVALID = 2,

  // ---------- Fan Resource CoAP Requests Application Error Codes ----------

  // The "fanRelSpeed" variable is missing from a fan PUT request
  ERR_FAN_PUT_NO_FANRELSPEED = 3,

  // An invalid "fanRelSpeed" value was received in a fan PUT request
  ERR_FAN_PUT_FANRELSPEED_INVALID = 4
 };


/* ================== ACTUATOR CoAP REQUESTS REPORTING MACROS ================== */

/**
 * REPORT_COAP_REQ_ERR_ macros, writing the formatted additional description
 * into the 'errDscr' buffer and invoking the "reportCoAPAppReqError"
 * function passing the information on the error that has occurred
 *   - 1 argument    -> reset "errDscr" and call reportCoAPAppReqError()
 *   - 2 arguments   -> snprintf in "errDscr" with format string only +
 *                      call reportCoAPAppReqError()
 *   - 3-6 arguments -> snprintf in "errDscr" with format string +
 *                      1-4 parameters + call reportCoAPAppReqError()
 */
#define REPORT_COAP_REQ_ERR_CODE_ONLY(coapAppReqErrCode) { pendingCoapReqErrDscr[0] = '\0'; reportCoAPAppReqError(coapAppReqErrCode,&request->src_ep->ipaddr,buffer,response); }
#define REPORT_COAP_REQ_ERR_DSCR(coapAppReqErrCode,dscr) { snprintf(pendingCoapReqErrDscr, COAP_REQ_ERR_DSCR_BUF_SIZE, dscr); reportCoAPAppReqError(coapAppReqErrCode,&request->src_ep->ipaddr,buffer,response); }
#define REPORT_COAP_REQ_ERR_1PARAM(coapAppReqErrCode,dscr,param1) { snprintf(pendingCoapReqErrDscr, COAP_REQ_ERR_DSCR_BUF_SIZE, dscr, param1); reportCoAPAppReqError(coapAppReqErrCode,&request->src_ep->ipaddr,buffer,response); }
#define REPORT_COAP_REQ_ERR_2PARAM(coapAppReqErrCode,dscr,param1,param2) { snprintf(pendingCoapReqErrDscr, COAP_REQ_ERR_DSCR_BUF_SIZE, dscr, param1, param2); reportCoAPAppReqError(coapAppReqErrCode,&request->src_ep->ipaddr,buffer,response); }
#define REPORT_COAP_REQ_ERR_3PARAM(coapAppReqErrCode,dscr,param1,param2,param3) { snprintf(pendingCoapReqErrDscr,COAP_REQ_ERR_DSCR_BUF_SIZE, dscr, param1, param2, param3); reportCoAPAppReqError(coapAppReqErrCode,&request->src_ep->ipaddr,buffer,response); }
#define REPORT_COAP_REQ_ERR_4PARAM(coapAppReqErrCode,dscr,param1,param2,param3,param4) { snprintf(pendingCoapReqErrDscr,COAP_REQ_ERR_DSCR_BUF_SIZE, dscr, param1, param2, param3, param4); reportCoAPAppReqError(coapAppReqErrCode,&request->src_ep->ipaddr,buffer,response); }

/**
 * Substitutes the appropriate REPORT_COAP_REQ_ERR_ depending on the
 * number of arguments passed to the REPORT_COAP_REQ_ERR variadic macro:
 *   - 1 argument    -> coapAppReqErrCode only
 *   - 2 arguments   -> coapAppReqErrCode +
 *                      additional description (snprintf format string)
 *   - 3-6 arguments -> coapAppReqErrCode +
 *                      additional description (snprintf format string + 1-4 snprintf params)
 */
#define GET_REPORT_COAP_REQ_ERR(_1,_2,_3,_4,_5,_6,REPORT_COAP_REQ_ERR_MACRO,...) REPORT_COAP_REQ_ERR_MACRO
#define REPORT_COAP_REQ_ERR(...) GET_REPORT_COAP_REQ_ERR(__VA_ARGS__,REPORT_COAP_REQ_ERR_4PARAM,REPORT_COAP_REQ_ERR_3PARAM,REPORT_COAP_REQ_ERR_2PARAM,REPORT_COAP_REQ_ERR_1PARAM,REPORT_COAP_REQ_ERR_DSCR,REPORT_COAP_REQ_ERR_CODE_ONLY)(__VA_ARGS__)


/* ===================== VARIABLES AND FUNCTIONS DECLARATIONS ===================== */

// The buffer to be used for storing an additional optional human-readable
// description of the CoAP request application error that has occurred
extern char pendingCoapReqErrDscr[];

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
void reportCoAPAppReqError(enum coapReqAppErrCode coapErrCode, const uip_ipaddr_t* cliIPAddr,
                           uint8_t* respBuffer, coap_message_t* response);


#endif //SAFETUNNELS_ACTUATORERRORS_H