package com.testpilot.repository;

import com.testpilot.entity.SysModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysModuleRepository extends JpaRepository<SysModule, Long> {

    List<SysModule> findByEnabledTrue();

    boolean existsByProjectNameAndModuleNameAndSubmoduleName(String project, String module, String submodule);

    List<SysModule> findAllByOrderByProjectNameAscModuleNameAscSubmoduleNameAsc();
}
