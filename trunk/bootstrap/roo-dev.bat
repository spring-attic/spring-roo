@echo off
setlocal enabledelayedexpansion

if not "%ROO_CLASSPATH_FILE%" == "" goto ok
echo Must set ROO_CLASSPATH_FILE to X:\checkout\spring-roo\bootstrap\target\roo_classpath.txt
exit /b

:ok

set ROO_HOME=%USERPROFILE%\roo-dev

rem echo Using Roo classpath file [%ROO_CLASSPATH_FILE%]

:launch
rem First two for-loops add only a single line from the given files, 
rem so no delayed variable expansion is needed
for /f "usebackq tokens=*" %%a in (%ROO_CLASSPATH_FILE%) do (
	set ROO_CP=%%a
)
for /f "usebackq tokens=*" %%a in (%ROO_ADDON_CLASSPATH_FILE%) do (
	set ROO_CP=%ROO_CP%;%%a
)
rem this loop requires delayed variable expension, hence the ! chars
for %%a in ("%ROO_HOME%\work\*.jar") do (
    set ROO_CP=!ROO_CP!;%%a
)

rem echo Using Roo classpath ["%ROO_CP%"]

java -Djline.nobell=true -DdevelopmentMode=true %ROO_OPTS% -Droo.home="%ROO_HOME%" -cp "%ROO_CP%" org.springframework.roo.bootstrap.Bootstrap "classpath:roo-bootstrap.xml" %*

if "%errorlevel%" == "100" goto launch
if "%errorlevel%" == "200" goto clean_work_dir
goto end

:clean_work_dir
echo Cleaning out %ROO_HOME%\work, just a moment...
rem first delete all work dir jars while they're not locked
del /q "%ROO_HOME%\work\*.jar"
rem now let Roo restore the work dir contents based on the current addons and quit
java -DdevelopmentMode=true %ROO_OPTS% -Droo.home="%ROO_HOME%" -cp "%ROO_CP%" org.springframework.roo.bootstrap.Bootstrap "classpath:roo-bootstrap.xml" addon cleanup > NUL
rem finally restart Roo again
goto launch

:end
