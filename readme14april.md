# Backend PMS Handoff

Date: 14 April 2026

## What this file is for

This file captures the backend work completed so far in the Spring Boot PMS application so future sessions can quickly recover project context.

## Backend stack

- Java Spring Boot
- Spring Web
- Spring Data JPA
- H2 file database for local development
- multi-tenant PMS-style domain structure

## Business model implemented

The current backend follows this hierarchy:

1. organization
2. property
3. user account
4. room type
5. room
6. guest
7. reservation
8. reservation room assignment
9. payment
10. audit log

This supports:

- one platform super admin
- multiple hotel owners
- multiple properties
- property-specific PMS operations
- future billing based on subscribed room count

## Backend modules implemented

### Auth

- login endpoint implemented
- currently no JWT or Spring Security
- login supports lookup by email or phone
- returns accessible properties for current user

### Super admin

- property onboarding support implemented
- creates organization, owner account, property, room types, and rooms
- tracks subscribed room count

### Properties

- property retrieval endpoints implemented

### Dashboard

- owner dashboard API implemented
- returns occupancy and booking summary data

### Stay view

- stay view API implemented
- supports:
  - `focusDate`
  - `days`
- returns grouped room timeline with booking blocks
- returns footer availability
- returns summary counters

### Rooms

- rooms API implemented
- supports room types and room inventory
- supports room creation
- supports room-type creation
- later extended to support selected `businessDate`
- room occupancy is resolved per selected day
- returns room states such as:
  - assigned
  - checked-in
  - checking out
  - maintenance
  - out of order
  - available

### Reservations

- reservation list endpoint implemented
- reservation detail endpoint implemented
- create reservation endpoint implemented
- reservation creation stores guest and room-assignment data

### Audit logs

- audit log persistence implemented
- audit log endpoint implemented per property
- auth/admin/rooms/reservations actions are logged where applicable

## Core domain entities implemented

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

## Database setup

Current local database:

- H2 file database
- path: `Backend-PMS/data/backend-pms`

Notes:

- seed data is additive
- if a fresh local dataset is needed, delete the H2 files under `Backend-PMS/data/`

## Seed data completed

### Platform account

- super admin account seeded

### Properties seeded

- Hotel Dwarika
- The White House
- Lakeside Suites

### Data seeded for each property

- owner account
- property record
- room types
- rooms
- maintenance / out-of-order examples
- reservations
- reservation-room assignments
- timeline-friendly booking data for stay view and rooms view

## Important seed credentials

- Super Admin
  - `superadmin@pms.local`
  - `9999999999`
  - password: `superadmin123`

- Hotel Dwarika owner
  - `owner@hoteldwarika.local`
  - `8888888888`
  - password: `owner123`

- The White House owner
  - `rhea@whitehousehotel.local`
  - `9000011122`
  - password: `whitehouse123`

- Lakeside Suites owner
  - `aman@lakesidesuites.local`
  - `9011122233`
  - password: `lakeside123`

## Important seed logic fixes completed

- fixed seeding so existing databases also repair missing owner accounts for seeded properties
- property owners for White House and Lakeside are recreated/relinked if missing
- helps older local databases self-heal on backend restart

## Backend/frontend integration completed

### Owner-facing modules fully connected

- auth login
- dashboard
- stay view
- rooms
- reservations
- audit logs

### Super admin integration status

- backend endpoints exist for admin onboarding and admin overview
- frontend super admin UI still needs full backend integration

## Important backend improvements completed recently

- rooms endpoint now accepts selected `businessDate`
- rooms endpoint now resolves correct occupancy status for chosen date
- room response includes:
  - occupancy status
  - current guest
  - reservation number
  - check-in date
  - check-out date
- seeded owner login self-healing fix added

## Current API areas in active use

- `POST /api/auth/login`
- `GET /api/properties/{propertyId}/dashboard`
- `GET /api/properties/{propertyId}/stay-view`
- `GET /api/properties/{propertyId}/rooms`
- `GET /api/properties/{propertyId}/reservations`
- `GET /api/properties/{propertyId}/reservations/{reservationId}`
- `POST /api/properties/{propertyId}/reservations`
- `GET /api/properties/{propertyId}/audit-logs`

