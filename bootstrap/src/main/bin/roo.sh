#!/bin/sh

if [ -z "$ROO_HOME" ]; then
    echo "ERROR: ROO_HOME environment variable is not set"
else
    java -Djava.ext.dirs=$ROO_HOME/lib:$ROO_HOME/dist org.springframework.roo.bootstrap.Bootstrap "classpath:/roo-bootstrap.xml"
fi
