package com.example.springbatchinvestment.repository;

import com.example.springbatchinvestment.domain.entity.SavingOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface SavingOptionRepository extends JpaRepository<SavingOption, Long> {

    @Transactional
    @Modifying
    @Query(value = "truncate tb_saving_option", nativeQuery = true)
    void truncateSavingOption();
}
