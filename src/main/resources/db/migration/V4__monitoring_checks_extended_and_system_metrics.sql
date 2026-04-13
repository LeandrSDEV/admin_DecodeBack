-- V4: extend site_checks + create system metric samples

ALTER TABLE site_checks ADD COLUMN dns_ms INT NULL;
ALTER TABLE site_checks ADD COLUMN tls_ms INT NULL;
ALTER TABLE site_checks ADD COLUMN ttfb_ms INT NULL;
ALTER TABLE site_checks ADD COLUMN bytes_read BIGINT NULL;
ALTER TABLE site_checks ADD COLUMN resolved_ip VARCHAR(64) NULL;
ALTER TABLE site_checks ADD COLUMN final_url TEXT NULL;
ALTER TABLE site_checks ADD COLUMN redirects_count INT NULL;
ALTER TABLE site_checks ADD COLUMN ssl_valid_to TIMESTAMP NULL;
ALTER TABLE site_checks ADD COLUMN ssl_days_left INT NULL;
ALTER TABLE site_checks ADD COLUMN metrics_endpoint TEXT NULL;
ALTER TABLE site_checks ADD COLUMN metrics_json TEXT NULL;

CREATE TABLE IF NOT EXISTS system_metric_samples (
    id CHAR(36) PRIMARY KEY,
    sampled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cpu_load DOUBLE PRECISION NOT NULL DEFAULT 0,
    mem_used_bytes BIGINT NOT NULL DEFAULT 0,
    mem_total_bytes BIGINT NOT NULL DEFAULT 0,
    disk_used_bytes BIGINT NOT NULL DEFAULT 0,
    disk_total_bytes BIGINT NOT NULL DEFAULT 0,
    uptime_seconds BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_system_metric_samples_sampled_at ON system_metric_samples(sampled_at);
