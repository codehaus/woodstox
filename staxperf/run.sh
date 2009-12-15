#!/bin/sh

#-server\
#-XX:CompileThreshold=2000 \

java \
 -Xmx100m \
 -server\
 -cp build/classes:lib/\*:lib/xsl/\* \
 $*
