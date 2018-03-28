#!/usr/bin/env bash


JAVA_CMD="`which java` -jar"

QUEUE_PORT=${Q_PORT:-8080}
WEB_PORT=${PORT:-80}


read_dom () {
    local IFS=\>
    read -d \< ENTITY CONTENT
    local RET=$?
    TAG_NAME=${ENTITY%% *}
    ATTRIBUTES=${ENTITY#* }
    return $RET
}

get_target_name () {
    let CTR=0
    JARNAME=''
    JVER=''
    JPKG=''

    while read_dom; do

        if [[ $ENTITY = "artifactId" ]]; then
            JARNAME="$CONTENT"
            ((CTR+=1))
        elif [[ $ENTITY = "version" ]]; then
            JVER="$CONTENT"
            ((CTR+=1))
        elif [[ $ENTITY = "packaging" ]]; then
            JPKG="$CONTENT"
            ((CTR+=1))
        fi
        if [[ $CTR -eq 3 ]]; then
            echo "${JARNAME}-${JVER}.${JPKG}"
            break;
        fi
    done < 'pom.xml'
}

function start_node {

    DIRP=0
    NODE_CMD="`which npm`"

    if [ -z "NODE_CMD" ]; then
        echo STDERR "[WARN] 'npm' command not found"
        NODE_CMD="`which node`"
    fi

    if [ -z "NODE_CMD" ]; then
        echo STDERR "[ERROR] 'node' command not found"
    fi

    if [ ! -z "NODE_CMD" ]; then
        if [ -z "$1" ]; then
            cd $1
            DIRP=1
        fi
        $NODE_CMD 1>&2 &
        let PID=$!
        echo -n $PID > $NODE_PIDFILE
        [ $DIRP -eq 1 ] && cd -
    fi

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



function stop_zqueue {

  PID=''

  if [ ! -z "$ZQUEUE_PIDFILE" ] && [ -f $ZQUEUE_PIDFILE ]; then
      PID=`cat "$ZQUEUE_PIDFILE"`
      while [ $SLEEP -ge 0 ]; do
        kill -9 "$PID" >/dev/null 2>&1
        if [ $? -gt 0 ]; then
          rm -f "$ZQUEUE_PIDFILE" >/dev/null 2>&1
          echo  "Killed Process: $PID" >&2
          return 0
        else
          sleep $SLEEP
          ((SLEEP-=1))
        fi
      done
  else

      PID=`ps -eo 'tty pid args' | grep "$target_name" | grep -v grep | tr -s ' ' | cut -f2 -d ' '`

      if [ ! -z "$PID" ]; then
         echo  "Killed Process: $PID"
       kill -KILL $PID >/dev/null 2>&1
      fi

      PID=`ps -eo 'tty pid args' | grep "$PRG" | grep -v grep | tr -s ' ' | cut -f2 -d ' '`
      if [ ! -z "$PID" ]; then
        ps -p $PID >/dev/null 2>&1
        if [ $? -eq 0 ] ; then
           echo  "Killing process: $PID" >&2
           kill -9 $PID 1>/dev/null
        fi
      fi

  fi
}


function start_zqueue {
#while [[ nc -z localhost $QUEUE_PORT && $max_tries -ne 0 ]]; do
    if nc -z localhost $QUEUE_PORT; then
        # port $QUEUE_PORT is not open! >&2
        stop_zqueue
    fi

    ${JAVA_CMD} $JTARGET "$@" &

    let QUEUE_PID=$!
	## Store PID
    echo  -n $QUEUE_PID > $ZQUEUE_PIDFILE
	echo  "Starting 'Router' (pid: ${QUEUE_PID}) ..." >&2
}

