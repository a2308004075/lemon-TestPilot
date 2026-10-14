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
  `batch_no` varchar(20) NOT NULL COMMENT '批次号：YYYYMMDDNNN',
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
  UNIQUE KEY `uk_batch_no` (`batch_no`),
  KEY `idx_batch_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用例执行批次';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `case_execution_batch`
--

LOCK TABLES `case_execution_batch` WRITE;
/*!40000 ALTER TABLE `case_execution_batch` DISABLE KEYS */;
INSERT INTO `case_execution_batch` VALUES (2,'20261013001','用例执行批次 2026-10-13 16:19','已完成',4,1,2,1,0,'2026-10-13 16:19:08','2026-10-13 17:33:31'),(3,'20261013002','测试1111','已完成',4,1,1,1,1,'2026-10-13 17:49:35','2026-10-13 17:49:54');
/*!40000 ALTER TABLE `case_execution_batch` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-14 14:38:29
