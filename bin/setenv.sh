#!/usr/bin/env bash

BIN_DIR=$(dirname $0)
B2_HOME=$(dirname $BIN_DIR)

B2_RELBASE=$B2_HOME

path_regex="$PWD/(.+)"

[[ $B2_RELBASE =~ $path_regex ]] && B2_RELBASE="${BASH_REMATCH[1]}";

if [ ! -d "${B2_HOME}/run" ]; then
	mkdir -p ${B2_HOME}/run
fi

CMD="${BIN_DIR}/name_from_pom.sh"

B2_JARFILE=$($CMD ${B2_HOME}/pom.xml)

B2_TARGET=${B2_HOME}/target/${B2_JARFILE}


CONFF=${BIN_DIR}/.b2conf

# Reset conf
echo -n '' > $CONFF

grep 'B2_HOME' $CONFF >/dev/null || echo "export B2_HOME=${B2_HOME}" >> $CONFF
grep 'B2_RUN' $CONFF >/dev/null || echo "export B2_RUN=${B2_HOME}/run" >> $CONFF
grep 'B2_RELBASE' $CONFF >/dev/null || echo "export B2_RELBASE=${B2_RELBASE}" >> $CONFF
grep 'B2_JARFILE' $CONFF >/dev/null || echo "export B2_JARFILE=${B2_JARFILE}" >> $CONFF
grep 'B2_TARGET' $CONFF >/dev/null || echo "export B2_TARGET=${B2_TARGET}" >> $CONFF

setFromEnv () {
	for vline in `env | grep -i '^(GRAPHENE|SENDGRID|JAVA_)'`; do
		vname=$(echo $vline | cut -d'=' -f 1)
		grep $vname $CONFF >/dev/null || echo "export $vline" >> $CONFF
	done
}

setFromEnvFile () {
	envfile=${1:-.env}

    for vline in $(fgrep -E '^(GRAPHENE|SENDGRID|JAVA_|B2)' $envfile); do
        vname=$(echo $vline | cut -d'=' -f 1)
        grep $vname $CONFF  >/dev/null || echo "export $vline" >> $CONFF
    done
}

setFromEnv
if [ -f .env ]; then
	setFromEnvFile
fi
