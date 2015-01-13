#/bin/shell

usage() {
cat << EOF
usage: $0 options

OPTIONS:
    -c CMD   Command name (CMD = assembly|deploy|next)
    -n VER   Next version number (for "next" command)
    -s ID    Add suffix to assembly filename (for "assembly" command)
    -t       Test assembly with default Maven repo (for "assembly" command)
    -T       Test assembly with empty Maven repo (for "assembly" command)
    -d       Dry run (for "deploy" command)
    -v       Verbose
    -h       Show this message

COMMAND NAMES:
    "assembly" -> creates a release ZIP (use after "mvn site")
    "deploy" -> deploys the release ZIP (use after "assembly")
    "next" -> modifies Roo version numbers to that given by -n

DESCRIPTION:
    Automates building deployment ZIPs and allow later deployment.
    Note "assembly" and "deploy" always test ZIP integrity via GPG and SHA.
    The -t and -T options are both slow as they make and run user projects.
    The -T option is extremely slow as it forces Maven to download everything.
    Use "-c assembly -tv" to build and test the assembly in most cases.
    Use "-c assembly -s _12-24" to add "_12-24" to the assembly filename.
    Use "-c deploy -vd" to see what will happen, but without uploading.
    Use "-c next -n 3.4.5.RC1" to change next version to 3.4.5.RC1.

REQUIRES:
    s3cmd (ls should list the SpringSource buckets)
    ~/.m2/settings.xml contains a <gpg.passphrase> for the GPG key
    mvn and wget (only required if using -t or -T)
    sha1sum, zip, unzip and other common *nix commands
EOF
}

log() {
    if [ "$VERBOSE" = "1" ]; then
        echo "$@"
    fi
}

l_error() {
    echo "### ERROR: $@"
}

s3_execute() {
    type -P s3cmd &>/dev/null || { l_error "s3cmd not found. Aborting." >&2; exit 1; }
    S3CMD_OPTS=''
    if [ "$DRY_RUN" = "1" ]; then
        S3CMD_OPTS="$S3CMD_OPTS --dry-run"
    fi
    if [ "$VERBOSE" = "1" ]; then
        S3CMD_OPTS="$S3CMD_OPTS -v"
    fi
    s3cmd $S3CMD_OPTS $@
    EXITED=$?
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "s3cmd failed (exit code $EXITED)." >&2; exit 1;
    fi
}

quick_zip_gpg_tests() {
    pushd $DIST_DIR &>/dev/null

    # Test the hash worked OK
    # (for script testing purposes only:) sed -i 's/0/1/g' $ASSEMBLY_SHA
    sha1sum --status --check $ASSEMBLY_SHA
    if [[ ! "$?" = "0" ]]; then
        l_error "sha1sum verification of $ASSEMBLY_SHA failed" >&2; exit 1;
    fi
    log "sha1sum test pass: $ASSEMBLY_SHA"

    # Test the signature is OK
    # (for script testing purposes only:) sed -i 's/0/1/g' $ASSEMBLY_ASC
    if [ "$VERBOSE" = "1" ]; then
        gpg -v --batch --verify $ASSEMBLY_ASC
        EXITED=$?
    else
        gpg --batch --verify $ASSEMBLY_ASC &>/dev/null
        EXITED=$?
    fi
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "GPG detached signature verification failed (gpg exit code $EXITED)." >&2; exit 1;
    fi
    log "gpg signature verification test pass: $ASSEMBLY_ASC"

    popd &>/dev/null
}

