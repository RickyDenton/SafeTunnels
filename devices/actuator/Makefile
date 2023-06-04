CONTIKI_PROJECT = safetunnels-actuator-node

all: $(CONTIKI_PROJECT)

# Include the "common" folder
MODULES_REL += ../common

# Do not try to build on Sky because of code size limitation
PLATFORMS_EXCLUDE = sky z1

# Include CoAP resources
MODULES_REL += ./resources

CONTIKI=/home/user/contiki-ng

# Include the CoAP implementation
include $(CONTIKI)/Makefile.dir-variables
MODULES += $(CONTIKI_NG_APP_LAYER_DIR)/coap

include $(CONTIKI)/Makefile.include

# Testing purposes
# CFLAGS += -Wno-error