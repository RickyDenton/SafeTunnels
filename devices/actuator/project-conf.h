#ifndef PROJECT_CONF_H_
#define PROJECT_CONF_H_

/*
 * Increase the default maximum number of open CoAP
 * transactions so as to increase, as a side effect, the
 * maximum number of observers (COAP_MAX_OBSERVERS =
 * COAP_MAX_OPEN_TRANSACTIONS -1, file coap-conf.h, line 82)
 */
#undef COAP_MAX_OPEN_TRANSACTIONS  // default = 4
#define COAP_MAX_OPEN_TRANSACTIONS 6

/*
 * Increase the CoAP messages chunk size so as to allow to
 * return detailed error descriptions to invalid requests
 */
#undef COAP_MAX_CHUNK_SIZE      // default = 64
#define COAP_MAX_CHUNK_SIZE     90

#endif /* PROJECT_CONF_H_ */