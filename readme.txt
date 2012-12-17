======================================================================
SPRING ROO - DEVELOPER INSTRUCTIONS
======================================================================

Thanks for checking out Spring Roo from Git. These instructions detail
how to get started with your freshly checked-out source tree.

These instructions are aimed at experienced developers looking to
develop Spring Roo itself. If you are new to Spring Roo or would
simply like to try a release that has already been built, tested and
distributed by the core development team, we recommend that you visit
the Spring Roo home page and download an official release:

   http://www.springsource.org/roo

======================================================================
ONE-TIME SETUP INSTRUCTIONS
======================================================================

We'll assume you typed the following to checkout Roo (if not, adjust
the paths in the following instructions accordingly):

  cd ~
  git clone git@github.com:SpringSource/spring-roo.git

In the instructions below, $ROO_HOME refers to the location where you
checked out Roo (in this case it would be ROO_HOME="~/roo"). You do NOT
need to add a $ROO_HOME variable. It is simply used in these docs.

Next double-check you meet the installation requirements:

 * A proper installation of Java 6 or above
 * Maven 3.0.1+ properly installed and working with your Java 6+
 * Internet access so that Maven can download required dependencies
 * A Git *command line* client installed (required by Roo's Maven build for
   inserting the current revision number into OSGi bundle manifests)

Next you need to setup an environment variable called MAVEN_OPTS.
If you already have a MAVEN_OPTS, just check it has the memory sizes
shown below (or greater).  If you're following our checkout
instructions above and are on a *nix machine, you can just type:

  echo export MAVEN_OPTS=\"-Xmx1024m -XX:MaxPermSize=512m\" >> ~/.bashrc
  source ~/.bashrc
  echo $MAVEN_OPTS
     (example result: MAVEN_OPTS=-Xmx1024m -XX:MaxPermSize=512m)

You're almost finished. You just need to wrap up with a symbolic link
(Windows users instead add $ROO_HOME/bootstrap to your path):

  sudo ln -s $ROO_HOME/bootstrap/roo-dev /usr/bin/roo-dev
  sudo chmod +x /usr/bin/roo-dev

======================================================================
GPG (PGP) SETUP
======================================================================

Roo now uses GPG to automatically sign build outputs. If you haven't
installed GPG, download and install it:

  * Main site: http://www.gnupg.org/download/
  * Apple Mac option: http://macgpg.sourceforge.net/

Ensure you have a valid signature. Use "gpg --list-secret-keys". You
should see some output like this:

$ gpg --list-secret-keys
/home/balex/.gnupg/secring.gpg
------------------------------
sec   1024D/00B5050F 2009-03-28
uid                  Ben Alex <ben.alex@acegi.com.au>
uid                  Ben Alex <ben.alex@springsource.com>
uid                  Ben Alex <balex@vmware.com>
ssb   4096g/2DB6833B 2009-03-28

If you don't see the output, it means you first need to create a key.
It's very easy to do this. Just use "gpg --gen-key". Then verify
your newly-created key was indeed created: "gpg --list-secret-keys".

Next you need to publish your key to a public keyserver. Take a note
of the "sec" key ID shown from the --list-secret-keys. In my case it's
key ID "00B5050F". Push your public key to a keyserver via the command
"gpg --keyserver hkp://pgp.mit.edu --send-keys 00B5050F" (of course
changing the key ID at the end). Most public key servers share keys,
so you don't need to send your public key to multiple key servers.

Finally, every time you build you will be prompted for the password of
your key. You have several options:

 * Type the password in every time
 * Include a -Dgpg.passphrase=thephrase argument when calling "mvn"
 * Edit ~/.bashrc and add -Dgpg.passphrase=thephrase to MAVEN_OPTS
 * Edit your active Maven profile to include a "gpg.passphrase" property:
     <profiles>
         <profile>
             <properties>
                 <gpg.passphrase>roorules</gpg.passphrase>

Of course the most secure option is to type the password every time.
However, if you're doing a lot of builds you might prefer automation.

One final note if you're new to GPG: don't lose your private key!
Backup the secring.gpg file, as you'll need it to ever revoke your key
or sign a replacement key (the public key servers offer no way to
revoke a key unless you can sign the revocation request).

======================================================================
DEVELOPING WITHIN ECLIPSE
======================================================================

Spring Roo itself does not use AspectJ and therefore any standard IDE
can be used for development. No extra plugins are needed and the team
use "mvn clean eclipse:clean eclipse:eclipse" to produce Eclipse
project files that can be imported via File > Import > Existing
Projects into Workspace. In theory you could use the m2eclipse plugin.
The Roo team just tends to use eclipse:clean eclipse:eclipse instead.

======================================================================
RUNNING THE COMMAND LINE TOOL
======================================================================

