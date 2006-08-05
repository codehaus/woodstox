#!/bin/sh

# Need to use more than 16 megs, as files may be over 1 meg...
# Note: only one of wstx jars should be there...

java -XX:CompileThreshold=500 -Xmx32m -server\
 -cp lib/stax-api-1.0.1.jar:\
lib/wstx.jar:\
lib/stax-ri-1.2.0.jar:\
lib/sjsxp-1.0.jar:\
lib/wool.jar:\
lib/xercesImpl.jar:lib/xml-apis.jar:\
lib/jdom.jar:\
build/classes $*
