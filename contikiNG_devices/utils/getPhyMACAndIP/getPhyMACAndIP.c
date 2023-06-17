/* Utility firmware publishing a node's MAC and IP addresses to the local MQTT broker */

/* ================================== INCLUDES ================================== */

/* ------------------------------ Standard Headers ------------------------------ */
#include <string.h>
#include <uip-debug.h>

/* ----------------------------- Contiki-NG Headers ----------------------------- */
#include "contiki.h"
#include "dev/leds.h"
#include "sys/log.h"
#include "mqtt.h"
#include "net/ipv6/uip.h"
#include "sys/etimer.h"


/* ============================= DEFINES AND MACROS ============================= */
#define LOG_MODULE "getPhyMACAndIP"
#define LOG_LEVEL LOG_LEVEL_ERR

// The size of an 8-byte MAC address string in hexadecimal
// format separated by column (XX:XX:XX:XX:XX:XX:XX:XX)
#define MAC_ADDR_HEX_STR_SIZE 24

#define MQTT_CLIENT_BROKER_IP_ADDR "fd00::1"

#define STATE_INIT    		  0
#define STATE_NET_OK    	  1
#define STATE_CONNECTING      2
#define STATE_CONNECTED       3
#define STATE_SUBSCRIBED      4
#define STATE_DISCONNECTED    5

/* Maximum TCP segment size for outgoing segments of our socket */
#define MAX_TCP_SEGMENT_SIZE    32
#define CONFIG_IP_ADDR_STR_LEN   64

// Default config values
#define DEFAULT_BROKER_PORT         1883
#define DEFAULT_PUBLISH_INTERVAL    (30 * CLOCK_SECOND)

// Periodic timer to check the state of the MQTT client
#define STATE_MACHINE_PERIODIC     (CLOCK_SECOND >> 1)


/* ================ CONTIKI-NG PROCESS DEFINITION AND AUTOSTART ================ */
PROCESS(getPhyMACAndIP, "Contiki-NG Border Router");
AUTOSTART_PROCESSES(&getPhyMACAndIP);


/* ============================== GLOBAL VARIABLES ============================== */

// The node's 8-byte MAC address in hexadecimal
// format with each byte separated by ':'
char nodeMACAddr[MAC_ADDR_HEX_STR_SIZE] = "";

// The node's IPv6 address
char nodeIPv6Addrs[200] = "";

// The MQTT publish buffer
char MQTTOutBuf[300] = "";


char MQTTCliID[20] = "";
static const char* MQTTBrokerIP = MQTT_CLIENT_BROKER_IP_ADDR;
static uint8_t MQTTState;
static struct etimer periodic_timer;
static struct mqtt_connection conn;
char broker_address[CONFIG_IP_ADDR_STR_LEN];




/* =========================== FUNCTIONS DEFINITIONS =========================== */

/**
 * @brief Stores the node's 8-byte MAC address in hexadecimal
 *        format into the 'nodeMACAddr' global variable
 */
void getNodeMACAddr()
 {
  // MAC address printing index
  unsigned int i = 0;

  // Print the first MAC address byte in hexadecimal
  // format into the 'nodeMAC' global variable
  sprintf(nodeMACAddr, "%02x", linkaddr_node_addr.u8[i++]);

  // Print the remaining 7 MAC address bytes in hexadecimal
  // format into the 'nodeMAC' global variable separated by ':'
  for(; i < LINKADDR_SIZE; i++)
   {
    sprintf(nodeMACAddr + strlen(nodeMACAddr), ":");
    sprintf(nodeMACAddr + strlen(nodeMACAddr)
      , "%02x", linkaddr_node_addr.u8[i]);
   }
 }

static void printAllIPv6Addresses()
 {
  int i;
  uint8_t state;
  for(i = 0; i < UIP_DS6_ADDR_NB; i++)
   {
    state = uip_ds6_if.addr_list[i].state;
    if(uip_ds6_if.addr_list[i].isused && (state == ADDR_TENTATIVE || state == ADDR_PREFERRED))
     {
      uiplib_ipaddr_snprint(nodeIPv6Addrs + strlen(nodeIPv6Addrs), sizeof(nodeIPv6Addrs), &uip_ds6_if.addr_list[i].ipaddr);
      sprintf(nodeIPv6Addrs + strlen(nodeIPv6Addrs), ", ");
     }
   }
 }


