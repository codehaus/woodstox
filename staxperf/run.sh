#!/bin/sh

# Need to use more than 16 megs, as files may be over 1 meg...
# Note: only one of wstx jars should be there...

java -Xmx32m -server\
 -cp lib/stax-api-1.0.jar:\
lib/wstx.jar:\
lib/wstx-asl-2.5.jar:\
lib/wstx-asl-2.8.1.jar:\
lib/stax_ri.jar:\
lib/sjsxp-1.0.jar:\
lib/xercesImpl.jar:lib/xml-apis.jar:\
lib/jdom.jar:\
build/classes $*
