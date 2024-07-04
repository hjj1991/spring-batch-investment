package com.example.springbatchinvestment.repository;

import com.example.springbatchinvestment.domain.entity.FinancialProductEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialProductRepository extends JpaRepository<FinancialProductEntity, Long> {

    @EntityGraph(
            attributePaths = {"financialProductOptionEntities"},
            type = EntityGraph.EntityGraphType.LOAD)
    Optional<FinancialProductEntity>
            findByFinancialCompanyEntityFinancialCompanyCodeAndFinancialProductCode(
                    String financialCompanyCode, String financialProductCode);
}
