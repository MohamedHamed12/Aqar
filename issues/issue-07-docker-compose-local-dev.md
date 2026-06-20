# Issue #7 — Docker Compose local dev environment

**Labels:** `phase/1` `type/infra` `priority/critical`

**Description** A single `docker-compose up` must bring up all Phase 1 dependencies (PostgreSQL + PostGIS, Redis) and the Spring Boot application itself, ready to accept API calls.

**Current state:** `aqar/docker-compose.yml` exists with only a `postgres` service. No Redis, no app container, no Dockerfile, no `.env.example`, no actuator health endpoint.

## Tasks

- [ ] Add `spring-boot-starter-actuator` to `pom.xml`
- [ ] Configure `management.endpoints.web.exposure.include=health` in `application.yml` so `GET /actuator/health` returns `{"status":"UP"}` with DB connectivity
- [ ] Create `aqar/Dockerfile` for the Spring Boot app:
  - Multi-stage build: Maven compile stage → JRE runtime stage
  - Expose port 8080
  - Use build args for Maven cache efficiency
  - Entrypoint: `java -jar aqar.jar`
- [ ] Update `aqar/docker-compose.yml`:
  - Add `redis:7` service (no persistence needed for dev)
  - Add `app` service building from `./Dockerfile`, depends on healthy `postgres` and `redis`
  - Add health checks to `postgres` (`pg_isready`) and `redis` (`redis-cli ping`)
  - Wire env vars from an `.env` file (not hardcoded in compose)
  - Add `aqar_redis_data` volume (optional, for dev persistence)
- [ ] Enable PostGIS extension: add `CREATE EXTENSION IF NOT EXISTS postgis` init script mounted into `postgres`'s `docker-entrypoint-initdb.d/`, or add a Flyway migration `V0__enable_postgis.sql` placed before `V1` (since V3 uses `geometry` columns but no extension is created)
- [ ] Create `aqar/.env.example` with all required env vars documented:

  ```env
  # PostgreSQL
  SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/aqar
  SPRING_DATASOURCE_USERNAME=aqar
  SPRING_DATASOURCE_PASSWORD=aqar

  # Redis
  REDIS_HOST=redis
  REDIS_PORT=6379

  # JWT
  APP_SECURITY_JWT_SECRET=change-this-to-a-long-random-secret-in-production
  APP_SECURITY_JWT_ACCESS_TOKEN_TTL=15m
  APP_SECURITY_REFRESH_TOKEN_TTL=7d

  # Dropbox (optional for local dev, leave empty)
  DROPBOX_ACCESS_TOKEN=
  ```

- [ ] Add `.env` to `.gitignore` (ensure it's not committed), keep `.env.example` tracked
- [ ] Update `README.md` "Local Development" section to reflect the new workflow:
  - Prerequisites (Java 21, Maven, Docker)
  - `cp .env.example .env` (edit vars if needed)
  - `docker compose up --build` (builds app + starts all services)
  - API at `http://localhost:8080`
  - Swagger at `http://localhost:8080/swagger-ui.html`
  - Health at `http://localhost:8080/actuator/health`
  - Or use `docker compose up -d postgres redis` + `mvn spring-boot:run` for faster dev iteration

## Notes

- Kafka and Elasticsearch are not yet implemented in the codebase (search and messaging are package-info placeholders only). They will be added in later phases and are excluded from this docker-compose scope. The compose file can be extended later.
- Springdoc was added in Issue #6; `/swagger-ui.html` will already be available.
- The `postgis/postgis` image ships with PostGIS pre-installed but requires the extension to be explicitly enabled — either via init script or Flyway.

## Acceptance criteria

- [ ] `docker compose up --build` from a clean checkout produces a running API without errors
- [ ] `GET http://localhost:8080/actuator/health` returns `{"status":"UP"}`
- [ ] `POST http://localhost:8080/api/v1/auth/register` works against the local stack
- [ ] `POST http://localhost:8080/api/v1/auth/login` returns a Bearer token
- [ ] `POST http://localhost:8080/api/v1/listings` (with token) creates a listing in the local Postgres
- [ ] Flyway migrations run successfully on first startup (including PostGIS extension creation)
- [ ] App gracefully handles missing `DROPBOX_ACCESS_TOKEN` (image endpoints return appropriate errors rather than crashing on startup)
