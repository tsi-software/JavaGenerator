#!/bin/bash
# mvn-clean-exec.sh
#
set -e
set -u

shopt -s -o nounset
declare -rx SCRIPT=${0##*/}
SCRIPT_DIR=$(dirname "${0}")
SCRIPT_DIR=$(realpath "${SCRIPT_DIR}")
NOW=$(date +"%Y-%m-%d_%H-%M")
echo "$NOW"
echo "SCRIPT=$SCRIPT"
echo "SCRIPT_DIR=$SCRIPT_DIR"

cd "${SCRIPT_DIR}"
mvn clean install exec:java -Dexec.mainClass="ca.taylorsoftware.javagenerator.examples.FileDirectoryTraversalExample"
