@echo off
chcp 65001 > nul
echo === 启动后端 (WSL) ===
wsl bash /mnt/c/SE26Project-13/scripts/start-backend.sh
pause
