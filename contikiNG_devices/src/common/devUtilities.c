/* SafeTunnels service contikiNG_devices utilities */

/* ================================== INCLUDES ================================== */

/* ------------------------------ Standard Headers ------------------------------ */
#include <stdio.h>

/* ----------------------------- Contiki-NG Headers ----------------------------- */
#include "contiki.h"
#include "net/routing/routing.h"
#include "os/net/linkaddr.h"
#include "net/ipv6/uip-ds6.h"

/* ------------------------ SafeTunnels Service Headers ------------------------ */
#include "devUtilities.h"


/* ============================== GLOBAL VARIABLES ============================== */

// The node's 8-byte MAC address in hexadecimal
// format with each byte separated by ':'
char nodeMACAddr[MAC_ADDR_HEX_STR_SIZE] = "";


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


/**
 * @return Whether a node can communicate with hosts
 *         external to the LLN, which is verified if:
 *           1) One of its NIC has assigned a global IPv6 address
 *           2) Has a parent in the DODAG to forward packets to
 *           3) The node is supposedly reachable downwards in the DODAG
 */
bool isNodeOnline()
 {
  if(uip_ds6_get_global(ADDR_PREFERRED) != NULL &&   // 1)
     uip_ds6_defrt_choose() != NULL &&               // 2)
     NETSTACK_ROUTING.node_is_reachable())           // 3)
   return true;
  return false;
 }