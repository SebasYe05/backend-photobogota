@echo off
if "%1"=="dev" (
    .\mvnw.cmd spring-boot:run
) else if "%1"=="test" (
    .\mvnw.cmd clean test
) else if "%1"=="build" (
    .\mvnw.cmd clean package -DskipTests
) else if "%1"=="clean" (
    .\mvnw.cmd clean
) else (
    echo Uso: run [dev | test | build | clean]
)