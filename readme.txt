======================================================================
SPRING ROO - DEVELOPER INSTRUCTIONS
======================================================================

Thanks for checkout out Spring Roo from Git. These instructions detail
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
  git clone git://git.springsource.org/roo/roo.git

Next double-check you meet the installation requirements:

 * A *nix machine (Windows users should be OK if they write a .bat)
 * A proper installation of Java 5 or above
 * Maven 2.0.9+ properly installed and working with your Java 5+
 * Internet access so that Maven can download required dependencies

Next you need to setup an environment variable called MAVEN_OPTS.
If you already have a MAVEN_OPTS, just check it has the memory sizes
shown below (or greater).  If you're following our checkout
instructions above and are on a *nix machine, you can just type:

  echo export MAVEN_OPTS=\"-Xmx1024m -XX:MaxPermSize=512m\" >> ~/.bashrc
  source ~/.bashrc
  echo $MAVEN_OPTS
     (example result: MAVEN_OPTS=-Xmx1024m -XX:MaxPermSize=512m)

You're almost finished. You just need to wrap up with a symbolic link:

  sudo ln -s ~/roo/bootstrap/roo-dev /usr/bin/roo-dev
  sudo chmod +x /usr/bin/roo-dev

Note: You do not need a ROO_CLASSPATH_FILE environment variable. This
was only required for the Roo 1.0.x development series.

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
your key. You have three options:

 * Type the password in every time
 * Include a -Dgpg.passphrase=thephrase argument when calling "mvn"
 * Edit ~/.bashrc and add -Dgpg.passphrase=thephrase to MAVEN_OPTS

Of course the most secure option is to type the password every time.
However, if you're doing a lot of builds you might prefer automation.

One final note if you're new to GPG: don't lose your private key!
Backup the secring.gpg file, as you'll need it to ever revoke your key
or sign a replacement key (the public key servers offer no way to
revoke a key unless you can sign the recovation request).

======================================================================
OSGI WRAPPING JARS
======================================================================

Some Roo modules require JARs that are not already in OSGi form. That
is, they don't have an OSGi-aware manifest. Roo has a special project
called "wrapper" that can convert normal JARs into OSGi JARs.

You'll need to run the wrapper before trying to do anything with Roo.
If you don't do this, you'll see errors which state Maven cannot find
"org.springframework.roo.wrapping.some_module".

To create the wrapped JARs, from the root Roo checkout location type:

  cd wrapping
  mvn clean install
  cd ..

You need not do this too often. If you get an error about a missing
wrapper JAR (org.springframework.roo.wrapping.some_module), simply
repeat the above commands and you should be fine.

======================================================================
DEVELOPING WITHIN ECLIPSE
======================================================================

The Spring Roo team use SpringSource Tool Suite to develop Roo, which
is our free IDE. While you can use any IDE at all, these instructions
assume you're using STS. The main difference to be aware of is STS has
setup the M2_REPO variable correctly, and thus the Maven paths work
out of the box. You can setup M2_REPO manually within a normal Eclipse
if you wish; just use Window > Preferences > Java > Build Path >
Classpath Variables and set M2_REPO to the ~/.m2/repository directory.

First of all change into the directory where you checked out Roo. Now
you need to instruct Maven to produce .classpath and .project files
for Eclipse:

  mvn clean eclipse:clean eclipse:eclipse

If this fails, please review the "OSGi Wrapping JARs" section above.

You should now be able to import the projects into STS/Eclipse. Click
File > Import > Existing Projects into Workspace, and select the
same directory as where you ran the "mvn" command from. Several dozen
Spring Roo projects will be listed and can be imported.

At this stage you're free to open any class and edit it as normal.

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

  mvn install   (from the root Roo checkout location)
  cd ~/some-directory
  roo-dev

It's important that you run roo-dev from a directory that you'd like
to eventually contain a Roo-created project. Don't try to run roo-dev
unless your current working directory is empty or contains a
previously-created Roo project.

If this fails, please review the "OSGi Wrapping JARs" section above.

Notice we used "mvn install" rather than "mvn package". This is simply
for convenience, as it will allow you to change into any Roo module
subdirectory and "mvn install". If you never "mvn install", you will
need to "mvn install" from the root directory so internal build
dependencies are preserved. You can use "mvn package" from the root if
you prefer. "mvn install" just gives you more flexibility.

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

  rm -rf ~/roo/bootstrap/target/osgi; roo-dev

The above guarantees your Felix instance is fully cleaned. The
"roo-dev" command line tool doesn't do this for you, as you might
wish to test the operation of other bundles with Roo core.

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

  git clone git@git.springsource.org:roo/roo.git

Let's take the simple case where you just want to make a minor change
against master. You don't want a new branch etc, and you only want a
single commit to eventually show up in "git log". The easiest way is
to start your editing session with this:

  git pull

That will give you the latest code. Go and edit files. Determine the
changes with:

  git status

You can use "gwt add -A" if you just want to add everything you see.

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

Roo is released on a regular basis by the Roo project team.

To release you should edit your ~/.m2/settings.xml so it includes:

<settings>
  <servers>
    <server>
      <id>spring-roo-repository</id>
      <username>the_amazon_s3_main_account_access_id</username>
      <passphrase>the_amazon_s3_main_account_secret_key</passphrase>
    </server>
  </servers>
</settings>

The following command is used from the root checkout location:

  mvn clean package site assembly:assembly deploy site:deploy

This will create a ZIP in the "target" directory.

The target/spring-roo-<version>.zip should be uploaded to
/dist.springframework.org/milestone/ROO/. Also upload an SHA1 file.
The following S3 properties must be set on the upload release ZIP:

x-amz-meta-bundle.version:1.1.0.M1
x-amz-meta-release.type:milestone
x-amz-meta-package.file.name:spring-roo-1.1.0.M1.zip
x-amz-meta-project.name:Spring Roo

If performing a GA release (ie *.RELEASE) upload the ZIP to
/dist.springframework.org/release/ROO/ and change the S3 props to:

x-amz-meta-release.type:release

In addition, a Git tag should be created in the form w.x.y.zzzz (note
there is no prefix or suffix to the Git tag).

======================================================================
HELP
======================================================================

There are no developer-specific forums or mailing lists for Roo. If
you have any questions, please use the community support forum at
http://forum.springsource.org/forumdisplay.php?f=67. Thanks for your
interest in Spring Roo!

