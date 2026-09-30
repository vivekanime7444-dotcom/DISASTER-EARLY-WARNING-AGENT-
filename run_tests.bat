@echo off
title Disaster Early-Warning Automated Test Suite
echo ====================================================================
echo   RUNNING DISASTER EARLY-WARNING AUTOMATED VERIFICATION SUITE
echo ====================================================================

set JAVA_HOME=C:\Users\shanm\jdk-17
set PATH=C:\Users\shanm\jdk-17\bin;%PATH%

cd java-agent
java -ea -cp bin com.disaster.agent.AgentTestSuite
pause
