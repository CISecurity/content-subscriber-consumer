@ECHO OFF

SET JAVA=java

::
:: Detect if Java is in the path
::

%JAVA% 2> NUL > NUL

IF NOT %ERRORLEVEL%==9009 IF NOT %ERRORLEVEL%==3 GOTO RUNAPPLICATION

IF %ERRORLEVEL%==9009 GOTO NOJAVAERROR
IF %ERRORLEVEL%==3 GOTO NOJAVAERROR

::
:: Run the Subscriber
::

:RUNAPPLICATION

%JAVA% -Xmx2048M -jar "%~dp0\optimus-subscriber.jar" %*

GOTO EXIT

:NOJAVAERROR

ECHO The Java runtime was not found on the system PATH.  Please ensure Java is installed.
PAUSE

:EXIT


