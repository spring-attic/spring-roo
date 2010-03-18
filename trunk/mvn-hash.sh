#!/bin/bash

if [ -n "$1" ]
then
  root_dir=$1
else
  root_dir=.
fi

echo $root_dir

find $root_dir -name '*.pom' | while read FILE
do
  echo $FILE
  checksum=`md5sum $FILE`
  echo -n ${checksum%% *} > $FILE.md5
  checksum=`sha1sum $FILE`
  echo -n ${checksum%% *} > $FILE.sha1
done

find $root_dir -name '*.jar' | while read FILE
do
  echo $FILE
  checksum=`md5sum $FILE`
  echo -n ${checksum%% *} > $FILE.md5
  checksum=`sha1sum $FILE`
  echo -n ${checksum%% *} > $FILE.sha1
done

