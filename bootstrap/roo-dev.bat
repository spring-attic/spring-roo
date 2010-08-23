@echo off
setlocal enabledelayedexpansion

for %%? in ("%~dp0..") do set ROO_HOME=%%~f?
rem echo Resolved ROO_HOME: "%ROO_HOME%"

if not exist "%ROO_HOME%\bootstrap\target\osgi\bin"    mkdir "%ROO_HOME%\bootstrap\target\osgi\bin" 
if not exist "%ROO_HOME%\bootstrap\target\osgi\bundle" mkdir "%ROO_HOME%\bootstrap\target\osgi\bundle" 
if not exist "%ROO_HOME%\bootstrap\target\osgi\conf"   mkdir "%ROO_HOME%\bootstrap\target\osgi\conf" 

copy "%ROO_HOME%\bootstrap\src\main\bin\*.*"  "%ROO_HOME%\bootstrap\target\osgi\bin"  > NUL
copy "%ROO_HOME%\bootstrap\src\main\conf\*.*" "%ROO_HOME%\bootstrap\target\osgi\conf" > NUL

rem Most Roo bundles are not special and belong in "bundle"
copy "%ROO_HOME%\target\all\*.jar" "%ROO_HOME%\bootstrap\target\osgi\bundle" > NUL

rem Move the startup-related JAR from the "bundle" directory to the "bin" directory
move "%ROO_HOME%\bootstrap\target\osgi\bundle\org.springframework.roo.bootstrap-*.jar" "%ROO_HOME%\bootstrap\target\osgi\bin" > NUL 2>&1
move "%ROO_HOME%\bootstrap\target\osgi\bundle\org.apache.felix.framework-*.jar" "%ROO_HOME%\bootstrap\target\osgi\bin" > NUL 2>&1

rem Build a classpath containing our two magical startup JARs
for %%a in ("%ROO_HOME%\bootstrap\target\osgi\bin\*.jar") do set ROO_CP=!ROO_CP!%%a;

rem Hop, hop, hop...
java -Djline.nobell=true %ROO_OPTS% -Droo.args="%*" -DdevelopmentMode=true -Dorg.osgi.framework.storage="%ROO_HOME%\bootstrap\target\osgi\cache" -Dfelix.auto.deploy.dir="%ROO_HOME%\bootstrap\target\osgi\bundle" -Dfelix.config.properties="file:%ROO_HOME%\bootstrap\target\osgi\conf\config.properties" -cp "%ROO_CP%" org.springframework.roo.bootstrap.Main
echo Roo exited with code %errorlevel%
