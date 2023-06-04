#ifndef SAFETUNNELS_ACTUATORERRORS_H
#define SAFETUNNELS_ACTUATORERRORS_H

#define COAP_REQ_ERR_DSCR_SIZE 100

extern struct ctimer coapReqErrNotifyObsTimer;
extern enum coapReqAppErrCode coapReqErrCode;
extern char coapReqErrCodeStr[];
extern char coapReqErrDscr[];

void reportCoAPReqError(enum coapReqAppErrCode coapErrCode,const uip_ipaddr_t* cliIPAddr,uint8_t* respBuffer,coap_message_t* response);


/* ------------------- CoAP Requests Application Error Codes ------------------- */
enum coapReqAppErrCode
 {
  // No Error
  COAP_REQ_OK,

  // --------- Light Resource CoAP Requests Application Error Codes ---------

  // The "lightState" variable is missing from a light POST or PUT request
  ERR_LIGHT_POST_PUT_NO_LIGHTSTATE,

  // An invalid "lightState" value was passed in a light POST or PUT request
  ERR_LIGHT_POST_PUT_LIGHTSTATE_INVALID,

  // ---------- Fan Resource CoAP Requests Application Error Codes ----------

  // The "fanRelSpeed" variable is missing from a fan POST or PUT request
  ERR_FAN_POST_PUT_NO_FANRELSPEED,

  // An invalid "fanRelSpeed" value was passed in a fan POST or PUT request
  ERR_FAN_POST_PUT_FANRELSPEED_INVALID
 };


/* ================== ACTUATOR CoAP REQUESTS REPORTING MACROS ================== */

/**
 * LOG_PUB_ERROR_ macros, writing the formatted additional description into the 'errDscr' buffer and invoking the "logPublish" error function with the passes sensorErrCode
 *  - 1 argument    -> reset "errDscr" and call logPublishError(sensorErrCode)
 *  - 2 arguments   -> snprintf in "errDscr" with format string only + call logPublishError(sensorErrCode)
 *  - 3-6 arguments -> snprintf in "errDscr" with format string + 1- parameters + call logPublishError(sensorErrCode)
 */

#define REPORT_COAP_REQ_ERR_CODE_ONLY(coapAppReqErrCode) { coapReqErrDscr[0] = '\0'; reportCoAPReqError(coapAppReqErrCode,&request->src_ep->ipaddr,buffer,response); }
#define REPORT_COAP_REQ_ERR_DSCR(coapAppReqErrCode,dscr) { snprintf(coapReqErrDscr, COAP_REQ_ERR_DSCR_SIZE, dscr); reportCoAPReqError(coapAppReqErrCode,&request->src_ep->ipaddr,buffer,response); }
#define REPORT_COAP_REQ_ERR_1PARAM(coapAppReqErrCode,dscr,param1) { snprintf(coapReqErrDscr, COAP_REQ_ERR_DSCR_SIZE, dscr, param1); reportCoAPReqError(coapAppReqErrCode,&request->src_ep->ipaddr,buffer,response); }
#define REPORT_COAP_REQ_ERR_2PARAM(coapAppReqErrCode,dscr,param1,param2) { snprintf(coapReqErrDscr, COAP_REQ_ERR_DSCR_SIZE, dscr, param1, param2); reportCoAPReqError(coapAppReqErrCode,&request->src_ep->ipaddr,buffer,response); }
#define REPORT_COAP_REQ_ERR_3PARAM(coapAppReqErrCode,dscr,param1,param2,param3) { snprintf(coapReqErrDscr,COAP_REQ_ERR_DSCR_SIZE, dscr, param1, param2, param3); reportCoAPReqError(coapAppReqErrCode,&request->src_ep->ipaddr,buffer,response); }
#define REPORT_COAP_REQ_ERR_4PARAM(coapAppReqErrCode,dscr,param1,param2,param3,param4) { snprintf(coapReqErrDscr,COAP_REQ_ERR_DSCR_SIZE, dscr, param1, param2, param3, param4); reportCoAPReqError(coapAppReqErrCode,&request->src_ep->ipaddr,buffer,response); }


/**
 * Substitutes the appropriate LOG_PUB_ERROR_ depending on the
 * number of arguments passed to the LOG_PUB_ERROR variadic macro:
 *  - 1 argument    -> sensorErrCode only
 *  - 2 arguments   -> sensorErrCode + additional description (snprintf format string)
 *  - 3-6 arguments -> sensorErrCode + additional description (snprintf format string + 1-4 snprintf params)
 */


#define GET_REPORT_COAP_REQ_ERR(_1,_2,_3,_4,_5,_6,REPORT_COAP_REQ_ERR_MACRO,...) REPORT_COAP_REQ_ERR_MACRO
#define REPORT_COAP_REQ_ERR(...) GET_REPORT_COAP_REQ_ERR(__VA_ARGS__,REPORT_COAP_REQ_ERR_4PARAM,REPORT_COAP_REQ_ERR_3PARAM,REPORT_COAP_REQ_ERR_2PARAM,REPORT_COAP_REQ_ERR_1PARAM,REPORT_COAP_REQ_ERR_DSCR,REPORT_COAP_REQ_ERR_CODE_ONLY)(__VA_ARGS__)

#endif //SAFETUNNELS_ACTUATORERRORS_H