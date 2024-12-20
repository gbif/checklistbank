#!/usr/bin/env bash
#Simple script for pushing a image containing the named modules build artifact
set -e

MODULE="checklistbank-workflows"

POM_VERSION=$1
IMAGE=docker.gbif.org/${MODULE}:${POM_VERSION}
IMAGE_LATEST=docker.gbif.org/${MODULE}:latest

echo "Building Docker image module:version - ${MODULE}:${POM_VERSION}"
docker build -f ./${MODULE}/docker/Dockerfile ./${MODULE} --build-arg JAR_FILE=${MODULE}-${POM_VERSION}-shaded.jar -t ${IMAGE}

echo "Pushing docker image to the repository"
docker push ${IMAGE}
if [[ $IS_M2RELEASEBUILD = true ]]; then
  echo "Updated latest tag pointing to the newly released ${IMAGE}"
  docker tag ${IMAGE} ${IMAGE_LATEST}
  docker push ${IMAGE_LATEST}
fi

echo "Removing local docker image: ${IMAGE}"
docker rmi -f ${IMAGE}

if [[ $IS_M2RELEASEBUILD = true ]]; then
  echo "Removing local docker image with latest tag: ${IMAGE_LATEST}"
  docker rmi -f ${IMAGE_LATEST}
fi
