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

# Build a classpath containing our two magical startup JARs (we look for " /" as per ROO-905)
ROO_CP=`echo "$ROO_HOME"/bin/*.jar | sed 's/ \//:\//g'`
# echo ROO_CP: $ROO_CP

# Store file locations in variables to facilitate Cygwin conversion if needed

ROO_OSGI_FRAMEWORK_STORAGE="$ROO_HOME/cache"
# echo "ROO_OSGI_FRAMEWORK_STORAGE: $ROO_OSGI_FRAMEWORK_STORAGE"

ROO_AUTO_DEPLOY_DIRECTORY="$ROO_HOME/bundle"
# echo "ROO_AUTO_DEPLOY_DIRECTORY: $ROO_AUTO_DEPLOY_DIRECTORY"

ROO_CONFIG_FILE_PROPERTIES="$ROO_HOME/conf/config.properties"
# echo "ROO_CONFIG_FILE_PROPERTIES: $ROO_CONFIG_FILE_PROPERTIES"

LOG_CONFIG_FILE_PROPERTIES="$ROO_HOME/conf/logging.properties"
# echo "LOG_CONFIG_FILE_PROPERTIES: $LOG_CONFIG_FILE_PROPERTIES"

ROO_DEVELOPMENT_MODE=false
# echo "ROO_DEVELOPMENT_MODE : $ROO_DEVELOPMENT_MODE"

cygwin=false;
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;
esac

if [ "$cygwin" = "true" ]; then
    export ROO_HOME=`cygpath -wp "$ROO_HOME"`
    export ROO_CP=`cygpath -wp "$ROO_CP"`
    export ROO_OSGI_FRAMEWORK_STORAGE=`cygpath -wp "$ROO_OSGI_FRAMEWORK_STORAGE"`
    export ROO_AUTO_DEPLOY_DIRECTORY=`cygpath -wp "$ROO_AUTO_DEPLOY_DIRECTORY"`
    export ROO_CONFIG_FILE_PROPERTIES=`cygpath -wp "$ROO_CONFIG_FILE_PROPERTIES"`
    # echo "Modified ROO_HOME: $ROO_HOME"
    # echo "Modified ROO_CP: $ROO_CP"
    # echo "Modified ROO_OSGI_FRAMEWORK_STORAGE: $ROO_OSGI_FRAMEWORK_STORAGE"
    # echo "Modified ROO_AUTO_DEPLOY_DIRECTORY: $ROO_AUTO_DEPLOY_DIRECTORY"
    # echo "Modified ROO_CONFIG_FILE_PROPERTIES: $ROO_CONFIG_FILE_PROPERTIES"
fi

# make sure to disable the flash message feature for the default OSX terminal, we recommend to use a ANSI compliant terminal such as iTerm if flash message support is desired
APPLE_TERMINAL=false;
if [ "$TERM_PROGRAM" = "Apple_Terminal" ]; then
	APPLE_TERMINAL=true
fi

ANSI="-Droo.console.ansi=true"
LOG="-Dorg.eclipse.jetty.util.log.class=org.eclipse.jetty.util.log.Slf4jLog -Dorg.apache.felix.http.log.jul=true -Djava.util.logging.config.file=${LOG_CONFIG_FILE_PROPERTIES}"
# Hop, hop, hop...
java $LOG -Dis.apple.terminal=$APPLE_TERMINAL $ROO_OPTS $ANSI -Droo.args="$*" -DdevelopmentMode=$ROO_DEVELOPMENT_MODE -Dorg.osgi.framework.storage="$ROO_OSGI_FRAMEWORK_STORAGE" -Dorg.osgi.framework.system.packages.extra=org.w3c.dom.traversal -Dfelix.auto.deploy.dir="$ROO_AUTO_DEPLOY_DIRECTORY" -Dfelix.config.properties="file:$ROO_CONFIG_FILE_PROPERTIES" -cp "$ROO_CP" org.springframework.roo.bootstrap.Main
EXITED=$?
# echo Roo exited with code $EXITED
