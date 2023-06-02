CONTIKI_PROJECT = safetunnels-sensor-node
all: $(CONTIKI_PROJECT)

# Include the "common" folder
MODULES_REL += ../common

CONTIKI = /home/user/contiki-ng

include $(CONTIKI)/Makefile.dir-variables
MODULES += $(CONTIKI_NG_APP_LAYER_DIR)/mqtt

-include $(CONTIKI)/Makefile.identify-target

MODULES_REL += arch/platform/$(TARGET)

include $(CONTIKI)/Makefile.include

# Testing purposes
# CFLAGS += -Wno-error