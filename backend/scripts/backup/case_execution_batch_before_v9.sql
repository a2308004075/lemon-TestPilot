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
-- Table structure for table `case_execution_batch`
--

DROP TABLE IF EXISTS `case_execution_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `case_execution_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL COMMENT '批次标题（自动：用例执行批次 yyyy-MM-dd HH:mm）',
  `status` varchar(10) NOT NULL DEFAULT '执行中' COMMENT '状态：执行中/已完成',
  `total` int NOT NULL DEFAULT '0' COMMENT '用例总数',
  `passed` int NOT NULL DEFAULT '0' COMMENT '通过数',
  `failed` int NOT NULL DEFAULT '0' COMMENT '失败数',
  `blocked` int NOT NULL DEFAULT '0' COMMENT '阻塞数',
  `skipped` int NOT NULL DEFAULT '0' COMMENT '跳过数',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_batch_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用例执行批次';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `case_execution_batch`
--

LOCK TABLES `case_execution_batch` WRITE;
/*!40000 ALTER TABLE `case_execution_batch` DISABLE KEYS */;
INSERT INTO `case_execution_batch` VALUES (2,'用例执行批次 2026-10-13 16:19','已完成',4,1,2,1,0,'2026-10-13 16:19:08','2026-10-13 17:25:44');
/*!40000 ALTER TABLE `case_execution_batch` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `case_execution_item`
--

DROP TABLE IF EXISTS `case_execution_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `case_execution_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL COMMENT '批次ID',
  `seq_order` int NOT NULL COMMENT '顺序（按勾选顺序）',
  `case_id` bigint NOT NULL COMMENT '用例ID（联动用例状态）',
  `case_no_snapshot` varchar(50) NOT NULL COMMENT '用例编号快照',
  `title_snapshot` varchar(200) NOT NULL COMMENT '用例标题快照',
  `priority_snapshot` varchar(10) NOT NULL COMMENT '优先级快照',
  `status` varchar(10) NOT NULL DEFAULT '待执行' COMMENT '状态：待执行/通过/失败/阻塞/跳过',
  `note` varchar(500) NOT NULL DEFAULT '' COMMENT '执行备注',
  `executed_at` datetime DEFAULT NULL COMMENT '执行时间（待执行为空）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_item_batch` (`batch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用例执行条目';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `case_execution_item`
--

LOCK TABLES `case_execution_item` WRITE;
/*!40000 ALTER TABLE `case_execution_item` DISABLE KEYS */;
INSERT INTO `case_execution_item` VALUES (4,2,0,4,'TC-COUPON-001','取消订单返还优惠券','P1','通过','','2026-10-13 17:23:21','2026-10-13 16:19:08','2026-10-13 17:23:21'),(5,2,1,3,'TC-PAY-003','三方回调签名异常','P0','失败','','2026-10-13 17:25:44','2026-10-13 16:19:08','2026-10-13 17:25:44'),(6,2,2,2,'TC-PAY-002','支付成功后订单状态同步','P1','失败','','2026-10-13 17:25:29','2026-10-13 16:19:08','2026-10-13 17:25:29'),(7,2,3,1,'TC-PAY-001','支付回调幂等校验','P0','阻塞','','2026-10-13 17:23:45','2026-10-13 16:19:08','2026-10-13 17:23:45');
/*!40000 ALTER TABLE `case_execution_item` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-13 17:33:20
