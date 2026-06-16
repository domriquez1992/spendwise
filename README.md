# Spendwise — Expense Tracker REST API

![CI](https://github.com/domriquez1992/spendwise/actions/workflows/ci.yml/badge.svg)

A small but production-shaped REST API for tracking personal expenses, built with **Java 21** and **Spring Boot 4**. It demonstrates a clean layered architecture, **stateless JWT authentication** (Spring Security), **per-user data ownership** and **role-based authorization**, input validation, consistent error handling, an aggregation endpoint, automated tests (including full security and data-isolation integration tests), containerization, and a CI pipeline.

---

## Tech stack

| Area | Choice |
|------|--------|
| Language | Java 21 (LTS) |
| Framework | Spring Boot 4.0.x (Spring Web, Spring Data JPA, Bean Validation) |
| Security | Spring Security 7, JWT (jjwt 0.12), BCrypt password hashing |
| Database | PostgreSQL 17 (H2 in-memory for tests) |
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

Authentication is a separate concern in its own packages:

```
HTTP  ─▶  JwtAuthenticationFilter   (reads "Authorization: Bearer <token>", verifies it,
          │                          sets the SecurityContext — once per request)
          ▼
          SecurityFilterChain       (stateless; /api/v1/auth/** is public, everything else
          │                          requires a valid token)
          ▼
          AuthController ─▶ AuthService ─▶ { UserRepository, BCrypt, JwtService }
```

The `auth`, `security`, and `user` packages hold authentication and authorization; the `admin` package holds admin-only endpoints. Two authorization layers work together: the **filter chain** decides *whether you are authenticated*, and **method security** (`@PreAuthorize`) decides *whether your role permits a given operation*. Within the expense layer, every query is **scoped to the current user** — the service resolves the caller via a small `CurrentUserProvider` and only ever reads or writes rows the caller owns, so one user can never see another's data.

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
| `JWT_SECRET` | dev-only default (**override in production**) |
| `JWT_EXPIRATION_MS` | `3600000` (1 hour) |

> **Security note:** `JWT_SECRET` ships with a development default so the app runs out of the box. In any real deployment it **must** be overridden with a strong, secret value of at least 32 bytes — anyone who knows the signing secret can mint valid tokens.

---

## API reference

### Authentication

All expense endpoints require a JWT. Register a user, log in to receive a token, then send it as a `Bearer` token on every subsequent request.

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| `POST` | `/api/v1/auth/register` | Create a user account | `201 Created` |
| `POST` | `/api/v1/auth/login` | Exchange credentials for a JWT | `200 OK` |

**Register**

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}'
```

**Log in** — returns a token:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}'
```

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "username": "alice"
}
```

**Call a protected endpoint** — pass the token in the `Authorization` header:

```bash
curl http://localhost:8080/api/v1/expenses \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

A request to a protected endpoint without a valid token returns `401 Unauthorized`.

### Expenses

Base path: `/api/v1/expenses` — **all endpoints below require a `Bearer` token** (see above). Every expense is owned by the user who created it; each request only ever sees that user's own expenses. Requesting an expense that belongs to someone else returns `404 Not Found` — the same as a non-existent id, so the API never reveals that another user's row exists.

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

### Admin

Base path: `/api/v1/admin` — **requires a `Bearer` token from a user with the `ADMIN` role.** Access is enforced with method security (`@PreAuthorize("hasRole('ADMIN')")`); an authenticated `USER` calling these endpoints receives `403 Forbidden`.

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| `GET` | `/api/v1/admin/users` | List all users (id, username, role — never the password) | `200 OK` |

New accounts always register as `USER`. The first `ADMIN` is provisioned out of band (a direct row in the `users` table); promoting users through the API is a natural future addition.

---

## Testing

```bash
mvn test
```