load_roo_build_and_test() {
    type -P mvn &>/dev/null || { l_error "mvn not found. Aborting." >&2; exit 1; }
    log "Beginning test script: $@"
    rm -rf /tmp/rootest
    mkdir -p /tmp/rootest
    pushd /tmp/rootest &>/dev/null
    if [ "$VERBOSE" = "1" ]; then
        $ROO_CMD $@
        EXITED=$?
    else
        $ROO_CMD $@ &>/dev/null
        EXITED=$?
    fi
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "Test failed: $ROO_CMD $@" >&2; exit 1;
    fi
    if [ -f /tmp/rootest/src/main/resources/log4j.properties ]; then
        sed -i 's/org.apache.log4j.ConsoleAppender/org.apache.log4j.varia.NullAppender/g' /tmp/rootest/src/main/resources/log4j.properties
    fi
    $MVN_CMD -e -B clean test
    if [[ ! "$?" = "0" ]]; then
        l_error "Test failed: $MVN_CMD -e -B clean test" >&2; exit 1;
    fi
    popd &>/dev/null
}

tomcat_stop_start_get_stop() {
    type -P wget &>/dev/null || { l_error "wget not found. Aborting." >&2; exit 1; }
    log "Performing MVC testing; expecting GET success for URL: $@"
    pushd /tmp/rootest &>/dev/null
    if [ "$TERM_PROGRAM" = "Apple_Terminal" ]; then
        MVN_TOMCAT_PID=`ps -e | grep Launcher | grep tomcat7:run | cut -b "1-6" | sed "s/ //g"`
    else
        MVN_TOMCAT_PID=`ps -eo "%p %c %a" | grep Launcher | grep tomcat7:run | cut -b "1-6" | sed "s/ //g"`
    fi
    if [ ! "$MVN_TOMCAT_PID" = "" ]; then
        # doing a kill -9 as it was hanging around for some reason, when it really should have been killed by now
        log "kill -9 of old mvn tomcat7:run with PID $MVN_TOMCAT_PID"
        kill -9 $MVN_TOMCAT_PID
        sleep 5
    fi
    log "Invoking mvn tomcat7:run in background"
    $MVN_CMD -e -B -Dmaven.tomcat.port=8888 tomcat7:run &>/dev/null 2>&1 &
    WGET_OPTS="-q"
    if [ "$VERBOSE" = "1" ]; then
        WGET_OPTS="-v"
    fi
    wget $WGET_OPTS --retry-connrefused --tries=30 -O /tmp/rootest/wget.html $@
    EXITED=$?
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "wget failed: $@ (returned code $EXITED)" >&2; exit 1;
    fi
    if [ "$TERM_PROGRAM" = "Apple_Terminal" ]; then
        MVN_TOMCAT_PID=`ps -e | grep Launcher | grep tomcat7:run | cut -b "1-6" | sed "s/ //g"`
    else
        MVN_TOMCAT_PID=`ps -eo "%p %c %a" | grep Launcher | grep tomcat7:run | cut -b "1-6" | sed "s/ //g"`
    fi
    if [ ! "$MVN_TOMCAT_PID" = "" ]; then
        log "Terminating background mvn tomcat7:run process with PID $MVN_TOMCAT_PID"
        kill $MVN_TOMCAT_PID
        # no need to sleep, as we'll be at least running Roo between now and the next Tomcat start
    fi
    popd &>/dev/null
}