Roo uses OSGi and OSGi requires compiled JARs. Therefore as you make
changes in Roo, you'd normally need to "mvn package" the relevant
project(s), then copy the resulting JAR files to the OSGi container.

To simplify development and OSGi-related procedures, Roo's Maven POMs
have been carefully configured to emit manifests, SCR descriptors and
dependencies. These are mostly emitted when you use "mvn package".

To try Roo out, you should type the following:

  cd $ROO_HOME
  mvn install
  cd ~/some-directory
  roo-dev

It's important that you run roo-dev from a directory that you'd like
to eventually contain a Roo-created project. Don't try to run roo-dev
from your $ROO_HOME directory.

If this fails, please review the "OSGi Wrapping JARs" section above.

Notice we used "mvn install" rather than "mvn package". This is simply
for convenience, as it will allow you to "cd" into any Roo module
subdirectory and "mvn install". This saves considerable build time if
changes are only being made in a single module.

Roo ships with a command line tool called "roo-dev". This is also a
Windows equivalent. It copies all relevant JARs from the Roo
directories into ~/roo/bootstrap/target/osgi. This directory
represents a configured Roo OSGi instance. "roo-dev" also launches the
OSGi container, which is currently Apache Felix. It also activates
"development mode", which gives fuller exceptions, more file activity
reporting, extra flash messages related to OSGi events etc.

Be aware that Felix will cache the bundles you have installed each
run (in /roo/bootstrap/target/osgi/cache). It's therefore more
common that instead of using "roo-dev", you will type a command like:

  rm -rf $ROO_HOME/bootstrap/target/osgi; roo-dev

The above guarantees your Felix instance is fully cleaned. The
"roo-dev" command line tool doesn't do this for you, as you might
wish to test the operation of other bundles with Roo core (ie bundles
you have installed via the "addon install" commands etc).

======================================================================
GIT POLICIES
======================================================================

When checking into Git, you must provide a commit message which begins
with the relevant Roo Jira issue tracking number. The message should
be in the form "ROO-xxx: Title of the Jira Issue". For example:

  ROO-1234: Name of the task as stated in Jira

You are free to place whatever text you like after this prefix. The
prefix ensures FishEye is able to correlate the commit with Jira. eg:

  ROO-1234: Name of the task as stated in Jira - add extra file

You should not commit any IDE or Maven-generated files into Git.

Try to avoid "git pull", as it creates lots of commit messages like
"Merge branch 'master' of git.springsource.org:roo/roo". You can avoid
this with "git pull --rebase". See the "Git Tips" below for advice.

======================================================================
GIT TIPS
======================================================================

Setup Git correctly before you do anything else:

  git config --global user.name "Kanga Roo"
  git config --global user.email joeys@marsupial.com

Perform the initial checkout with this:

  git clone git@github.com:SpringSource/spring-roo.git

Let's take the simple case where you just want to make a minor change
against master. You don't want a new branch etc, and you only want a
single commit to eventually show up in "git log". The easiest way is
to start your editing session with this:

  git pull

That will give you the latest code. Go and edit files. Determine the
changes with:

  git status

You can use "git add -A" if you just want to add everything you see.

Next you need to make a commit. Do this via:

  git commit -e

The -e will cause an editor to load, allowing you to edit the message.
Every commit message should reflect the "Git Policies" above.

Now if nobody else has made any changes since your original "git
pull", you can simply type this:

  git push origin

If the result is '[ok]', you're done. If the result is '[rejected]',
someone else beat you to it. The simplest way to workaround this is:

  git pull --rebase

The --rebase option will essentially do a 'git pull', but then it will
reapply your commits again as if they happened after the 'git pull'.
This avoids verbose logs like "Merge branch 'master'".

If you're doing something non-trivial, it's best to create a branch.
Learn more about this at http://sysmonblog.co.uk/misc/git_by_example/.

======================================================================
RELEASING
======================================================================

Roo is released on a regular basis by the Roo project team. To perform
releases and make the associated announcements you require appropriate
permissions to many systems (as listed below). As such these notes are
intended to assist developers with such permissions complete releases.

Our release procedure may seem long, but that's because it includes
many steps related to final testing and staging releases with other
teams.

PREREQUISITES:

   * GPG setup (probably already setup if you followed notes above)
   * Git push privileges (if you can commit, you have this)
   * VPN access for SSH into static.springsource.org
   * SSH keypair for auto login into static.springsource.org
   * s3cmd setup (so "s3cmd ls" lists spring-roo-repository.springsource.org)
   * ~/.m2/settings.xml for spring-roo-repository-release and
     spring-roo-repository-snapshot IDs with S3 username/password
   * @SpringRoo twitter account credentials
   * forum.springsource.org moderator privileges
   * www.springsource.org editor privileges
   * JIRA project administrator privileges
   * Close down your IDE before proceeding

RELEASE PROCEDURE:

