package com.testpilot.repository;

import com.testpilot.entity.SysLlmConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SysLlmConfigRepository extends JpaRepository<SysLlmConfig, Long> {

    List<SysLlmConfig> findAllByOrderByIdAsc();

    List<SysLlmConfig> findByEnabledTrue();

    /** 按用途取启用配置：chat / embedding */
    List<SysLlmConfig> findByEnabledTrueAndPurpose(String purpose);

    Optional<SysLlmConfig> findByVendorCode(String vendorCode);
}
