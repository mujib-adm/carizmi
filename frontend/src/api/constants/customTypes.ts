/**
 * Custom Type Definitions — Frontend-Only DTOs
 *
 * This file contains TypeScript types for API request/response shapes that do NOT
 * have a corresponding Java DTO on the backend (and therefore are not auto-generated
 * by Orval from the OpenAPI spec).
 *
 * Common reasons a type lives here:
 *   - The endpoint is handled by a framework filter (e.g. Spring Security) rather
 *     than a @RestController, so Springdoc does not include it in openapi.json.
 *   - The shape is a frontend-only concern (e.g. composite view models).
 *
 * ⚠️  Before adding a new type here, verify that it truly cannot be generated.
 *     If a backend DTO exists (or can be created), prefer the auto-generated type
 *     from  src/api/generated/types  to keep the contract in sync.
 *
 * @see docs/API_GENERATION.md for the full code-generation pipeline documentation.
 */

// ---------------------------------------------------------------------------
// Auth
// ---------------------------------------------------------------------------

/**
 * Login request payload consumed by Spring Security's UsernamePasswordAuthenticationFilter.
 * There is no backend DTO for this — the filter reads `username` and `password` directly
 * from the request body.
 */
export interface LoginRequestDto {
  username: string;
  password: string;
}