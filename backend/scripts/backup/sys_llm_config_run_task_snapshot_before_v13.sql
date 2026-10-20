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
-- Table structure for table `sys_llm_config`
--

DROP TABLE IF EXISTS `sys_llm_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_llm_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `vendor_code` varchar(50) NOT NULL COMMENT '厂商编码',
  `vendor_name` varchar(100) NOT NULL COMMENT '厂商名称',
  `base_url` varchar(255) NOT NULL DEFAULT '' COMMENT 'API Base URL',
  `model_name` varchar(100) NOT NULL DEFAULT '' COMMENT '模型名',
  `api_key` varchar(500) NOT NULL DEFAULT '' COMMENT 'API Key（AES-GCM 密文，enc: 前缀；兼容存量明文）',
  `temperature` decimal(3,2) NOT NULL DEFAULT '0.20' COMMENT '温度',
  `timeout_seconds` int NOT NULL DEFAULT '60' COMMENT '超时（秒）',
  `max_output` int NOT NULL DEFAULT '4096' COMMENT '最大输出',
  `enabled` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否启用',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vendor` (`vendor_code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='大模型厂商配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_llm_config`
--

LOCK TABLES `sys_llm_config` WRITE;
/*!40000 ALTER TABLE `sys_llm_config` DISABLE KEYS */;
INSERT INTO `sys_llm_config` VALUES (1,'deepseek','DeepSeek','https://api.deepseek.com/v1','deepseek-chat','',0.20,60,4096,0,'2026-10-02 09:00:00'),(2,'openai','OpenAI','https://api.openai.com/v1','gpt-5-mini','',0.20,60,4096,0,'2026-10-02 09:00:00'),(3,'custom','自定义兼容接口','','','',0.20,60,4096,0,'2026-10-02 09:00:00'),(4,'qwen','通义千问','https://dashscope.aliyuncs.com/compatible-mode/v1','qwen-plus','',0.20,60,4096,0,'2026-10-02 09:00:00');
/*!40000 ALTER TABLE `sys_llm_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `run_task_snapshot`
--

DROP TABLE IF EXISTS `run_task_snapshot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `run_task_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `run_id` bigint NOT NULL COMMENT '执行记录ID',
  `task_type` varchar(50) NOT NULL COMMENT '任务类型',
  `task_name` varchar(100) NOT NULL COMMENT '任务名称',
  `role` varchar(20) NOT NULL COMMENT '角色：main/aux',
  `seq` int NOT NULL COMMENT '顺序',
  `completeness` int NOT NULL DEFAULT '0' COMMENT '完整度%',
  `missing_json` varchar(1000) DEFAULT NULL COMMENT '缺项 JSON',
  `output_json` longtext COMMENT '输出 JSON',
  PRIMARY KEY (`id`),
  KEY `idx_snapshot_run` (`run_id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='执行任务快照';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `run_task_snapshot`
--

LOCK TABLES `run_task_snapshot` WRITE;
/*!40000 ALTER TABLE `run_task_snapshot` DISABLE KEYS */;
INSERT INTO `run_task_snapshot` VALUES (1,1,'bug_analysis','Bug 分析','main',1,100,'[]','{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[],\"phenomenon\":\"支付成功后订单仍为待支付（staging 环境 v2.8.3）\",\"affected_modules\":[\"电商平台 / 支付中心 / 订单同步\"],\"possible_causes\":[\"从现象推测存在状态同步或事务边界异常，尚需证据验证\",\"Lock wait timeout 指向数据库锁等待，可能阻塞订单状态更新事务\"],\"recommended_logs\":[\"按 traceId=demo-1024 检索支付回调与订单同步全链路日志\",\"检查回调消费与订单更新的事务边界日志\"],\"recommended_sql\":[\"查询订单状态流转表在时间范围内的变更记录\",\"核对支付流水与订单状态一致性\"],\"related_history_bugs\":[\"支付回调重复消费导致重复扣款（kb_001）\"],\"regression_scope\":[\"直接变更模块\",\"上下游状态同步\",\"失败与重复请求场景\"]}'),(2,1,'log_triage','日志排查','aux',2,67,'[\"时间范围\"]','{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[\"时间范围\"],\"anomaly_summary\":\"已提取异常关键词与时间线，根因仍需结合完整上下文确认\",\"timeline\":[\"接收用户证据\",\"定位首个异常片段（Lock wait timeout）\",\"向上下游扩展调用链\"],\"error_fragments\":[\"日志出现 Lock wait timeout\"],\"possible_causes\":[\"锁等待可能由幂等表唯一索引冲突或长事务导致，尚需证据验证\"],\"suggestions\":[\"按 traceId=demo-1024 检索回调消费线程与数据库事务日志\",\"命中 Lock wait timeout 时优先检查幂等表唯一索引与事务粒度（引用日志规律库 kb_002）\"],\"related_kb\":[\"支付回调日志关键字（kb_002）\"]}'),(3,1,'sql_analysis','SQL 分析','aux',3,50,'[\"SQL 文本\"]','{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[\"SQL 文本\"],\"sql_correctness\":\"未提供 SQL 文本，暂无法进行语法与正确性检查\",\"performance_risk\":\"缺少执行计划与表结构信息，无法评估索引命中情况\",\"impact_scope\":[\"支付中心 / 订单同步\",\"上下游状态同步\",\"历史数据回溯范围\"],\"optimization_suggestions\":[\"补充 SQL 文本与执行计划后评估索引与锁行为\",\"按 traceId 与时间范围对齐订单状态与支付流水\"],\"review_conclusion\":\"证据不足，需人工复核后给出最终结论\",\"completeness_score\":50}'),(4,1,'regression_list','回归清单','aux',4,100,'[]','{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[],\"regression_items\":[\"支付成功回调主流程\",\"支付失败回调处理\",\"重复回调幂等验证\",\"订单状态同步查询\",\"支付流水一致性核对\",\"优惠券返还与退款场景\",\"三方回调签名异常（关联用例 TC-PAY-003）\"],\"item_count\":7,\"completion_rate\":100,\"sources_summary\":[\"高风险用例 2 条\",\"历史 Bug 1 条（kb_001）\",\"回归规则库 1 条（kb_004）\"]}'),(5,2,'testcase_gen','测试用例生成','main',1,100,'[]','{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[],\"test_points\":[\"支付成功主流程\",\"订单状态同步边界\",\"回归范围验证\"],\"cases\":[{\"title\":\"支付成功后订单状态同步校验\",\"priority\":\"P1\",\"type\":\"功能\",\"preconditions\":\"订单为待支付状态\",\"steps\":\"1. 完成支付\n2. 等待 30 秒查询订单\",\"expected\":\"订单状态为已支付\"},{\"title\":\"支付重复回调幂等校验\",\"priority\":\"P0\",\"type\":\"异常\",\"preconditions\":\"已存在支付成功流水\",\"steps\":\"1. 重复发送相同回调\",\"expected\":\"只扣款一次且状态不回退\"},{\"title\":\"回调超时重试边界\",\"priority\":\"P1\",\"type\":\"边界\",\"preconditions\":\"回调响应超时\",\"steps\":\"1. 触发重试\n2. 查询订单状态\",\"expected\":\"重试幂等且状态一致\"}],\"coverage\":\"正常 / 异常 / 边界\"}'),(6,2,'bug_analysis','Bug 分析','aux',2,100,'[]','{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[\"时间范围\"],\"phenomenon\":\"支付成功后订单仍为待支付\",\"affected_modules\":[\"电商平台 / 支付中心 / 订单同步\"],\"possible_causes\":[\"状态同步或事务边界异常，尚需证据验证\"],\"regression_scope\":[\"直接变更模块\",\"上下游状态同步\"]}'),(7,2,'log_triage','日志排查','aux',3,33,'[\"时间范围\",\"日志上下文\"]','{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[\"时间范围\",\"日志上下文\"],\"anomaly_summary\":\"未提供日志片段，等待补充材料\",\"timeline\":[\"接收用户证据\"],\"suggestions\":[\"补充日志上下文后提取时间线\"]}'),(8,2,'sql_analysis','SQL 分析','aux',4,50,'[\"SQL 文本\"]','{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[\"SQL 文本\"],\"sql_correctness\":\"未提供 SQL 文本，暂无法检查\",\"performance_risk\":\"缺少执行计划信息\",\"review_conclusion\":\"证据不足，需人工复核\"}'),(9,2,'regression_list','回归清单','aux',5,100,'[]','{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[],\"regression_items\":[\"支付成功回调主流程\",\"支付失败回调处理\",\"重复回调幂等验证\",\"订单状态同步查询\",\"支付流水一致性核对\",\"优惠券返还与退款场景\"],\"item_count\":6,\"completion_rate\":100,\"sources_summary\":[\"回归规则库 1 条（kb_004）\"]}'),(10,3,'bug_analysis','Bug 分析','main',1,100,'[]','{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[\"时间范围\",\"SQL 文本\"],\"phenomenon\":\"支付成功后订单仍为待支付（staging 环境 v2.8.3）\",\"affected_modules\":[\"电商平台 / 支付中心 / 订单同步\"],\"possible_causes\":[\"状态同步或事务边界异常，尚需证据验证\"],\"regression_scope\":[\"直接变更模块\",\"上下游状态同步\",\"失败与重复请求场景\"]}'),(11,3,'log_triage','日志排查','aux',2,67,'[\"时间范围\"]','{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[\"时间范围\"],\"anomaly_summary\":\"已提取异常关键词与时间线，根因仍需结合完整上下文确认\",\"timeline\":[\"接收用户证据\",\"定位首个异常片段（Lock wait timeout）\",\"向上下游扩展调用链\"],\"suggestions\":[\"按 traceId=demo-1024 检索全链路日志\"]}'),(12,3,'sql_analysis','SQL 分析','aux',3,50,'[\"SQL 文本\"]','{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[\"SQL 文本\"],\"sql_correctness\":\"未提供 SQL 文本，暂无法检查\",\"review_conclusion\":\"证据不足，需人工复核\"}'),(13,3,'regression_list','回归清单','aux',4,100,'[]','{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[],\"regression_items\":[\"支付成功回调主流程\",\"支付失败回调处理\",\"重复回调幂等验证\",\"订单状态同步查询\",\"支付流水一致性核对\",\"优惠券返还与退款场景\"],\"item_count\":6,\"completion_rate\":100,\"sources_summary\":[\"回归规则库 1 条（kb_004）\"]}'),(14,4,'bug_analysis','Bug 分析','main',1,100,'[]','{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[],\"phenomenon\":\"复现步骤：完成支付后等待 30 秒查询订单，实际结果仍为待支付，预期订单应为已支付\",\"affected_modules\":[\"电商平台 / 支付中心 / 订单同步\"],\"possible_causes\":[\"从现象推测存在状态同步或事务边界异常，尚需证据验证\",\"Lock wait timeout 指向数据库锁等待，可能阻塞核心状态更新事务\"],\"recommended_logs\":[\"按 traceId=demo-1024 检索支付回调与订单同步全链路日志\",\"检查关键事务边界与状态流转日志\"],\"recommended_sql\":[\"查询核心业务状态流转表在时间范围内的变更记录\",\"核对上下游数据一致性\"],\"related_history_bugs\":[\"支付回调重复消费导致重复扣款（kb_001）\"],\"regression_scope\":[\"直接变更模块\",\"上下游状态同步\",\"失败与重复请求场景\"]}'),(15,4,'log_triage','日志排查','aux',2,67,'[\"时间范围\"]','{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[\"时间范围\"],\"anomaly_summary\":\"已提取异常关键词与时间线，根因仍需结合完整上下文确认\",\"timeline\":[\"接收用户证据\",\"定位首个异常片段（Lock wait timeout）\",\"向上下游扩展调用链\"],\"error_fragments\":[\"traceId=demo-1024，日志出现 Lock wait timeout\"],\"possible_causes\":[\"锁等待可能由幂等表唯一索引冲突或长事务导致，尚需证据验证\"],\"suggestions\":[\"按 traceId=demo-1024 检索回调消费线程与数据库事务日志\",\"命中日志规律：支付回调日志关键字（kb_002）\"],\"related_kb\":[\"支付回调日志关键字（kb_002）\"]}'),(16,4,'sql_analysis','SQL 分析','aux',3,50,'[\"SQL 文本\"]','{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[\"SQL 文本\"],\"sql_correctness\":\"未提供 SQL 文本，暂无法进行语法与正确性检查\",\"performance_risk\":\"缺少执行计划与表结构信息，无法评估索引命中情况\",\"impact_scope\":[\"电商平台 / 支付中心 / 订单同步\",\"上下游状态同步\",\"历史数据回溯范围\"],\"optimization_suggestions\":[\"按 traceId 与时间范围对齐上下游数据后核对一致性\",\"补充执行计划后评估索引与锁行为\"],\"review_conclusion\":\"证据不足，需人工复核后给出最终结论\",\"completeness_score\":50}'),(17,4,'regression_list','回归清单','aux',4,100,'[]','{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[],\"regression_items\":[\"支付成功回调主流程\",\"支付失败回调处理\",\"重复回调幂等验证\",\"订单状态同步查询\",\"支付流水一致性核对\",\"优惠券返还与退款场景\",\"三方回调签名异常（关联用例 TC-PAY-003）\",\"支付成功后订单状态同步（关联用例 TC-PAY-002）\",\"支付回调幂等校验（关联用例 TC-PAY-001）\",\"支付回调重复消费导致重复扣款（kb_001）\"],\"item_count\":10,\"completion_rate\":100,\"sources_summary\":[\"高风险用例 3 条\",\"历史 Bug 1 条\",\"回归规则库 1 条\"]}'),(18,5,'bug_analysis','Bug 分析','main',1,100,'[]','{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[],\"phenomenon\":\"复现步骤：完成支付后等待 30 秒查询订单，实际结果仍为待支付，预期订单应为已支付\",\"affected_modules\":[\"测试平台 / 质量工具 / 任务编排\"],\"possible_causes\":[\"从现象推测存在状态同步或事务边界异常，尚需证据验证\",\"Lock wait timeout 指向数据库锁等待，可能阻塞核心状态更新事务\"],\"recommended_logs\":[\"按 traceId=demo-1024 检索支付回调与订单同步全链路日志\",\"检查关键事务边界与状态流转日志\"],\"recommended_sql\":[\"查询核心业务状态流转表在时间范围内的变更记录\",\"核对上下游数据一致性\"],\"related_history_bugs\":[\"支付回调重复消费导致重复扣款（kb_001）\"],\"regression_scope\":[\"直接变更模块\",\"上下游状态同步\",\"失败与重复请求场景\"]}'),(19,5,'log_triage','日志排查','aux',2,67,'[\"时间范围\"]','{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[\"时间范围\"],\"anomaly_summary\":\"已提取异常关键词与时间线，根因仍需结合完整上下文确认\",\"timeline\":[\"接收用户证据\",\"定位首个异常片段（Lock wait timeout）\",\"向上下游扩展调用链\"],\"error_fragments\":[\"traceId=demo-1024，日志出现 Lock wait timeout\"],\"possible_causes\":[\"锁等待可能由幂等表唯一索引冲突或长事务导致，尚需证据验证\"],\"suggestions\":[\"按 traceId=demo-1024 检索回调消费线程与数据库事务日志\",\"命中日志规律：支付回调日志关键字（kb_002）\"],\"related_kb\":[\"支付回调日志关键字（kb_002）\"]}'),(20,5,'sql_analysis','SQL 分析','aux',3,50,'[\"SQL 文本\"]','{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[\"SQL 文本\"],\"sql_correctness\":\"未提供 SQL 文本，暂无法进行语法与正确性检查\",\"performance_risk\":\"缺少执行计划与表结构信息，无法评估索引命中情况\",\"impact_scope\":[\"测试平台 / 质量工具 / 任务编排\",\"上下游状态同步\",\"历史数据回溯范围\"],\"optimization_suggestions\":[\"按 traceId 与时间范围对齐上下游数据后核对一致性\",\"补充执行计划后评估索引与锁行为\"],\"review_conclusion\":\"证据不足，需人工复核后给出最终结论\",\"completeness_score\":50}'),(21,5,'regression_list','回归清单','aux',4,100,'[]','{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[],\"regression_items\":[\"支付成功回调主流程\",\"支付失败回调处理\",\"重复回调幂等验证\",\"订单状态同步查询\",\"支付流水一致性核对\",\"优惠券返还与退款场景\",\"三方回调签名异常（关联用例 TC-PAY-003）\",\"支付成功后订单状态同步（关联用例 TC-PAY-002）\",\"支付回调幂等校验（关联用例 TC-PAY-001）\",\"支付回调重复消费导致重复扣款（kb_001）\"],\"item_count\":10,\"completion_rate\":100,\"sources_summary\":[\"高风险用例 3 条\",\"历史 Bug 1 条\",\"回归规则库 1 条\"]}');
/*!40000 ALTER TABLE `run_task_snapshot` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-14 16:37:16
