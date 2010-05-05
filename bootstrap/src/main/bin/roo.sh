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
# echo "JAVA_HOME $JAVA_HOME"

cygwin=false;
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;
esac

if [ "$cygwin" = "true" ]; then
	export ROO_HOME="`cygpath -wp $ROO_HOME`"
	export JAVA_HOME="`cygpath -wp "$JAVA_HOME"`"
	export EXT_DIR=""$ROO_HOME\\dist";"$ROO_HOME\\lib";"$ROO_HOME\\work";"$JAVA_HOME\\jre\\lib\\ext""
	# echo "Modified ROO_HOME: $ROO_HOME"
	# echo "Modified JAVA_HOME: $JAVA_HOME"
else
	export EXT_DIR="$ROO_HOME/dist:$ROO_HOME/lib:$ROO_HOME/work:$JAVA_HOME/jre/lib/ext"
fi

# echo "Final EXT_DIR: $EXT_DIR"

while true; do
	java -Djava.ext.dirs="$EXT_DIR" $ROO_OPTS -Droo.home="$ROO_HOME" org.springframework.roo.bootstrap.Bootstrap "classpath:roo-bootstrap.xml" $@
    EXITED=$?
    # echo Exited with $EXITED
    if [ $EXITED -ne 100 -a $EXITED -ne 200 ]; then
		break
	fi
done