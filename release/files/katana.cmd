@echo off

set KATANA_HOME=%~dp0
IF %KATANA_HOME:~-1%==\ SET KATANA_HOME=%KATANA_HOME:~0,-1%

%KATANA_HOME%\jre\bin\java.exe -jar %KATANA_HOME%\compiler.jar %*
