#!/bin/bash
# 启动后端 - English Learning Copilot
# 在 WSL 中执行

cd "$(dirname "$0")/../backend" || { echo "无法进入 backend 目录"; exit 1; }

export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:3306/english_learning_copilot?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=""
export APP_JWT_SECRET="dev-only-change-me-dev-only-change-me-32-bytes"

set -a
if [ -f "../llm-prompt-lab/.env" ]; then
  source "../llm-prompt-lab/.env"
fi
if [ -f ".env.local" ]; then
  # Local secrets for ASR, Super Smart TTS, and SJTU LLM. This file is gitignored.
  source ".env.local"
fi
set +a

echo "=== 启动后端 (8080) ==="
mvn spring-boot:run -DskipTests