## Security status

Not implemented yet by project decision:

- JWT
- Spring Security authorization
- refresh tokens
- password reset flows
- secure session management

## Suggested next steps

- integrate super admin Angular screens with backend APIs
- add proper security layer
- move database from H2 to PostgreSQL or MySQL
- add folio, charges, reports, billing, and housekeeping modules
- add stronger audit detail and operational filters later

## Reservation lifecycle update added on 14 April

The backend now supports operational reservation actions beyond simple creation:

- `POST /api/properties/{propertyId}/reservations/{reservationId}/check-in`
- `POST /api/properties/{propertyId}/reservations/{reservationId}/check-out`
- `POST /api/properties/{propertyId}/reservations/{reservationId}/cancel`
- `POST /api/properties/{propertyId}/reservations/{reservationId}/no-show`
- `POST /api/properties/{propertyId}/reservations/{reservationId}/payments`

## Lifecycle rules now implemented

- only `CONFIRMED` reservations can be checked in
- only `CHECKED_IN` reservations can be checked out
- only `CONFIRMED` reservations can be cancelled
- only `CONFIRMED` reservations can be marked `NO_SHOW`
- no-show is allowed only on or after the check-in date
- payment recording recalculates:
  - paid amount
  - balance amount
  - reservation payment status
- audit logs are written for lifecycle and payment actions

## Payment handling now present

- payment methods supported:
  - `CASH`
  - `CARD`
  - `UPI`
  - `BANK_TRANSFER`
  - `ADVANCE`
  - `POSTPAID`
- reservation-level payment status now behaves as:
  - `PENDING`
  - `PARTIAL`
  - `PAID`
- seeded demo properties now include reservations with:
  - checked-in stays
  - checked-out stays
  - cancelled bookings
  - no-show bookings
  - partial payments
  - fully paid bookings

## Dashboard analytics update added on 14 April

The dashboard API now returns a richer PMS analytics payload, including:

- occupancy percent
- ADR today
- RevPAR today
- arrivals today
- departures today
- due in
- due out
- in-house reservations
- no-show count
- cancellation count
- room status summary
- revenue today
- monthly revenue
- outstanding balance
- payment snapshot
- arrival trend
- occupancy trend
- unpaid reservations
- recent audit activity

This allows the Angular dashboard to behave more like a full PMS command center instead of only showing basic room counts and movement lists.

## Super admin platform APIs added on 17 April

The backend now exposes a broader platform-admin layer beyond simple property onboarding.

### New platform admin API areas

- `GET /api/admin/platform/plans`
- `PUT /api/admin/platform/plans/{planId}`
- `GET /api/admin/platform/reports`
- `GET /api/admin/platform/support`
- `GET /api/admin/platform/support/{propertyId}`
- `PUT /api/admin/platform/support/{propertyId}`
- `POST /api/admin/platform/support/{propertyId}/open`
- `GET /api/admin/platform/communications`
- `POST /api/admin/platform/communications`
- `GET /api/admin/platform/ledger`
- `POST /api/admin/platform/ledger/properties/{propertyId}/invoice`
- `POST /api/admin/platform/ledger/invoices/{invoiceId}/payments`

### New platform data now modeled

- platform plans
- platform subscription invoices
- platform invoice payments
- property communication logs
- owner `lastLoginAt`
- property CRM stage
- account manager
- support notes
- commercial notes
- module entitlements
- organization billing / GST profile

### Platform features now covered

- subscription payment ledger
- invoice history
- plan management
- support-mode opening with audit trail
- owner access / last-login visibility
- platform-wide reports
- onboarding completeness tracking
- feature entitlement controls
- communication logging
- deeper super-admin audit visibility
- tax / commercial setup fields
- CRM / lifecycle controls

### Seeded platform context

Demo data now includes:

- starter / growth / enterprise plans
- active / partial / overdue platform invoices
- sample platform invoice payments
- support communication history
- CRM/account-manager/commercial annotations on properties
