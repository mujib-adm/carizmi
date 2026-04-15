/**
 * Runtime Configuration
 *
 * This file is loaded synchronously by index.html before React mounts.
 * It provides runtime-configurable values that can be changed without rebuilding.
 *
 * For Docker deployments, config.template.js is used instead — environment
 * variables are substituted by docker-entrypoint.sh at container startup.
 *
 * Branding values here are for the current deployment (Sof'umar Community).
 * To rebrand for a different organization, change only these values.
 */
window.APP_CONFIG = {
  API_URL: 'http://localhost:8080/api',
  branding: {
    platformName: 'Carizmi',
    organizationName: "Sof'umar Community of Minnesota",
    headerTitle: "SOF'UMAR",
    headerSubtitle: 'COMMUNITY OF MINNESOTA',
    logoAlt: "Sof'umar Logo",
    copyright: "© " + new Date().getFullYear() + " Sof'umar Community of Minnesota.",
  },
};