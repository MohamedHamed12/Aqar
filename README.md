
# Aqar — Real Estate Listing & Analytics Platform

**Aqar** is a production-grade real estate listing and analytics platform built for the Egyptian and MENA market.

It allows property owners and agents to publish listings, enables buyers and renters to search using advanced filters and geospatial queries, and provides market intelligence through neighborhood-level price analytics.

---

## Overview

Real estate search in Egypt and the MENA region often lacks accurate location-based discovery, price transparency, Arabic search quality, and reliable listing verification.

Aqar solves these problems with:

- Geospatial property search using PostGIS
- Arabic and English full-text search using Elasticsearch
- Saved search alerts for matching new listings
- Neighborhood price analytics and price-per-square-meter insights
- Image upload and storage through Dropbox
- Event-driven indexing, alerts, and analytics using Kafka
- Production-ready architecture with caching, observability, CI/CD, and cloud deployment support

---

## Key Features

### Property Listings

- Create, update, publish, archive, and delete listings
- Support for sale and rent listings
- Upload up to 20 property images
- Store property details such as price, area, bedrooms, amenities, location, and neighborhood
- Track listing price history automatically

### Advanced Search

- Search by purpose, property type, price range, bedrooms, area, amenities, and neighborhood
- Search by distance from a specific location
- Sort results by newest, price, distance, and price per square meter
- Arabic and English full-text search
- Elasticsearch-backed search with PostgreSQL fallback

### Geospatial Intelligence

- Find listings within a radius of a map point
- Query listings inside neighborhood boundaries
- Sort results by distance
- Store coordinates using SRID 4326 / WGS84

### Saved Search Alerts

- Users can save search criteria
- New listings are matched against saved searches
- Matching users receive email alerts
- Kafka-based event processing prevents blocking the listing creation flow

### Analytics

- Neighborhood-level median price per square meter
- Average, minimum, and maximum listing prices
- Listing count by neighborhood, purpose, and property type
- Price history tracking for individual listings
- Materialized views for efficient analytics reads

### User & Agent System

- Email/password authentication
- Google OAuth support
- JWT access tokens and refresh tokens
- Agent profiles
- Admin agent verification
- Favorites and shortlists

---

## Tech Stack

### Backend

- Java 21
- Spring Boot 3
- Spring Security 6
- Spring Data JPA
- Hibernate Spatial
- MapStruct
- Flyway

### Database & Search

- PostgreSQL 15
- PostGIS 3
- Elasticsearch 8
- Redis 7

### Messaging & Background Processing

- Apache Kafka
- Kafka consumers for indexing, alerts, and analytics
- Dead letter topics for failed event handling

### Infrastructure

- AWS ECS Fargate
- AWS RDS PostgreSQL
- AWS ElastiCache Redis
- Amazon MSK
- Dropbox API (image storage)
- Dropbox shared links (image serving)
- Application Load Balancer
- Route 53

### Observability

- Spring Boot Actuator
- Micrometer
- Prometheus
- Grafana
- CloudWatch Logs
- OpenTelemetry / Zipkin tracing

### Testing

- JUnit 5
- Mockito
- Testcontainers
- PostGIS test containers
- Elasticsearch test containers
- Redis test containers
- Kafka test containers

---

## Architecture

Aqar starts as a **modular monolith** with clear internal boundaries. This keeps deployment simple while making future microservice extraction easier.

```text
com.aqar
├── listing       # Listings, images, price history
├── search        # Elasticsearch queries and search DTOs
├── user          # Users, auth, agents, favorites
├── neighborhood  # Cities, neighborhoods, analytics
├── alert         # Saved searches and notifications
├── analytics     # Price trends and view tracking
└── shared        # Exceptions, base entities, utilities
````

The platform uses PostgreSQL as the source of truth, Elasticsearch as the search index, Redis for caching and rate limiting, and Kafka for asynchronous workflows.

```text
Client
  │
  ▼
API Gateway
  │
  ▼
Spring Boot Application
  │
  ├── PostgreSQL + PostGIS
  ├── Elasticsearch
  ├── Redis
  ├── Kafka
  └── Dropbox
```

---

## Core Architectural Decisions

### Modular Monolith First

The project begins as a modular monolith instead of microservices to reduce operational complexity, simplify transactions, and keep infrastructure costs low during early development.

### PostgreSQL + PostGIS

PostGIS enables accurate geospatial queries such as:

* Listings within a radius
* Listings inside a neighborhood polygon
* Distance-based sorting

### Elasticsearch for Search

Elasticsearch powers:

* Arabic full-text search
* Geo-distance filtering
* Relevance scoring
* Faceted search results

### Kafka for Events

Kafka is used for:

* Listing indexing
* Saved search matching
* View tracking
* Analytics processing

### Redis for Caching and Rate Limiting

Redis supports:

* Listing detail cache
* Neighborhood stats cache
* Agent profile cache
* API rate limiting
* Distributed locking

### Transactional Outbox

A transactional outbox ensures listings and related Kafka events are persisted atomically. This prevents listings from being saved without later being indexed or processed.

---

## Main API Areas

### Authentication

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
```

### Listings

