#/bin/shell

usage() {
cat << EOF
usage: $0 options

OPTIONS:
    -d       Dry run (ie do not deploy or prune older releases)
    -t       Test assembly with default Maven repo (slow)
    -T       Test assembly with empty Maven repo (very slow, but thorough)
    -v       Verbose
    -f       Force execution, ignoring normal version checks
    -h       Show this message

DESCRIPTION:
    Drives the overall Hudson continuous integration process. This script
    is designed so it can be manually executed by a committer if desired
    for testing purposes. The script produces assembly filenames that
    reflect local system time and a Git hash for easy identification. The
    overall process is as follow, with any failure causing an early exit:
      * Aborts if version != *.BUILD-SNAPSHOT (unless -f was indicated)
      * Performs a "mvn clean package" from the project root
      * Performs a "mvn clean site" from deployment-support
      * Performs "roo-deploy-dist.sh assembly" (with -T/t if requested)
      * Performs "mvn deploy" from the project root
      * Performs "roo-deploy-dist.sh deploy" to release the assembly
      * Removes older snapshot releases from S3

RETURNS:
    0 if successful or the version was != *.BUILD-SNAPSHOT
    1 if there was a failure of any kind

REQUIRES:
    See requirements in readme.txt for releasing (ie SSH, GPG etc)
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
    if [ "$DRY_RUN" = "0" ]; then
        type -P s3cmd &>/dev/null || { l_error "s3cmd not found. Aborting." >&2; exit 1; }
        S3CMD_OPTS=''
        if [ "$VERBOSE" = "1" ]; then
            S3CMD_OPTS="$S3CMD_OPTS -v"
        fi
        s3cmd $S3CMD_OPTS $@
        EXITED=$?
        if [[ ! "$EXITED" = "0" ]]; then
            l_error "s3cmd failed (exit code $EXITED)." >&2; exit 1;
        fi
    fi
}

VERBOSE='0'
DRY_RUN='0'
FORCE='0'
TEST=''
while getopts "vdhftT" OPTION
do
    case $OPTION in
        h)
            usage
            exit 1
            ;;
        d)
            DRY_RUN=1
            ;;
        v)
            VERBOSE=1
            ;;
        t)
            TEST="-t"
            ;;
        T)
            TEST="-T"
            ;;
        f)
            FORCE=1
            ;;
        ?)
            usage
            exit
            ;;
    esac
done

type -P mvn &>/dev/null || { l_error "mvn not found. Aborting." >&2; exit 1; }
type -P gpg &>/dev/null || { l_error "gpg not found. Aborting." >&2; exit 1; }
type -P s3cmd &>/dev/null || { l_error "s3cmd not found. Aborting." >&2; exit 1; }

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
TIMESTAMP=$(date --utc +"%Y%m%d.%H%M%S")
log "Timestamp......: $TIMESTAMP"

# Determine the assembly filename (we don't rely on Hudson variables, as we'd like this to work for normal developers as well)
SUFFIX="_$TIMESTAMP-${GIT_HASH:0:7}"
log "Suffix.........: $SUFFIX"

# Determine the version as required by the AWS dist.springframework.org "x-amz-meta-release.type" header
case $VERSION in
    *BUILD-SNAPSHOT) TYPE=snapshot;;
    *RC*) TYPE=milestone;;
    *M*) TYPE=milestone;;
    *RELEASE) TYPE=release;;
    *) l_error "Unsupported release type ($VERSION). Aborting." >&2; exit 1;;
esac
log "Release Type...: $TYPE"

# Gracefully abort if this isn't a BUILD-SNAPSHOT (unless the user has forced us to continue via -f)
if [[ "$FORCE" = "0" ]]; then
    if [[ ! "$TYPE" = "snapshot" ]]; then
        l_error "Gracefully aborting as not a snapshot ($VERSION)." >&2; exit 0;  # code 0 is correct, this is not a serious error
    fi
fi

# Setup correct options for a dry run vs normal run
if [[ "$DRY_RUN" = "0" ]]; then
    MAVEN_MAIN_OPTS='-e -B clean install'
    MAVEN_SITE_OPTS='-e -B clean site'
    MAVEN_DEPLOY_OPTS='-e -B deploy'
    ROO_DEPLOY_OPTS=''