- **Service unit tests** (`ExpenseServiceImplTest`) — pure JUnit 5 + Mockito, no Spring context or database. The current user is supplied through a mocked `CurrentUserProvider`, so the owner-scoping logic is exercised without a security context. Covers creation (owner assignment), owner-scoped retrieval, the not-found/not-owned path, delete guarding, and summary aggregation.
- **Web layer tests** (`ExpenseControllerTest`) — `@WebMvcTest` slice with `MockMvc`, verifying status codes, validation responses, and error mapping. Security filters are disabled in this slice so it stays focused on the controller.
- **Security & isolation integration test** (`AuthIntegrationTest`) — a full `@SpringBootTest` against an in-memory **H2** database that drives the real filter chain, JWT issuance/verification, and BCrypt. It registers, logs in, and proves the protected endpoint returns `401` without a token and `200` with a valid one (plus wrong-password and duplicate-username cases). It also proves **data isolation** — one user gets `404` trying to read or delete another user's expense — and **role enforcement** — a `USER` gets `403` on the admin endpoint while an `ADMIN` gets `200`.

The unit and slice tests run without any database; the integration test uses H2, so CI still needs no external services.

---

## Design decisions

A few choices worth calling out:

- **`BigDecimal` for money, never `double`.** Binary floating point can't represent values like `0.10` exactly; using it for currency invites rounding errors.
- **DTOs over exposing the entity.** The request/response records form a stable API contract decoupled from the persistence model.
- **Centralized error handling with `ProblemDetail` (RFC 7807).** Every endpoint reports errors in one consistent, documented shape.
- **`open-in-view: false`.** The Open Session In View pattern is disabled so the persistence context stays inside the transaction boundary rather than leaking into view rendering.
- **Database-side aggregation.** The summary uses a grouped JPQL query instead of pulling every row into memory and summing in Java.
- **Constructor injection.** Dependencies are `final` and injected via the constructor — immutable, explicit, and easy to test.
- **Stateless JWT, no server sessions.** The token carries identity on each request, so the API holds no session state — the natural fit for a horizontally scalable REST service. CSRF protection is disabled deliberately: there is no cookie-based session or browser form to protect.
- **Passwords are only ever stored as BCrypt hashes.** The raw password is hashed on registration and never persisted or logged; login compares against the stored hash.
- **A custom `OncePerRequestFilter` for tokens.** Authentication is handled by a single filter placed before the username/password filter, which keeps the flow explicit and easy to reason about.
- **Gson serializer for jjwt, on purpose.** The JWT library serializes claims with Gson rather than Jackson, keeping token (de)serialization independent of the application's own JSON stack and its version.
- **`401` vs `403`.** Unauthenticated requests to protected endpoints return `401 Unauthorized` via an explicit authentication entry point, rather than Spring's default `403`, which is the more accurate signal for "you need to log in."
- **Non-revealing login errors.** Both "no such user" and "wrong password" return the same generic message so the API doesn't disclose which usernames exist.
- **Owner-scoped queries, not in-memory filtering.** Each expense belongs to a user, and the repository queries are scoped by owner at the database level (`findByOwnerUsername`, `findByIdAndOwnerUsername`). The application never loads a row it then has to check ownership on — it only ever fetches rows the caller owns.
- **`404`, not `403`, for another user's data.** Asking for an expense you don't own returns `404 Not Found`, identical to a non-existent id. Returning `403` would confirm the row exists; `404` reveals nothing about other users' data.
- **A `CurrentUserProvider` instead of static security calls in services.** The service layer depends on a tiny injectable component to learn who is logged in, rather than reaching for Spring Security's static `SecurityContextHolder`. This keeps business logic free of framework plumbing and trivially unit-testable with a mock.
- **Method security for roles (`@PreAuthorize`).** Role checks live as annotations on the admin endpoints (`hasRole('ADMIN')`), enforced by a Spring AOP interceptor before the method runs — co-located with the code they protect and independent of URL-pattern configuration. A denied call surfaces as a `403` `ProblemDetail` via a dedicated exception handler.

---

## Roadmap

This is the foundation of a larger portfolio. Natural next steps:

- ~~JWT authentication~~ ✅ done — see the Authentication section above
- ~~Per-user data ownership and role-based authorization (`USER` / `ADMIN`)~~ ✅ done — see the Admin section and design decisions
- Interactive API docs with OpenAPI / Swagger UI
- Versioned schema migrations (Flyway)
- Event-driven processing with Apache Kafka
- Containerized deployment to a cloud provider
