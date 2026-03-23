FROM node:24-alpine AS build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend ./
RUN chmod +x node_modules/.bin/* || true

ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL

# Regenerate TypeScript API client from the committed OpenAPI contract before building
RUN npm run generate:api && npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine
COPY --from=build /app/frontend/dist /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf

# Switch to root to perform system-level tasks (setting permissions)
# This is necessary because the base image is unprivileged by default
USER root
RUN apk add --no-cache gettext # Ensure envsubst is available

COPY frontend/docker-entrypoint.sh /usr/local/bin/
# Normalize line endings (remove \r) and set execution permissions
RUN tr -d '\r' < /usr/local/bin/docker-entrypoint.sh > /usr/local/bin/docker-entrypoint.sh.tmp && \
    mv /usr/local/bin/docker-entrypoint.sh.tmp /usr/local/bin/docker-entrypoint.sh && \
    chmod +x /usr/local/bin/docker-entrypoint.sh && \
    chown -R 101:101 /usr/share/nginx/html

# Revert back to the unprivileged 'nginx' user (uid 101) for secure execution
USER 101

EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
