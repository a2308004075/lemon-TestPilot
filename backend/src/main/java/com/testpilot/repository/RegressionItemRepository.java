package com.testpilot.repository;

import com.testpilot.entity.RegressionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegressionItemRepository extends JpaRepository<RegressionItem, Long> {

    List<RegressionItem> findByListIdOrderBySeqAsc(Long listId);
}
