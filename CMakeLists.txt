cmake_minimum_required(VERSION 3.25)
project(SafeTunnels C)

set(CMAKE_C_STANDARD 11)


set(CONTIKI /home/osboxes/contiki-ng)

include_directories(${CONTIKI})
include_directories(${CONTIKI}/os)
include_directories(${CONTIKI}/os/lib)
include_directories(${CONTIKI}/arch/platform/cooja)
include_directories(${CONTIKI}/os/net/app-layer/mqtt)

#include_directories(/home/osboxes/contiki-ng/os)
#include_directories(/home/osboxes/contiki-ng/os/sys)
#include_directories(/home/osboxes/contiki-ng/os/dev)
#include_directories(/home/osboxes/contiki-ng/os/sys)
#include_directories(/home/osboxes/contiki-ng/arch/platform/cooja)

include_directories(devices/rpl-border-router)
include_directories(devices/rpl-border-router/sky)
include_directories(devices/rpl-border-router/webserver)
include_directories(devices/sensor)

add_executable(SafeTunnels
        devices/rpl-border-router/sky/module-macros.h
        devices/rpl-border-router/webserver/httpd-simple.c
        devices/rpl-border-router/webserver/httpd-simple.h
        devices/rpl-border-router/webserver/webserver.c
        devices/rpl-border-router/border-router.c
        devices/rpl-border-router/project-conf.h
        devices/sensor/project-conf.h
        devices/sensor/sensor.c devices/sensor/sensor.h)
