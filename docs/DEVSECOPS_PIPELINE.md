# Enterprise DevSecOps & CI/CD Pipeline

This document details the comprehensive, multi-stage GitHub Actions pipeline driving the Carizmi Community Platform. The pipeline is engineered to go far beyond standard continuous integration by embedding strict security controls (DevSecOps), architectural invariant checks, and zero-downtime continuous deployment to Google Cloud Platform (GCP).

---

## Pipeline Architecture Overview

The pipeline is split across two primary GitHub Actions workflows:
1. **`secret-scan.yml`** — An isolated workflow dedicated exclusively to repository-wide secret leak prevention.
2. **`ci-cd.yml`** — The monolithic pipeline that orchestrates build, test, structural validation, vulnerability scanning, and deployment.

**Trigger Matrix:**
- **Pull Requests (to `main`)**: Triggers Secret Scanning, Security Audits, and CI Quality Gates. Prevents bad code or vulnerabilities from entering the default branch.
- **Push / Merge (to `main`)**: Triggers the entire suite, appending the Continuous Deployment (CD) phase to push immutable artifacts to GCP.

---

## 1. Continuous Security (DevSecOps)

Security is treated as a first-class citizen and is baked directly into the PR validation process.

### TruffleHog Secret Scanning
To prevent catastrophic credentials leaks (API keys, database passwords, JWT secrets), the pipeline utilizes the official `trufflesecurity/trufflehog` action.
- Scans the **entire git history** (fetch-depth: 0) rather than just the latest commit.
- Runs independently on all PRs to catch secrets *before* they are merged.

### Dependency Vulnerability Management
Before code is compiled, the dependency trees of both the backend and frontend are audited for known CVEs.
- **Backend (OWASP Dependency-Check):** Analyzes all Java/Maven dependencies, failing the pipeline if critical vulnerabilities (CVSS 9+) are introduced.
- **Frontend (NPM Audit):** Runs `npm audit --audit-level=critical` to ensure the React application relies on a secure dependency chain.

### Container Security (Trivy)
Before any Docker image is allowed to deploy to Google Cloud Run, it must pass a vulnerability scan.
- Uses `aquasecurity/trivy-action` to scan the newly built images.
- Scans for OS-level and library vulnerabilities, generating comprehensive reports that are attached as GitHub workflow artifacts.

---

## 2. Continuous Integration (CI) Quality Gates

The CI phase ensures that the application is functionally correct and architecturally sound.

### Backend Validation
- **Compilation & Testing:** Executes `mvn verify` to run both unit tests (Surefire) and integration tests (Failsafe).
- **Test Reporting:** XML reports are automatically uploaded as artifacts and published to the GitHub Actions UI for immediate developer feedback.

### Architectural Integrity Checks
The pipeline enforces structural invariants that protect the platform as it scales:
1. **API Contract Drift Detection:** The pipeline checks if the auto-generated `openapi.json` contract matches the actual Spring Boot controller annotations. If a developer alters an API without committing the updated contract, the build **fails immediately**.
2. **Schema Immutability Lock:** The pipeline executes a specialized `git diff` against `V1__DB_Schema_Baseline.sql`. It strictly prohibits any modifications to the baseline database schema, forcing developers to write proper forward-moving Liquibase/Flyway migrations.

### Frontend Validation
- **Type Safety & Linting:** Enforces strict TypeScript compilation (`npm run type-check`) and ESLint rules.
- **API Client Generation:** Executes Orval to auto-generate the frontend API client from the verified OpenAPI contract.
- **Testing:** Runs the Vitest suite, exporting JUnit XML reports to the GitHub UI.

---

## 3. Continuous Deployment (CD)

Upon a successful merge to `main`—and only if all DevSecOps and CI gates pass—the CD pipeline automatically ships the platform to production.

### Google Artifact Registry (GAR)
- **Immutable Tagging:** Docker Buildx compiles the `carizmi-backend` and `carizmi-frontend` images. The images are tagged using an immutable strategy (tying them to the exact Git SHA) alongside standard semantic versioning.
- **Push:** The authenticated images are securely pushed to GCP's Artifact Registry.

### Zero-Downtime Deployment to Google Cloud Run
The deployment leverages Google Cloud Run for serverless, autoscaling execution.
- **Authentication:** Uses Workload Identity Federation (WIF) instead of long-lived JSON service account keys to authenticate GitHub Actions to GCP securely.
- **Dynamic Configuration Injection:** The pipeline uses `google-github-actions/deploy-cloudrun@v2` to dynamically inject environment variables (`PROD_DB_URL`, `PROD_REDIS_HOST`) at deploy time.
- **Secret Manager Integration:** Highly sensitive variables (like `DB_PASS` and `JWT_SECRET`) are mapped directly from Google Cloud Secret Manager to the container, ensuring they never exist in plaintext in the workflow configuration.
- **CORS Management:** Dynamically updates the backend's allowed origins to match the frontend's production domains via `gcloud run services update`.

---

## Conclusion

This pipeline guarantees that **no code reaches production unless it is functionally tested, architecturally sound, and strictly audited for security vulnerabilities.** It operates entirely autonomously, allowing the engineering team to focus solely on shipping features while the platform handles safety and delivery.
