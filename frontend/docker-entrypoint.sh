#!/bin/sh

# Perform environment variable substitution in the config template
# This allows changing the API URL at runtime without rebuilding the Docker image
echo "Injecting runtime configuration..."
envsubst '${VITE_API_URL}' < /usr/share/nginx/html/config.template.js > /usr/share/nginx/html/config.js

# Execute the original CMD
exec "$@"
