package com.example.springbatchinvestment.repository;

import com.example.springbatchinvestment.domain.entity.DepositOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface DepositOptionRepository extends JpaRepository<DepositOption, Long> {

    @Transactional
    @Modifying
    @Query(value = "truncate tb_deposit_option", nativeQuery = true)
    void truncateDepositOption();
}