jetty_stop_start_get_stop() {
    type -P wget &>/dev/null || { l_error "wget not found. Aborting." >&2; exit 1; }
    log "Performing JSF testing; expecting GET success for URL: $@"
    pushd /tmp/rootest &>/dev/null
    if [ "$TERM_PROGRAM" = "Apple_Terminal" ]; then
        MVN_JETTY_PID=`ps -e | grep Launcher | grep jetty:run-exploded | cut -b "1-6" | sed "s/ //g"`
    else
        MVN_JETTY_PID=`ps -eo "%p %c %a" | grep Launcher | grep jetty:run-exploded | cut -b "1-6" | sed "s/ //g"`
    fi
    if [ ! "$MVN_JETTY_PID" = "" ]; then
        # doing a kill -9 as it was hanging around for some reason, when it really should have been killed by now
        log "kill -9 of old mvn jetty:run-exploded with PID $MVN_JETTY_PID"
        kill -9 $MVN_JETTY_PID
        sleep 5
    fi
    log "Invoking mvn jetty:run-exploded in background"
    $MVN_CMD -e -B -Djetty.port=8888 jetty:run-exploded &>/dev/null 2>&1 &
    WGET_OPTS="-q"
    if [ "$VERBOSE" = "1" ]; then
        WGET_OPTS="-v"
    fi
    wget $WGET_OPTS --retry-connrefused --tries=30 -O /tmp/rootest/wget.html $@
    EXITED=$?
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "wget failed: $@ (returned code $EXITED)" >&2; exit 1;
    fi
    if [ "$TERM_PROGRAM" = "Apple_Terminal" ]; then
        MVN_JETTY_PID=`ps -e | grep Launcher | grep jetty:run-exploded | cut -b "1-6" | sed "s/ //g"`
    else
        MVN_JETTY_PID=`ps -eo "%p %c %a" | grep Launcher | grep jetty:run-exploded | cut -b "1-6" | sed "s/ //g"`
    fi
    if [ ! "$MVN_JETTY_PID" = "" ]; then
        log "Terminating background mvn grep jetty:run-exploded process with PID $MVN_JETTY_PID"
        kill $MVN_JETTY_PID
        # no need to sleep, as we'll be at least running Roo between now and the next Jetty start
    fi
    popd &>/dev/null
}

