package com.testpilot.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_llm_config")
public class SysLlmConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendor_code")
    private String vendorCode;

    @Column(name = "vendor_name")
    private String vendorName;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "temperature")
    private BigDecimal temperature;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds;

    @Column(name = "max_output")
    private Integer maxOutput;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
