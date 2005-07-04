#!/bin/sh

# Need to use more than 16 megs, as files may be over 1 meg...

java -Xmx32m -server\
 -cp lib/stax1.0.jar:lib/wstx.jar:lib/stax_ri.jar:\
lib/xercesImpl.jar:lib/xml-apis.jar:\
/home/tatu/java/ant/lib/junit.jar:\
lib/jdom.jar:\
build/classes $*
