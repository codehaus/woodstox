#!/bin/sh
 
echo "About to run full xsl test on parser+processor combos (input dir: $1)"

# For multi-threaded tests, do need much more memory
# -Djapex.runTime=30 \
$JAVA_HOME/bin/java -server -cp lib/\* \
 -Xmx512M \
 -Djava.awt.headless=true \
 -Djapex.runsPerDriver=1 \
 -Djapex.warmupTime=4 \
 -Djapex.runTime=16 \
 -Djapex.numberOfThreads=16 \
 -Djapex.reportsDirectory=japex-reports \
 -Djapex.plotGroupSize=10 \
 -Djapex.inputDir="$1" \
 com.sun.japex.Japex \
 testcfg/xslt-all.xml

echo "Done!";
