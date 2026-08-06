-- TeamSync Learn — 新增用户个性化资料字段迁移(对已存在的库执行一次,可重复执行)
-- 针对 MySQL 8.0(不支持 ADD COLUMN IF NOT EXISTS),用 information_schema 守卫。
-- 新部署的全新库直接用 init.sql 建全,无需执行本文件。

USE teamsync_mvp;

-- nickname
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='teamsync_mvp' AND TABLE_NAME='sys_user' AND COLUMN_NAME='nickname');
SET @sql := IF(@c=0, 'ALTER TABLE sys_user ADD COLUMN nickname VARCHAR(50) DEFAULT \'\'', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- bio
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='teamsync_mvp' AND TABLE_NAME='sys_user' AND COLUMN_NAME='bio');
SET @sql := IF(@c=0, 'ALTER TABLE sys_user ADD COLUMN bio VARCHAR(500) DEFAULT \'\'', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- gender
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='teamsync_mvp' AND TABLE_NAME='sys_user' AND COLUMN_NAME='gender');
SET @sql := IF(@c=0, 'ALTER TABLE sys_user ADD COLUMN gender VARCHAR(10) DEFAULT \'\'', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- age
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='teamsync_mvp' AND TABLE_NAME='sys_user' AND COLUMN_NAME='age');
SET @sql := IF(@c=0, 'ALTER TABLE sys_user ADD COLUMN age INT DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- address
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='teamsync_mvp' AND TABLE_NAME='sys_user' AND COLUMN_NAME='address');
SET @sql := IF(@c=0, 'ALTER TABLE sys_user ADD COLUMN address VARCHAR(200) DEFAULT \'\'', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
