#!/bin/sh

#-Xrunhprof:help

# No need to stress GC, let's add bit more memory
java -Xmx128m -Xrunhprof:cpu=samples -server\
 -cp lib/stax1.0.jar:lib/wstx.jar:lib/stax_ri.jar:\
lib/xercesImpl.jar:lib/xml-apis.jar:\
/home/tatu/java/ant/lib/junit.jar:\
lib/jdom.jar:\
build/classes $*

#lib/stax_ri.jar:\
#lib/wstx.jar:\
