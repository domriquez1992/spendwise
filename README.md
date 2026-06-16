# Spendwise — Expense Tracker REST API

![CI](https://github.com/domriquez1992/spendwise/actions/workflows/ci.yml/badge.svg)

A small but production-shaped REST API for tracking personal expenses, built with **Java 21** and **Spring Boot 4**. It demonstrates a clean layered architecture, input validation, consistent error handling, an aggregation endpoint, automated tests, containerization, and a CI pipeline.

---

## Tech stack

| Area | Choice |
|------|--------|
| Language | Java 21 (LTS) |
| Framework | Spring Boot 4.0.x (Spring Web, Spring Data JPA, Bean Validation) |
| Database | PostgreSQL 17 |
| Build | Maven |
| Testing | JUnit 5, Mockito, Spring MockMvc, AssertJ |
| Container | Docker (multi-stage build), Docker Compose |
| CI | GitHub Actions |

---

## Architecture

A feature-based package layout (`expense`) with a conventional three-layer flow:

```
HTTP  ─▶  ExpenseController        (REST endpoints, validation, status codes)
          │
          ▼
          ExpenseService           (business logic, transactions)
          │
          ▼
          ExpenseRepository        (Spring Data JPA + a JPQL aggregation query)
          │
          ▼
          PostgreSQL
```

DTOs (`ExpenseRequest` / `ExpenseResponse`) keep the API contract separate from the JPA entity, so the two can change independently and internal fields are never exposed over the wire. Errors are produced centrally as RFC 7807 `ProblemDetail` responses.

---

## Getting started

### Option A — Docker (recommended, nothing to install but Docker)

```bash
docker compose up --build
```

This starts PostgreSQL and the API together. The API is available at `http://localhost:8080`.

### Option B — Run locally

Requires JDK 21, Maven, and a PostgreSQL instance. Point the app at your database via environment variables (defaults shown):

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/spendwise
export SPRING_DATASOURCE_USERNAME=spendwise
export SPRING_DATASOURCE_PASSWORD=spendwise

mvn spring-boot:run
```

---

## Configuration

All datasource settings are externalized (12-factor style) and read from environment variables, with sensible local defaults:

| Variable | Default |
|----------|---------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/spendwise` |
| `SPRING_DATASOURCE_USERNAME` | `spendwise` |
| `SPRING_DATASOURCE_PASSWORD` | `spendwise` |

---

## API reference

Base path: `/api/v1/expenses`

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| `POST` | `/api/v1/expenses` | Create an expense | `201 Created` |
| `GET` | `/api/v1/expenses` | List expenses (paged, sorted) | `200 OK` |
| `GET` | `/api/v1/expenses/{id}` | Get one expense | `200 OK` |
| `PUT` | `/api/v1/expenses/{id}` | Update an expense | `200 OK` |
| `DELETE` | `/api/v1/expenses/{id}` | Delete an expense | `204 No Content` |
| `GET` | `/api/v1/expenses/summary` | Totals per category (+ grand total) | `200 OK` |

### Create

```bash
curl -X POST http://localhost:8080/api/v1/expenses \
  -H "Content-Type: application/json" \
  -d '{"description":"Lunch","amount":120.50,"category":"FOOD","date":"2026-06-16"}'
```

Response (`201 Created`):

```json
{
  "id": 1,
  "description": "Lunch",
  "amount": 120.50,
  "category": "FOOD",
  "date": "2026-06-16",
  "createdAt": "2026-06-16T10:15:30Z"
}
```

Valid categories: `FOOD`, `TRANSPORT`, `HOUSING`, `ENTERTAINMENT`, `HEALTH`, `UTILITIES`, `OTHER`.

### List (paged)

```bash
curl "http://localhost:8080/api/v1/expenses?page=0&size=10&sort=date,desc"
```

### Summary (optional date range)

```bash
curl "http://localhost:8080/api/v1/expenses/summary?from=2026-06-01&to=2026-06-30"
```

```json
{
  "categories": [
    { "category": "FOOD", "total": 540.00 },
    { "category": "TRANSPORT", "total": 220.00 }
  ],
  "grandTotal": 760.00
}
```

### Validation errors

A bad payload returns `400 Bad Request` as a `ProblemDetail`:

```json
{
  "type": "about:blank",
  "title": "Validation Failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "errors": {
    "description": "description is required",
    "amount": "amount must be greater than zero"
  }
}
```

---

## Testing

```bash
mvn test
```

- **Service unit tests** (`ExpenseServiceImplTest`) — pure JUnit 5 + Mockito, no Spring context or database. Covers creation, retrieval, the not-found path, delete guarding, and summary aggregation.
- **Web layer tests** (`ExpenseControllerTest`) — `@WebMvcTest` slice with `MockMvc`, verifying status codes, validation responses, and error mapping.

Both run without a live database, so CI stays fast and deterministic.

---

## Design decisions

A few choices worth calling out:

- **`BigDecimal` for money, never `double`.** Binary floating point can't represent values like `0.10` exactly; using it for currency invites rounding errors.
- **DTOs over exposing the entity.** The request/response records form a stable API contract decoupled from the persistence model.
- **Centralized error handling with `ProblemDetail` (RFC 7807).** Every endpoint reports errors in one consistent, documented shape.
- **`open-in-view: false`.** The Open Session In View pattern is disabled so the persistence context stays inside the transaction boundary rather than leaking into view rendering.
- **Database-side aggregation.** The summary uses a grouped JPQL query instead of pulling every row into memory and summing in Java.
- **Constructor injection.** Dependencies are `final` and injected via the constructor — immutable, explicit, and easy to test.

---

## Roadmap

This is the foundation of a larger portfolio. Natural next steps:

- Interactive API docs with OpenAPI / Swagger UI
- JWT / OAuth2 authentication and per-user expenses
- Versioned schema migrations (Flyway)
- Integration tests against a real PostgreSQL using Testcontainers
- Containerized deployment to a cloud provider
