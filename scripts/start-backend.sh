#!/bin/bash
cd "$(dirname "$0")/.."
cp -n .env.example .env 2>/dev/null
docker compose up --build -d
echo "Waiting for MySQL..."
sleep 25
cd backend/auth-service
npm install
DB_HOST=localhost DB_PORT=3306 DB_USER=hestia_user DB_PASSWORD=hestia_pass DB_NAME=hestia_db node src/seed.js
echo "Backend ready at http://localhost:3000"
