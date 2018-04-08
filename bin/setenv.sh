#!/usr/bin/env bash

MYDIR=$(dirname $0)
B2_HOME=$(dirname $MYDIR)

B2_RELBASE=$B2_HOME

path_regex="$PWD/(.+)"

[[ $B2_RELBASE =~ $path_regex ]] && B2_RELBASE="${BASH_REMATCH[1]}";

if [ ! -d "${B2_HOME}/run" ]; then
	mkdir -p ${B2_HOME}/run
fi

CMD="${B2_HOME}/bin/name_from_pom.sh"

B2_JARFILE=$($CMD ${B2_HOME}/pom.xml)

B2_TARGET=${B2_HOME}/target/${B2_JARFILE}

# echo '#!/usr/bin/env bash' > .b2conf
#touch .b2conf

# Reset conf
echo -n '' > .b2conf

grep 'B2_HOME' .b2conf || echo "B2_HOME=${B2_HOME}" >> .b2conf
grep 'B2_RUN' .b2conf || echo "B2_RUN=${B2_HOME}/run" >> .b2conf
grep 'B2_RELBASE' .b2conf || echo "B2_RELBASE=${B2_RELBASE}" >> .b2conf
grep 'B2_JARFILE' .b2conf || echo "B2_JARFILE=${B2_JARFILE}" >> .b2conf
grep 'B2_TARGET' .b2conf || echo "B2_TARGET=${B2_TARGET}" >> .b2conf

setFromEnv () {
	for vline in `env | grep -i '^(GRAPHENE|SENDGRID)'`; do
		vname=$(echo $vline | cut -d'=' -f 1)
		grep $vname .b2conf || echo "$vline" >> .b2conf
	done
}

setFromEnvFile () {
	envfile=${1:-.env}

    for vline in $(fgrep -E '^(GRAPHENE|SENDGRID|B2)' $envfile); do
        vname=$(echo $vline | cut -d'=' -f 1)
    #	echo $vline;
        grep $vname .b2conf || echo "$vline" >> .b2conf
    done
}

setFromEnv
if [ -f .env ]; then
	setFromEnvFile
fi

# grep 'B2_HOME' .b2conf || echo "export B2_HOME=${B2_HOME}" >> .b2conf
# echo "echo 1" >> .b2conf
# exit 0
#  for i in $(fgrep -E '^(GRAPHENE|B2)' .b2conf); do export $i; done
