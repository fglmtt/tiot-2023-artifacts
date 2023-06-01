#!/bin/bash

echo "Stopping DT-1 ..."
docker stop wldt-mqtt-dt-1
docker rm wldt-mqtt-dt-1

echo "Stopping DT-2 ..."
docker stop wldt-mqtt-dt-2
docker rm wldt-mqtt-dt-2

echo "Stopping DT-3 ..."
docker stop wldt-mqtt-dt-3
docker rm wldt-mqtt-dt-3