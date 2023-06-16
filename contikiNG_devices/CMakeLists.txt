cmake_minimum_required(VERSION 3.25)
project(SafeTunnels C)

set(CMAKE_C_STANDARD 11)


set(CONTIKI /home/osboxes/contiki-ng)

include_directories(${CONTIKI})
include_directories(${CONTIKI}/os)
include_directories(${CONTIKI}/os/lib)
include_directories(${CONTIKI}/arch/platform/cooja)
include_directories(${CONTIKI}/os/net/app-layer/mqtt)
include_directories(${CONTIKI}/os/net/app-layer/coap)
include_directories(${CONTIKI}/os/net/ipv6)



#include_directories(/home/osboxes/contiki-ng/os)
#include_directories(/home/osboxes/contiki-ng/os/sys)
#include_directories(/home/osboxes/contiki-ng/os/dev)
#include_directories(/home/osboxes/contiki-ng/os/sys)
#include_directories(/home/osboxes/contiki-ng/arch/platform/cooja)

include_directories(src/border-router)
include_directories(src/border-router/sky)
include_directories(src/border-router/webserver)
include_directories(src/sensor)
include_directories(src/common)

add_executable(SafeTunnels
        src/border-router/sky/module-macros.h
        src/border-router/webserver/httpd-simple.c
        src/border-router/webserver/httpd-simple.h
        src/border-router/webserver/webserver.c
        src/border-router/border-router.c
        src/border-router/project-conf.h
        src/sensor/project-conf.h
        src/sensor/sensor.c src/sensor/sensor.h src/common/devUtilities.c src/common/devUtilities.h src/actuator/actuator.c src/actuator/actuator.h src/actuator/actuator.h src/actuator/actuator.c src/actuator/actuator.c src/actuator/project-conf.h src/actuator/resources/light.c src/actuator/resources/fan.c src/actuator/resources/actuatorErrors.c src/actuator/resources/actuatorErrors.h src/utils/getPhyMACAndIP/getPhyMACAndIP.c)
