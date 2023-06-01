#!/bin/bash

docker run --name=wldt-composed-mqtt-dt \
    -p 5558:5555 \
    -p 1237:1234 \
    -v $(pwd)/cdt_conf.yaml:/usr/local/src/mvnapp/cdt_conf.yaml \
    -v $(pwd)/logback.xml:/usr/local/src/mvnapp/src/main/resources/logback.xml \
    -v $(pwd)/logback.xml:/usr/local/src/mvnapp/target/classes/logback.xml \
    -d <YOUR_CONTAINER_ID>:<YOUR_VERSION>