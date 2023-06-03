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


#include_directories(/home/osboxes/contiki-ng/os)
#include_directories(/home/osboxes/contiki-ng/os/sys)
#include_directories(/home/osboxes/contiki-ng/os/dev)
#include_directories(/home/osboxes/contiki-ng/os/sys)
#include_directories(/home/osboxes/contiki-ng/arch/platform/cooja)

include_directories(devices/border-router)
include_directories(devices/border-router/sky)
include_directories(devices/border-router/webserver)
include_directories(devices/sensor)
include_directories(devices/common)

add_executable(SafeTunnels
        devices/border-router/sky/module-macros.h
        devices/border-router/webserver/httpd-simple.c
        devices/border-router/webserver/httpd-simple.h
        devices/border-router/webserver/webserver.c
        devices/border-router/border-router.c
        devices/border-router/project-conf.h
        devices/sensor/project-conf.h
        devices/sensor/sensor.c devices/sensor/sensor.h devices/common/devUtilities.c devices/common/devUtilities.h devices/actuator/actuator.c devices/actuator/actuator.h devices/actuator/actuator.h devices/actuator/actuator.c devices/actuator/actuator.c devices/actuator/project-conf.h devices/actuator/resources/light.c devices/actuator/resources/fan.c)