pizzashop_tests() {
	type -P curl &>/dev/null || { l_error "curl not found. Aborting." >&2; exit 1; }
	log "Performing MVC REST testing;"
	pushd /tmp/rootest &>/dev/null
	if [ "$TERM_PROGRAM" = "Apple_Terminal" ]; then
        MVN_TOMCAT_PID=`ps -e | grep Launcher | grep tomcat7:run | cut -b "1-6" | sed "s/ //g"`
    else  
        MVN_TOMCAT_PID=`ps -eo "%p %c %a" | grep Launcher | grep tomcat7:run | cut -b "1-6" | sed "s/ //g"`
    fi
    if [ ! "$MVN_TOMCAT_PID" = "" ]; then
        # doing a kill -9 as it was hanging around for some reason, when it really should have been killed by now
        log "kill -9 of old mvn tomcat7:run with PID $MVN_TOMCAT_PID"
        kill -9 $MVN_TOMCAT_PID
        sleep 5
    fi
    log "Invoking mvn tomcat7:run in background"
    $MVN_CMD -e -B -Dmaven.tomcat.port=8888 tomcat7:run &>/dev/null 2>&1 &

    wget --retry-connrefused --tries=30 --header 'Accept: application/json' --quiet http://localhost:8888/pizzashop/bases 2>&1
	
	log "Testing RESTful POST to PizzaShop application"
	curl -H "Content-Type: application/json" -H "Accept: application/json" -o /tmp/rootest/curl.txt -i -s -X POST -d "{name: \"Thin Crust\"}" http://localhost:8888/pizzashop/bases
	EXITED=$?
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "curl failed: $@ (returned code $EXITED)" >&2; exit 1;
    fi
	head -n 1 /tmp/rootest/curl.txt | grep "201 Created"
	EXITED=$?
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "RESTful POST to PizzaShop application failed" >&2; exit 1;
    fi

	log "Testing RESTful array data POST to PizzaShop application"
	curl -H "Content-Type: application/json" -H "Accept: application/json" -o /tmp/rootest/curl.txt -i -s -X POST -d "[{name: \"Cheesy Crust\"},{name: \"Thick Crust\"}]" http://localhost:8888/pizzashop/bases/jsonArray	
	EXITED=$?
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "curl failed: $@ (returned code $EXITED)" >&2; exit 1;
    fi
	head -n 1 /tmp/rootest/curl.txt | grep "201 Created"
	EXITED=$?
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "RESTful array data POST to PizzaShop application failed" >&2; exit 1;
    fi

	log "Testing RESTful array data POST to PizzaShop application"
	curl -H "Content-Type: application/json" -H "Accept: application/json" -o /tmp/rootest/curl.txt -i -s -X POST -d "[{name: \"Fresh Tomato\"},{name: \"Prawns\"},{name: \"Mozarella\"},{name: \"Bogus\"}]" http://localhost:8888/pizzashop/toppings/jsonArray
	EXITED=$?
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "curl failed: $@ (returned code $EXITED)" >&2; exit 1;
    fi
	head -n 1 /tmp/rootest/curl.txt | grep "201 Created"
	EXITED=$?
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "RESTful array data POST to PizzaShop application failed" >&2; exit 1;
    fi

	log "Testing RESTful PUT to PizzaShop application"
    curl -i -s -X PUT -H "Content-Type: application/json" -H "Accept: application/json" -o /tmp/rootest/curl.txt -d "{id:6,name:\"Mozzarella\",version:1}" http://localhost:8888/pizzashop/toppings/6
	EXITED=$?
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "curl failed: $@ (returned code $EXITED)" >&2; exit 1;
    fi
	head -n 1 /tmp/rootest/curl.txt | grep "200 OK"
	EXITED=$?
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "RESTful PUT to PizzaShop application failed" >&2; exit 1;
    fi

	log "Testing RESTful GET to PizzaShop application"
	curl -i -s -H "Accept: application/json" -o /tmp/rootest/curl.txt http://localhost:8888/pizzashop/toppings
	EXITED=$?
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "curl failed: $@ (returned code $EXITED)" >&2; exit 1;
    fi
	head /tmp/rootest/curl.txt | grep "Tomato" | grep "Prawns" | grep "Mozzarella"
	EXITED=$?
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "RESTful GET to PizzaShop application failed" >&2; exit 1;
    fi

	log "Testing RESTful GET to PizzaShop application"
	curl -i -s -H "Accept: application/json" -o /tmp/rootest/curl.txt http://localhost:8888/pizzashop/toppings/6
	EXITED=$?
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "curl failed: $@ (returned code $EXITED)" >&2; exit 1;
    fi
	head /tmp/rootest/curl.txt | grep "Mozzarella"
	EXITED=$?
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "RESTful GET to PizzaShop application failed" >&2; exit 1;
    fi

	log "Testing RESTful complex POST to PizzaShop application"
	curl -i -s -X POST -H "Content-Type: application/json" -H "Accept: application/json"  -o /tmp/rootest/curl.txt -d "{name:\"Napolitana\",price:7.5,base:{id:1},toppings:[{name: \"Anchovy fillets\"},{name: \"Mozzarella\"}]}" http://localhost:8888/pizzashop/pizzas
	EXITED=$?
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "curl failed: $@ (returned code $EXITED)" >&2; exit 1;
    fi
	head /tmp/rootest/curl.txt | grep "201 Created"
	EXITED=$?
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "RESTful complex POST to PizzaShop application failed" >&2; exit 1;
    fi

	#log "Testing RESTful complex POST to PizzaShop application"
	#curl -i -s -X POST -H "Content-Type: application/json" -H "Accept: application/json" -o /tmp/rootest/curl.txt -d "{name:\"Stefan\",total:7.5,address:\"Sydney, AU\",deliveryDate:1314595427866,id:{shopCountry:\"AU\",shopCity:\"Sydney\",shopName:\"Pizza Pan 1\"},pizzas:[{id:8,version:1}]}" http://localhost:8888/pizzashop/pizzaorders	
	#EXITED=$?
    #if [[ ! "$EXITED" = "0" ]]; then
    #    l_error "curl failed: $@ (returned code $EXITED)" >&2; exit 1;
    #fi
	#head /tmp/rootest/curl.txt | grep "201 Created"
	#EXITED=$?
    #if [[ ! "$EXITED" = "0" ]]; then
    #    l_error "RESTful complex POST to PizzaShop application failed" >&2; exit 1;
    #fi

    if [ "$TERM_PROGRAM" = "Apple_Terminal" ]; then
        MVN_TOMCAT_PID=`ps -e | grep Launcher | grep tomcat7:run | cut -b "1-6" | sed "s/ //g"`
    else
        MVN_TOMCAT_PID=`ps -eo "%p %c %a" | grep Launcher | grep tomcat7:run | cut -b "1-6" | sed "s/ //g"`
    fi
    if [ ! "$MVN_TOMCAT_PID" = "" ]; then
        log "Terminating background mvn tomcat7:run process with PID $MVN_TOMCAT_PID"
        kill $MVN_TOMCAT_PID
        # no need to sleep, as we'll be at least running Roo between now and the next Tomcat start
    fi
	popd &>/dev/null
}

