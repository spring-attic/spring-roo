======================================================================
SPRING ROO - DEVELOPER INSTRUCTIONS
======================================================================

Thanks for checkout out Spring Roo from Subversion. These instructions
details how to get started with your freshly checked-out source tree.

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

  mkdir ~/spring-roo
  cd ~/spring-roo
  svn co https://anonsvn.springframework.org/svn/spring-roo/trunk/

Those with commit rights should use the following SVN URL instead:

  svn co https://src.springframework.org/svn/spring-roo/trunk/

Next double-check you meet the installation requirements:

 * A *nix machine (Windows users should be OK if they write a .bat)
 * A proper installation of Java 5 or above
 * Maven 2.0.9+ properly installed and working with your Java 5+
 * Internet access so that Maven can download required dependencies

Next you need to setup an environment variable called MAVEN_OPTS.
If you already have a MAVEN_OPTS, just check it has the memory sizes
shown below (or greater).  If you're following our checkout
instructions above and are on a *nix machine, you can just type:

  cd trunk
  echo export MAVEN_OPTS=\"-Xmx1024m -XX:MaxPermSize=512m\" >> ~/.bashrc
  source ~/.bashrc
  echo $MAVEN_OPTS
     (example result: MAVEN_OPTS=-Xmx1024m -XX:MaxPermSize=512m)

You're almost finished. You just need to wrap up with a symbolic link:

  sudo ln -s ~/spring-roo/trunk/bootstrap/roo-dev /usr/bin/roo-dev
  sudo chmod +x /usr/bin/roo-dev

Note: You do not need a ROO_CLASSPATH_FILE environment variable. This
was only required for the Roo 1.0.x development series.

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

Notice we used "mvn install" rather than "mvn package". This is simply
for convenience, as it will allow you to change into any Roo module
subdirectory and "mvn install". If you never "mvn install", you will
need to "mvn install" from the root directory so internal build
dependencies are preserved. You can use "mvn package" from the root if
you prefer. "mvn install" just gives you more flexibility.

Roo ships with a command line tool called "roo-dev". This is only
maintained for *nix. It copies all relevant JARs from the Roo
directories into ~/spring-roo/bootstrap/target/osgi. This directory
represents a configured Roo OSGi instance. "roo-dev" also launches the
OSGi container, which is currently Apache Felix.

Be aware that Felix will cache the bundles you have installed each
run (in /spring-roo/bootstrap/target/osgi/cache). It's therefore more
common that instead of using "roo-dev", you will type a command like:

  rm -rf ~/spring-roo/bootstrap/target/osgi; roo-dev

The above guarantees your Felix instance is fully cleaned. The
"roo-dev" command line tool doesn't do this for you, as you might
wish to test the operation of other bundles with Roo core.

======================================================================
DEBUGGING VIA ECLIPSE
======================================================================

       **** Note as of ROO-728 this section is out of date ****

Most of the time we just use the roo-dev command line tool directly
from the command line. This we have found is the fastest approach and
also lets us see exactly what a user would see, including the TAB
completion features. Still, sometimes you have a tricky issue you'd
prefer to work through via the STS/Eclipse debugger. When you do this
you need to be aware that you lose the full capabilities of the shell,
as the JLine library (used for command line parsing) is unable to
fully hook into your operating system's keyboard and ANSI services.
Anyhow, for some issues a debugger is worth the minor price of losing
your full keyboard and colour services! :-)

To setup debugging, open org.springframework.roo.bootstrap.Main.
Note it has a Java "main" method. Execute the class using Run As >
Java Application. Note the "Console" tab in Eclipse/STS will open.
Type "quit" then hit enter. Now select Run > Debug Configurations.
Select the Java Application > Main entry. Click on Arguments
and then add the following VM Arguments:

*nix machines:
  -Djline.terminal=org.springframework.roo.shell.jline.EclipseTerminal

Windows machines:
  -Djline.WindowsTerminal.directConsole=false
  -Djline.terminal=jline.UnsupportedTerminal

Finally, set the working directory to "Other" and a location on your
disk where you'd like the Roo shell to be loaded. This is usually a
project you're intending to test with.

======================================================================
SHELL DIAGNOSTIC FEATURES
======================================================================

The Roo shell includes several commands especially for Roo developers.

Firstly, any exception thrown by Roo or one of its add-ons is always
caught by the shell infrastructure and a simplified message displayed
to the user. This is generally what is desired, as it allows you to
simply throw exceptions whenever something is in an incorrect state.
However, full exceptions can be displayed by typing this command:

  development mode

There is also a "development mode -enabled false" to deactivate.

There are also several metadata-related commands:

  metadata summary
  metadata trace -level x
  metadata for type -type com.foo.bar.TypeName
  metadata for id -metadataId MID:com.metadata.Class#theIdentifier

The most practical command is "metadata for type", which shows you how
Roo internally sees a particular Java type. It will also show you all
of the downstream dependencies of a particular type, complete with the
various metadata identifiers (strings starting with "MID:"). You can
present those strings to the "metadata for id" command and see extra
information about how Roo internally understands that metadata.

The "metadata trace -level 1" command is useful for seeing how changes
to metadata are notified to downstream dependencies, including any
nested notifications that are taking place. You can also use level 2
if you would like even more verbose details, or level 0 to switch off
the metadata tracing. An example of a condensed level 1 log follows:

