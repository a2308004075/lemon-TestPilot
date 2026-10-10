package com.testpilot.service;

import com.testpilot.entity.AuditLog;
import com.testpilot.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 操作审计：关键动作统一登记，失败静默（审计不阻断业务主流程）。
 */
@Service
public class AuditService {

    @Autowired
    private AuditLogRepository repo;

    public void record(String entityType, String entityNo, String action, String detail) {
        try {
            if (entityNo == null || entityNo.isEmpty()) {
                return;
            }
            AuditLog log = new AuditLog();
            log.setEntityType(entityType);
            log.setEntityNo(entityNo);
            log.setAction(action);
            log.setDetail(detail == null ? "" : detail);
            repo.save(log);
        } catch (Exception ignore) {
            // 审计失败不影响业务主流程
        }
    }

    public List<AuditLog> list(String entityType, String entityNo) {
        if (entityType == null || entityType.isEmpty()
                || entityNo == null || entityNo.isEmpty()) {
            return Collections.emptyList();
        }
        return repo.findTop50ByEntityTypeAndEntityNoOrderByIdDesc(entityType, entityNo);
    }
}
