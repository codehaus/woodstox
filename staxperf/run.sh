#!/bin/sh

# Need to use more than 16 megs, as files may be over 1 meg...
# Note: only one of wstx jars should be there...

#-server\
#-XX:CompileThreshold=2000 \

java \
 -Xmx128m \
 -server\
 -cp build/classes:lib/\*:lib/xsl/\* \
 $*
