-- Core tables for Decode panel: decodes, leads, interactions and monitoring.

CREATE TABLE IF NOT EXISTS decodes (
    id CHAR(36) PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    city VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL,
    users_count INT NOT NULL DEFAULT 0,
    monthly_revenue DECIMAL(12,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS leads (
    id CHAR(36) PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    source VARCHAR(30) NOT NULL,
    stage VARCHAR(30) NOT NULL,
    owner_user_id CHAR(36) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_leads_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES users(id)
);

CREATE INDEX idx_leads_name ON leads(name);
CREATE INDEX idx_leads_owner ON leads(owner_user_id);

CREATE TABLE IF NOT EXISTS interactions (
    id CHAR(36) PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    contact_name VARCHAR(180) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    city VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    owner_user_id CHAR(36) NULL,
    lead_id CHAR(36) NULL,
    last_message_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_interactions_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES users(id),
    CONSTRAINT fk_interactions_lead
        FOREIGN KEY (lead_id) REFERENCES leads(id)
);

CREATE INDEX idx_interactions_contact ON interactions(contact_name);
CREATE INDEX idx_interactions_owner ON interactions(owner_user_id);
CREATE INDEX idx_interactions_lead ON interactions(lead_id);

CREATE TABLE IF NOT EXISTS interaction_messages (
    id CHAR(36) PRIMARY KEY,
    interaction_id CHAR(36) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    body TEXT NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_interaction_messages_interaction
        FOREIGN KEY (interaction_id) REFERENCES interactions(id)
);

CREATE INDEX idx_interaction_messages_interaction ON interaction_messages(interaction_id);

CREATE TABLE IF NOT EXISTS monitored_sites (
    id CHAR(36) PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    url TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    slow_threshold_ms INT NOT NULL DEFAULT 600,
    timeout_ms INT NOT NULL DEFAULT 5000,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_monitored_sites_name ON monitored_sites(name);

CREATE TABLE IF NOT EXISTS site_checks (
    id CHAR(36) PRIMARY KEY,
    site_id CHAR(36) NOT NULL,
    checked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    rtt_ms INT NOT NULL DEFAULT 0,
    http_status INT NULL,
    error_message TEXT NULL,
    CONSTRAINT fk_site_checks_site
        FOREIGN KEY (site_id) REFERENCES monitored_sites(id)
);

CREATE INDEX idx_site_checks_site_checked_at ON site_checks(site_id, checked_at);

CREATE TABLE IF NOT EXISTS incidents (
    id CHAR(36) PRIMARY KEY,
    site_id CHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    opened_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP NULL,
    message TEXT NULL,
    last_check_id CHAR(36) NULL,
    CONSTRAINT fk_incidents_site
        FOREIGN KEY (site_id) REFERENCES monitored_sites(id),
    CONSTRAINT fk_incidents_last_check
        FOREIGN KEY (last_check_id) REFERENCES site_checks(id)
);

CREATE INDEX idx_incidents_site_status ON incidents(site_id, status);
