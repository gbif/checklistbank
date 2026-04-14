#!/bin/bash -e

IS_M2RELEASEBUILD=$1
POM_VERSION=$2

MODULE="checklistbank-workflows"

# Resolve directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

# If no version was passed, try to detect it from the module pom using Maven
if [[ -z "${POM_VERSION}" ]]; then
  echo "POM_VERSION not provided; attempting to detect from module pom..."

  if command -v mvn >/dev/null 2>&1; then
    POM_VERSION=$(mvn -f "${MODULE_DIR}/pom.xml" -q help:evaluate -Dexpression=project.version -DforceStdout 2>/dev/null || true)
    POM_VERSION="$(echo "${POM_VERSION}" | tr -d '\r' | tr -d '\n' | xargs)"
  else
    echo "mvn not found in PATH; cannot auto-detect POM version."
  fi

  if [[ -z "${POM_VERSION}" ]]; then
    echo "ERROR: Could not determine POM_VERSION. Provide it as the second argument to the script (e.g. ./build.sh true 1.0.0)."
    exit 1
  fi
  echo "Detected module version: ${POM_VERSION}"
fi

IMAGE=docker.gbif.org/${MODULE}:${POM_VERSION}
IMAGE_LATEST=docker.gbif.org/${MODULE}:latest

JAR_NAME=${MODULE}-${POM_VERSION}.jar
JAR_PATH="${MODULE_DIR}/target/${JAR_NAME}"

# If the jar doesn't exist, try to build it
if [[ ! -f "${JAR_PATH}" ]]; then
  echo "Jar ${JAR_PATH} not found; attempting to build the module..."
  if command -v mvn >/dev/null 2>&1; then
    mvn -f "${MODULE_DIR}/pom.xml" -DskipTests package
  else
    echo "ERROR: ${JAR_PATH} not found and mvn not available to build it. Aborting."
    exit 1
  fi
fi

# Allow specifying the build platform to avoid base image platform mismatch (3rd arg or DOCKER_BUILD_PLATFORM env)
BUILD_PLATFORM=""
if [[ -n "${3}" ]]; then
  BUILD_PLATFORM="${3}"
elif [[ -n "${DOCKER_BUILD_PLATFORM}" ]]; then
  BUILD_PLATFORM="${DOCKER_BUILD_PLATFORM}"
fi
BUILD_PLATFORM_ARG=""
if [[ -n "${BUILD_PLATFORM}" ]]; then
  echo "Using docker build --platform=${BUILD_PLATFORM}"
  BUILD_PLATFORM_ARG="--platform=${BUILD_PLATFORM}"
fi

# Build context: module root, Dockerfile path: this script's Dockerfile
BUILD_CONTEXT="${MODULE_DIR}"
DOCKERFILE_PATH="${SCRIPT_DIR}/Dockerfile"

# Verify the jar is now present in the build context
if [[ ! -f "${BUILD_CONTEXT}/target/${JAR_NAME}" ]]; then
  echo "ERROR: expected jar ${BUILD_CONTEXT}/target/${JAR_NAME} not found in build context. Aborting."
  exit 1
fi

echo "Building Docker image: ${IMAGE}"
docker build ${BUILD_PLATFORM_ARG} -f "${DOCKERFILE_PATH}" "${BUILD_CONTEXT}" --build-arg JAR_FILE=${JAR_NAME} -t "${IMAGE}"

echo "Pushing Docker image to the repository"
docker push ${IMAGE}
if [[ $IS_M2RELEASEBUILD = true ]]; then
  echo "Updated latest tag pointing to the newly released ${IMAGE}"
  docker tag ${IMAGE} ${IMAGE_LATEST}
  docker push ${IMAGE_LATEST}
fi

echo "Removing local Docker image: ${IMAGE}"
docker rmi -f ${IMAGE} || true

if [[ $IS_M2RELEASEBUILD = true ]]; then
  echo "Removing local Docker image with latest tag: ${IMAGE_LATEST}"
  docker rmi -f ${IMAGE_LATEST} || true
fi

