#!/bin/sh

#-Xrunhprof:help

# No need to stress GC, let's add bit more memory
java -Xmx32m -XX:CompileThreshold=200 -Xrunhprof:cpu=samples,depth=6 -server\
 -cp lib/stax-api-1.0.1.jar:\
lib/wstx-asl-2.9.4.jar:\
lib/wstx.jar:\
lib/stax_ri.jar:\
lib/wool-asl-0.5.jar:\
lib/xercesImpl.jar:lib/xml-apis.jar:\
build/classes $*

#lib/stax_ri.jar:\
#lib/wstx.jar:\
