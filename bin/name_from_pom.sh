#!/usr/bin/env bash

# Determines the name of the JAR file from the POM.
# Usage:
#   bin/name_from_pom.sh [ path/to/pom.xml ]

PRG=`basename "$0"`
BIN_DIR="`dirname $0`"

source $BIN_DIR/functions.sh

pom_file=${1:-'pom.xml'}

echo $(get_target_name $pom_file)
