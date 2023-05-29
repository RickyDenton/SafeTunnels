#ifndef SAFETUNNELS_DEVUTILITIES_H
#define SAFETUNNELS_DEVUTILITIES_H


#define MAC_ADDRESS_SIZE 64




// Print node MAC to buffer
void writeNodeMAC(char* dest);


// Whether the node is connected to a RPL DODAG
bool isNodeConnected();


#endif //SAFETUNNELS_DEVUTILITIES_H
