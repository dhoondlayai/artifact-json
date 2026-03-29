$ErrorActionPreference = "Stop"

# Use the Java 21 embedded within IntelliJ since the global JAVA_HOME points to Java 17
$IntelliJ_JBR = "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.2\jbr"
$env:JAVA_HOME = $IntelliJ_JBR

Write-Host "Set JAVA_HOME to IntelliJ's Java 21: $env:JAVA_HOME" -ForegroundColor Cyan
Write-Host "Building project using Maven Wrapper..." -ForegroundColor Cyan

# Use the maven wrapper cmd file directly with the remaining args
.\mvnw.cmd $args
