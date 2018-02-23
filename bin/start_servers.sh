#!/usr/bin/env bash

PRG="$0"

JAR_FILE='target/b2intgr-0.0.1-jar-with-dependencies.jar'
JAR_FILE='target/b2intgr-0.0.1.jar'

JAVA_CMD="`which java` -jar"
NODE_CMD="`which node`"

QUEUE_PORT=${Q_PORT:-8080}
WEB_PORT=${PORT:-80}

# Get standard environment variables
BIN=$(dirname "$PRG")
APP_HOME=`cd "$BIN/.." >/dev/null; pwd`
if [ -z $APP_HOME ]; then
    APP_HOME=./
fi

JTARGET="${APP_HOME}/${JAR_FILE} -DQPORT=$QUEUE_PORT"
NODE_CMD="$NODE_CMD ${APP_HOME}/index.js"

if [ ! -f "$JTARGET" ]; then
    do_mvn_build;
fi

RUN_DIR="${APP_HOME}/run"

if [ ! -d "$RUN_DIR" ]; then
    mkdir $RUN_DIR;
fi

ZQUEUE_PIDFILE="${RUN_DIR}/zqueue.pid"
NODE_PIDFILE="${RUN_DIR}/node.pid"

let SLEEP=2

echo  "APP_HOME: ${APP_HOME}, ZQUEUE_PIDFILE: ${ZQUEUE_PIDFILE}, \
RUN_DIR Directory: ${RUN_DIR}, NODE_CMD: ${NODE_CMD}, JTARGET: ${JTARGET}" >&2

# Clear user defined CLASSPATH
# CLASSPATH=


function start_node {


 $NODE_CMD >/dev/null &
 let PID=$!
# echo "$PID" >&2
#    if nc -z localhost $WEB_PORT; then
#      echo  port $WEB_PORT is not free! >&2
#      echo  Attempting shutdown! >&2
##        stop_node
#      PID=`ps -eo 'tty pid args' | grep 'node' | grep -v grep | tr -s ' ' | cut -f2 -d ' '`
#
#      echo  "NODE PID: $PID" >&2
#
#      ps -p $PID >/dev/null 2>&1
#      if [ $? -eq 0 ] ; then
#        kill -9 $PID >/dev/null 2>&1
#      fi
#    fi
#
#    exec "$NODE_CMD" &
#    let PID=$!
#    echo  $PID >&2
#
#	## Store PID
#    echo  -n $PID > $NODE_PIDFILE >&2
#	echo  "Starting 'Node' (pid: ${PID}) ..." >&2
#	return $PID
}

function do_mvn_build() {
    mvn clean compile package >/dev/null 2>&1
}


function stop_zqueue() {

  if [ ! -z "$ZQUEUE_PIDFILE" ]; then
    if [ -f $ZQUEUE_PIDFILE ]; then
      PID=`cat "$ZQUEUE_PIDFILE"`
      while [ $SLEEP -ge 0 ]; do
        kill -9 "$PID" >/dev/null 2>&1
        if [ $? -gt 0 ]; then
          rm -f "$ZQUEUE_PIDFILE" >/dev/null 2>&1
          echo  "Killed Process: $PID" >&2
          break
        else
          sleep $SLEEP
          let SLEEP-=1
        fi
      done
    fi
  fi
  PID=`ps -eo 'tty pid args' | grep 'b2intgr' | grep -v grep | tr -s ' ' | cut -f2 -d ' '`

  ps -p $PID >/dev/null 2>&1
  if [ $? -eq 0 ] ; then
    kill -9 $PID >/dev/null 2>&1
  fi
}


function start_zqueue {
#while [[ nc -z localhost $QUEUE_PORT && $max_tries -ne 0 ]]; do
    if nc -z localhost $QUEUE_PORT; then
#        echo  port $QUEUE_PORT is not free! >&2
#        echo  Attempting shutdown! >&2
        stop_zqueue
    fi

#    echo  "$JAVA_CMD $JTARGET $@" >&2

    ${JAVA_CMD} $JTARGET "$@" &

    let QUEUE_PID=$!
#    echo  $QUEUE_PID >&2
	## Store PID
    echo  -n $QUEUE_PID > $ZQUEUE_PIDFILE
	echo  "Starting 'Router' (pid: ${QUEUE_PID}) ..." >&2
}

if [ "$1" = "start" ]; then
    $(start_node) && $(start_zqueue) >/dev/null 2>&1
elif [ "$1" = "stop" ]; then
  stop_zqueue
fi



exit 0