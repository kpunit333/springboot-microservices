# Organization Service — Implementation Steps

- [x] Update `application.properties` to connect to Neon PostgreSQL
- [x] Add `name` + `password` fields to `model/OrganizationRequest.java`
- [x] Add `findByCode` + `deleteByCode` to `repository/OrganizationRepository.java`
- [x] Add `create`, `getByCode`, `deleteByCode` to `service/OrganizationService.java`
- [x] Add `POST /api/org`, `GET /api/org/{code}`, `DELETE /api/org/{code}` to `controller/OrganizationController.java`
- [x] Compile with Maven to verify
- [x] Fix JSON parse error on POST /api/org (controller accepts `JsonNode` and handles both JSON object and double-encoded JSON string payloads via lenient re-parse)
- [x] Fix DELETE `TransactionRequiredException` (added `@Transactional` to `deleteByCode`)

