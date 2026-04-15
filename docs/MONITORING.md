# Monitoring & Alerting

## Actuator Endpoints

The application exposes Spring Boot Actuator endpoints for monitoring:

| Endpoint | Profile | Access |
|---|---|---|
| `/api/actuator/health` | All | Public (details hidden) |
| `/api/actuator/info` | All | Public |
| `/api/actuator/metrics` | All | Authenticated |
| `/api/actuator/prometheus` | `prod` | Authenticated |

## Uptime Monitoring

For the Oracle Cloud "Always Free" deployment, set up a free uptime monitor:

1. **[UptimeRobot](https://uptimerobot.com/)** (recommended, free tier)
   - Monitor URL: `https://your-domain.com/api/actuator/health`
   - Check interval: 5 minutes
   - Alert contacts: Email or Slack webhook

2. Alternatively, use Oracle Cloud's built-in Health Checks service.

## Prometheus / Grafana (Future)

When ready to add full observability:

1. Add `prometheus.yml` scrape config targeting `/api/actuator/prometheus`
2. Deploy Prometheus + Grafana via Docker Compose
3. Import Spring Boot dashboard (Grafana ID: `4701`)
4. Set up alerts for:
   - Response time > 2s (p95)
   - Error rate > 5%
   - JVM heap usage > 80%
   - HikariCP active connections > 15

## Structured Logging

Production uses JSON-structured logging via Logstash encoder (`logback-spring.xml`).
Compatible with ELK Stack, Loki, or any JSON log aggregator.
