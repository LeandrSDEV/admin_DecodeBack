-- V6: CRM platform foundation (PostgreSQL)

-- 1) Extend leads to support CRM fields
ALTER TABLE leads ADD COLUMN phone VARCHAR(30) NULL;
ALTER TABLE leads ADD COLUMN email VARCHAR(180) NULL;
ALTER TABLE leads ADD COLUMN status VARCHAR(30) NULL;
ALTER TABLE leads ADD COLUMN score INT NOT NULL DEFAULT 0;
ALTER TABLE leads ADD COLUMN last_contact_at TIMESTAMP NULL;

CREATE INDEX idx_leads_phone ON leads(phone);
CREATE INDEX idx_leads_email ON leads(email);

-- 2) Tags (N:N)
CREATE TABLE IF NOT EXISTS tags (
  id CHAR(36) PRIMARY KEY,
  name VARCHAR(80) NOT NULL UNIQUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lead_tags (
  lead_id CHAR(36) NOT NULL,
  tag_id CHAR(36) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (lead_id, tag_id),
  CONSTRAINT fk_lead_tags_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE,
  CONSTRAINT fk_lead_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- 3) RBAC
CREATE TABLE IF NOT EXISTS roles (
  id CHAR(36) PRIMARY KEY,
  code VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(120) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS permissions (
  id CHAR(36) PRIMARY KEY,
  code VARCHAR(80) NOT NULL UNIQUE,
  name VARCHAR(160) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS role_permissions (
  role_id CHAR(36) NOT NULL,
  permission_id CHAR(36) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
  CONSTRAINT fk_role_permissions_perm FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_roles (
  user_id CHAR(36) NOT NULL,
  role_id CHAR(36) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- 4) Refresh tokens (hashed)
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id CHAR(36) PRIMARY KEY,
  user_id CHAR(36) NOT NULL,
  token_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP NOT NULL,
  revoked_at TIMESTAMP NULL,
  replaced_by CHAR(36) NULL,
  user_agent TEXT NULL,
  ip VARCHAR(80) NULL,
  CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id, created_at DESC);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);

-- 5) Audit log
CREATE TABLE IF NOT EXISTS audit_log (
  id CHAR(36) PRIMARY KEY,
  occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actor_user_id CHAR(36) NULL,
  action VARCHAR(80) NOT NULL,
  entity_type VARCHAR(80) NOT NULL,
  entity_id VARCHAR(80) NULL,
  ip VARCHAR(80) NULL,
  user_agent TEXT NULL,
  trace_id VARCHAR(80) NULL,
  before_json JSONB NULL,
  after_json JSONB NULL
);

CREATE INDEX idx_audit_log_occurred_at ON audit_log(occurred_at DESC);
CREATE INDEX idx_audit_log_actor ON audit_log(actor_user_id, occurred_at DESC);

-- 6) Event Store
CREATE TABLE IF NOT EXISTS events (
  event_id BIGSERIAL PRIMARY KEY,
  type VARCHAR(80) NOT NULL,
  occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  lead_id CHAR(36) NULL,
  channel VARCHAR(40) NULL,
  source VARCHAR(60) NULL,
  payload JSONB NULL,
  actor_user_id CHAR(36) NULL,
  trace_id VARCHAR(80) NULL,
  severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
  ip VARCHAR(80) NULL,
  user_agent TEXT NULL
);

CREATE INDEX idx_events_occurred_at ON events(occurred_at DESC);
CREATE INDEX idx_events_type_time ON events(type, occurred_at DESC);
CREATE INDEX idx_events_lead_time ON events(lead_id, occurred_at DESC);
CREATE INDEX idx_events_trace_id ON events(trace_id);

-- 7) Jobs
CREATE TABLE IF NOT EXISTS jobs (
  id CHAR(36) PRIMARY KEY,
  queue VARCHAR(60) NOT NULL,
  name VARCHAR(120) NOT NULL,
  status VARCHAR(30) NOT NULL,
  payload JSONB NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS job_runs (
  id CHAR(36) PRIMARY KEY,
  job_id CHAR(36) NOT NULL,
  status VARCHAR(30) NOT NULL,
  started_at TIMESTAMP NULL,
  finished_at TIMESTAMP NULL,
  attempts INT NOT NULL DEFAULT 0,
  error_message TEXT NULL,
  trace_id VARCHAR(80) NULL,
  logs TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_job_runs_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);

CREATE INDEX idx_jobs_queue_status ON jobs(queue, status);
CREATE INDEX idx_job_runs_job ON job_runs(job_id, created_at DESC);

-- 8) Seed roles & permissions (deterministic UUIDs from md5)
INSERT INTO roles (id, code, name) VALUES
  (regexp_replace(md5('role_admin'),      '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5'), 'admin',      'Admin'),
  (regexp_replace(md5('role_manager'),    '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5'), 'manager',    'Manager'),
  (regexp_replace(md5('role_support'),    '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5'), 'support',    'Support'),
  (regexp_replace(md5('role_viewer'),     '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5'), 'viewer',     'Viewer'),
  (regexp_replace(md5('role_automation'), '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5'), 'automation', 'Automation')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (id, code, name) VALUES
  (regexp_replace(md5('perm_leads_read'),   '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5'), 'leads:read',   'Listar/Ver Leads'),
  (regexp_replace(md5('perm_leads_write'),  '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5'), 'leads:write',  'Criar/Editar Leads'),
  (regexp_replace(md5('perm_events_read'),  '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5'), 'events:read',  'Listar/Ver Eventos'),
  (regexp_replace(md5('perm_events_write'), '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5'), 'events:write', 'Ingerir Eventos'),
  (regexp_replace(md5('perm_monitor_read'), '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5'), 'monitor:read', 'Ver Monitoramento'),
  (regexp_replace(md5('perm_users_admin'),  '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5'), 'users:admin',  'Administrar Usuarios'),
  (regexp_replace(md5('perm_audit_read'),   '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5'), 'audit:read',   'Ver Auditoria'),
  (regexp_replace(md5('perm_jobs_admin'),   '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5'), 'jobs:admin',   'Administrar Jobs')
ON CONFLICT (code) DO NOTHING;

-- role -> permissions (admin gets everything)
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'admin'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- manager gets leads/events/monitor/audit
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('leads:read','leads:write','events:read','monitor:read','audit:read')
WHERE r.code = 'manager'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- support gets leads read + events read
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('leads:read','events:read','monitor:read')
WHERE r.code = 'support'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- viewer gets reads
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('leads:read','events:read','monitor:read')
WHERE r.code = 'viewer'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- automation gets events write
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('events:write','events:read')
WHERE r.code = 'automation'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Assign existing users.role -> user_roles
INSERT INTO user_roles(user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON LOWER(r.code) = LOWER(u.role)
ON CONFLICT (user_id, role_id) DO NOTHING;
