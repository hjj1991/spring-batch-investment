package com.example.springbatchinvestment.repository;

import com.example.springbatchinvestment.domain.FinancialProductType;
import com.example.springbatchinvestment.domain.ProductStatus;
import com.example.springbatchinvestment.domain.entity.FinancialProductEntity;
import java.time.ZonedDateTime;
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
            findByFinancialCompanyEntityFinancialCompanyCodeAndFinancialProductCodeAndFinancialProductType(
                    String financialCompanyCode,
                    String financialProductCode,
                    FinancialProductType financialProductType);

    @Modifying
    @Query(
            """
             UPDATE FinancialProductEntity fp
                SET fp.status = :status
              WHERE fp.financialProductType = :financialProductType
                AND (fp.lastSeenAt IS NULL OR fp.lastSeenAt < :runStartedAt)
             """)
    void updateStatusForNotSeenProducts(
            FinancialProductType financialProductType,
            ProductStatus status,
            ZonedDateTime runStartedAt);
}
