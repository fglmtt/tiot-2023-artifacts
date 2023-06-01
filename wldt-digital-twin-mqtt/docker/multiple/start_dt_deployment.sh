#!/bin/bash

echo "Starting DT-1 ..."
docker run --name=wldt-mqtt-dt-1 \
    -p 5555:5555 \
    -p 1234:1234 \
    -v $(pwd)/dt_conf_1.yaml:/usr/local/src/mvnapp/dt_conf.yaml \
    -v $(pwd)/logback.xml:/usr/local/src/mvnapp/src/main/resources/logback.xml \
    -v $(pwd)/logback.xml:/usr/local/src/mvnapp/target/classes/logback.xml \
    -d <YOUR_CONTAINER_ID>:<YOUR_VERSION>

echo "Starting DT-2 ..."
docker run --name=wldt-mqtt-dt-2 \
    -p 5556:5555 \
    -p 1235:1234 \
    -v $(pwd)/dt_conf_2.yaml:/usr/local/src/mvnapp/dt_conf.yaml \
    -v $(pwd)/logback.xml:/usr/local/src/mvnapp/src/main/resources/logback.xml \
    -v $(pwd)/logback.xml:/usr/local/src/mvnapp/target/classes/logback.xml \
    -d <YOUR_CONTAINER_ID>:<YOUR_VERSION>

echo "Starting DT-3 ..."
docker run --name=wldt-mqtt-dt-3 \
    -p 5557:5555 \
    -p 1236:1234 \
    -v $(pwd)/dt_conf_3.yaml:/usr/local/src/mvnapp/dt_conf.yaml \
    -v $(pwd)/logback.xml:/usr/local/src/mvnapp/src/main/resources/logback.xml \
    -v $(pwd)/logback.xml:/usr/local/src/mvnapp/target/classes/logback.xml \
    -d <YOUR_CONTAINER_ID>:<YOUR_VERSION>