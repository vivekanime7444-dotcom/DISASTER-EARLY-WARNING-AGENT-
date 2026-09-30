@echo off
title Universal Disaster Early-Warning System Launcher
echo ====================================================================
echo   STARTING 24/7 DISASTER EARLY-WARNING & WEATHER INTELLIGENCE AGENT
echo ====================================================================


echo [1/3] Initializing Database...
python database\db_manager.py

echo [2/3] Launching Python Risk Engine (Port 8000)...
start "Python Risk Engine (Port 8000)" cmd /k "cd python-risk-engine && python main.py"

timeout /t 3 /nobreak > nul

echo [3/3] Launching Java Rational Agent (Port 8080)...
start "Java Rational Agent (Port 8080)" cmd /k "cd java-agent && java -cp bin com.disaster.agent.DisasterAgentApp 8080"

timeout /t 3 /nobreak > nul

echo ====================================================================
echo   SYSTEM READY!
echo   Dashboard UI: http://localhost:8080/
echo   Python Engine: http://localhost:8000/
echo ====================================================================

start http://localhost:8080/
pause