COMMAND=
NEXT=
VERBOSE='0'
DRY_RUN='0'
TEST='0'
SUFFIX=''
while getopts "s:c:n:tTvdh" OPTION
do
    case $OPTION in
        h)
            usage
            exit 1
            ;;
        c)
            COMMAND=$OPTARG
            ;;
        n)
            NEXT=$OPTARG
            ;;
        s)
            SUFFIX=$OPTARG
            ;;
        d)
            DRY_RUN=1
            ;;
        t)
            TEST=1
            ;;
        T)
            TEST=2
            ;;
        v)
            VERBOSE=1
            ;;
        ?)
            usage
            exit
            ;;
    esac
done

if [[ -z $COMMAND ]]; then
    usage
    exit 1
fi

if [[ "$COMMAND" = "assembly" ]] || [[ "$COMMAND" = "deploy" ]] || [[ "$COMMAND" = "next" ]]; then
    log "Command........: $COMMAND"
else
    usage
    exit 1
fi

type -P zip &>/dev/null || { l_error "zip not found. Aborting." >&2; exit 1; }
type -P unzip &>/dev/null || { l_error "unzip not found. Aborting." >&2; exit 1; }
type -P sha1sum &>/dev/null || { l_error "sha1sum not found. Aborting." >&2; exit 1; }

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
log "Location.......: $ROO_HOME"

# Compute version and Git-related info
GIT_HASH=`git log "--pretty=format:%H" -n1 $ROO_HOME`
log "Git Hash.......: $GIT_HASH"
GIT_TAG=`git tag -l "*\.*\.*\.*" $ROO_HOME | tail -n 1`
log "Git Tag........: $GIT_TAG"
VERSION=`grep "<version>" $ROO_HOME/pom.xml | head -n 1 | sed 's/<version>//' | sed 's/<\/version>//' | sed 's/ //g'`
log "Version........: $VERSION"
SHORT_VERSION=`echo $VERSION | sed 's/\([0-9].[0-9].[0-9]\).*\.BUILD-SNAPSHOT/\1/'`
log "Short Version..: $SHORT_VERSION"

# Determine the version as required by the AWS spring-roo-repository.springsource.org "x-amz-meta-release.type" header
case $VERSION in
    *BUILD-SNAPSHOT) TYPE=snapshot;;
    *RC*) TYPE=milestone;;
    *M*) TYPE=milestone;;
    *RELEASE) TYPE=release;;
    *) l_error "Unsupported release type ($VERSION). Aborting." >&2; exit 1;;
esac
log "Release Type...: $TYPE"

# Product release identifier
RELEASE_IDENTIFIER="spring-roo-$VERSION"
log "Release ID.....: $RELEASE_IDENTIFIER"

