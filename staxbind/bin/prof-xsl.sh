#!/bin/sh
 
echo "About to run xsl test profiler (input dir: $1)"

$JAVA_HOME/bin/java -server -cp lib/\* \
 -server -Xrunhprof:cpu=samples,depth=10,verbose=n,interval=2 \
 -Xmx128M \
 -Djava.awt.headless=true \
 -Djapex.runsPerDriver=1 \
 -Djapex.warmupTime=4 \
 -Djapex.runTime=36 \
 -Djapex.numberOfThreads=1 \
 -Djapex.reportsDirectory=build \
 -Djapex.inputDir="$1" \
 com.sun.japex.Japex \
 testcfg/xslt-prof.xml

echo "Done!";
