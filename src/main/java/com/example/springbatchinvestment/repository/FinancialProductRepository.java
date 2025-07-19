package com.example.springbatchinvestment.repository;

import com.example.springbatchinvestment.domain.FinancialProductType;
import com.example.springbatchinvestment.domain.ProductStatus;
import com.example.springbatchinvestment.domain.entity.FinancialProductEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FinancialProductRepository extends JpaRepository<FinancialProductEntity, Long> {

    @EntityGraph(
            attributePaths = {"financialProductOptionEntities"},
            type = EntityGraph.EntityGraphType.LOAD)
    Optional<FinancialProductEntity>
            findByFinancialCompanyEntityFinancialCompanyCodeAndFinancialProductCode(
                    String financialCompanyCode, String financialProductCode);

    @Modifying
    @Query(
            "UPDATE FinancialProductEntity fp SET fp.status = :status WHERE fp.financialProductType = :financialProductType")
    void updateStatusByFinancialProductType(
            FinancialProductType financialProductType, ProductStatus status);
}
