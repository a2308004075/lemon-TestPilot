-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: lemon_testpilot
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `test_case`
--

DROP TABLE IF EXISTS `test_case`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_case` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `case_no` varchar(50) NOT NULL COMMENT '用例编号',
  `title` varchar(200) NOT NULL COMMENT '用例标题',
  `project_name` varchar(100) NOT NULL DEFAULT '',
  `module_name` varchar(100) NOT NULL DEFAULT '',
  `submodule_name` varchar(100) NOT NULL DEFAULT '',
  `priority` varchar(10) NOT NULL DEFAULT 'P1' COMMENT '优先级 P0/P1/P2',
  `type` varchar(20) NOT NULL DEFAULT '功能' COMMENT '用例类型',
  `preconditions` text COMMENT '前置条件',
  `steps` text COMMENT '测试步骤（每行一步）',
  `expected` text COMMENT '预期结果',
  `test_data` text COMMENT '测试数据',
  `status` varchar(20) NOT NULL DEFAULT '使用' COMMENT '状态：使用/废弃',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本',
  `linked_bug_no` varchar(50) NOT NULL DEFAULT '' COMMENT '关联 Bug',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_date` date DEFAULT NULL COMMENT '生成日期',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_case_no` (`case_no`),
  KEY `idx_case_module` (`module_name`,`submodule_name`),
  KEY `idx_case_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='测试用例';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test_case`
--

LOCK TABLES `test_case` WRITE;
/*!40000 ALTER TABLE `test_case` DISABLE KEYS */;
INSERT INTO `test_case` VALUES (1,'TC-20261013170947','支付回调幂等校验','电商平台','支付中心','支付回调','P0','接口','已存在支付成功流水','1. 发送支付成功回调\n2. 重复发送相同回调','订单状态为已支付且只扣款一次','traceId=demo-1024','使用',2,'','2026-10-05 12:40:35','2026-10-02','2026-10-13 17:09:47'),(2,'TC-20261013170948','支付成功后订单状态同步','电商平台','支付中心','订单同步','P1','功能','订单为待支付状态','1. 完成支付\n2. 等待 30 秒查询订单\n3. 核对订单状态','订单状态为已支付','traceId=demo-1024','使用',1,'bug_0001','2026-10-05 12:40:35','2026-10-02','2026-10-13 17:09:47'),(3,'TC-20261013170949','三方回调签名异常','电商平台','支付中心','支付回调','P0','异常','准备错误签名','1. 发送异常回调','拒绝处理并记录安全日志','错误 sign','使用',1,'','2026-10-05 12:40:35','2026-10-02','2026-10-13 17:09:47'),(4,'TC-20261013171907','取消订单返还优惠券','电商平台','订单中心','优惠券','P1','功能','下单使用优惠券','1. 取消订单\n2. 查询优惠券状态','优惠券返还且状态可用','coupon_id=c-1024','使用',4,'','2026-10-05 12:40:35','2026-10-02','2026-10-13 17:19:07');
/*!40000 ALTER TABLE `test_case` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-14 15:45:08
