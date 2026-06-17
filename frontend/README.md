# Spendwise — Frontend

A single-page web client for the [Spendwise](../) expense-tracking API, built with **React 18, TypeScript, and Vite**. It covers registration and login, paged expense management (create / edit / delete), and a per-category spending summary rendered as a donut chart.

This package lives in the `frontend/` directory of the Spendwise monorepo and talks to the Spring Boot backend over its REST API.

## Stack

- **React 18** + **TypeScript** (strict, `verbatimModuleSyntax`)
- **Vite 5** — dev server, HMR, and production bundling
- **TanStack Query** — server-state, caching, and cache invalidation
- **React Router 6** — routing and route-based code splitting
- **React Hook Form** + **Zod** — typed forms and schema validation
- **Tailwind CSS** — styling via a small semantic design-token palette
- **Recharts** — the summary donut chart (lazy-loaded with its page)
- **Vitest** + **React Testing Library** — unit tests

## Getting started

The client expects the API to be reachable. Start the backend first (from the repository root — see the main README for options):

```bash
docker compose up --build      # brings up the API on http://localhost:8080
```

Then, in this directory:

```bash
npm install
npm run dev
```

The dev server runs on **http://localhost:5173** and proxies `/api` to `http://localhost:8080`, so the browser talks to the same origin and there are no CORS concerns in development.

## Scripts

| Script | What it does |
|--------|--------------|
| `npm run dev` | Start the Vite dev server with HMR |
| `npm run build` | Type-check (`tsc -b`) then produce a production bundle in `dist/` |
| `npm run preview` | Serve the production build locally |
| `npm run typecheck` | Type-check only, no emit |
| `npm run lint` | Run ESLint over the project |
| `npm run test` | Run the Vitest unit suite once |

`lint`, `typecheck`, `test`, and `build` all run in CI on every push.

## Project structure

```
src/
  api/         TanStack Query hooks (auth, expenses, notifications)
  auth/        Auth context, provider, and a RequireAuth route guard
  components/  Reusable UI (buttons, inputs, table, form dialog, layout, states)
  lib/         API client, formatting, category metadata, helpers (+ unit tests)
  pages/       LoginPage, ExpensesPage, SummaryPage
  types/       Shared API request/response types
  test/        Vitest setup
  App.tsx      Routes (lazy-loaded pages, public /login, protected app)
  main.tsx     App bootstrap — Query, Router, and Auth providers
```

## How it talks to the API

A small typed `fetch` wrapper in `src/lib/api.ts` is the single point of contact with the backend:

- The base URL is `import.meta.env.VITE_API_BASE_URL ?? '/api/v1'`.
- After login, the JWT is stored in `localStorage` and attached as an `Authorization: Bearer …` header on authenticated requests.
- RFC 7807 `application/problem+json` error bodies are parsed into a typed `ApiError`, including field-level validation errors which the forms map back onto the offending inputs.
- A `401` clears the stored token and emits an `auth:unauthorized` event; the auth provider listens for it and returns the user to the login screen.

## Configuration

Both are optional and only needed to override the defaults (e.g. for a production build served on a different origin):

| Variable | Default | Purpose |
|----------|---------|---------|
| `VITE_API_BASE_URL` | `/api/v1` | Base path/URL for the API |
| `VITE_CURRENCY` | `USD` | ISO 4217 currency code used to format money |

## Design notes

The UI aims for a calm, legible, fintech feel rather than a generic dashboard look: **Inter** for the interface, **Fraunces** for the wordmark and a few display moments, and **tabular figures** so monetary columns align. Colours are driven by a small set of semantic Tailwind tokens (`surface`, `canvas`, `brand`, `muted`, `alert`, …) and per-category accent colours defined as CSS custom properties, so the palette is changed in one place.

## Production build & deployment

`npm run build` emits static assets to `dist/` that can be served by any static host or CDN. Pages are code-split per route, so the chart library only loads when the summary page is opened.

In development the Vite proxy handles cross-origin calls. In production, either serve the built assets from the same origin as the API (so `/api/v1` resolves directly) or point `VITE_API_BASE_URL` at the API's URL and enable CORS on the backend for that origin.
