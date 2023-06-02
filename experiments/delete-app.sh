#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: delete-app.sh <url>"
  exit 1
fi

curl -X DELETE $1
