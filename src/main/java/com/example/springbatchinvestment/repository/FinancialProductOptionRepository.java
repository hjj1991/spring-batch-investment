package com.example.springbatchinvestment.repository;

import com.example.springbatchinvestment.domain.entity.FinancialProductOptionEntity;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialProductOptionRepository
        extends JpaRepository<FinancialProductOptionEntity, Long> {
    List<FinancialProductOptionEntity> findAllByFinancialProductEntityFinancialProductIdIn(
            Set<Long> financialProductIds);
}
