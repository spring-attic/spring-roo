@echo off

setlocal

set PROJECT_DIRECTORY="%cd%"

rem %~dp0 is location of current script under NT
set _REALPATH=%~dp0

cd %_REALPATH%
cd ..
set ROO_HOME="%cd%"

cd %PROJECT_DIRECTORY%

rem echo Resolved ROO_HOME: %ROO_HOME%
rem echo Resolved PROJECT_DIRECTORY %PROJECT_DIRECTORY%


java -Djava.ext.dirs=%ROO_HOME%/lib;%ROO_HOME%/dist org.springframework.roo.bootstrap.Bootstrap "classpath:/roo-bootstrap.xml"