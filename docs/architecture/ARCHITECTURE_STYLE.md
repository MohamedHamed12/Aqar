# Architecture Style — Aqar Platform

The Aqar platform follows a **Modular Monolith** architectural style as its starting point, layered on top of several well-established patterns that govern how the system is organized, how it communicates across boundaries, and how it behaves under failure.

Below is a concise, well-structured description of the architecture patterns and tradeoffs.

---

## 1. Modular Monolith (Primary Style)

The application ships as one deployable Spring Boot JAR, but internals are divided into named modules with strictly enforced boundaries.

Package layout example:

```
com.aqar
├── listing
├── search
├── user
├── neighborhood
├── alert
├── analytics
└── shared
```

Each module owns its entities, repositories, services, and controllers. Cross-module calls happen only through service interfaces — never by a repository in module A calling a repository in module B directly.

Why not microservices day one:
- Distributed transactions are hard and add complexity.
- Operational overhead scales with service count.
- Package/module boundaries prepare the codebase for safe extraction later.

---

## 2. Layered Architecture (Within Each Module)

Controller → Service → Repository

- Controllers: thin, validate input and map DTOs — no business logic.
- Services: own business logic and transaction boundaries (`@Transactional`).
- Repositories: data access only.

Enforced by code review and structure.

---

## 3. Domain-Driven Design Principles (Applied Selectively)

- Ubiquitous language (Listing, PriceHistory, AgentProfile).
- Module = Bounded Context.
- Value objects for domain concepts (e.g., `Money`, `SearchCriteria`).

---

## 4. Event-Driven Architecture (Cross-Boundary Communication)

Publishing events instead of directly calling other modules decouples the write path from consumers (indexer, alerting, analytics). Eventual consistency is an explicit tradeoff.

---

## 5. Transactional Outbox Pattern

Use an outbox table written in the same DB transaction as the domain change. A poller sends outbox rows to Kafka, guaranteeing delivery even across crashes.

---

## 6. CQRS (Light Form)

Separate read models from write models at the representation layer (DTOs, Elasticsearch docs, summary/detail responses).

---

## 7. Repository Pattern

All DB access goes through Spring Data JPA repositories. Use `@Query` or native SQL for complex queries; prefer readable named queries over huge derived method names.

---

## 8. Circuit Breaker Pattern (Resilience)

Wrap external dependencies (Elasticsearch, external APIs) with circuit breakers to provide graceful degradation and automatic recovery.

---

## 9. Cache-Aside Pattern (Two-Level)

L1: Caffeine (in-process), L2: Redis (distributed). Application explicitly populates/evicts caches; search results are not cached.

---

## 10. API Gateway Pattern

Gateway handles JWT validation, rate limiting, and routing (Spring Cloud Gateway). Services trust the gateway for auth claims.

---

## 11. Blue/Green Deployments

Use blue/green on AWS ECS with backwards-compatible schema migrations.

---

## Architecture Principles Summary

| Principle | Implementation |
|---|---|
| Single source of truth | PostgreSQL for entities; Elasticsearch as derived index |
| Fail gracefully | Postgres fallback for search; degraded UX without downtime |
| No shared mutable state | Modules communicate via events or interfaces |
| DTOs only at controller boundary | MapStruct for mappings |
| Versioned schema changes | Flyway migrations; `ddl-auto=validate` in non-local profiles |
| Tests use real infra | Testcontainers for integration tests |
| Observability required | Trace ID carried through requests and Kafka headers |

---

## Visual System Diagram (Mermaid)

```mermaid
flowchart LR
  subgraph Gateway
    GW[Spring Cloud Gateway]\nJWT Validation, Rate Limiting
  end

  GW --> API[Spring Boot Modular Monolith]

  subgraph Monolith
    API --> ListingModule[Listing Module]
    API --> SearchModule[Search Module]
    API --> UserModule[User / Auth]
    API --> AlertModule[Alerting]
    API --> AnalyticsModule[Analytics]
    API --> Shared[Shared / Common]
  end

  ListingModule --> Postgres[(PostgreSQL)]
  SearchModule --> ES[(Elasticsearch)]
  UserModule --> Postgres

  ListingModule --> Outbox[(Outbox Table)]
  Outbox --> Kafka[(Kafka)]

  Kafka --> IndexerWorker[Indexer Worker]
  Kafka --> AlertWorker[Alert Worker]
  Kafka --> AnalyticsWorker[Analytics Worker]

  IndexerWorker --> ES
  AlertWorker --> Email[(SMTP/SES)]
  AnalyticsWorker --> AnalyticsDB[(Analytics Store)]

  Postgres -- TestContainers --> Tests[Testcontainers]
  ES -- CircuitBreaker --> API
  Redis[(Redis)] --- API

  click Postgres "https://www.postgresql.org/" "PostgreSQL"
```

---

This document is intended to live in `docs/architecture/ARCHITECTURE_STYLE.md` and serve as the authoritative architecture overview for onboarding, design decisions, and future extraction to microservices.

If you want, I can:
- open a GitHub issue summarizing goals and tasks from this doc,
- create a pull request with this doc added,
- or generate PNG/SVG of the mermaid diagram and add it to the repo.
