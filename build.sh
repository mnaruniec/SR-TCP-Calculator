#!/bin/bash

export TERM=xterm-color

./gradlew :server:installDist
./gradlew :client:installDist
