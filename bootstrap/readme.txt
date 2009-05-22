======================================================================
WHAT IS ROO?
======================================================================

Roo's mission is to fundamentally and sustainably improve Java
developer productivity without compromising engineering integrity or
flexibility.

The above mission statement guided the development of Roo, resulting
in interactive, lightweight, user customizable tooling that enables
rapid delivery of high performance, standards-compliant enterprise
Java applications.

Quite simply, Roo delivers productivity without compromise. Spring Roo
melds the development advantages that have emerged in dynamic
frameworks with the robustness, reliability, performance, familiarity,
maturity and quality tooling of enterprise Java. Roo is designed for
developers that want to build Java applications faster than ever
without having to learn a new language or syntax. Roo is designed to
be incorporated into the majority of development environments -
including visual development tools - and utilizes the widely
understood implementations of relevant Java standards. Roo is also
highly non-invasive, has no runtime component and is easily removed.

In terms of implementation, Spring Roo provides a command line shell
with tab completion, context aware operations, intelligent command
hinting and a complete knowledge of your application artifacts. It
constructs your application in a standard directory format, manages
and updates your build configuration files, helps you create domain
objects, integrates with popular persistence choices and provides
automatic web tier generation for easy REST-based web user interfaces.
It also offers dynamic finders (freeing you from you the need to
write query expressions), automated and passing JUnit integration
tests, automatic round trip processing and many more features via the
add-ons shipped with Roo.

We've provided a FAQ at our current project home page, where you can
find links to the latest release and other project resources (see also
the bottom of this readme for further links):

   http://www.springsource.org/roo

Best of all, you're only moments away from these benefits! We hope
that you find Roo as much fun to work with as we had in building it.

======================================================================
ROO INSTALLATION
======================================================================

  **** This is a milestone release - see 'Known Issues' section ****

The following steps explain how to install the Roo distribution ZIP.
Installing Roo is very similar to installing Ant or Maven: you just
unzip it, add a ROO_HOME, and add the ROO_HOME/bin to your path.
More detailed instructions are provided below.

* mvn -v
  Ensure it emits Maven 2.0.9 or above (*NOT* 2.0.8 or earlier)
  Roo itself does need Maven - it's only to work with created projects

* java -version
  Roo supports Java 5 and above
  Refer to 'Known Issues' section for non-Sun JREs

* unzip spring-roo-1.0.0.M1.zip

* For Windows users, add \path\to\roo\bin to your path (NB: the \bin)

* For Linux/Mac users, execute the following command as root:
    ln -s /path/to/roo/bin/roo.sh /usr/bin/roo

* For Eclipse users, install AJDT 1.6.5 or above (mandatory)
  Roo will still work with an earlier AJDT version, but you may notice
  some code assist limitations and no automatic aspects library import

* We highly recommend you download the SpringSource Tool Suite (STS)
  2.1.0.M1 or above, which is a free Eclipse-based IDE offered by
  SpringSource that includes significant inbuilt Roo integration

Installation should now be complete.

======================================================================
USING ROO
======================================================================

After installing, you can change into any directory and type "roo".

Once you've loaded Roo, just type "hint" and press ENTER for help.
Follow the instructions precisely, right down to when to press a key.
This will help you learn about the shell's tab completion features,
and how to differentiate between optional and mandatory arguments.
You'll also see command hiding, which makes commands that are not
legal at a particular time from being displayed. 

You can exit Roo by typing "exit" and then pressing ENTER. Alternately
type "quit" and press ENTER. You can also abbreviate commands ("q").

======================================================================
ROO SAMPLES
======================================================================

If you'd like to see what Roo can do for you, try the following:

# mkdir petclinic
# cd petclinic
# roo

roo> script clinic.roo
roo> exit

# mvn eclipse:eclipse

This will cause Maven files to be produced. Next:

* In Eclipse, select File > Import > Existing Project into Workspace
    1. Select the "petclinic" directory you created above
    2. The project will import

* In Eclipse, right-click the "petclinic" project and then select
  Run As > JUnit Test
  
