# Car Rental

A simulated car rental reservation system built as a React and Java
technical exercise.

## Repository structure

```text
.
├── backend   Spring Boot application
├── frontend  React application
└── docs      Architecture documentation
```

## Prerequisites

- Java 21
- Node.js
- npm

## Features

- Check vehicle availability for a selected period
- Reserve a Sedan, SUV or Van
- Prevent overbooking within a single application instance
- View, filter and cancel reservations
- Validate reservation dates on both frontend and backend

## Running locally

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Runs on http://localhost:8080.

### Frontend

```bash
cd frontend
npm ci
npm run dev
```

Runs on http://localhost:5173. The Vite development server proxies
`/api` requests to the backend.

## Tests

```bash
cd backend
./mvnw clean verify

cd frontend
npm test
npm run lint
npm run build
```

## Design notes

- Reservations use half-open periods: `[start, end)`.
- Availability is calculated per car type.
- Reservation creation is synchronized per car type to prevent
  overbooking inside one JVM.
- Expected business outcomes (unavailable car, non-cancellable
  reservation, invalid period) are represented explicitly rather than
  through generic exceptions.
- The backend uses an in-memory repository.

## Limitations

- Data is lost when the backend restarts.
- The concurrency mechanism works only within a single application
  instance.
- Authentication is represented by a fixed demo customer.
- UTC is used as the demo business time zone.
