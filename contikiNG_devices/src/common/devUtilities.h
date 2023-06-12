#ifndef SAFETUNNELS_DEVUTILITIES_H
#define SAFETUNNELS_DEVUTILITIES_H

/* ========================== APPLICATION PARAMETERS ========================== */

// The size of an 8-byte MAC address string in hexadecimal
// format separated by column (XX:XX:XX:XX:XX:XX:XX:XX)
#define MAC_ADDR_HEX_STR_SIZE 24


/* ============================ FORWARD DECLARATIONS ============================ */

// The node's 8-byte MAC address in hexadecimal
// format with each byte separated by ':'
extern char nodeMACAddr[MAC_ADDR_HEX_STR_SIZE];


/* =========================== FUNCTIONS DECLARATIONS =========================== */

/**
 * @brief Stores the node's 8-byte MAC address in hexadecimal
 *        format into the 'nodeMACAddr' global variable
 */
void getNodeMACAddr();


/**
 * @return Whether a node can communicate with hosts
 *         external to the LLN, which is verified if:
 *           1) One of its NIC has assigned a global IPv6 address
 *           2) Has a parent in the DODAG to forward packets to
 *           3) The node is supposedly reachable downwards in the DODAG
 */
bool isNodeOnline();


#endif //SAFETUNNELS_DEVUTILITIES_H