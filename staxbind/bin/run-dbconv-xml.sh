#!/bin/sh
 
echo "About to run full 'dbconv' test on XML drivers (input dir: $1)"

# Nothing big stored in memory, heap can remain modest 
# -Djapex.runTime=30 \
$JAVA_HOME/bin/java -server -cp lib/\* \
 -Xmx128M \
 -Djava.awt.headless=true \
 -Djapex.runsPerDriver=1 \
 -Djapex.warmupTime=3 \
 -Djapex.runTime=30 \
 -Djapex.numberOfThreads=1 \
 -Djapex.reportsDirectory=japex-reports \
 -Djapex.inputDir="$1" \
 com.sun.japex.Japex \
 testcfg/dbconv-xml.xml

echo "Done!";
