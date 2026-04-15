/**
 * Branding Configuration Reader
 *
 * Reads branding configuration from the runtime config injected via
 * window.APP_CONFIG (set by /config.js, loaded synchronously in index.html).
 *
 * This decouples all organization-specific branding from the codebase,
 * enabling multi-tenant branding support without code changes.
 *
 * To customize branding:
 *   - Development: Edit frontend/public/config.js
 *   - Docker: Edit config.template.js + environment variables
 *   - Future: Fetch from /api/branding/{orgSlug} at runtime
 */

export interface BrandingConfig {
  platformName: string;
  organizationName: string;
  headerTitle: string;
  headerSubtitle: string;
  logoAlt: string;
  copyright: string;
}

interface AppConfig {
  API_URL?: string;
  branding?: Partial<BrandingConfig>;
}

declare global {
  interface Window {
    APP_CONFIG?: AppConfig;
  }
}

const currentYear = new Date().getFullYear();

const defaultBranding: BrandingConfig = {
  platformName: 'Carizmi',
  organizationName: 'Organization',
  headerTitle: 'CARIZMI',
  headerSubtitle: 'COMMUNITY PLATFORM',
  logoAlt: 'Logo',
  copyright: `© ${currentYear} Carizmi.`,
};

/**
 * Returns the current branding configuration.
 * Merges runtime config (from window.APP_CONFIG.branding) over defaults.
 *
 * Empty strings are treated as "not set" — this is critical for Docker
 * deployments where envsubst replaces unset variables with "".
 */
export function getBranding(): BrandingConfig {
  const runtimeBranding = window.APP_CONFIG?.branding;
  if (!runtimeBranding) return defaultBranding;

  // Filter out empty/blank values so defaults are preserved when
  // Docker env vars are not set (envsubst produces "" for unset vars)
  const branding = Object.fromEntries(
    Object.entries(runtimeBranding).filter(
      ([, value]) => value !== undefined && value !== null && String(value).trim() !== '',
    ),
  );

  return {
    ...defaultBranding,
    ...branding,
  };
}
