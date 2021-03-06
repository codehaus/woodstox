#!/bin/sh
 
echo "About to run full Japex test suite over 'dbconv' test (input dir: $1)"

# Nothing big stored in memory, heap can remain modest 
$JAVA_HOME/bin/java -server -cp lib/\* \
 -Xrunhprof:cpu=samples,depth=10,verbose=n,interval=2 \
 -Xmx128M \
 -Djava.awt.headless=true \
 -Djapex.runsPerDriver=1 \
 -Djapex.warmupTime=3 \
 -Djapex.runTime=30 \
 -Djapex.numberOfThreads=1 \
 -Djapex.reportsDirectory=japex-reports \
 -Djapex.inputDir="$1" \
 com.sun.japex.Japex \
 testcfg/dbconv-full.xml

echo "Done!";
