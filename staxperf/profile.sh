#!/bin/sh

#-Xrunhprof:help

# No need to stress GC, let's add bit more memory
java -Xmx64m -XX:CompileThreshold=2000 -Xrunhprof:cpu=samples,depth=7,interval=2,verbose=n -server\
 -cp lib/stax-api-1.0.1.jar:\
lib/wstx.jar:\
lib/stax_ri.jar:\
lib/wool.jar:\
lib/xercesImpl.jar:lib/xml-apis.jar:\
build/classes $*

#lib/stax_ri.jar:\
#lib/wstx.jar:\
