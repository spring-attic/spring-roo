#/bin/shell

usage() {
cat << EOF
usage: $0 options

OPTIONS:
    -j JAR   Maven JAR to upload (a .pom should be present as well)
    -d       Dry run
    -v       Verbose
    -h       Show this message

DESCRIPTION:
    Copies the designated Maven JAR and POM to a work directory, signs it
    using GPG, and then uploads the resulting resources to
    http://spring-roo-repository.springsource.org/bundles.

    This simplifies the production of OBR and RooBot compliant bundles,
    assuming the designated Maven JAR is already a valid OSGi bundle.

    NOTE: You MUST ensure the -j JAR name is under a "repository" directory.
    A valid POM should also exist at the same location.

REQUIRES:
    s3cmd (ls should list the SpringSource buckets)
    ~/.m2/settings.xml contains a <gpg.passphrase> for the GPG key
EOF
}

log() {
    if [ "$VERBOSE" = "1" ]; then
        echo "$@"
    fi
}

l_error() {
    echo "### ERROR: $@" >&2
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

JAR=
VERBOSE='0'
DRY_RUN='0'
while getopts "j:vdh" OPTION
do
    case $OPTION in
        h)
            usage
            exit 1
            ;;
        j)
            JAR=$OPTARG
            ;;
        d)
            DRY_RUN=1
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

if [[ -z $JAR ]]; then
    usage
    exit 1
fi

if [[ ! -f $JAR ]]; then
    l_error "$JAR not found"
    exit 1
fi

type -P gpg &>/dev/null || { l_error "gpg not found. Aborting." >&2; exit 1; }
type -P sha1sum &>/dev/null || { l_error "sha1sum not found. Aborting." >&2; exit 1; }

PRG="$0"

# File locations
WORK_DIR="/tmp/misc-bundle-deploy"
log "Work Dir.......: $WORK_DIR"
log "JAR............: $JAR"
POM=`echo "$JAR" | sed "s/.jar/.pom/"`
log "POM............: $POM"

if [[ ! -f $POM ]]; then
    l_error "$POM not found"
    exit 1
fi

# Take a copy
rm -rf $WORK_DIR
mkdir -p $WORK_DIR
cp $JAR $WORK_DIR/
cp $POM $WORK_DIR/

# Get final names
BASENAME_JAR=`echo $JAR | sed "s/.*\/repository\///"`
BASENAME_POM=`echo $POM | sed "s/.*\/repository\///"`
FINAL_JAR="$WORK_DIR/`basename $JAR`"
FINAL_POM="$WORK_DIR/`basename $POM`"
log "JAR Basename...: $BASENAME_JAR"
log "POM Basename...: $BASENAME_POM"
log "JAR Work.......: $FINAL_JAR"
log "POM Work.......: $FINAL_POM"

# Sign the JAR
GPG_OPTS='-q'
if [ "$VERBOSE" = "1" ]; then
    GPG_OPTS='-v'
fi
grep "<gpg.passphrase>" ~/.m2/settings.xml &>/dev/null
EXITED=$?
if [[ ! "$EXITED" = "1" ]]; then
    log "Found gpg.passphrase in ~/.m2/settings.xml..."
    PASSPHRASE=`grep "<gpg.passphrase>" ~/.m2/settings.xml | sed 's/<gpg.passphrase>//' | sed 's/<\/gpg.passphrase>//' | sed 's/ //g'`
    echo "$PASSPHRASE" | gpg $GPG_OPTS --batch --passphrase-fd 0 -a --output "$FINAL_JAR.asc" --detach-sign $FINAL_JAR
else
    log "gpg.passphrase NOT found in ~/.m2/settings.xml. Trying with gpg agent."
    gpg $GPG_OPTS -a --use-agent --output $ASSEMBLY_ASC --detach-sign $ASSEMBLY_ZIP
fi
EXITED=$?
if [[ ! "$EXITED" = "0" ]]; then
    l_error "GPG detached signature creation failed (gpg exit code $EXITED)." >&2; exit 1;
fi

# Deploy
AWS_PREFIX='s3://spring-roo-repository.springsource.org/bundles'
s3_execute put --acl-public "$FINAL_JAR" "$AWS_PREFIX/$BASENAME_JAR"
s3_execute put --acl-public "$FINAL_POM" "$AWS_PREFIX/$BASENAME_POM"
s3_execute put --acl-public "$FINAL_JAR.asc" "$AWS_PREFIX/$BASENAME_JAR.asc"

# Clean up
# rm -rf $WORK_DIR

