@echo off

if "%ROO_HOME%"=="" goto instructions

java -Djava.ext.dirs=%ROO_HOME%/lib;%ROO_HOME%/dist org.springframework.roo.bootstrap.Bootstrap "classpath:/roo-bootstrap.xml"
goto end

:instructions
echo "ERROR: ROO_HOME environment variable is not set"

:end