else
    MAVEN_MAIN_OPTS='-e -B clean install'
    MAVEN_SITE_OPTS='-e -B clean site'
    MAVEN_DEPLOY_OPTS='never_invoked'
    ROO_DEPLOY_OPTS='-d'
fi

# Setup correct options for -v (verbose) (we default maven to quiet mode unless -v has been specified, as it's just too noisy)
if [[ "$VERBOSE" = "0" ]]; then
    MAVEN_MAIN_OPTS="$MAVEN_MAIN_OPTS -q"
    MAVEN_SITE_OPTS="$MAVEN_SITE_OPTS -q"
    MAVEN_DEPLOY_OPTS="$MAVEN_DEPLOY_OPTS -q"
else
    ROO_DEPLOY_OPTS="$ROO_DEPLOY_OPTS -v"
fi

pushd $ROO_HOME/ &>/dev/null
# Do the initial mvn packaging (but don't dpeloy)
mvn $MAVEN_MAIN_OPTS
EXITED=$?
if [[ ! "$EXITED" = "0" ]]; then
    l_error "Maven main build failed (exit code $EXITED)." >&2; exit 1;
fi

# Build reference guide docs (and deploy them; it's not a big deal if the later tests fail but the docs were updated)
pushd $ROO_HOME/deployment-support &>/dev/null
mvn $MAVEN_SITE_OPTS
EXITED=$?
if [[ ! "$EXITED" = "0" ]]; then
    l_error "Maven site build failed (exit code $EXITED)." >&2; exit 1;
fi

# Build (and test if user used -T or -t) the assembly
./roo-deploy-dist.sh -c assembly -s $SUFFIX $ROO_DEPLOY_OPTS $TEST
EXITED=$?
if [[ ! "$EXITED" = "0" ]]; then
    l_error "roo-deploy -c assembly failed (exit code $EXITED)." >&2; exit 1;
fi

# Deploy the Maven JARs (we do this first to avoid people getting the latest snapshot assembly ZIP before the latest annotation JAR is visible)
if [[ "$DRY_RUN" = "0" ]]; then
    pushd $ROO_HOME/ &>/dev/null
    mvn $MAVEN_DEPLOY_OPTS
    EXITED=$?
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "Maven deploy failed (exit code $EXITED)." >&2; exit 1;
    fi
    popd &>/dev/null
fi

# Deploy the assembly so people can download it (note roo-deploy-dist.sh will prune old snapshots from the download site automatically)
if [[ "$DRY_RUN" = "0" ]]; then
    ./roo-deploy-dist.sh -c deploy -s $SUFFIX $ROO_DEPLOY_OPTS
    EXITED=$?
    if [[ ! "$EXITED" = "0" ]]; then
        l_error "roo-deploy -c deploy failed (exit code $EXITED)." >&2; exit 1;
    fi
fi

# Prune some old releases. We can rely on the fact CI runs at least every 24 hours and thus we can prune anything older than say 3 days
if [[ "$DRY_RUN" = "0" ]]; then
    OK_DATE_0=`date +%Y-%m-%d`
    OK_DATE_1=`date --date '-1 day' +%Y-%m-%d`
    OK_DATE_2=`date --date '-2 day' +%Y-%m-%d`
    log "Obtaining listing of all snapshot resources on S3"
    s3_execute ls -r s3://spring-roo-repository.springsource.org/snapshot > /tmp/dist_snapshots.txt
    log "Retain Dates...: $OK_DATE_0 $OK_DATE_1 $OK_DATE_2"
    log "S3 Found.......: `grep -v "/$" /tmp/dist_snapshots.txt | wc -l`"
    grep -v -e $OK_DATE_0 -e $OK_DATE_1 -e $OK_DATE_2 /tmp/dist_snapshots.txt > /tmp/dist_delete.txt
    log "S3 To Delete...: `grep -v "/$" /tmp/dist_delete.txt | wc -l`"
    cat /tmp/dist_delete.txt | cut -c "30-" > /tmp/dist_delete_cut.txt
    for filename in `grep -v "/$" /tmp/dist_delete_cut.txt`; do
        s3_execute del "$filename"
    done
    log "Pruning old snapshots completed successfully"
fi

# Return to the original directory
popd &>/dev/null
popd &>/dev/null