roo> add field string -fieldName test
Managed SRC_MAIN_JAVA/com/hello/Foo.java
00000008 MID:xxx.PhysicalTypeIdentifier#SRC_MAIN_JAVA?com.hello.Foo -> MID:xxx.BeanInfoMetadata#SRC_MAIN_JAVA?com.hello.Foo
00000009  MID:xxx.BeanInfoMetadata#SRC_MAIN_JAVA?com.hello.Foo -> MID:xxx.FinderMetadata#SRC_MAIN_JAVA?com.hello.Foo
00000009  MID:xxx.BeanInfoMetadata#SRC_MAIN_JAVA?com.hello.Foo -> MID:xxx.ToStringMetadata#SRC_MAIN_JAVA?com.hello.Foo
00000008 MID:xxx.PhysicalTypeIdentifier#SRC_MAIN_JAVA?com.hello.Foo -> MID:xxx.PluralMetadata#SRC_MAIN_JAVA?com.hello.Foo
0000000c  MID:xxx.PluralMetadata#SRC_MAIN_JAVA?com.hello.Foo -> MID:xxx.EntityMetadata#SRC_MAIN_JAVA?com.hello.Foo
0000000d   MID:xxx.EntityMetadata#SRC_MAIN_JAVA?com.hello.Foo -> MID:xxx.BeanInfoMetadata#SRC_MAIN_JAVA?com.hello.Foo
0000000e    MID:xxx.BeanInfoMetadata#SRC_MAIN_JAVA?com.hello.Foo -> MID:xxx.FinderMetadata#SRC_MAIN_JAVA?com.hello.Foo
0000000e    MID:xxx.BeanInfoMetadata#SRC_MAIN_JAVA?com.hello.Foo -> MID:xxx.ToStringMetadata#SRC_MAIN_JAVA?com.hello.Foo
0000000d   MID:xxx.EntityMetadata#SRC_MAIN_JAVA?com.hello.Foo -> MID:xxx.FinderMetadata#SRC_MAIN_JAVA?com.hello.Foo
00000008 MID:xxx.PhysicalTypeIdentifier#SRC_MAIN_JAVA?com.hello.Foo -> MID:xxx.ConfigurableMetadata#SRC_MAIN_JAVA?com.hello.Foo
00000008 MID:xxx.PhysicalTypeIdentifier#SRC_MAIN_JAVA?com.hello.Foo -> MID:xxx.FinderMetadata#SRC_MAIN_JAVA?com.hello.Foo
00000008 MID:xxx.PhysicalTypeIdentifier#SRC_MAIN_JAVA?com.hello.Foo -> MID:xxx.ToStringMetadata#SRC_MAIN_JAVA?com.hello.Foo
00000008 MID:xxx.PhysicalTypeIdentifier#SRC_MAIN_JAVA?com.hello.Foo -> MID:xxx.JavaBeanMetadata#SRC_MAIN_JAVA?com.hello.Foo
Created SRC_MAIN_JAVA/com/hello/Foo_Roo_JavaBean.aj

The numbers in the very first column are in hex format and increment
by one for each metadata notification. There are then one or more
spaces, with the spaces being used to denote nested notifications. The
metadata identification (MID) on the left hand side of the "->" token
is the "upstream" dependency which is notifying of a change, and the
metadata identification (MID) on the right hand side of the "->" token
is the downstream dependency that is receiving the notification. This
continues until all notifications have been delivered. Circular loops
are automatically avoided by the system, and string-based MID keys are
used to ensure metadata remains immutable, cachable and memory
efficient even in a large project.

======================================================================
SUBVERSION POLICIES
======================================================================

When checking into SVN, you must provide a commit message which begins
with the relevant Roo Jira issue tracking number. For example:

  ROO-1234: Name of the task as stated in Jira

You are free to place whatever text you like under this prefix. The
prefix ensures FishEye is able to correlate the commit with Jira.

You should not commit any IDE or Maven-generated files to Subversion.
In practical terms this means whenever you create a new directory that
contains pom.xml, set the svn:ignore property as follows:

  svn propset svn:ignore ".*
  target" directory_name_to_set

======================================================================
RELEASING
======================================================================

       **** Note as of ROO-728 this section is out of date ****

Roo is released on a regular basis by the Roo project team. To build a
release, ensure you perform an "svn update" first and then run
"svnversion" to verify a standard Subversion revision number is given.

the following command is used from the root SVN location:

  mvn clean install site assembly:assembly deploy site:deploy

This will create a ZIP in the "target" directory.

The org.springframework.roo.annotations JAR should be uploaded to
repository.springsource.com/maven/bundles/release/org/springframework/
roo/org.springframework.roo/org.sfw.roo.annotations/<version>. Ensure
SHA1/MD5 files, plus the POM and JAR is uploaded. Set public read ACL.
Use the mvn-hash.sh if required to create the SHA1/MD5 files.

The target/spring-roo-<version>.zip should be uploaded to
/dist.springframework.org/milestone/ROO/. Also upload an SHA1 file.
The following S3 properties must be set on the upload release ZIP:

x-amz-meta-bundle.version:1.0.0.M1
x-amz-meta-release.type:milestone
x-amz-meta-package.file.name:spring-roo-1.0.0.M1.zip
x-amz-meta-project.name:Spring Roo

If performing a GA release (ie *.RELEASE) upload the ZIP to
/dist.springframework.org/release/ROO/ and change the S3 props to:

x-amz-meta-release.type:release

In addition, a SVN branch should be created using the copy command:

  svn copy https://src.springframework.org/svn/spring-roo/trunk \
    https://src.springframework.org/svn/spring-roo/release-1.0.0.GA \
    -m "ROO-1234: Tagging the 1.0.0.GA release"

Once you've built a release, be aware your $ROO_CLASSPATH_FILE will no
longer point to the correct target directories. You should re-run the
"mvn clean eclipse:clean eclipse:eclipse compile" command to fix this.

======================================================================
HELP
======================================================================

There are no developer-specific forums or mailing lists for Roo. If
you have any questions, please use the community support forum at
http://forum.springsource.org/forumdisplay.php?f=67. Thanks for your
interest in Spring Roo!

