-- =====================================================================
-- 笔记工坊 AI 读书笔记管理系统 - MySQL 8 建表脚本
-- 约定：InnoDB、utf8mb4、BIGINT 自增主键、蛇形命名、逻辑删除 deleted
-- 所有时间字段使用 DATETIME，默认 CURRENT_TIMESTAMP
-- 所有表名使用单数小写
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `note`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `note`;

SET NAMES utf8mb4;

-- 外键关系仅作逻辑说明，不创建 FOREIGN KEY 约束，避免删除顺序问题：
-- user.id            -> note.user_id
-- note.id            -> note_image.note_id
-- user.id / note.id  -> note_like.user_id / note_like.note_id
-- user.id            -> follow.follower_id / follow.followee_id
-- note.id / user.id  -> comment.note_id / comment.user_id
-- comment.id         -> comment.parent_id（自关联）
-- user.id            -> ai_log.user_id
-- note.id            -> note_ai.note_id
-- user.id            -> conversation.user_id
-- conversation.id    -> conversation_message.conversation_id

DROP TABLE IF EXISTS `conversation_message`;
DROP TABLE IF EXISTS `conversation`;
DROP TABLE IF EXISTS `note_ai`;
DROP TABLE IF EXISTS `ai_log`;
DROP TABLE IF EXISTS `comment`;
DROP TABLE IF EXISTS `favorite_book`;
DROP TABLE IF EXISTS `follow`;
DROP TABLE IF EXISTS `note_like`;
DROP TABLE IF EXISTS `note_image`;
DROP TABLE IF EXISTS `note`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
    `nickname` VARCHAR(50) NULL COMMENT '昵称',
    `avatar` VARCHAR(255) NULL COMMENT '头像地址',
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：USER/ADMIN',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
    `token_version` INT NOT NULL DEFAULT 0 COMMENT '密码版本，用于密码修改后旧token失效',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `note` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '笔记主键',
    `user_id` BIGINT NOT NULL COMMENT '作者用户ID',
    `title` VARCHAR(100) NOT NULL COMMENT '笔记标题',
    `content` TEXT NOT NULL COMMENT '笔记正文',
    `book_name` VARCHAR(100) NOT NULL COMMENT '关联书名',
    `visibility` TINYINT NOT NULL DEFAULT 0 COMMENT '可见性：0私有 1公开',
    `audit_status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0待审核 1已通过 2已驳回',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_note_user_id` (`user_id`),
    KEY `idx_note_visibility_audit_status` (`visibility`, `audit_status`),
    KEY `idx_note_like_count` (`like_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='读书笔记表';

CREATE TABLE `note_image` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '图片主键',
    `note_id` BIGINT NOT NULL COMMENT '所属笔记ID',
    `url` VARCHAR(500) NOT NULL COMMENT '图片URL',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_note_image_note_id` (`note_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记图片表';

CREATE TABLE `note_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '点赞记录主键',
    `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
    `note_id` BIGINT NOT NULL COMMENT '被点赞笔记ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_note_like_user_note` (`user_id`, `note_id`),
    KEY `idx_note_like_note_id` (`note_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记点赞表';

CREATE TABLE `follow` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关注记录主键',
    `follower_id` BIGINT NOT NULL COMMENT '关注者用户ID',
    `followee_id` BIGINT NOT NULL COMMENT '被关注者用户ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_follow_follower_followee` (`follower_id`, `followee_id`),
    KEY `idx_follow_followee_id` (`followee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注表';

CREATE TABLE `favorite_book` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏书目主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `book_name` VARCHAR(100) NOT NULL COMMENT '书名',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_favorite_book_user_book` (`user_id`, `book_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏书目表';

CREATE TABLE `comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论主键',
    `note_id` BIGINT NOT NULL COMMENT '所属笔记ID',
    `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
    `parent_id` BIGINT NULL COMMENT '父评论ID，NULL表示顶级评论',
    `content` VARCHAR(1000) NOT NULL COMMENT '评论内容',
    `media` TEXT NULL COMMENT '评论媒体地址JSON数组',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_comment_note_id` (`note_id`),
    KEY `idx_comment_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记评论表';

CREATE TABLE `ai_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'AI调用记录主键',
    `user_id` BIGINT NULL COMMENT '发起调用的用户ID，可为空',
    `type` VARCHAR(30) NOT NULL COMMENT 'AI调用类型',
    `prompt` TEXT NULL COMMENT '请求提示词',
    `response` TEXT NULL COMMENT 'AI返回内容',
    `success` TINYINT NOT NULL DEFAULT 1 COMMENT '调用结果：1成功 0失败',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
    PRIMARY KEY (`id`),
    KEY `idx_ai_log_user_id` (`user_id`),
    KEY `idx_ai_log_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI调用日志表';

CREATE TABLE `note_ai` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'AI辅助结果主键',
    `note_id` BIGINT NOT NULL COMMENT '关联笔记ID',
    `type` VARCHAR(30) NOT NULL COMMENT 'AI辅助结果类型',
    `content` TEXT NOT NULL COMMENT 'AI生成内容',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_note_ai_note_type` (`note_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记AI辅助结果表';

CREATE TABLE `conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话主键',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
    `title` VARCHAR(100) NULL COMMENT '会话标题',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_conversation_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI会话表';

CREATE TABLE `conversation_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话消息主键',
    `conversation_id` BIGINT NOT NULL COMMENT '所属会话ID',
    `role` VARCHAR(20) NOT NULL COMMENT '消息角色：user/assistant',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_conversation_message_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI会话消息表';
