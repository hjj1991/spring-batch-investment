package com.example.springbatchinvestment.repository;

import com.example.springbatchinvestment.domain.entity.FinancialCompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FinancialCompanyRepository extends JpaRepository<FinancialCompanyEntity, Long> {
    Optional<FinancialCompanyEntity> findByFinancialCompanyCode(String financialCompanyCode);
}
