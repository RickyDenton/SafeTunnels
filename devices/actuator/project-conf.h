#ifndef PROJECT_CONF_H_
#define PROJECT_CONF_H_

// #define LOG_LEVEL_APP LOG_LEVEL_DBG


/*
 * Increase the default maximum number of open
 * transactions for the purpose of increasing the maximum
 * number of allowed observers (COAP_MAX_OBSERVERS =
 * COAP_MAX_OPEN_TRANSACTIONS -1, file coap-conf.h, line 82)
 */
#undef COAP_MAX_OPEN_TRANSACTIONS       // default = 4
#define COAP_MAX_OPEN_TRANSACTIONS 6

// TODO: Attempt to avoid error messages being truncated
#undef COAP_MAX_CHUNK_SIZE
#define COAP_MAX_CHUNK_SIZE     90

#endif /* PROJECT_CONF_H_ */
