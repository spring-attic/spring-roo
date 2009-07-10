#!/bin/sh

PRG="$0"

while [ -h "$PRG" ]; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`/"$link"
    fi
done
ROO_HOME=`dirname "$PRG"`

# Absolute path
ROO_HOME=`cd "$ROO_HOME/.." ; pwd`

# echo Resolved ROO_HOME: $ROO_HOME

java -Djava.ext.dirs="$ROO_HOME/lib:$ROO_HOME/dist:$JAVA_HOME/jre/lib/ext" org.springframework.roo.bootstrap.Bootstrap "classpath:/roo-bootstrap.xml"
