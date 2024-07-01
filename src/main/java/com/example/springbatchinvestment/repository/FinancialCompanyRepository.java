package com.example.springbatchinvestment.repository;

import com.example.springbatchinvestment.domain.entity.FinancialCompanyEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialCompanyRepository extends JpaRepository<FinancialCompanyEntity, Long> {
    Optional<FinancialCompanyEntity> findByFinancialCompanyCode(String financialCompanyCode);
}
