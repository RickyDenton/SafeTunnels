CONTIKI_PROJECT = getPhyMACAndIP
all: $(CONTIKI_PROJECT)
CONTIKI = /home/user/contiki-ng

# The BR is either native or embedded, and in the latter case must support SLIP
PLATFORMS_EXCLUDE = nrf52dk z1
BOARDS_EXCLUDE = nrf52840/dongle

# Include RPL BR module
include $(CONTIKI)/Makefile.dir-variables
MODULES += $(CONTIKI_NG_SERVICES_DIR)/rpl-border-router
MODULES += $(CONTIKI_NG_APP_LAYER_DIR)/mqtt

# Include webserver module
MODULES_REL += webserver

# Include optional target-specific module
include $(CONTIKI)/Makefile.identify-target
MODULES_REL += $(TARGET)

MODULES_REL += arch/platform/$(TARGET)

include $(CONTIKI)/Makefile.include