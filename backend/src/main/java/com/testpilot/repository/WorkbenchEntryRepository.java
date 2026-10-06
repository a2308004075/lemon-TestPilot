package com.testpilot.repository;

import com.testpilot.entity.WorkbenchEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkbenchEntryRepository extends JpaRepository<WorkbenchEntry, Long> {

    List<WorkbenchEntry> findAllByOrderByIdAsc();
}