```http
POST   /api/v1/listings
GET    /api/v1/listings/{id}
PATCH  /api/v1/listings/{id}
DELETE /api/v1/listings/{id}
PATCH  /api/v1/listings/{id}/status
```

### Images

```http
POST /api/v1/listings/{id}/images/upload
POST /api/v1/listings/{id}/images/confirm
DELETE /api/v1/listings/{id}/images/{imageId}
PATCH /api/v1/listings/{id}/images/order
```

### Search

```http
GET /api/v1/search
```

Example:

```http
GET /api/v1/search?lat=30.0266&lng=31.2101&radius_km=5&purpose=RENT&type=APARTMENT
```

### Saved Searches

```http
POST   /api/v1/saved-searches
GET    /api/v1/saved-searches
DELETE /api/v1/saved-searches/{id}
```

### Analytics

```http
GET /api/v1/neighborhoods/{id}/stats
GET /api/v1/listings/{id}/price-history
```

---

## Local Development

### Prerequisites

* Java 21
* Maven 3.9+
* Docker
* Docker Compose

### Quick Start (everything in Docker)

```bash
git clone https://github.com/MohamedHamed12/Aqar
cd aqar
cp .env.example .env
docker compose up --build
```

The API is available at `http://localhost:8080`.

| Endpoint | URL |
|---|---|
| API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Health check | `http://localhost:8080/actuator/health` |

### Faster Dev Loop (app on host, deps in Docker)

```bash
docker compose up -d postgres redis
mvn spring-boot:run
```

This starts only PostgreSQL and Redis in Docker while the app runs directly on your machine with hot reload. The `application.yml` defaults point at `localhost` so no env file is needed.

### Configuration

Copy `.env.example` to `.env` and adjust as needed. The `.env` file is loaded automatically by Docker Compose and is git-ignored.

Key environment variables:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/aqar
REDIS_HOST=redis
APP_SECURITY_JWT_SECRET=change-this-to-a-long-random-secret-in-production
DROPBOX_ACCESS_TOKEN=    # optional for local dev
```

---

## Configuration

Application configuration is managed through Spring profiles.

Common profiles:

```text
local
test
staging
production
```

Example:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Important environment variables:

```env
SPRING_DATASOURCE_URL=
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=

JWT_SECRET=
JWT_ACCESS_TOKEN_TTL=
JWT_REFRESH_TOKEN_TTL=

REDIS_HOST=
REDIS_PORT=

KAFKA_BOOTSTRAP_SERVERS=

ELASTICSEARCH_URIS=

DROPBOX_ACCESS_TOKEN=
```

---

## Database Migrations

Aqar uses Flyway for database schema migrations.

Migration files live in:

```text
src/main/resources/db/migration
```

Example naming:

```text
V1__create_users_table.sql
V2__create_listings_table.sql
V3__enable_postgis.sql
```

Hibernate `ddl-auto` should not be used outside local development.

---

## Testing

### Run Unit Tests

```bash
mvn test
```

### Run Integration Tests

```bash
mvn test -Dtest="**/*IntegrationTest"
```

Integration tests use Testcontainers with real infrastructure components, including PostgreSQL/PostGIS, Elasticsearch, Redis, and Kafka.

---

## CI/CD

The CI/CD pipeline includes:

1. Compile
2. Unit tests
3. Integration tests
4. Static analysis
5. OWASP dependency check
6. Docker build
7. Push image to Amazon ECR
8. Deploy to ECS

Production deployment is designed to use blue/green deployment with manual approval.

---

## Performance Targets

| Area                           | Target             |
| ------------------------------ | ------------------ |
| Search API P95 latency         | < 300ms            |
| Listing detail API P95 latency | < 100ms with cache |
| Active listings at launch      | 100,000            |
| Concurrent users               | 500                |
| New listings per day           | 10,000             |
| Uptime target                  | 99.5%              |

---

## Security

Aqar includes:

* BCrypt password hashing
* JWT authentication
* Refresh token rotation
* Role-based authorization
* Method-level security
* Structured validation errors
* Rate limiting
* HTTPS-only production traffic
* No sensitive data in JWT payloads
* Magic-byte validation for uploaded images

---

## Observability

The platform is designed with production observability in mind.

It includes:

* Structured JSON logs
* Request IDs for log correlation
* Metrics through Micrometer
* Prometheus and Grafana dashboards
* Distributed tracing
* Kafka consumer lag monitoring
* Database pool monitoring
* Elasticsearch health checks
* Redis cache metrics

---

## Roadmap

### Version 1

* Listing CRUD
* Authentication
* Image upload
* Geospatial search
* Elasticsearch search
* Saved search alerts
* Neighborhood analytics
* Production deployment

### Version 2

* ML-powered recommendations
* Duplicate listing detection
* Verified listing badge
* 3D virtual tours
* Price estimation

### Version 3

* Microservices extraction
* Multi-country support
* Agency management
* Mobile push notifications

---

## Project Status

Aqar is currently designed as a living technical project and production-grade backend architecture reference.

---

## License

Add your license here.

For example:

```text
MIT License
```

---

## Maintainers

Built and maintained by the Aqar engineering team.
