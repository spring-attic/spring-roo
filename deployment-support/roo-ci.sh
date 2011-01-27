#/bin/shell

usage() {
cat << EOF
usage: $0 options

OPTIONS:
    -d       Dry run (ie do not deploy)
    -f       Force execution, ignoring version checks
    -v       Verbose
    -h       Show this message

DESCRIPTION:
    Drives the overall Hudson continuous integration process. This script
    is designed so it can be manually executed by a committer if desired
    for testing purposes. The script produces assembly filenames that
    reflect local system time and a Git hash for easy identification. The
    overall process is as follow, with any failure causing an early exit:
      * Aborts if version != *.BUILD-SNAPSHOT (unless -f was indicated)
      * Performs a "mvn clean package deploy" from the project root
      * Performs a "mvn clean site" from deployment-support
      * Performs "roo-deploy.sh" assembly and deploy processes

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

VERBOSE='0'
DRY_RUN='0'
FORCE='0'
while getopts "vdhf" OPTION
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
TIMESTAMP=$(date +"%Y%m%d.%H%M%S")
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
    MAVEN_MAIN_OPTS='-e -B clean package deploy'
    MAVEN_SITE_OPTS='-e -B clean site'  # should be site site:deploy (deferred until SSH server inside VPN)
    ROO_DEPLOY_OPTS=''
else
    MAVEN_MAIN_OPTS='-e clean package'
    MAVEN_SITE_OPTS='-e clean site'
    ROO_DEPLOY_OPTS='-d'
fi

# Setup correct options for -v (verbose) (we default maven to quiet mode unless -v has been specified, as it's just too noisy)
if [[ "$VERBOSE" = "0" ]]; then
    MAVEN_MAIN_OPTS="$MAVEN_MAIN_OPTS -q"
    MAVEN_SITE_OPTS="$MAVEN_SITE_OPTS -q"
else
    ROO_DEPLOY_OPTS="$ROO_DEPLOY_OPTS -v"
fi

pushd $ROO_HOME/ &>/dev/null
mvn $MAVEN_MAIN_OPTS
EXITED=$?
if [[ ! "$EXITED" = "0" ]]; then
    l_error "Maven main build failed (exit code $EXITED)." >&2; exit 1;
fi

# Build (and probably deploy) reference guide docs
pushd $ROO_HOME/deployment-support &>/dev/null
mvn $MAVEN_SITE_OPTS
EXITED=$?
if [[ ! "$EXITED" = "0" ]]; then
    l_error "Maven site build failed (exit code $EXITED)." >&2; exit 1;
fi

./roo-deploy.sh -c assembly -s $SUFFIX $ROO_DEPLOY_OPTS
EXITED=$?
if [[ ! "$EXITED" = "0" ]]; then
    l_error "roo-deploy -c assembly failed (exit code $EXITED)." >&2; exit 1;
fi
./roo-deploy.sh -c deploy -s $SUFFIX $ROO_DEPLOY_OPTS
if [[ ! "$EXITED" = "0" ]]; then
    l_error "roo-deploy -c deploy failed (exit code $EXITED)." >&2; exit 1;
fi

# Return to the original directory
popd &>/dev/null
popd &>/dev/null

