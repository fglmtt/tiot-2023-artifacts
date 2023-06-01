#!/bin/bash

rm -r ../../pkg/webserver

openapi-generator generate \
	-i webserver.yaml \
	-g go-server -o \
	../../pkg/webserver \
	--additional-properties=packageName=webserver

rm ../../pkg/webserver/Dockerfile
rm ../../pkg/webserver/README.md
rm -r ../../pkg/webserver/api
rm ../../pkg/webserver/go.mod
rm ../../pkg/webserver/main.go

mv ../../pkg/webserver/go/* ../../pkg/webserver
rm -r ../../pkg/webserver/go