1. Complete a thorough testing build and assembly ZIP:

   cd $ROO_HOME
   git pull
   cd $ROO_HOME/deployment-support
  ./roo-deploy.sh -c next -n 4.5.6.RELEASE (use -v for logging)
   cd $ROO_HOME
   mvn clean install
   cd $ROO_HOME/deployment-support
   mvn clean site
   ./roo-deploy.sh -c assembly -tv (use -t for extra tests)

2. Verify the assembly ZIP ($ROO_HOME/target/roo-deploy/dist/*.zip) looks good:

   * Assembly ZIP unzips and is of a sensible size
   * Assembly ZIP runs correctly when installed on major platforms
   * Create Jira Task ticket "Release Spring Roo x.y.z.aaaaaa"
   * Run the "reference guide" command in the Roo shell, copy the resulting XML file
     into $ROO_HOME/deployment-support/src/site/docbook/reference,
     git commit and then git push (so the appendix is updated)

3. Tag the release (update the key ID, Jira ID and tag ID):

   cd $ROO_HOME
   git tag -a -m "ROO-XXXX: Release Spring Roo 4.5.6.RELEASE" 4.5.6.RELEASE

4. Build JARs:

   cd $ROO_HOME
   mvn clean package

5. Build the reference guide and deploy to the static staging server.
   You must be connected to the VPN for deployment to work. Note that
   http://www.springsource.org/roo is updated bi-hourly from staging:

   cd $ROO_HOME/deployment-support
   mvn clean site site:deploy

6. Create the final assembly ZIP (must happen *after* site built). We
   run full tests here, even ensuring all the Maven artifacts used by
   user projects are available. This takes a lot of time, but it is
   very helpful for our users:

   cd $ROO_HOME/deployment-support
   ./roo-deploy.sh -c assembly -Tv (-T means Maven tests with empty repo)

7. Repeat the verification tests on the assembly ZIP (see above). See
   note below if coordinating a release with the STS team.

8. If the verifications pass, push the Git tag up to the server:

   cd $ROO_HOME
   git push --tags

9. Deploy the JARs and assembly ZIP to the production download servers
   (it takes up to an hour for these to be made fully downloadable):

   cd $ROO_HOME
   mvn deploy
   cd $ROO_HOME/deployment-support
   ./roo-deploy.sh -c deploy (use -dv for a dry-run and verbose logging)

10. Increment the version number to the next BUILD-SNAPSHOT number:

    cd $ROO_HOME/deployment-support
    ./roo-deploy.sh -c next -n 4.5.6.BUILD-SNAPSHOT (use -v for logging)
    cd $ROO_HOME
    mvn clean install eclipse:clean eclipse:eclipse
    cd ~/some-directory; roo-dev script clinic.roo; mvn test
    cd $ROO_HOME
    git diff
    git commit -a -m "ROO-XXXX: Update to next version"
    git push

Typically after step 7 you'll send the tested assembly ZIP to the STS
team for a concurrent release. Allow time for them to test the ZIP
before starting step 8. This allows verification of STS embeddeding.
Keep your ROO_HOME intact during this time, as you need the **/target
and /.git directories for steps 8 and 9 to be completed.

If any problems are detected before step 8, simply fix, push and start
from step 1 again. You have not deployed anything substantial (ie only
the reference guide) until step 8, so some corrections and re-tagging
can be performed without any difficulty. The critical requirement is
to defer step 8 (and beyond) until you're sure everything is fine.

PRE-NOTIFICATION TESTING:

   * Visit http://www.springsource.org/roo/start, click "DOWNLOAD!"
   * Ensure it unzips OK and the sha1sum matches the downloaded .sha
   * rm -rf ~/.m2/repository/org/springframework/roo
   * Use "roo script clinic.roo" to build a new Roo project
   * Use "mvn clean test" to verify Roo's annotation JAR downloads

NOTIFICATIONS AND ADMINISTRATION:

Once the release is completed (ie all steps above) you'll typically:

   * Mark the version as "released" in JIRA (Admin > JIRA Admin...)
   * Publish a blog.springsource.com entry explaining what's new
   * Update http://en.wikipedia.org/wiki/Spring_Roo with the version
   * Update http://www.springsource.org/node/2/ with the version
   * Add a "News" announcement http://www.springsource.org
   * Add a "News" announcement http://forum.springframework.org
   * Add a "Roo" forum announcement http://forum.springframework.org
   * Edit http://forum.springsource.org/showthread.php?t=71985
   * Tweet from @SpringRoo (NB: ensure #SpringRoo is in the message)
   * Tweet from your personal account
   * Email dev list
   * Resolve the "release ticket" in JIRA

======================================================================
HELP
======================================================================

There are no developer-specific forums or mailing lists for Roo. If
you have any questions, please use the community support forum at
http://forum.springsource.org/forumdisplay.php?f=67. Thanks for your
interest in Spring Roo!

