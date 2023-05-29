/* SafeTunnels service devices utilities */

/* ================================== INCLUDES ================================== */

/* ------------------------------- System Headers ------------------------------- */
#include "contiki.h"
#include "net/routing/routing.h"
#include "net/ipv6/uip-ds6.h"
#include <stdio.h>

/* ------------------------------- Other Headers ------------------------------- */
#include "devUtilities.h"


/* =========================== FUNCTIONS DEFINITIONS =========================== */

// Print node MAC to buffer
void writeNodeMAC(char* dest)
 {
  snprintf(dest, MAC_ADDRESS_SIZE, "%02x%02x%02x%02x%02x%02x",
           linkaddr_node_addr.u8[0], linkaddr_node_addr.u8[1],
           linkaddr_node_addr.u8[2], linkaddr_node_addr.u8[5],
           linkaddr_node_addr.u8[6], linkaddr_node_addr.u8[7]);
 }


// Whether the node is connected to a RPL DODAG
bool isNodeConnected()
 {
  if(uip_ds6_get_global(ADDR_PREFERRED) == NULL || uip_ds6_defrt_choose() == NULL)
   return false;
  return true;
 }