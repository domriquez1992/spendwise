# Spendwise — Expense Tracker REST API

![CI](https://github.com/domriquez1992/spendwise/actions/workflows/ci.yml/badge.svg)

A small but production-shaped REST API for tracking personal expenses, built with **Java 21** and **Spring Boot 4**. It demonstrates a clean layered architecture, **stateless JWT authentication** (Spring Security), **per-user data ownership** and **role-based authorization**, **event-driven processing with Apache Kafka** (creating an expense asynchronously raises budget notifications), **polyglot persistence** (a **MongoDB** audit log of domain events and a **Redis** cache over the spending summary), health probes via Spring Boot Actuator, input validation, consistent error handling, an aggregation endpoint, automated tests (including full security, data-isolation, and end-to-end Kafka/Mongo/Redis integration tests), full-stack containerization with Docker Compose, and a CI pipeline that builds the image and smoke-tests the running stack.

---

## Tech stack

| Area | Choice |
|------|--------|
| Language | Java 21 (LTS) |
| Framework | Spring Boot 4.0.x (Spring Web, Spring Data JPA, Bean Validation) |
| Security | Spring Security 7, JWT (jjwt 0.12), BCrypt password hashing |
| Messaging | Apache Kafka (Spring for Apache Kafka 4, KRaft) |
| Data stores | PostgreSQL 17 (transactional data), MongoDB 7 (audit log), Redis 7 (summary cache); H2 in-memory for tests |
| Caching | Spring Cache backed by Redis (`RedisCacheManager`) |
| Observability | Spring Boot Actuator — `/actuator/health` with liveness/readiness probe groups |
| Build | Maven |
| Testing | JUnit 5, Mockito, Spring MockMvc, AssertJ, Testcontainers (Kafka, MongoDB, Redis), Awaitility |
| Container | Docker (multi-stage build, non-root), Docker Compose (full stack) |
| CI | GitHub Actions (build + tests, plus a Compose smoke test) |

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

### Event-driven processing (Kafka)

Creating an expense does more than write a row: it emits an event that is processed **asynchronously**, so the write path stays fast and the reaction (checking budgets, raising notifications) is decoupled from it.

```
POST /expenses ─▶ ExpenseService.create()
                    │  saves the expense, then publishes a Spring event
                    ▼
                  ExpenseEventPublisher          (@TransactionalEventListener, AFTER_COMMIT)
                    │  relays to Kafka only once the DB transaction has committed
                    ▼
                  Kafka topic  "expense.created"  (JSON value, keyed by username)
                    │
                    ▼
                  ExpenseEventListener           (@KafkaListener)
                    │  sums the user's spend in that category for the month;
                    │  if it exceeds the budget limit…
                    ▼
                  Notification  ─▶  GET /api/v1/notifications
```

Two details make this correct rather than just wired up:

- **Publish after commit, not during.** The service publishes an in-process Spring application event; a listener bound to `AFTER_COMMIT` is what actually sends to Kafka. If the transaction rolls back, no message is ever sent — this sidesteps the dual-write hazard where a consumer reacts to data that was never persisted (or that it cannot yet read).
- **Keyed by username.** Each user's events share a partition key, so a single user's events preserve their order.

The producer/consumer are split across an `event` package (the event and the Kafka relay) and a `notification` package (the consumer, the `Notification` entity, and its read endpoint). The notification references its owner by username — the data already on the event — keeping that module independent of the user/expense aggregates.

### Polyglot persistence — audit log (MongoDB) and summary cache (Redis)

Different data shapes get the store that fits them. The transactional expense and user data stay in **PostgreSQL**; two cross-cutting concerns each get a more suitable engine:

- **Audit log → MongoDB.** Registrations, logins, and every expense create/update/delete publish an in-process `AuditableEvent`; a listener bound to `AFTER_COMMIT` writes an append-only document to MongoDB (so a rolled-back action leaves no misleading entry). Audit data is append-only, write-heavy, and queried by a few fields — a natural fit for a document store rather than another relational table. Each user reads their own trail at `GET /api/v1/audit`; admins see everything at `GET /api/v1/audit/all`. Writes are best-effort: a failure to record an audit entry is logged and swallowed so it can never break the user-facing operation.
- **Summary cache → Redis.** The per-user spending summary (a `GROUP BY` aggregation) is cached in Redis keyed by username, and evicted whenever that user writes an expense. The cache lives in its own bean so Spring's caching proxy actually intercepts the call, and only the unfiltered default view is cached, which keeps the key simple and eviction precise.

Both are wired in tests with real **Testcontainers** MongoDB and Redis instances (via Spring Boot's `@ServiceConnection`), so the audit trail and the cache populate/evict against the genuine engines, not mocks.

---

## Getting started

### Option A — Docker Compose (recommended, nothing to install but Docker)

```bash
docker compose up --build
```

This builds the application image and starts the **entire stack** — PostgreSQL, a single-node Kafka broker (KRaft mode, no ZooKeeper), MongoDB, Redis, and the API. Each datastore has a healthcheck, and the app waits for all four to report healthy before it starts. Once up:

- API: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health` (reports `UP` once the datasource, MongoDB, and Redis are reachable; Kafka readiness is gated by the stack's healthchecks)

### Option B — Run locally

Requires JDK 21 and Maven, plus the four backing services. The quickest path is to start just those with Compose and run the app from your IDE/Maven against them; otherwise point the app at your own instances via the environment variables below (defaults shown):

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/spendwise
export SPRING_DATASOURCE_USERNAME=spendwise
export SPRING_DATASOURCE_PASSWORD=spendwise
export SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/spendwise
export SPRING_DATA_REDIS_HOST=localhost

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
| `SPRING_DATA_MONGODB_URI` | `mongodb://localhost:27017/spendwise` |
| `SPRING_DATA_REDIS_HOST` | `localhost` |
| `SPRING_DATA_REDIS_PORT` | `6379` |
| `JWT_SECRET` | dev-only default (**override in production**) |
| `JWT_EXPIRATION_MS` | `3600000` (1 hour) |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `APP_BUDGET_MONTHLY_CATEGORY_LIMIT` | `1000` |

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

### Notifications

Base path: `/api/v1/notifications` — **requires a `Bearer` token**; returns only the caller's own notifications. Notifications are generated automatically by the Kafka consumer when a user's spending in a category for the month exceeds `APP_BUDGET_MONTHLY_CATEGORY_LIMIT`.

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| `GET` | `/api/v1/notifications` | List the current user's budget notifications (newest first) | `200 OK` |

```bash
curl http://localhost:8080/api/v1/notifications \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

```json
[
  {
    "id": 1,
    "message": "You have spent 1120.50 on FOOD so far this month, over your budget of 1000.",
    "createdAt": "2026-06-16T10:15:31Z"
  }
]
```

### Audit log

Base path: `/api/v1/audit` — **requires a `Bearer` token**. Entries are recorded automatically (in MongoDB) for registrations, logins, and expense create/update/delete, newest first.

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| `GET` | `/api/v1/audit` | The caller's own audit trail | `200 OK` |
| `GET` | `/api/v1/audit/all` | The full audit trail across all users (**admin only**) | `200 OK` |

```bash
curl http://localhost:8080/api/v1/audit \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

```json
[
  {
    "id": "665f...",
    "type": "EXPENSE_CREATED",
    "username": "alice",
    "detail": "Created expense 'Lunch'",
    "entityId": 1,
    "timestamp": "2026-06-16T10:15:30Z"
  }
]
```

---

## Testing

```bash
mvn test
```

- **Service unit tests** (`ExpenseServiceImplTest`) — pure JUnit 5 + Mockito, no Spring context or database. The current user is supplied through a mocked `CurrentUserProvider`, so the owner-scoping logic is exercised without a security context. Covers creation (owner assignment), owner-scoped retrieval, the not-found/not-owned path, delete guarding, and summary aggregation.
- **Web layer tests** (`ExpenseControllerTest`) — `@WebMvcTest` slice with `MockMvc`, verifying status codes, validation responses, and error mapping. Security filters are disabled in this slice so it stays focused on the controller.
- **Security & isolation integration test** (`AuthIntegrationTest`) — a full `@SpringBootTest` against an in-memory **H2** database that drives the real filter chain, JWT issuance/verification, and BCrypt. It registers, logs in, and proves the protected endpoint returns `401` without a token and `200` with a valid one (plus wrong-password and duplicate-username cases). It also proves **data isolation** — one user gets `404` trying to read or delete another user's expense — and **role enforcement** — a `USER` gets `403` on the admin endpoint while an `ADMIN` gets `200`.
- **Event-driven integration test** (`KafkaNotificationIntegrationTest`) — a full `@SpringBootTest` that exercises the Kafka pipeline end to end against a **real broker started with Testcontainers** (`apache/kafka`, version-matched to the client). It creates an over-budget expense through the API, then polls (with Awaitility) until the asynchronously-produced notification has been consumed and persisted, and finally reads it back through `GET /api/v1/notifications`. Both integration tests share a single Spring context and one reused broker via a common `AbstractIntegrationTest` base class.
- **Polyglot-persistence integration test** (`AuditAndCacheIntegrationTest`) — a full `@SpringBootTest` against **real MongoDB and Redis containers** (Testcontainers, wired via Spring Boot's `@ServiceConnection`). It proves the MongoDB audit log records the registration, login, and expense-creation events, and that the Redis summary cache is populated on first read and evicted on the next write.

The unit and slice tests run without any database or broker. The integration tests use H2 plus throwaway Kafka, MongoDB, and Redis containers, so the only external requirement in CI is a Docker daemon — which the GitHub Actions runners provide out of the box. A separate CI job builds the Docker image and brings up the full Docker Compose stack, waiting on the app's `/actuator/health` endpoint to confirm it connected to every backing service before passing.

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
- **Publish to Kafka after commit, via a Spring event.** The expense service raises an in-process application event; a `@TransactionalEventListener(AFTER_COMMIT)` is what relays it to Kafka. This guarantees a message is only sent once the originating transaction has committed, so consumers never react to rolled-back or not-yet-visible data — the lightweight alternative to a full transactional-outbox for a single service. (It also means the integration tests commit for real rather than rolling back, so the event actually fires.)
- **Jackson-3 Kafka serializers.** Spring Boot 4 / Spring for Apache Kafka 4 ship Jackson 3, so events use `JacksonJsonSerializer` / `JacksonJsonDeserializer` with the consumer pinned to the event type and a trusted-packages allow-list, rather than the now-deprecated Jackson-2 serializers.
- **A real broker in tests, not a mock.** The Kafka flow is verified against an actual broker via Testcontainers (pinned to the same Kafka version as the client) rather than mocking `KafkaTemplate` — the test proves the message genuinely round-trips through Kafka and is consumed, which a mock could not.

---

## Roadmap

This is the foundation of a larger portfolio. Natural next steps:

- ~~JWT authentication~~ ✅ done — see the Authentication section above
- ~~Per-user data ownership and role-based authorization (`USER` / `ADMIN`)~~ ✅ done — see the Admin section and design decisions
- ~~Event-driven processing with Apache Kafka~~ ✅ done — see the Event-driven processing section and the Notifications endpoint
- ~~Polyglot persistence — MongoDB audit log and Redis summary cache~~ ✅ done — see the Polyglot persistence section and the Audit endpoints
- ~~Full-stack Docker Compose (app + PostgreSQL + Kafka + MongoDB + Redis), health-gated startup, and a CI smoke test of the running stack~~ ✅ done — see Getting started
- Kubernetes deployment (the liveness/readiness probes are already in place)
- Deployment to a cloud provider with a CI/CD pipeline
- Interactive API docs with OpenAPI / Swagger UI
- Versioned schema migrations (Flyway)
