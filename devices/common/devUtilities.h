#ifndef SAFETUNNELS_DEVUTILITIES_H
#define SAFETUNNELS_DEVUTILITIES_H


#define MAC_ADDRESS_STR_SIZE 71



// The node ID consisting of its stringyfied MAC address
extern char nodeID[MAC_ADDRESS_STR_SIZE];



// Initializes the node's ID as its stringyfied MAC address
void initNodeID();

// Whether the node can communicate with hosts external to the LLN
bool isNodeOnline();


#endif //SAFETUNNELS_DEVUTILITIES_H
