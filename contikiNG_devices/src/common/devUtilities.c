/* SafeTunnels service contikiNG_devices utilities */

/* ================================== INCLUDES ================================== */

/* ------------------------------- System Headers ------------------------------- */
#include "contiki.h"
#include "net/routing/routing.h"
#include "net/ipv6/uip-ds6.h"
#include <stdio.h>

/* ------------------------------- Other Headers ------------------------------- */
#include "devUtilities.h"


// The node ID consisting of its stringyfied MAC address
char nodeID[MAC_ADDRESS_STR_SIZE];


/* =========================== FUNCTIONS DEFINITIONS =========================== */

// Initializes the node's ID as its stringyfied MAC address
void initNodeID()
 {
  snprintf(nodeID, sizeof(nodeID), "%02x:%02x:%02x:%02x:%02x:%02x",
           linkaddr_node_addr.u8[0], linkaddr_node_addr.u8[1],
           linkaddr_node_addr.u8[2], linkaddr_node_addr.u8[5],
           linkaddr_node_addr.u8[6], linkaddr_node_addr.u8[7]);
 }



// Whether the node can communicate with hosts external to the
// LLN, which is verified if one of its interfaces has a global
// IPv6 address assigned and has a neighbor to route packets to
bool isNodeOnline()
 {
  if(uip_ds6_get_global(ADDR_PREFERRED) == NULL || uip_ds6_defrt_choose() == NULL)
   return false;
  return true;
 }