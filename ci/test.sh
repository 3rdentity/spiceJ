#!/bin/bash

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

cd "$DIR/../spiceJ"

# -P release to activate release-like things like javadoc generation (and warnings)
mvn -P release clean verify
