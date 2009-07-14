@echo off

if not "%ROO_HOME%" == "" goto gotRooHome
    echo Must set ROO_HOME
    exit /b

:gotRooHome

if not "%ROO_CLASSPATH_FILE%" == "" goto gotRooClasspathFile
    set ROO_CLASSPATH_FILE="target/roo_classpath.txt"
    echo "WARNING: ROO_CLASSPATH_FILE environment variable is not set (using default '$ROO_CLASSPATH_FILE')"

:gotRooClasspathFile

echo Using Roo classpath file [%ROO_CLASSPATH_FILE%]
   
    
rem Will be only one line, but the following obvious form doesn't work
rem set /p ROO_CP=< %ROO_CLASSPATH_FILE%

for /f "tokens=* delims= " %%a in (%ROO_CLASSPATH_FILE%) do (
	set ROO_CP=%%a
)


rem Escape in case of long file names
set ROO_CP="%ROO_CP%"
    
echo Using Roo classpath [%ROO_CP%]

java -DdevelopmentMode=true -cp %ROO_CP% org.springframework.roo.bootstrap.Bootstrap "classpath:/roo-bootstrap.xml"
