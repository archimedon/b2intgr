#!/usr/bin/env bash

# Determines the name of the JAR file from the POM.
# Usage:
#   getMvnTarget [ path/to/pom.xml ]


pom_file=${1:-'pom.xml'}

echo $(get_target_name $pom_file)
