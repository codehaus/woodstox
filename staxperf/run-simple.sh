#!/bin/sh

#-server\

java -XX:CompileThreshold=2000 -server \
 -cp \
:build/classes\
  $*
