#!/bin/bash

Echo "Sign jars"
for file in dist/*.jar;
  gpg -ab $file
;

for file in dist/*.pom;
  gpg -ab $file
;

