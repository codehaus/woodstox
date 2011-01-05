#!/bin/sh
 
echo "About to run full 'dbconv' test on Json drivers (input dir: $1)"

# Nothing big stored in memory, heap can remain modest 
# -Djapex.runTime=30 \
java -server -cp lib/\* \
 -Xmx128M \
 -Djapex.runsPerDriver=1 \
 -Djapex.warmupTime=3 \
 -Djapex.runTime=30 \
 -Djapex.numberOfThreads=1 \
 -Djapex.reportsDirectory=japex-reports \
 -Djapex.inputDir="$1" \
 com.sun.japex.Japex \
 testcfg/dbconv-json.xml

echo "Done!";
