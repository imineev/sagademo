#!/bin/bash

SCRIPT_DIR=$(dirname $0)

IMAGE_NAME=travelagency-helidon-se
IMAGE_VERSION=0.1

if [ -z "DEMOREGISTRY" ]; then
    echo "Error: DEMOREGISTRY env variable needs to be set!"
    exit 1
fi

export IMAGE=${DEMOREGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}

mvn install
mvn package docker:build

if [ $DOCKERBUILD_RETCODE -ne 0 ]; then
    exit 1
fi
docker push $IMAGE
if [  $? -eq 0 ]; then
    docker rmi ${IMAGE}
fi