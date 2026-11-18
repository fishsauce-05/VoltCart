@echo off
cd /d "%~dp0"
call mvnw.cmd -q spring-boot:run
