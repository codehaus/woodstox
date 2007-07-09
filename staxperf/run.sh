#!/bin/sh

# Need to use more than 16 megs, as files may be over 1 meg...
# Note: only one of wstx jars should be there...

#-server\

java -XX:CompileThreshold=1000 -Xmx64m -server \
 -cp lib/stax-api-1.0.1.jar\
:lib/wstx.jar\
:lib/stax-ri-1.2.0.jar\
:lib/sjsxp-1.0.1.jar\
:lib/wool.jar\
:lib/xerces-2.8.0.jar\
:lib/jdom.jar\
:lib/sjsxp.jar\
:lib/javolution-4.0.2.jar\
:build/classes\
  $*
