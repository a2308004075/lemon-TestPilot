package com.testpilot.repository;

import com.testpilot.entity.ValidationCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ValidationCaseRepository extends JpaRepository<ValidationCase, Long> {

    List<ValidationCase> findAllByOrderByIdAsc();
}
