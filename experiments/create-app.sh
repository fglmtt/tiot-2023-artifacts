#!/bin/bash

if [ -z "$1" ] || [ -z "$2" ]
then
    echo "usage: create-app.sh <url> <spec>"
    exit 1
fi

curl -X POST -H "Content-Type: application/json" --data-binary "@$2" $1 -vvv
