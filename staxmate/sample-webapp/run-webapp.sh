#!/bin/sh

java -Djetty.home=./jetty \
 -Djetty.port=7272 -jar jetty/start.jar \
jetty/jetty.xml 