* If your tests fail with a message such as "Entity manager has not
  been injected (is the Spring Aspects JAR configured as an AJC/AJDT
  aspects library?", re-check you really have AJDT 1.6.5+ installed
  in your Eclipse (as noted in the installation directions above)

* In Eclipse, right-click the "petclinic" project and then select
  Run As > Run On Server

* In a browser, visit http://localhost:8080/petclinic

Now you should have a working Petclinic sample application. 

You can also build a deployment artifact (.war) or run tests using:

# mvn test
# mvn package

You can even immediately deploy your application with Tomcat:

# mvn tomcat:run

Note that the "mvn package" command automatically runs tests, so there
is no need to separately "mvn test". Also before packaging you may
edit your project's src/main/resources/META-INF/persistence.xml and
change the "hibernate.hbm2ddl.auto" value from "create" to "update".
If you don't do this, Hibernate will delete your tables whenever it
starts up (which is fine if you're just experimenting and not trying
to build an application for production deployment).

We've also included the contents of several Roo scripts in 
ROO_HOME/samples. Please note these are not the actual files parsed by
the "script" command, so don't edit them in the samples directory and
expect your changes to be used. If you'd like to edit a sample file,
simply copy it into the freshly-created directory before you run Roo.
In this case the "script" command will use the file from the current
working directory. Alternately, you can use a fully-qualified path
to the script, such as "script /home/balex/roo/samples/clinic.roo".
Or you can use an operating system command like "mkdir petclinic;
cd petclinic; cp /home/balex/roo/samples/clinic.roo .; roo". The
benefit of the latter approach is you can then just use 
"script clinic.roo" once you load the Roo shell, and your command
history will contain the copy command (useful given you'll probably
delete your project directory several times when experimenting with
the script).

You can also just type commands directly into the shell, as suggested
in the "Using Roo" section above. The script command is just a
convenient way of repeating commands later on. You certainly do not
need to use the script facility unless you wish to.

======================================================================
KNOWN ISSUES
======================================================================

* This is beta level software. There has been minimal testing. Not
  everything works reliably yet. Don't use Roo for important projects!
  Having said that, given Roo isn't part of your runtime, it isn't a
  major issue if it doesn't work perfectly.

* There appears to be an incompatibility with OpenJDK. We recommend
  ensuring JAVA_HOME points to a Sun JDK until this is investigated.

* Pluralisation will always be performed in English.

* Do not run more than one concurrent instance of Roo on the same
  project directory. Multiple instances will compete to manage the
  files in the directory. As both instances are guaranteed to always
  emit identical files, it isn't a major problem if you happen to run
  two concurrent instances by accident. Your machine will just be a
  little slower (unnecessary file monitoring will be occurring) and
  some advanced and uncommon events (eg file undo operation) will not
  behave as designed. You may also not receive reliable shell messages
  regarding which file resources were created, managed or deleted.

* In certain cases the shell's TAB completion may not parse the
  current line correctly. This happens mainly when skipping mandatory
  options or introducing escaped sequences, both of which are uncommon
  when using the TAB key to build the line. We're intending to revisit
  the shell parsing modules in due course and tidy this up.

* Roo does not prevent you from creating package, entity, field or
  other members names which are reserved words. Please ensure you
  not only avoid Java keywords in such names, but also JPA, JDBC
  and SQL-related reserved words. For example, creating an 'Order'
  class would cause issues because 'ORDER' is a SQL reserved word.
  You can still use an entity named 'Order', though, if you edit the
  resulting Order.java file and add @Table(name="T_ORDER") as a type-
  level annotation.

* Only Hibernate is supported as a JPA provider, despite multiple JPA
  providers being shown. Issue reports have been logged the relevant
  JPA provider projects.
  
* If using a database other than H2 or Hypersonic, you should edit the
  generated src/main/resources/database.properties file to reflect the
  applicable connection string, username, password etc. Note the
  H2_PERSISTENT and HYPERSONIC_PERSISTENT will by default make the
  persistent database in your home directory (you can use Roo's
  "database properties" command to view your present configuration).

* Using DEBUG level logging with Spring's BeanConfigurerSupport will
  yield a significant number of exception traces. Nothing is wrong
  with Roo, but rather Spring is reporting a circular dependency
  because we use @Configurable @Entity types which in turn use
  @PersistenceContext. As such, the log level defaults to INFO.
  See SPR-5752 for the related issue in Spring Framework.

* The data on demand mechanism (which is used for integration tests)
  has limited JSR 303 (Bean Validator) compatibility. Roo supports
  fields using @NotNull, @Past and @Future. No other validator
  annotations are formally supported, although many will work. To use
  other validator annotations, you may need to edit your 
  DataOnDemand.java file and add a manual getNewTransientEntity(int)
  method. Refer to a generated *_Roo_DataOnDemand.aj file for an
  example. Alternately, do not use the integration test functionality
  in Roo unless you have relatively simple validation constraints or
  you are willing to provide a data on demand method.

======================================================================
TROUBLESHOOTING AND FURTHER HELP
======================================================================

We're very happy to help you and very much welcome community feedback!
The best place to ask questions or obtain Roo usage advice is the
community support forum. This can be found at the following URL:

   http://forum.springsource.org/forumdisplay.php?f=67

We also operate a public issue tracker, which can be used to find and
file bug reports, feature requests and improvement ideas:

   http://jira.springframework.org/browse/ROO

The Roo home page is also a good source of links and other resources:

   http://www.springsource.org/roo

We also regularly post about our development activities and other
interesting events on the #roo Twitter channel. You can follow it via:

   http://search.twitter.com/search?q=%23roo

If you find Roo useful, please let others know! If you publish a blog
or forum entry you think others in the Roo community would like to
read, please email ben.alex@springsource.com or Tweet including #roo.

Finally, those interested in accessing the Roo source code can do so
using either anonymous Subversion or viewing it via FishEye. Build
instructions are provided in the root checkout directory: 

   https://anonsvn.springframework.org/svn/spring-roo/trunk/
   https://fisheye.springsource.org/browse/spring-roo

Good luck, and thanks for taking the time to look at Spring Roo. We
hope that you enjoy using it!
