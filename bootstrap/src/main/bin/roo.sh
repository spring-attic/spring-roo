#!/bin/sh

PRG="$0"
SYM_LINK=`readlink "$PRG"`
BIN_HOME=`dirname "$SYM_LINK"`
ROO_HOME=`dirname "$BIN_HOME"`

# echo Resolved SYM_LINK: $SYM_LINK
# echo Resolved BIN_HOME: $BIN_HOME
# echo Resolved ROO_HOME: $ROO_HOME

java -Djava.ext.dirs="$ROO_HOME/lib:$ROO_HOME/dist" org.springframework.roo.bootstrap.Bootstrap "classpath:/roo-bootstrap.xml"