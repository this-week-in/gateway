#!/usr/bin/env bash

APP_NAME=gateway
IMAGE_NAME=gcr.io/${GCLOUD_PROJECT}/twi-${APP_NAME}
cd "$GITHUB_WORKSPACE"
./gradlew  bootBuildImage --imageName="$IMAGE_NAME"
IMAGE_ID=$(docker images -q $APP_NAME)
docker tag "${IMAGE_ID}" "${IMAGE_NAME}"
docker push "${IMAGE_NAME}"