static bool have_connectivity(void)
 {
  if(uip_ds6_get_global(ADDR_PREFERRED) == NULL)
   return false;
  return true;
 }


static void mqtt_event(__attribute__((unused)) struct mqtt_connection *m, mqtt_event_t event, void *data)
 {
  switch(event)
   {
    case MQTT_EVENT_CONNECTED:
      printf("Application has a MQTT connection\n");
    MQTTState = STATE_CONNECTED;
      break;

    case MQTT_EVENT_DISCONNECTED:
      printf("MQTT Disconnect. Reason %u\n", *((mqtt_event_t *)data));
    MQTTState = STATE_DISCONNECTED;
      process_poll(&getPhyMACAndIP);
      break;

    case MQTT_EVENT_PUBLISH:
     break;

    case MQTT_EVENT_SUBACK:
      printf("Application is subscribed to topic successfully\n");
      break;

    case MQTT_EVENT_UNSUBACK:
      printf("Application is unsubscribed to topic successfully\n");
      break;

    case MQTT_EVENT_PUBACK:
      printf("Publishing complete.\n");
      break;

    default:
     printf("Application got a unhandled MQTT event: %i\n", event);
     break;
   }
 }

PROCESS_THREAD(getPhyMACAndIP, ev, data)
{
  PROCESS_BEGIN()

  // Turn ON the LEDS_GREEN LED at power on
  leds_single_on(LEDS_GREEN);

#if BORDER_ROUTER_CONF_WEBSERVER
  PROCESS_NAME(webserver_nogui_process);
  process_start(&webserver_nogui_process, NULL);
#endif /* BORDER_ROUTER_CONF_WEBSERVER */

  // Initialize the node's MAC address
  getNodeMACAddr();

  // Generate random MQTT client IP
  unsigned short randMQTTCliID = random_rand();
  memcpy(MQTTCliID,&randMQTTCliID,sizeof(randMQTTCliID));
  MQTTCliID[16] = '\0';

  // Broker registration
  mqtt_register(&conn, &getPhyMACAndIP, MQTTCliID, mqtt_event,MAX_TCP_SEGMENT_SIZE);
  MQTTState = STATE_INIT;

  // Initialize periodic timer to check the status
  etimer_set(&periodic_timer, 3 * CLOCK_SECOND);

  /* Main loop */
  while(1)
   {
    PROCESS_YIELD();

    if((ev == PROCESS_EVENT_TIMER && data == &periodic_timer) || ev == PROCESS_EVENT_POLL)
     {
      switch(MQTTState)
       {
        case STATE_INIT:
         if(have_connectivity())
          MQTTState = STATE_NET_OK;
         break;

        case STATE_NET_OK:

         // Prepare the message to be periodically published
         printAllIPv6Addresses();
         sprintf(MQTTOutBuf,"MAC = \"%s\"",nodeMACAddr);
         snprintf(MQTTOutBuf + strlen(MQTTOutBuf), sizeof(MQTTOutBuf), ", IP(s) = \"%s\"", nodeIPv6Addrs);

         // Attempt to connect with the broker
         strcpy(broker_address, MQTTBrokerIP);
         mqtt_connect(&conn, broker_address, DEFAULT_BROKER_PORT, (DEFAULT_PUBLISH_INTERVAL * 3) / CLOCK_SECOND, MQTT_CLEAN_SESSION_ON);
         MQTTState = STATE_CONNECTING;
         break;

        case STATE_CONNECTED:
         MQTTState = STATE_SUBSCRIBED;
        case STATE_SUBSCRIBED:

         mqtt_publish(&conn, NULL, "ADDRS", (uint8_t*)MQTTOutBuf, strlen(MQTTOutBuf), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);
         break;

        default:
         LOG_ERR("ERROR, in default!\n");
       }
     }
      etimer_set(&periodic_timer, STATE_MACHINE_PERIODIC);

   }
  PROCESS_END()
}
