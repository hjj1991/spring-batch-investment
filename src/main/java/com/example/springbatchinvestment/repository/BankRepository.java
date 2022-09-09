package com.example.springbatchinvestment.repository;

import com.example.springbatchinvestment.domain.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BankRepository extends JpaRepository<Bank, String> {

    @Modifying(clearAutomatically = true)   // 벌크 연산 직후 자동으로 영속성 컨택스트를 Clear
    @Query("update Bank set enable = :enable")
    void updateBankAllEnable(@Param("enable") int enable);
}
