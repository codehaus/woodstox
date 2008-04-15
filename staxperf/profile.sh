#!/bin/sh

#-Xrunhprof:help

#  -XX:+PrintGC \
#-XX:+UseConcMarkSweepGC
#-Xrunhprof:cpu=samples,depth=7,interval=2,verbose=n \
# -XX:+UseConcMarkSweepGC \
 
# No need to stress GC, let's add bit more memory
java -Xmx64m -XX:CompileThreshold=2000 -server \
-Xrunhprof:cpu=samples,depth=12,verbose=n,interval=2 \
 -cp build/classes:lib/\* \
$*
