REM**Authored by Shivam Kumar on 2025-12-06 with open source community ideas and services
REM and with the help of my project team Tushar, Saurabh, and Shreya. **

@echo off
if not exist bin mkdir bin

echo Compiling...
javac -d bin src\p2p\*.java src\p2p\net\*.java src\p2p\ui\*.java src\p2p\security\*.java

if "%1"=="server" goto run_server
if "%1"=="client" goto run_client
goto end

:run_server
echo Starting Discovery Server...
java -cp bin p2p.DiscoveryServer
goto end

:run_client
echo Starting Client...
java -cp bin p2p.App
goto end

:end
echo Done.
