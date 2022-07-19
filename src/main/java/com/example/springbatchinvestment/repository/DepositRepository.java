package com.example.springbatchinvestment.repository;

import com.example.springbatchinvestment.domain.entity.Deposit;
import com.example.springbatchinvestment.domain.entity.DepositPK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepositRepository extends JpaRepository<Deposit, DepositPK> {

    @Modifying(flushAutomatically = true)
    @Query("update Deposit set enable = :enable")
    void updateAllDepositEnable(@Param("enable") int enable);
}
