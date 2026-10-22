-- 说明：管理员账号由应用启动时初始化（用户名 admin，角色 ADMIN），密码使用 BCrypt 哈希，
-- 本文件不保存任何真实账号、密码或密钥。

-- 可选：创建数据库和业务用户时，请替换下面密码后再取消注释。
-- CREATE DATABASE IF NOT EXISTS `note` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- CREATE USER IF NOT EXISTS 'note_app'@'%' IDENTIFIED BY 'REPLACE_WITH_STRONG_PASSWORD';
-- GRANT ALL PRIVILEGES ON `note`.* TO 'note_app'@'%';
-- FLUSH PRIVILEGES;
