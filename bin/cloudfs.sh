#!/usr/bin/env bash

PRG=`basename "$0"`
BIN_DIR="`dirname $0`"
CMD_SWITCH="$1"

QUEUE_PORT=${QUEUE_PORT:-8080}

APP_DIR="`dirname $BIN_DIR`"

ZQUEUE_PIDFILE="${APP_DIR}/zqueue.pid"


source $BIN_DIR/functions.sh

## Commands
JRE_CMD="`which java`"
$(fatal_check_file $JRE_CMD)

JRE_CMD="${JRE_CMD} -jar"

MAVEN="`which mvn`"

if [ "$CMD_SWITCH" = "stop" ]; then
    stop_zqueue
elif [ "$CMD_SWITCH" = "start" ] || [ "$CMD_SWITCH" = "test" ]; then

	#	[ ! -f .b2conf ] && $BIN_DIR/setenv.sh
	$BIN_DIR/setenv.sh

	# B2_HOME, B2_RUN, B2_RELBASE, B2_JARFILE, B2_TARGET

	# cat .b2conf

	# B2_HOME, B2_RUN, B2_RELBASE, B2_JARFILE, B2_TARGET
	# source .b2conf

	for vline in $(fgrep -E '^(GRAPHENE|SENDGRID|B2)' .b2conf); do
		export $vline
	done

	fatal_check_bolt $GRAPHENEDB_BOLT_URL

	if [ "$CMD_SWITCH" = "start" ]; then
		if [ ! -f "$B2_TARGET" ]; then
			$(fatal_check_file $MAVEN)

			echo "building ..." >&2

			# $MAVEN clean compile package -DskipTests --file $B2_HOME ; #
			$MAVEN clean compile package -DskipTests --file $B2_HOME  2>&1
		fi
		$(start_zqueue) 2>&1 &
	elif [ "$CMD_SWITCH" = "test" ]; then
		test_zqueue
	fi
fi


exit 0

