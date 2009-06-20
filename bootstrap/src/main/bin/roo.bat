@echo off

for %%? in ("%~dp0..") do set ROO_HOME=%%~f?

rem echo Resolved ROO_HOME: %ROO_HOME%

java -Djline.nobell=true -Djava.ext.dirs=%ROO_HOME%/lib;%ROO_HOME%/dist org.springframework.roo.bootstrap.Bootstrap "classpath:/roo-bootstrap.xml"