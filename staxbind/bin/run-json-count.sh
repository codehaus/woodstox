#!/bin/sh
 
echo "About to run full Json counter test suite (input dir: $1)"

# Nothing big stored in memory, heap can remain modest 
# -Djapex.runTime=30 \
$JAVA_HOME/bin/java -server -cp lib/\* \
 -Xmx128M \
 -Djava.awt.headless=true \
 -Djapex.runsPerDriver=1 \
 -Djapex.warmupTime=4 \
 -Djapex.runTime=16 \
 -Djapex.numberOfThreads=1 \
 -Djapex.reportsDirectory=japex-reports \
 -Djapex.plotGroupSize=10 \
 -Djapex.inputDir="$1" \
 com.sun.japex.Japex \
 testcfg/json-count.xml

echo "Done!";
