#!/bin/sh

# Perform environment variable substitution in the config template
# This allows changing the API URL at runtime without rebuilding the Docker image
echo "Injecting runtime configuration..."
envsubst '${VITE_API_URL} ${APP_ORG_NAME} ${APP_HEADER_TITLE} ${APP_HEADER_SUBTITLE} ${APP_LOGO_ALT} ${APP_COPYRIGHT}' < /usr/share/nginx/html/config.template.js > /usr/share/nginx/html/config.js

# Execute the original CMD
exec "$@"
