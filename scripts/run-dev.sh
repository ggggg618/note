#!/usr/bin/env bash
set -e

cd "$(dirname "$0")/.."

set -a
if [ -f .env ]; then
    source .env
fi
set +a

if [ -z "$AI_API_KEY" ] && [ -n "$DASHSCOPE_API_KEY" ]; then
    export AI_API_KEY="$DASHSCOPE_API_KEY"
fi

mvn spring-boot:run
