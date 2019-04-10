#!/bin/bash

export MAKEFLAGS=-j2

# ./gradlew clean assembleRelease bintrayUpload
./gradlew bintrayUpload