# Issue #6 — Swagger UI + OpenAPI spec

**Labels:** `phase/1` `type/infra` `priority/normal`

**Description** Add interactive API documentation via Springdoc OpenAPI, accessible at `GET /swagger-ui.html`. The project currently has no OpenAPI setup and 3 controllers (AuthController, ListingController, ImageUploadController) under `/api/v1/`.

## Tasks

- [ ] Add `springdoc-openapi-starter-webmvc-ui` (v2.8.x) to `pom.xml` — no actuator dependency exists, so no actuator exclusion needed
- [ ] Create `OpenApiConfig` in `com.aqar.shared.config` with `@OpenAPIDefinition`:
  - Info: title="Aqar API", version="0.1.0", description="Real estate listing and analytics platform for the MENA market"
  - Security scheme: `BearerJwt` — type `http`, scheme `bearer`, bearerFormat `JWT`
  - `@SecurityScheme` using `SecuritySchemeType.HTTP`
  - Apply security scheme globally via `@SecurityRequirement(name = "BearerJwt")` with `operationSelector` excluding auth endpoints
- [ ] Annotate controllers and endpoints (all in `com.aqar.*.controller`):

  | Controller | `@Tag` | Endpoints + `@Operation` / `@ApiResponse` |
  |---|---|---|
  | `AuthController` | `Auth` | `POST /register`, `POST /login`, `POST /refresh`, `POST /logout` — all public, skip security requirement |
  | `ListingController` | `Listings` | `POST /` (201 created), `GET /{id}` (public, 200/404), `PUT /{id}` (200/403/404), `PATCH /{id}/status` (200/403/404), `DELETE /{id}` (204/403/404), `GET /` (paginated `Page<ListingSummaryResponse>`) |
  | `ImageUploadController` | `Listing Images` | `POST /{id}/images/upload` (multipart, 200), `POST /{id}/images/confirm` (202), `DELETE /{id}/images/{imageId}` (204), `PATCH /{id}/images/order` (204) — all require ownership |

- [ ] Add `@ApiResponse` for error schemas referencing `ErrorResponse` for relevant HTTP statuses (400, 404, 403, 409, 500)
- [ ] Configure in `application.yml`:

  ```yaml
  springdoc:
    swagger-ui:
      path: /swagger-ui.html
    api-docs:
      path: /api-docs
    show-actuator: false
    cache:
      disabled: true
    paths-to-exclude: /error
  ```

- [ ] Add `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**` to `SecurityConfig` public matchers (or use `springdoc.security.oauth2.ignore-unused=false` equivalent)
- [ ] Verify schemas render correctly in Swagger UI:
  - All request/response DTOs: `CreateListingRequest`, `UpdateListingRequest`, `PatchListingStatusRequest`, `ListingDetailResponse`, `ListingSummaryResponse`, `AuthResponse`, `LoginRequest`, `RegisterRequest`, `ConfirmUploadRequest`, `DropboxUploadResponse`, `ReorderImagesRequest`, `ListingImageResponse`
  - `ErrorResponse` and `FieldError` appear in 400/404/409 responses
  - `Page<ListingSummaryResponse>` pagination fields (`content`, `totalPages`, `totalElements`, `size`, `number`, `sort`) render correctly

## Acceptance criteria

- [ ] `GET /swagger-ui.html` loads without authentication
- [ ] `GET /v3/api-docs` returns valid OpenAPI 3.0 JSON without authentication
- [ ] The **Authorize** button works with a Bearer token obtained from `POST /api/v1/auth/login`
- [ ] Executing a protected endpoint (e.g., `POST /api/v1/listings`) from Swagger UI with the token works end-to-end
- [ ] Public endpoints (`POST /api/v1/auth/*`, `GET /api/v1/listings/{id}`) have no padlock icon and work without a token
- [ ] Error responses (400, 404, 409) display the `ErrorResponse` schema correctly

## Notes

- Actuator is not yet configured in the project, so no exclusion is needed. Add `paths-to-exclude: /error` for the default error path.
- Security config must permit Swagger UI resources: add `.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()` before `.anyRequest().authenticated()`.
