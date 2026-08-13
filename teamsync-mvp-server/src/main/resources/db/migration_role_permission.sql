-- TeamSync Learn — 三级权限角色迁移（存量库执行一次，可重复执行，幂等）
-- 目标：将既有默认 admin 账号提升为唯一 SUPER_ADMIN（最终管理员）。
-- 只处理 admin，绝不批量提升其他账号；SUPER_ADMIN 唯一性由后端代码防呆保证（不能授予他人、不能改自己/他人 SUPER_ADMIN）。

USE teamsync_mvp;

-- 仅当 admin 尚未是 SUPER_ADMIN 时才升级（NULL/空/ADMIN 都升级；已是 SUPER_ADMIN 或账号已更名则不动）
UPDATE sys_user
SET role = 'SUPER_ADMIN'
WHERE username = 'admin'
  AND (role IS NULL OR role = '' OR role = 'ADMIN');
