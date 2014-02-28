#!/bin/bash

Echo "Sign jars"
for file in dist/*.jar
do
  gpg -ab $file
done

for file in dist/*.pom
do
  gpg -ab $file
done

