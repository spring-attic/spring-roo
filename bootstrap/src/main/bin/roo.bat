@echo off

for %%? in ("%~dp0..") do set ROO_HOME=%%~f?

rem echo Resolved ROO_HOME: "%ROO_HOME%"

:launch
java -Djline.nobell=true -Djava.ext.dirs="%ROO_HOME%/dist;%ROO_HOME%/lib;%ROO_HOME%/work;%JAVA_HOME%/jre/lib/ext" %ROO_OPTS% -Droo.home="%ROO_HOME%" org.springframework.roo.bootstrap.Bootstrap "classpath:/roo-bootstrap.xml" %*
if "%errorlevel%" == "100" goto launch
