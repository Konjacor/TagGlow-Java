/*
 Navicat Premium Data Transfer

 Source Server         : MySQL57
 Source Server Type    : MySQL
 Source Server Version : 50738
 Source Host           : localhost:3306
 Source Schema         : tagglow

 Target Server Type    : MySQL
 Target Server Version : 50738
 File Encoding         : 65001

 Date: 04/06/2025 16:20:30
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for deriver
-- ----------------------------
DROP TABLE IF EXISTS `deriver`;
CREATE TABLE `deriver`  (
  `id` varchar(19) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '导出内容id',
  `user_id` varchar(19) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '导出内容所属的用户id',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '导出的内容或是导出文件的url',
  `gmt_create` datetime(0) NOT NULL COMMENT '记录创建时间',
  `gmt_modified` datetime(0) NOT NULL COMMENT '记录修改时间',
  `is_deleted` int(11) NOT NULL COMMENT '是否被逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for note
-- ----------------------------
DROP TABLE IF EXISTS `note`;
CREATE TABLE `note`  (
  `id` varchar(19) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '笔记实体id',
  `user_id` varchar(19) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '该笔记所属用户id',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '笔记内容',
  `position` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '笔记所在地理位置',
  `weather` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '记笔记时的天气信息',
  `time` datetime(0) NOT NULL COMMENT '记笔记时的时间',
  `gmt_create` datetime(0) NOT NULL COMMENT '记录创建时间',
  `gmt_modified` datetime(0) NOT NULL COMMENT '记录修改时间',
  `classification` integer NOT NULL COMMENT '笔记分类：0学习 1工作 2日常 3生活 4旅行 5情感 6美食',
  `is_deleted` int(11) NOT NULL COMMENT '是否被逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for note_tag_relation
-- ----------------------------
DROP TABLE IF EXISTS `note_tag_relation`;
CREATE TABLE `note_tag_relation`  (
  `id` varchar(19) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'note和tag关系id',
  `note_id` varchar(19) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '关系中note的id',
  `tag_id` varchar(19) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '关系中tag的id',
  `gmt_create` datetime(0) NOT NULL COMMENT '记录创建时间',
  `gmt_modified` datetime(0) NOT NULL COMMENT '记录修改时间',
  `is_deleted` int(11) NOT NULL COMMENT '是否被逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for picture
-- ----------------------------
DROP TABLE IF EXISTS `picture`;
CREATE TABLE `picture`  (
  `id` varchar(19) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '图片id',
  `note_id` varchar(19) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '图片所属的笔记id',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '图片url',
  `gmt_create` datetime(0) NOT NULL COMMENT '记录创建时间',
  `gmt_modified` datetime(0) NOT NULL COMMENT '记录修改时间',
  `is_deleted` int(11) NOT NULL COMMENT '是否被逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tag
-- ----------------------------
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag`  (
  `id` varchar(19) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标签id',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标签内容',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标签类型',
  `gmt_create` datetime(0) NOT NULL COMMENT '记录创建时间',
  `gmt_modified` datetime(0) NOT NULL COMMENT '记录修改时间',
  `is_deleted` int(11) NOT NULL COMMENT '是否被逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` varchar(19) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户id',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户密码',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户头像',
  `signature` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户个性签名',
  `gmt_create` datetime(0) NOT NULL COMMENT '记录创建时间',
  `gmt_modified` datetime(0) NOT NULL COMMENT '记录修改时间',
  `is_deleted` int(11) NOT NULL COMMENT '记录是否被逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_tag_relation
-- ----------------------------
DROP TABLE IF EXISTS `user_tag_relation`;
CREATE TABLE `user_tag_relation`  (
  `id` varchar(19) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'user和tag关系id',
  `user_id` varchar(19) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '关系中user的id',
  `tag_id` varchar(19) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '关系中tag的id',
  `gmt_create` datetime(0) NOT NULL COMMENT '记录创建时间',
  `gmt_modified` datetime(0) NOT NULL COMMENT '记录修改时间',
  `is_deleted` int(11) NOT NULL COMMENT '是否被逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
