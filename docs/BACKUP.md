# Database Backup Strategy

## Automated Backups (mysqldump)

For self-hosted Docker Compose deployments, add the following cron job on the host:

```bash
# /etc/cron.d/carizmi-backup
# Daily backup at 2:00 AM UTC, retain 7 days
0 2 * * * root docker exec carizmi-db mysqldump -u root -p"${MYSQL_ROOT_PASSWORD}" --single-transaction --routines --triggers carizmi | gzip > /opt/backups/carizmi-$(date +\%Y\%m\%d).sql.gz && find /opt/backups -name "carizmi-*.sql.gz" -mtime +7 -delete
```

### Setup

```bash
# Create backup directory
sudo mkdir -p /opt/backups
sudo chmod 700 /opt/backups

# Test manually
docker exec carizmi-db mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" --single-transaction carizmi | gzip > /opt/backups/carizmi-test.sql.gz
```

### Restore

```bash
gunzip < /opt/backups/carizmi-20260217.sql.gz | docker exec -i carizmi-db mysql -u root -p"$MYSQL_ROOT_PASSWORD" carizmi
```

## Cloud-Managed Backups

For managed database services (e.g., GCP Cloud SQL), use the provider's automated backup feature instead of manual `mysqldump`. Configure daily backups with 7-day retention through the cloud console.

## Verification

Periodically test restores to ensure backup integrity:

```bash
# Restore to a test container
docker run --name test-restore -e MYSQL_ROOT_PASSWORD=test -d mysql:8.0
gunzip < /opt/backups/carizmi-latest.sql.gz | docker exec -i test-restore mysql -u root -ptest
docker rm -f test-restore
```
