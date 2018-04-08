#!/usr/bin/env bash


QUEUE_PORT=${QUEUE_PORT:-8080}

PRG=`basename "$0"`
CMD_SWITCH="$1"

source /Users/ronalddennison/Projects/zovida/workers/b2intgr/bin/functions.sh

## Commands
JRE_CMD="`which java`"
$(fatal_check_file $JRE_CMD)

JRE_CMD="${JRE_CMD} -jar"

MAVEN="`which mvn`"

# B2_HOME, B2_RUN, B2_RELBASE, B2_JARFILE, B2_TARGET
source .b2conf

ZQUEUE_PIDFILE="${B2_RUN}/zqueue.pid"

echo "B2_TARGET=$B2_TARGET, MAVEN=$MAVEN, ZQUEUE_PIDFILE=$ZQUEUE_PIDFILE"

if [ ! -f "$B2_TARGET" ]; then
	$(fatal_check_file $MAVEN)

	echo "building ..."

	# $MAVEN clean compile package -DskipTests --file $B2_HOME ; #
 	$MAVEN clean compile package -DskipTests --file $B2_HOME  2>&1
fi

if [ "$CMD_SWITCH" = "start" ]; then
    $(start_zqueue) 2>&1 &
elif [ "$CMD_SWITCH" = "stop" ]; then
    stop_zqueue
elif [ "$CMD_SWITCH" = "test" ]; then
    test_zqueue
fi

exit 0
