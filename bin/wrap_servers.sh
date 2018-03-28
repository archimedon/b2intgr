#!/usr/bin/env bash

PRG="$0"

. ./bin/functions.sh;

target_name=$(get_target_name)

JAR_FILE="target/${target_name}"

# Get standard environment variables
BIN=$(dirname "$PRG")

APP_HOME=`cd "$BIN/.." >/dev/null; pwd`
if [ -z $APP_HOME ]; then
    APP_HOME=./
fi

JTARGET="${APP_HOME}/${JAR_FILE}"
APP_HOME=./
NODE_HOME=$(find "${APP_HOME}" -name package\.json -type f)

if [ $? -eq 0 ]; then


if [ ! -f "$JTARGET" ]; then
    mvn clean compile package >/dev/null 2>&1
fi

# Add Java args
JTARGET="${JTARGET} -DQPORT=$QUEUE_PORT";

RUN_DIR="${APP_HOME}/run"

if [ ! -d "$RUN_DIR" ]; then
    mkdir $RUN_DIR;
fi

ZQUEUE_PIDFILE="${RUN_DIR}/zqueue.pid"
NODE_PIDFILE="${RUN_DIR}/node.pid"


#echo  "APP_HOME: ${APP_HOME}, ZQUEUE_PIDFILE: ${ZQUEUE_PIDFILE}, \
# RUN_DIR Directory: ${RUN_DIR}, NODE_CMD: ${NODE_CMD}, JTARGET: ${JTARGET}" >&2

# Clear user defined CLASSPATH
# CLASSPATH=


if [ "$1" = "start" ]; then

    $(start_node) && $(start_zqueue) >/dev/null 2>&1

elif [ "$1" = "stop" ]; then

    let SLEEP=2
    stop_zqueue
fi



exit 0
