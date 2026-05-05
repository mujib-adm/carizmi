# Security Policy

The Carizmi project takes security seriously. We appreciate responsible disclosure of
vulnerabilities and work to address confirmed issues promptly.

---

## Supported Versions

| Version | Supported          |
|---------|--------------------|
| 1.0.x   | ✅ Security updates |

We currently maintain a single release branch (`main`). Security patches are applied
to the latest version only.

---

## Reporting a Vulnerability

**Please do NOT report security vulnerabilities through public GitHub issues.**

If you discover a security vulnerability, please report it through one of the following
channels:

### Preferred: GitHub Private Vulnerability Reporting

1. Navigate to the [Security Advisories](https://github.com/mujib-adm/carizmi/security/advisories) page
2. Click **"Report a vulnerability"**
3. Provide a detailed description of the vulnerability

### Alternative: Email

If you are unable to use GitHub's private reporting, you may email:

📧 **mujib.adm@gmail.com**

### What to Include

- **Description** of the vulnerability
- **Steps to reproduce** (if applicable)
- **Impact assessment** — what an attacker could achieve
- **Affected component** (backend, frontend, framework, infrastructure)
- **Suggested fix** (if you have one)

---

## Response Policy

| Stage | Timeline |
|---|---|
| **Acknowledgement** | Within 48 hours of report |
| **Initial Assessment** | Within 5 business days |
| **Confirmed Fix** | Within 30 days for critical/high severity |
| **Public Disclosure** | Coordinated with reporter after fix is deployed |

We follow a coordinated disclosure model. We will work with reporters to agree on
a disclosure timeline and will credit reporters (unless anonymity is requested)
in the advisory.

---

## Scope

### In Scope

The following areas are covered by this security policy:

- **Authentication & Authorization** — JWT token handling, session management, role-based access control
- **API Security** — Input validation, injection vulnerabilities (SQL, XSS, SSRF), CORS misconfiguration
- **Secrets Management** — Exposure of API keys, database credentials, JWT secrets in source code or CI/CD logs
- **Dependency Vulnerabilities** — Known CVEs in backend (Maven) or frontend (npm) dependencies
- **Infrastructure Security** — Docker container escapes, Dockerfile misconfigurations, CI/CD pipeline security
- **Data Protection** — Unauthorized access to member records, payment data, or PII

### Out of Scope

- Vulnerabilities in third-party services (Google Cloud, GitHub, Upstash)
- Social engineering attacks against maintainers
- Denial-of-service (DoS) attacks against production infrastructure
- Issues in development-only configurations (e.g., `docker-compose.yml` with default passwords)
- Vulnerabilities requiring physical access to the server
- Issues already reported in public CVE databases for dependencies (use Dependabot instead)

---

## Security Architecture Overview

### Authentication

- **Stateless JWT** with HS512 HMAC signing (512-bit key minimum)
- **Refresh token rotation** via Redis with configurable TTL
- **BCrypt** password hashing with adaptive cost factor
- **Account lockout** after configurable failed login attempts (default: 5 attempts / 15-minute lockout)
- **Rate limiting** via Bucket4j (configurable requests per minute per IP)

### Data Protection

- All database credentials, JWT secrets, and admin passwords are injected at runtime via **GCP Secret Manager** (production) or Docker Secrets (local development)
- No secrets are hardcoded in source code or committed to version control
- `.env` files are `.gitignore`'d with no exceptions
- **TLS** is enforced in production via Cloud Run managed certificates
- **HSTS** headers are set with a 1-year max-age

### Supply Chain Security

- **Dependency scanning**: OWASP Dependency-Check (backend), `npm audit` (frontend), Trivy (container images)
- **Secret scanning**: TruffleHog (full git history scan on every PR) + GitHub Secret Protection
- **SBOM & Provenance**: Docker images are built with SBOM and SLSA provenance attestations
- **Lockfile integrity**: Backend uses exact Maven versions with BOM management; frontend uses `package-lock.json` (lockfileVersion 3) with `npm ci` for deterministic builds
- **Dependabot alerts** are enabled for automated vulnerability notifications

### Infrastructure

- **Non-root containers**: Both backend and frontend Docker images run as unprivileged users
- **Multi-stage builds**: Build tools and source code are excluded from production images
- **OS-level patches**: `apk upgrade --no-cache` is applied during every image build
- **Least-privilege CI/CD**: Each GitHub Actions job declares minimal `permissions`
- **GCP Workload Identity Federation**: No long-lived service account keys — uses OIDC federation

---

## Security Best Practices for Contributors

### Before Submitting a PR

1. **Never commit secrets** — Use environment variables for all sensitive values. If you accidentally commit a secret, notify the maintainers immediately.
2. **Pin dependency versions** — Use exact versions for new Maven dependencies. Frontend npm dependencies are locked via `package-lock.json`.
3. **Validate all user input** — Use the framework's built-in validation pipeline (`performDomainValidation`). Never trust client-side validation alone.
4. **Use parameterized queries** — Spring Data JPA and `@Query` annotations handle this automatically. Never concatenate user input into SQL strings.
5. **Follow the authorization model** — Protect new endpoints with `@PreAuthorize` annotations. The framework enforces role-based access at the method level.

### Dependency Guidelines

- Always specify **exact versions** for Maven dependencies (no version ranges)
- Run `npm audit` before submitting frontend PRs
- Check for known CVEs using `mvn dependency-check:check` for backend changes
- Review transitive dependency changes in PR diffs

### CI/CD Security

- GitHub Actions workflows use **least-privilege permissions** per job
- Third-party actions should be pinned to **specific versions or commit SHAs**
- Secrets are never logged — use `::add-mask::` if dynamically generated values must be referenced
- The `deploy` job requires explicit approval via the `production` environment gate

---

## Acknowledgments

We gratefully acknowledge security researchers who responsibly disclose vulnerabilities.
Contributors will be credited here (unless anonymity is requested):

- *No reports received yet.*

---

## Contact

For security-related questions that are not vulnerability reports, please open a
[GitHub Discussion](https://github.com/mujib-adm/carizmi/discussions) or email
**mujib.adm@gmail.com**.
