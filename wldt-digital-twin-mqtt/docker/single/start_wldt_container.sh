#!/bin/bash

docker run --name=wldt-mqtt-dt \
    -p 5555:5555 \
    -p 1234:1234 \
    -v $(pwd)/dt_conf.yaml:/usr/local/src/mvnapp/dt_conf.yaml \
    -v $(pwd)/logback.xml:/usr/local/src/mvnapp/src/main/resources/logback.xml \
    -v $(pwd)/logback.xml:/usr/local/src/mvnapp/target/classes/logback.xml \
    -d <YOUR_CONTAINER_ID>:<YOUR_VERSION>