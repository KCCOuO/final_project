# 72hourPrediction 強力啟動腳本
$jdkPath = "D:\programFile\Java\jdk-22\jdk-22.0.2+9"
$env:JAVA_HOME = $jdkPath
$env:PATH = "$jdkPath\bin;$env:PATH"

Write-Host "--- 環境檢查 ---" -ForegroundColor Yellow
Write-Host "JAVA_HOME 設為: $env:JAVA_HOME"
& java -version
Write-Host "----------------" -ForegroundColor Yellow

Write-Host "正在啟動專案 (使用 JDK 22)..." -ForegroundColor Cyan

# 直接使用絕對路徑執行 Maven，並確保它帶入正確的 JAVA_HOME
$mvnPath = "D:\programFile\IDEA\IntelliJ IDEA 2025.2.1\code\72hourPrediction\maven\apache-maven-3.9.6\bin\mvn.cmd"
& $mvnPath javafx:run
