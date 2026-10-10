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
-- Table structure for table `task_record`
--

DROP TABLE IF EXISTS `task_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_no` varchar(50) NOT NULL COMMENT '任务编号',
  `title` varchar(200) NOT NULL COMMENT '任务标题',
  `task_type` varchar(50) NOT NULL COMMENT '任务类型',
  `project_name` varchar(100) NOT NULL DEFAULT '',
  `module_name` varchar(100) NOT NULL DEFAULT '',
  `submodule_name` varchar(100) NOT NULL DEFAULT '',
  `risk` varchar(10) NOT NULL DEFAULT 'P1',
  `context` text COMMENT '问题上下文',
  `status` varchar(20) NOT NULL DEFAULT '已完成' COMMENT '状态：待补充/已完成',
  `review_status` varchar(20) NOT NULL DEFAULT '待复核' COMMENT '复核状态：待复核/通过/驳回补充',
  `knowledge_status` varchar(20) NOT NULL DEFAULT '未入库' COMMENT '知识入库状态：未入库/待审核/已入库',
  `completeness` int NOT NULL DEFAULT '0' COMMENT '输入完整度%',
  `output_json` longtext COMMENT '结构化输出 JSON',
  `engine_mode` varchar(20) NOT NULL DEFAULT 'rule' COMMENT '引擎模式 rule/llm',
  `source` varchar(20) NOT NULL DEFAULT 'manual' COMMENT '来源：manual/assistant',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_no` (`task_no`),
  KEY `idx_task_type` (`task_type`),
  KEY `idx_task_review` (`review_status`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分析任务记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `task_record`
--

LOCK TABLES `task_record` WRITE;
/*!40000 ALTER TABLE `task_record` DISABLE KEYS */;
INSERT INTO `task_record` VALUES (1,'task_bug_001','支付成功但订单状态未同步','bug_analysis','电商平台','支付中心','订单同步','P0','支付成功后订单仍为待支付，staging 环境 v2.8.3。复现步骤：完成支付后等待 30 秒查询订单；实际结果仍为待支付，预期订单应为已支付。traceId=demo-1024，日志出现 Lock wait timeout。','已完成','通过','未入库',100,'{\"risk_level\":\"P0\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[\"时间范围\",\"SQL 文本\"],\"phenomenon\":\"支付成功后订单仍为待支付（staging 环境 v2.8.3）\",\"affected_modules\":[\"电商平台 / 支付中心 / 订单同步\"],\"possible_causes\":[\"从现象推测存在状态同步或事务边界异常，尚需证据验证\",\"Lock wait timeout 指向数据库锁等待，可能阻塞订单状态更新事务\"],\"recommended_logs\":[\"按 traceId=demo-1024 检索支付回调与订单同步全链路日志\",\"检查回调消费与订单更新的事务边界日志\"],\"recommended_sql\":[\"查询订单状态流转表在时间范围内的变更记录\",\"核对支付流水与订单状态一致性\"],\"related_history_bugs\":[\"支付回调重复消费导致重复扣款（kb_001）\"],\"regression_scope\":[\"直接变更模块\",\"上下游状态同步\",\"失败与重复请求场景\"]}','rule','assistant','2026-10-02 13:10:11','2026-10-02 13:10:11'),(2,'task_log_001','支付回调消费超时排查','log_triage','电商平台','支付中心','支付回调','P1','支付回调消费出现超时，日志出现 Lock wait timeout，traceId=demo-1024。','已完成','通过','未入库',67,'{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[\"时间范围\"],\"anomaly_summary\":\"已提取异常关键词与时间线，根因仍需结合完整上下文确认\",\"timeline\":[\"接收用户证据\",\"定位首个异常片段（Lock wait timeout）\",\"向上下游扩展调用链\"],\"error_fragments\":[\"日志出现 Lock wait timeout\"],\"possible_causes\":[\"锁等待可能由幂等表唯一索引冲突或长事务导致，尚需证据验证\"],\"suggestions\":[\"按 traceId=demo-1024 检索回调消费线程与数据库事务日志\",\"命中 Lock wait timeout 时优先检查幂等表唯一索引与事务粒度（引用日志规律库 kb_002）\"],\"related_kb\":[\"支付回调日志关键字（kb_002）\"]}','rule','assistant','2026-10-02 13:08:40','2026-10-02 13:08:40'),(3,'task_sql_001','订单与支付流水一致性分析','sql_analysis','电商平台','支付中心','订单同步','P1','支付成功后订单状态未同步，需要给出 SQL 排查方向：核对订单状态与支付流水一致性。','已完成','待复核','未入库',50,'{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[\"SQL 文本\"],\"sql_correctness\":\"未提供 SQL 文本，暂无法进行语法与正确性检查\",\"performance_risk\":\"缺少执行计划与表结构信息，无法评估索引命中情况\",\"impact_scope\":[\"支付中心 / 订单同步\",\"上下游状态同步\",\"历史数据回溯范围\"],\"optimization_suggestions\":[\"补充 SQL 文本与执行计划后评估索引与锁行为\",\"按 traceId 与时间范围对齐订单状态与支付流水\"],\"review_conclusion\":\"证据不足，需人工复核后给出最终结论\",\"completeness_score\":50}','rule','assistant','2026-10-02 13:16:24','2026-10-02 13:16:24'),(4,'task_case_001','优惠券叠加规则用例生成','testcase_gen','电商平台','订单中心','优惠券','P1','需求：优惠券叠加规则。验收标准：可叠加券按规则计算金额，互斥券拦截，取消订单返还优惠券。','已完成','通过','未入库',100,'{\"risk_level\":\"P1\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[],\"test_points\":[\"优惠券叠加规则主流程\",\"叠加互斥与边界\",\"取消订单返还优惠券\",\"退款场景优惠券回收\"],\"cases\":[{\"title\":\"多优惠券叠加下单校验\",\"priority\":\"P1\",\"type\":\"功能\",\"preconditions\":\"账户有多张可用优惠券\",\"steps\":\"1. 选择可叠加优惠券下单\n2. 提交订单\",\"expected\":\"叠加金额按规则计算\"},{\"title\":\"互斥券叠加拦截\",\"priority\":\"P1\",\"type\":\"异常\",\"preconditions\":\"账户有互斥优惠券\",\"steps\":\"1. 同时选择互斥券下单\",\"expected\":\"拦截并提示互斥规则\"},{\"title\":\"取消订单返还优惠券\",\"priority\":\"P1\",\"type\":\"回归\",\"preconditions\":\"下单已使用优惠券\",\"steps\":\"1. 取消订单\n2. 查询优惠券状态\",\"expected\":\"优惠券返还且状态可用\"}],\"coverage\":\"正常 / 异常 / 边界\"}','rule','assistant','2026-10-02 13:12:05','2026-10-02 13:12:05'),(5,'task_005','婕旂ず路鏀粯鍥炶皟澶辫触鎺掓煡','bug_analysis','','','','P0','闂鐜拌薄锛氭敮浠樻垚鍔熷悗璁㈠崟浠嶄负寰呮敮浠橈紝staging v2.8.4\n澶嶇幇姝ラ锛歚n1. 鍙戣捣鏀粯骞跺畬鎴愪粯娆綻n2. 绛夊緟 30 绉掑悗鏌ヨ璁㈠崟璇︽儏\n瀹為檯缁撴灉锛氳鍗曠姸鎬佷粛涓哄緟鏀粯\n棰勬湡缁撴灉锛氳鍗曠姸鎬佸簲涓哄凡鏀粯\ntraceId=demo-2048锛屾棩蹇楀嚭鐜?Lock wait timeout','待补充','通过','待审核',0,'{\"risk_level\":\"P0\",\"human_review_points\":[\"确认事实与引用依据\",\"P0/P1 风险和上线结论必须由测试人最终确认\"],\"need_more_info\":[\"问题现象\",\"复现步骤\",\"预期结果\"],\"phenomenon\":\"闂鐜拌薄锛氭敮浠樻垚鍔熷悗璁㈠崟浠嶄负寰呮敮浠橈紝staging v2.8.4\",\"affected_modules\":[\"未指定模块\"],\"possible_causes\":[\"从现象推测存在状态同步或事务边界异常，尚需证据验证\",\"Lock wait timeout 指向数据库锁等待，可能阻塞核心状态更新事务\"],\"recommended_logs\":[\"按 traceId=demo-2048 检索支付回调与订单同步全链路日志\",\"检查关键事务边界与状态流转日志\"],\"recommended_sql\":[\"查询核心业务状态流转表在时间范围内的变更记录\",\"核对上下游数据一致性\"],\"related_history_bugs\":[\"支付回调重复消费导致重复扣款（kb_001）\"],\"regression_scope\":[\"直接变更模块\",\"上下游状态同步\",\"失败与重复请求场景\"]}','rule','manual','2026-10-06 17:04:12','2026-10-06 17:04:12');
/*!40000 ALTER TABLE `task_record` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-09 14:05:26
