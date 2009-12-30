@echo off
setlocal enabledelayedexpansion

for %%? in ("%~dp0..") do set ROO_HOME=%%~f?
rem echo Resolved ROO_HOME: "%ROO_HOME%"

rem parentheses might occur in path, so delayed variable expansion is needed, hence the ! chars
if exist "!JAVA_HOME!\jre" (set ROO_JRE=!JAVA_HOME!\jre) else (set ROO_JRE=!JAVA_HOME!)

:launch
java -Djline.nobell=true -Djava.ext.dirs="%ROO_HOME%\dist;%ROO_HOME%\lib;%ROO_HOME%\work;%ROO_JRE%\lib\ext" %ROO_OPTS% -Droo.home="%ROO_HOME%" org.springframework.roo.bootstrap.Bootstrap "classpath:roo-bootstrap.xml" %*
if "%errorlevel%" == "100" goto launch
if "%errorlevel%" == "200" goto clean_work_dir
goto end

:clean_work_dir
echo Cleaning out %ROO_HOME%\work, just a moment...
rem first delete all work dir jars while they're not locked
del /q "%ROO_HOME%\work\*.jar"
rem now let Roo restore the work dir contents based on the current addons and quit
java -Djava.ext.dirs="%ROO_HOME%\dist;%ROO_HOME%\lib;%ROO_JRE%\lib\ext" %ROO_OPTS% -Droo.home="%ROO_HOME%" org.springframework.roo.bootstrap.Bootstrap "classpath:roo-bootstrap.xml" addon cleanup > NUL
rem finally restart Roo again
goto launch

:end
