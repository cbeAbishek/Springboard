CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(120) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(80) NOT NULL,
    enabled BOOLEAN NOT NULL,
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(512),
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL,
    CONSTRAINT fk_project_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS test_cases (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(160) NOT NULL,
    type VARCHAR(10) NOT NULL,
    project_id BIGINT NOT NULL,
    definition_json LONGTEXT NOT NULL,
    last_run_status VARCHAR(32),
    last_run_at TIMESTAMP,
    last_error_message LONGTEXT,
    last_response_code INT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_test_case_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE IF NOT EXISTS scheduler_jobs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL,
    frequency VARCHAR(16) NOT NULL,
    cron_expression VARCHAR(120),
    active BOOLEAN NOT NULL,
    next_run_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_scheduler_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE IF NOT EXISTS reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    test_case_id BIGINT,
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    summary VARCHAR(2048),
    details LONGTEXT,
    screenshot_url VARCHAR(512),
    response_code INT,
    error_message LONGTEXT,
    environment VARCHAR(80),
    CONSTRAINT fk_report_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_report_test_case FOREIGN KEY (test_case_id) REFERENCES test_cases(id)
);

CREATE TABLE IF NOT EXISTS generated_action_files (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL,
    content LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    description VARCHAR(512),
    CONSTRAINT fk_actions_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE IF NOT EXISTS generated_reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    filter_type VARCHAR(10),
    filter_status VARCHAR(32),
    filter_from TIMESTAMP,
    filter_to TIMESTAMP,
    total_records INT,
    file_url VARCHAR(512) NOT NULL,
    file_name VARCHAR(160) NOT NULL,
    mime_type VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_generated_report_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_generated_report_user FOREIGN KEY (user_id) REFERENCES users(id)
);
