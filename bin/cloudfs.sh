#!/usr/bin/env bash

PRG=`basename "$0"`
BIN_DIR="`dirname $0`"
CMD_SWITCH="$1"

CONFF=${BIN_DIR}/.b2conf

QUEUE_PORT=${QUEUE_PORT:-8080}

APP_DIR="`dirname $BIN_DIR`"

ZQUEUE_PIDFILE="${APP_DIR}/zqueue.pid"


source $BIN_DIR/functions.sh
$BIN_DIR/setenv.sh
source $CONFF


if [ "$CMD_SWITCH" == "stop" ]; then

	stop_zqueue

elif [ "$CMD_SWITCH" == "start" ]; then

	if [ ! -f "$B2_TARGET" ]; then

		echo "building ..."

		MAVEN="`which mvn`"

		if [ "$B2_HOME" == "." ]; then
			MARGS=' -DskipTests '
		else
			MARGS=" -DskipTests --file ${B2_HOME}"
		fi

		$MAVEN clean compile package${MARGS} 1>&2
	fi

	[ -f "$B2_TARGET" ] && start_zqueue "$B2_TARGET"

elif [ "$CMD_SWITCH" == "clean" ]; then

		MAVEN="`which mvn`"
		$MAVEN clean 1>&2

		[ -f "$B2_HOME/dependency-reduced-pom.xml" ] && rm "$B2_HOME/dependency-reduced-pom.xml";
		[ -d "$B2_HOME/run" ] && rm -rf "$B2_HOME/run";

elif [ "$CMD_SWITCH" == "test" ]; then

	test_zqueue

fi

[ -f $CONFF ] && rm $CONFF;
