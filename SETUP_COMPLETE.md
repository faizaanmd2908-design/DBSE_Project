# HESTIA local setup

The project is prepared for local Windows development against the existing MySQL Workbench database `hestia_db`.

## Before starting
1. Open `.env` and replace `PASTE_YOUR_MYSQL_PASSWORD_HERE` with the password of the MySQL account used by Workbench.
2. Keep MySQL running.
3. Use the existing `hestia_db`; do not create a new database.

## Backend
From the project root:
`powershell -ExecutionPolicy Bypass -File .\scripts\start-all-local.ps1`

This opens Auth :3001, Property :3002, Interaction :3003, Gateway :3000.

## Verify
`http://localhost:3000/health`
`http://localhost:3000/properties`

## Buyer login
`demo@hestia.com` / `123456`

## Admin login
`admin@hestia.com` / `Admin@123456`

## Android
Open `android/` in Android Studio. The project already contains local.properties for the detected SDK path and uses `http://192.168.29.44:3000/` for the physical phone. PC and phone must be on the same Wi-Fi.
