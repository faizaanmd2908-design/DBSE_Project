# HESTIA — Find Where You Belong

Full-stack Android real-estate buyer self-service application.

## Stack
- Android: Kotlin + Jetpack Compose + Material 3
- HTTP: Retrofit + OkHttp
- API Gateway: Node.js + Express
- Microservices: Auth, Property, Interaction
- Database: MySQL (`hestia_db`)
- Authentication: bcrypt + JWT + role-based access control
- Property inventory: 24 seeded Hyderabad listings with images

## Architecture

Android → Retrofit → API Gateway → Auth/Property/Interaction services → MySQL

## Demo accounts
Buyer: `demo@hestia.com` / `123456`
Admin: `admin@hestia.com` / `Admin@123456`

## Run
Read `RUN_HERE_FIRST.md`.

The project is configured for the user's existing local MySQL database. Never commit the real `.env` or credentials.
