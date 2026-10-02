# HESTIA — Run Here First

## A. Existing MySQL database
Keep your existing `hestia_db` in MySQL Workbench. Do not create another database.

Create `.env` in this root folder from `.env.example` and set your real MySQL password.

Example local values:

DB_HOST=localhost
DB_PORT=3306
DB_NAME=hestia_db
DB_USER=root
DB_PASSWORD=YOUR_MYSQL_PASSWORD

Gateway/services:
GATEWAY_PORT=3000
AUTH_PORT=3001
PROPERTY_PORT=3002
INTERACTION_PORT=3003
AUTH_SERVICE_URL=http://localhost:3001
PROPERTY_SERVICE_URL=http://localhost:3002
INTERACTION_SERVICE_URL=http://localhost:3003
CORS_ORIGIN=*

## B. Seed / verify
Only run this when you need to populate/reset demo content. The script is non-destructive and ensures the demo accounts, 24 property records and images exist.

PowerShell:

cd backend\auth-service
npm install
npm run seed-existing

## C. Start backend
From the project root:

powershell -ExecutionPolicy Bypass -File .\scripts\start-all-local.ps1

Or start the four services manually with `npm start` in each service folder.

## D. Verify
Open:

http://localhost:3000/health
http://localhost:3000/properties

Login test:

Buyer: demo@hestia.com / 123456
Admin: admin@hestia.com / Admin@123456

## E. Android
Open `android/` in Android Studio.

For a physical Android phone on the same Wi-Fi as the PC, the app uses:

http://192.168.29.44:3000/

If your PC IP changes, edit `android/app/build.gradle.kts` and update `API_BASE_URL`.

For an Android Emulator use:

http://10.0.2.2:3000/

Create `android/local.properties` with your local SDK path, for example:

sdk.dir=C:\\Users\\faiza\\AppData\\Local\\Android\\Sdk

Then Sync, Rebuild and Run.
