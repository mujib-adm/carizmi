# Database Backup Strategy

## Automated Backups (mysqldump)

Add the following cron job on the Oracle Cloud instance:

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

## Oracle Cloud Volume Snapshots

For block volume backups:

1. Navigate to **Block Storage → Boot Volumes** in OCI Console
2. Select the compute instance's boot volume
3. Create a **Volume Backup Policy** with daily schedule
4. Retain snapshots for 7 days (free tier allows limited storage)

## Verification

Periodically test restores to ensure backup integrity:

```bash
# Restore to a test container
docker run --name test-restore -e MYSQL_ROOT_PASSWORD=test -d mysql:8.0
gunzip < /opt/backups/carizmi-latest.sql.gz | docker exec -i test-restore mysql -u root -ptest
docker rm -f test-restore
```
