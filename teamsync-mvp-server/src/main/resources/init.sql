-- TeamSync Learn MVP Database Init Script
-- Execute on MySQL 8.0

CREATE DATABASE IF NOT EXISTS teamsync_mvp DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE teamsync_mvp;

-- 1. User table
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) DEFAULT '',
    avatar VARCHAR(500) DEFAULT '',
    role VARCHAR(20) DEFAULT 'MEMBER',
    nickname VARCHAR(50) DEFAULT '',
    bio VARCHAR(500) DEFAULT '',
    gender VARCHAR(10) DEFAULT '',
    age INT DEFAULT NULL,
    address VARCHAR(200) DEFAULT '',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. Session table
CREATE TABLE IF NOT EXISTS sys_session (
    token VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    expire_at DATETIME NOT NULL
);

-- 3. Schedule table
CREATE TABLE IF NOT EXISTS schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    goal_desc TEXT,
    status TINYINT DEFAULT 0,
    plan_date DATE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 4. Achievement table
CREATE TABLE IF NOT EXISTS achievement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    schedule_id BIGINT NOT NULL,
    content_type VARCHAR(20),
    content TEXT,
    file_url VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 4b. Achievement item table (each row is one piece of content)
CREATE TABLE IF NOT EXISTS achievement_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    achievement_id BIGINT NOT NULL,
    content_type VARCHAR(20) NOT NULL COMMENT 'TEXT | MARKDOWN | FILE',
    content TEXT COMMENT 'text content for TEXT/MARKDOWN types',
    file_url VARCHAR(500) COMMENT 'file path for FILE type',
    file_name VARCHAR(255) COMMENT 'original file name',
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 5. AI result table
CREATE TABLE IF NOT EXISTS ai_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    achievement_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    diligence_score INT,
    ai_comment TEXT,
    ai_summary TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Insert default admin
INSERT IGNORE INTO sys_user (username, password, role) VALUES ('admin', 'admin123', 'ADMIN');
