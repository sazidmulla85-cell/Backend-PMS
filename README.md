# Backend PMS

Spring Boot 3.3 backend for the Angular hotel PMS frontend.

## What was implemented

This backend was expanded from the Spring Initializr starter into a multi-tenant PMS API with JWT authentication, password recovery, and property-level authorization.

Implemented modules:

- `auth` with JWT login and password reset flow
- `super admin` overview and hotel-owner/property onboarding
- `properties`
- `dashboard`
- `stay view`
- `rooms`
- `reservations`
- `audit logs`

## Business model

The backend follows this hierarchy:

1. `organization`
2. `property`
3. `user account`
4. `room type`
5. `room`
6. `guest`
7. `reservation`
8. `reservation room assignment`
9. `payment`
10. `audit log`

This supports your business idea:

- you are the platform `super admin`
- you can create hotel-owner accounts
- each hotel owner is linked to a property
- each property has a subscribed room count
- billing can later be based on subscribed room count
- operations are grouped by `propertyId`

## Database and persistence

Current database setup:

- `MySQL` for dev and prod
- recommended local database: `hotel_pms_dev`
- `Flyway` versioned migrations in `src/main/resources/db/migration`

Current JPA entity set:

- `Organization`
- `UserAccount`
- `Property`
- `RoomType`
- `Room`
- `Guest`
- `Reservation`
- `ReservationRoom`
- `Payment`
- `AuditLog`

Hibernate now validates mappings in `dev` and `prod`, while Flyway owns schema evolution before the application boots.

## Seeded demo data

The backend seeds and maintains these local development properties:

- one platform `super admin`
- `Hotel Dwarika`
- `The White House`
- `Lakeside Suites`
- room types, rooms, maintenance/out-of-order samples, and reservation timelines for each property

Seed credentials for every hotel are listed in:

- `../properties.md`

Seeded super admin credentials:

- phone: `9999999999`
- email: `superadmin@pms.local`
- password: `superadmin123`

Seeded hotel owner credentials include:

- `owner@hoteldwarika.local` / `owner123`
- `rhea@whitehousehotel.local` / `whitehouse123`
- `aman@lakesidesuites.local` / `lakeside123`

## API overview

### Auth

- `POST /api/auth/login`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`

### Super admin

- `GET /api/admin/overview`
- `POST /api/admin/properties`

### Properties

- `GET /api/properties`
- `GET /api/properties/{propertyId}`

### Dashboard

- `GET /api/properties/{propertyId}/dashboard`

### Stay view

- `GET /api/properties/{propertyId}/stay-view?focusDate=2026-04-13&days=7`

### Rooms

- `GET /api/properties/{propertyId}/rooms`
- `POST /api/properties/{propertyId}/rooms/room-types`
- `POST /api/properties/{propertyId}/rooms`

### Reservations

- `GET /api/properties/{propertyId}/reservations`
- `GET /api/properties/{propertyId}/reservations/{reservationId}`
- `POST /api/properties/{propertyId}/reservations`

### Audit logs

- `GET /api/properties/{propertyId}/audit-logs`

## Security note

The backend now includes:

- Spring Security
- JWT authentication for API access
- bcrypt password hashing
- password reset tokens with expiry
- role protection for super admin APIs
- property-level authorization guards for owner APIs

Still recommended later:

- refresh tokens
- rate limiting
- MFA for super admin

## Running locally

Requirements:

- Java 17 or newer
- a local MySQL server listening on `127.0.0.1:3306`

Create your local environment file from the root template:

```bash
cd /Users/sajidshaikh/Documents/PMS-System
cp .env.development.example .env.development
```

Edit `.env.development` when local database credentials or other settings differ.

Start or restart the Oracle MySQL Community Server LaunchDaemon on macOS:

```bash
sudo launchctl kickstart -k system/com.oracle.oss.mysql.mysqld
```

Verify that MySQL is accepting connections:

```bash
/usr/local/mysql/bin/mysqladmin --protocol=tcp -h 127.0.0.1 -P 3306 -u root -p ping
```

Do not run `/usr/local/mysql/support-files/mysql.server start` for this installation.
The Oracle installer already manages MySQL through the macOS LaunchDaemon, and starting a
second instance causes PID-file and data-directory conflicts.

Create the local database and development user:

```bash
/usr/local/mysql/bin/mysql -u root -p
```

```sql
CREATE DATABASE IF NOT EXISTS hotel_pms_dev;
CREATE USER IF NOT EXISTS 'pms_user'@'localhost' IDENTIFIED BY 'PmsUser@123';
ALTER USER 'pms_user'@'localhost' IDENTIFIED BY 'PmsUser@123';
GRANT ALL PRIVILEGES ON hotel_pms_dev.* TO 'pms_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

Run the backend from the `Backend-PMS` directory:

```bash
set -a
source ../.env.development
set +a
./mvnw spring-boot:run
```

The `dev` profile is active by default. It connects to:

- host: `127.0.0.1`
- port: `3306`
- database: `hotel_pms_dev`
- username: `pms_user`

Override any local database setting with `PMS_DB_HOST`, `PMS_DB_PORT`, `PMS_DB_NAME`,
`PMS_DB_USERNAME`, or `PMS_DB_PASSWORD`.

Verify the backend:

```bash
curl http://localhost:8080/actuator/health/readiness
```

Run tests:

```bash
./mvnw test
```

The API runs on the configured host and `SERVER_PORT` value, which defaults to port `8080`.

## Database migrations

- baseline schema: `src/main/resources/db/migration/V1__baseline_schema.sql`
- compatibility follow-up: `src/main/resources/db/migration/V2__align_existing_databases.sql`

Flyway now creates and maintains `flyway_schema_history` so database changes are explicit and repeatable across environments.

## Profiles

- `dev`: MySQL for local development
- `prod`: MySQL for deployment

Set the active profile with:

```bash
APP_PROFILE=prod
```

## Production deployment

Production deployment files now live at the project root:

- `../docker-compose.prod.yml`
- `../.env.production.example`
- `../DEPLOYMENT.md`

The production backend is environment-driven for:

- MySQL connection
- upload directory
- CORS origins
- email SMTP
- JVM options

Uploaded guest documents are stored outside the application container in a persistent volume.
The backend also exposes `/actuator/health/readiness` so Docker or a reverse proxy can verify the API is actually ready before routing traffic.

## Next recommended steps

1. Move guest documents from local persistent volume storage to object storage for multi-server scaling.
2. Add HTTPS termination and reverse proxy hardening.
3. Add database backups and monitoring.
4. Add broader integration and end-to-end test coverage.