# File locations
TARGET_DIR="$ROO_HOME/target/roo-deploy"
log "Target Dir.....: $TARGET_DIR"
WORK_DIR="$ROO_HOME/target/roo-deploy/work/$RELEASE_IDENTIFIER"
log "Work Dir.......: $WORK_DIR"
DIST_DIR="$ROO_HOME/target/roo-deploy/dist"
log "Output Dir.....: $DIST_DIR"
ASSEMBLY_ZIP="$DIST_DIR/$RELEASE_IDENTIFIER$SUFFIX.zip"
log "Assembly Zip...: $ASSEMBLY_ZIP"
ASSEMBLY_SHA="$DIST_DIR/$RELEASE_IDENTIFIER$SUFFIX.zip.sha1"
log "Assembly SHA...: $ASSEMBLY_SHA"
ASSEMBLY_ASC="$DIST_DIR/$RELEASE_IDENTIFIER$SUFFIX.zip.asc"
log "Assembly ASC...: $ASSEMBLY_ASC"

if [[ "$COMMAND" = "assembly" ]]; then

    if [ ! -f $ROO_HOME/target/all/org.springframework.roo.bootstrap-*.jar ]; then
        l_error "JARs missing; you must run mvn package before attempting assembly"
        exit 1
    fi
    if [ ! -f $ROO_HOME/deployment-support/target/site/reference/pdf/spring-roo-docs.pdf ]; then
        l_error "Site docs missing; you must run mvn site before attempting assembly"
        exit 1
    fi
    log "Cleaning $TARGET_DIR"
    rm -rf $TARGET_DIR
    log "Cleaning $WORK_DIR"
    rm -rf $WORK_DIR

    # Create a directory structure to match the desired assembly ZIP output
    mkdir -p $WORK_DIR/annotations
    mkdir -p $WORK_DIR/bin
    mkdir -p $WORK_DIR/bundle
    mkdir -p $WORK_DIR/conf
    mkdir -p $WORK_DIR/docs/pdf
    mkdir -p $WORK_DIR/docs/html
    mkdir -p $WORK_DIR/legal
    mkdir -p $WORK_DIR/samples
    cp $ROO_HOME/annotations/target/*-$VERSION.jar $WORK_DIR/annotations
    cp $ROO_HOME/target/all/*.jar $WORK_DIR/bundle
    rm $WORK_DIR/bundle/org.springframework.roo.annotations-$VERSION.jar
    rm $WORK_DIR/bundle/*junit*.jar
    rm $WORK_DIR/bundle/*jsch*.jar
    rm $WORK_DIR/bundle/*jgit*.jar
    rm $WORK_DIR/bundle/*git*.jar
    rm $WORK_DIR/bundle/*op4j*.jar
    rm $WORK_DIR/bundle/*aopalliance-*.jar
    rm $WORK_DIR/bundle/jackson-*.jar
    rm $WORK_DIR/bundle/jcl-over-slf4j-*.jar
    rm $WORK_DIR/bundle/servlet-api-*.jar
    rm $WORK_DIR/bundle/slf4j-*.jar
    rm $WORK_DIR/bundle/spring-*.jar
    # These have to be removed as the Cloud Foundry add-on requires dependencies that are not bundled and thus must be installed via the shell.
    rm $WORK_DIR/bundle/*cloud.foundry*.jar
    rm $WORK_DIR/bundle/*cloud-foundry-api*.jar
    rm $WORK_DIR/bundle/*AppCloudClient*.jar
    mv $WORK_DIR/bundle/org.springframework.roo.bootstrap-*.jar $WORK_DIR/bin
    mv $WORK_DIR/bundle/org.apache.felix.framework-*.jar $WORK_DIR/bin
    cp $ROO_HOME/bootstrap/src/main/bin/* $WORK_DIR/bin
    chmod 775 $WORK_DIR/bin/*.sh
    cp $ROO_HOME/bootstrap/src/main/conf/* $WORK_DIR/conf
    cp $ROO_HOME/bootstrap/readme.txt $WORK_DIR/
    cp `find $ROO_HOME -iname legal-\*.txt` $WORK_DIR/legal
    cp `find $ROO_HOME -iname \*.roo | grep -v "/target/"` $WORK_DIR/samples
    cp -r $ROO_HOME/deployment-support/target/site/reference/pdf/ $WORK_DIR/docs
    cp -r $ROO_HOME/deployment-support/target/site/reference/html/ $WORK_DIR/docs

    # Prepare to write the ZIP
    log "Cleaning $DIST_DIR" 
    rm -rf $DIST_DIR
    mkdir -p $DIST_DIR

    # Change directories to avoid absolute paths
    pushd $WORK_DIR/.. &>/dev/null
    log "Running ZIP from `pwd`"
    ZIP_OPTS='-q'
    if [ "$VERBOSE" = "1" ]; then
        ZIP_OPTS='-v'
    fi
    log "ZIP command: zip $ZIP_OPTS $ASSEMBLY_ZIP -r $RELEASE_IDENTIFIER"
    zip $ZIP_OPTS $ASSEMBLY_ZIP -r $RELEASE_IDENTIFIER
    EXITED=$?
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "ZIP process failed (zip exit code $EXITED)." >&2; exit 1;
    fi

    # Hash the ZIP
    pushd $DIST_DIR &>/dev/null
    sha1sum *.zip > $ASSEMBLY_SHA

    # Return to the original directory
    popd &>/dev/null
    popd &>/dev/null

    if [ ! "$TEST" = "0" ]; then
        log "Unzipping Roo distribution to test area"
        rm -rf /tmp/$RELEASE_IDENTIFIER
        ZIP_OPTS='-qq'
        if [ "$VERBOSE" = "1" ]; then
            ZIP_OPTS=''
        fi
        unzip $ZIP_OPTS $ASSEMBLY_ZIP -d /tmp/
        ROO_CMD="/tmp/$RELEASE_IDENTIFIER/bin/roo.sh"
        log "Roo command....: $ROO_CMD"

        # Setup Maven and the active repository (we must ensure the annotation JAR matches that in the assembly; it may not have yet been deployed publicly as it's just being tested at present)
        MVN_CMD='mvn'
        if [[ "$TEST" = "2" ]]; then
            MVN_CMD="mvn -gs /tmp/settings.xml"
            echo "<settings><localRepository>/tmp/repository</localRepository></settings>" > /tmp/settings.xml
            rm -rf /tmp/repository
            mkdir -p /tmp/repository/org/springframework/roo/org.springframework.roo.annotations/$VERSION/
            cp /tmp/$RELEASE_IDENTIFIER/annotations/* /tmp/repository/org/springframework/roo/org.springframework.roo.annotations/$VERSION/
        else
            cp /tmp/$RELEASE_IDENTIFIER/annotations/* ~/.m2/repository/org/springframework/roo/org.springframework.roo.annotations/$VERSION/
        fi
        if [ "$VERBOSE" = "0" ]; then
            MVN_CMD="$MVN_CMD -q"
        fi

        load_roo_build_and_test script vote.roo
        tomcat_stop_start_get_stop http://localhost:8888/vote

        load_roo_build_and_test script clinic.roo
        tomcat_stop_start_get_stop http://localhost:8888/petclinic

        load_roo_build_and_test script wedding.roo
        tomcat_stop_start_get_stop http://localhost:8888/wedding

		    load_roo_build_and_test script pizzashop.roo
        tomcat_stop_start_get_stop http://localhost:8888/pizzashop
		    pizzashop_tests

        load_roo_build_and_test script bikeshop.roo
        jetty_stop_start_get_stop http://localhost:8888/bikeshop/pages/main.jsf

        load_roo_build_and_test script multimodule.roo
        tomcat_stop_start_get_stop http://localhost:8888/mvc

        load_roo_build_and_test script embedding.roo
        tomcat_stop_start_get_stop http://localhost:8888/embedding

        log "Removing Roo distribution from test area"
        rm -rf /tmp/$RELEASE_IDENTIFIER
        log "Completed tests successfully"
    fi
fi

if [[ "$COMMAND" = "deploy" ]]; then
    type -P s3cmd &>/dev/null || { l_error "s3cmd not found. Aborting." >&2; exit 1; }

    if [ ! -f $ASSEMBLY_ZIP ]; then
        l_error "Unable to find $ASSEMBLY_ZIP"
        exit 1
    fi
    if [ ! -f $ASSEMBLY_SHA ]; then
        l_error "Unable to find $ASSEMBLY_SHA"
        exit 1
    fi

    quick_zip_gpg_tests

    ZIP_FILENAME=`basename $ASSEMBLY_ZIP`
    PROJECT_NAME="Spring Roo"
    AWS_PATH="s3://spring-roo-repository.springsource.org/$TYPE/ROO/"
    log "AWS bundle.ver.: $VERSION"
    log "AWS rel.type...: $TYPE"
    log "AWS pkg.f.name.: $ZIP_FILENAME"
    log "AWS proj.name..: $PROJECT_NAME"
    log "AWS Path.......: $AWS_PATH"

    type -P s3cmd &>/dev/null || { l_error "s3cmd not found. Aborting." >&2; exit 1; }
    S3CMD_OPTS=''
    if [ "$DRY_RUN" = "1" ]; then
        S3CMD_OPTS="$S3CMD_OPTS --dry-run"
    fi
    if [ "$VERBOSE" = "1" ]; then
        S3CMD_OPTS="$S3CMD_OPTS -v"
    fi
    s3cmd $S3CMD_OPTS put --acl-public \
        "--add-header=x-amz-meta-bundle.version:$VERSION" \
        "--add-header=x-amz-meta-release.type:$TYPE" \
        "--add-header=x-amz-meta-package.file.name:$ZIP_FILENAME" \
        "--add-header=x-amz-meta-project.name:$PROJECT_NAME" \
        $ASSEMBLY_ZIP $AWS_PATH
    EXITED=$?
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "s3cmd failed (exit code $EXITED)." >&2; exit 1;
    fi

    s3_execute put --acl-public $ASSEMBLY_SHA $AWS_PATH
    s3_execute put --acl-public $ASSEMBLY_ASC $AWS_PATH

    # Clean up old snapshot releases (if we just performed a snapshot release)
    if [[ "$TYPE" = "snapshot" ]]; then
        s3_execute ls s3://spring-roo-repository.springsource.org/snapshot/ROO/ | grep '.zip$' | cut -c "30-"> /tmp/dist_all.txt
        tail -n 5 /tmp/dist_all.txt > /tmp/dist_to_keep.txt
        cat /tmp/dist_all.txt /tmp/dist_to_keep.txt | sort | uniq -u > /tmp/dist_to_delete.txt
        for url in `cat /tmp/dist_to_delete.txt`; do
            s3_execute del "$url"
            s3_execute del "$url.asc"
            s3_execute del "$url.sha1"
        done
        rm /tmp/dist_*.txt
    fi
fi

if [[ "$COMMAND" = "next" ]]; then
    # We only need to change the _first_ <version> element in each pom.xml to complete a version update
    find $ROO_HOME -iname pom.xml -print0 | while read -d $'\0' file
    do
        log "Updating $file"
        sed -i "0,/<version>.*<\/version>/s//<version>$NEXT<\/version>/" $file
    done
    log "Updating project templates"
    sed -i "s/<roo.version>.*<\/roo.version>/<roo.version>$NEXT<\/roo.version>/" `find $ROO_HOME -iname *-template.xml`
    log "Updating documentation"
    sed -i "s/<releaseinfo>.*<\/releaseinfo>/<releaseinfo>$NEXT<\/releaseinfo>/" $ROO_HOME/deployment-support/src/site/docbook/reference/index.xml
fi

