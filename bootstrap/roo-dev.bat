@echo off

if not "%ROO_CLASSPATH_FILE%" == "" goto ok
    echo Must set ROO_CLASSPATH_FILE to X:\checkout\spring-roo\bootstrap\target\roo_classpath.txt
    exit /b

:ok

rem echo Using Roo classpath file [%ROO_CLASSPATH_FILE%]

rem Will be only one line, but the following obvious form doesn't work
rem set /p ROO_CP=< %ROO_CLASSPATH_FILE%

for /f "tokens=* delims= " %%a in (%ROO_CLASSPATH_FILE%) do (
	set ROO_CP=%%a
)

if not "%ROO_ADDON_CLASSPATH_FILE%" == "" goto run
for /f "tokens=* delims= " %%a in (%ROO_ADDON_CLASSPATH_FILE%) do (
	set EXTENDED_CP=%%a
)

set ROO_CP=%ROO_CP%;%EXTENDED_CP%

:run

rem Escape in case of long file names
set ROO_CP="%ROO_CP%"

rem echo Using Roo classpath [%ROO_CP%]

:launch
java -DdevelopmentMode=true -Droo.home="%USERPROFILE%\roo-dev" -cp %ROO_CP% -Djava.ext.dirs="%USERPROFILE%\roo-dev\work" org.springframework.roo.bootstrap.Bootstrap "classpath:/roo-bootstrap.xml" %*

if "%errorlevel%" == "999" goto launch
