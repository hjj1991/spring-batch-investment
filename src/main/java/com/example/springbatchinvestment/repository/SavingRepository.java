package com.example.springbatchinvestment.repository;

import com.example.springbatchinvestment.domain.entity.Saving;
import com.example.springbatchinvestment.domain.entity.SavingPK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavingRepository extends JpaRepository<Saving, SavingPK> {

    @Modifying(flushAutomatically = true)
    @Query("update Saving set enable = :enable")
    void updateAllSavingEnable(@Param("enable") int enable);
}
