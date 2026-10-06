package com.testpilot.repository;

import com.testpilot.entity.RegressionList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegressionListRepository extends JpaRepository<RegressionList, Long> {

    List<RegressionList> findAllByOrderByIdDesc();
}
