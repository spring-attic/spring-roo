@echo off

setlocal

rem %~dp0 is location of current script under NT
set _REALPATH=%~dp0

cd %_REALPATH%
cd ..
set ROO_HOME="%cd%"

rem echo Resolved ROO_HOME: %ROO_HOME%

java -Djava.ext.dirs=%ROO_HOME%/lib;%ROO_HOME%/dist org.springframework.roo.bootstrap.Bootstrap "classpath:/roo-bootstrap.xml"